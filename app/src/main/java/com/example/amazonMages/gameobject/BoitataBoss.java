package com.example.amazonMages.gameobject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.ai.EnemyManager.ParryRiposteListener;
import com.example.amazonMages.ai.EnemyProjectile;
import com.example.amazonMages.ai.EnemyTemplates;
import com.example.amazonMages.combat.DamageEvent;
import com.example.amazonMages.combat.DamageType;
import com.example.amazonMages.ecs.HostileProjectileWorld;
import com.example.amazonMages.map.BoitataArena;
import com.example.amazonMages.map.WorldDefinition;
import com.example.amazonMages.resources.ResourceManager;
import com.example.androidstudio2dgamedevelopment.R;
import java.util.Random;

/** Fixed-tick encounter. Windups establish the same geometry used for damage and ground warnings. */
public final class BoitataBoss extends Enemy {
    public static final int DEFEAT_TICKS = 66;
    private static final int WAKE_TICKS = 126;
    private static final double WAKE_RADIUS = 5 * WorldDefinition.TILE_SIZE;
    private static final double BITE_HALF_ANGLE = .46;
    private static final double GAP_HALF_ANGLE = .57;
    private static final double RING_RADIUS = 120;
    private static final double BLUE_RING_RADIUS = 146;
    private static final double MOVE_SPEED = 5.2;
    private static final float BODY_HALF_SIZE = 132f;
    private static final float RING_HALF_SIZE = 150f;

    private enum Stage { DORMANT, WAKE, ORBIT, WINDUP, STRIKE, RECOVER, SHIFT, DEFEATED }

    private enum Attack {
        BITE(51, 22, 72), FIREBALL(60, 24, 75), ENCIRCLE(81, 24, 102);

        final int windup, strike, recovery;
        Attack(int windup, int strike, int recovery) {
            this.windup = windup;
            this.strike = strike;
            this.recovery = recovery;
        }
    }

    private enum Clip {
        AWAKEN(R.drawable.boitata_awaken), EYES(R.drawable.boitata_eyes),
        ORBIT(R.drawable.boitata_orbit, R.drawable.boitata_orbit_ember),
        BITE(R.drawable.boitata_bite, R.drawable.boitata_bite_ember),
        CAST(R.drawable.boitata_cast, R.drawable.boitata_cast_ember),
        ENCIRCLE(R.drawable.boitata_encircle, R.drawable.boitata_encircle_ember),
        RECOVER(R.drawable.boitata_recover, R.drawable.boitata_recover_ember),
        SHIFT(R.drawable.boitata_phase_shift),
        DEFEAT(R.drawable.boitata_defeat, R.drawable.boitata_defeat_ember);

        final int blueAsset, emberAsset;
        Clip(int asset) { this(asset, asset); }
        Clip(int blueAsset, int emberAsset) {
            this.blueAsset = blueAsset;
            this.emberAsset = emberAsset;
        }
    }

    private final BoitataArena arena;
    private final Player player;
    private final ParryRiposteListener parry;
    private final ResourceManager resources;
    private final HostileProjectileWorld fireballs = new HostileProjectileWorld();
    private final Random random = new Random();
    private final Paint pixels = new Paint();
    private final Paint warning = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect source = new Rect();
    private final RectF target = new RectF();
    private final Bitmap[][] clips = new Bitmap[Clip.values().length][2];

    private Stage stage = Stage.DORMANT;
    private Attack attack, previousAttack;
    private int elapsed, duration, attacksCompleted, sinceEncircle;
    private boolean blue, facingLeft, ringGapLeft, biteResolved;
    private double orbitX, orbitY, orbitAngle, orbitRadius, orbitStep;
    private double originX, originY, aimAngle, biteDistance, ringX, ringY;

    public BoitataBoss(Context context, Player player, BoitataArena arena, ParryRiposteListener parry) {
        super(context, player, EnemyTemplates.BOITATA, arena.x, arena.y);
        this.arena = arena;
        this.player = player;
        this.parry = parry;
        resources = ResourceManager.shared(context);
        pixels.setFilterBitmap(false);
    }

    public boolean showHud() { return stage != Stage.DORMANT && stage != Stage.DEFEATED; }
    @Override public int getPhase() { return blue ? 1 : 0; }
    @Override public boolean finishedDeath() { return stage == Stage.DEFEATED && elapsed >= DEFEAT_TICKS; }
    @Override public EnemyProjectile fireProjectile() { return null; }
    @Override public void nudge(double dx, double dy) {}
    @Override public void applyImpulse(double vx, double vy) {}
    @Override public void stun(int ticks) {}

    @Override public boolean isTargetable() {
        return (stage == Stage.ORBIT || stage == Stage.WINDUP
            || stage == Stage.STRIKE || stage == Stage.RECOVER) && !phasePending();
    }

    @Override protected double minimumHealthAfterHit() { return blue ? 0 : phaseFloor(); }

    @Override public void receiveHit(DamageEvent hit) {
        if (!isTargetable()) return;
        super.receiveHit(hit);
        if (isDead()) {
            fireballs.clear();
            enter(Stage.DEFEATED, DEFEAT_TICKS);
        }
    }

    private int phaseFloor() { return (int) (getMaximumHealth() * .4); }
    private boolean phasePending() { return !blue && getHealth() <= phaseFloor(); }

    public void resetAttempt() {
        if (stage == Stage.DORMANT || stage == Stage.DEFEATED) return;
        fireballs.clear();
        restoreFullHealth();
        setPosition(arena.x, arena.y);
        attack = previousAttack = null;
        attacksCompleted = sinceEncircle = 0;
        blue = false;
        enter(Stage.DORMANT, 0);
    }

    @Override public void update(int elapsedTicks) {
        if (stage == Stage.DEFEATED) {
            elapsed += elapsedTicks;
            return;
        }
        double px = player.getPositionX(), py = player.getPositionY();
        if (!arena.contains(px, py) || player.getHealthPoint() <= 0) {
            resetAttempt();
            return;
        }
        if (stage == Stage.DORMANT) {
            if (arena.distance(px, py) <= WAKE_RADIUS) enter(Stage.WAKE, WAKE_TICKS);
            return;
        }
        for (int tick = 0; tick < elapsedTicks; tick++) {
            step(px, py);
            if (fireballs.update(player)) {
                parry.onParrySuccess(px + player.getAttackDirectionX() * 48,
                    py + player.getAttackDirectionY() * 48);
            }
            if (isDead() || player.getHealthPoint() <= 0) break;
        }
    }

    private void step(double px, double py) {
        elapsed++;
        switch (stage) {
            case WAKE:
                if (elapsed >= duration) beginOrbit(px, py);
                break;
            case ORBIT:
                orbit(px, py);
                if (phasePending()) {
                    attack = null;
                    enter(Stage.RECOVER, 28);
                }
                else if (elapsed >= duration) chooseAttack(px, py);
                break;
            case WINDUP:
                if (attack != Attack.ENCIRCLE && elapsed < duration - 22) aimAt(px, py);
                if (attack == Attack.ENCIRCLE) moveToRing();
                if (elapsed >= duration) releaseAttack();
                break;
            case STRIKE:
                if (attack == Attack.BITE) stepBite(px, py);
                if (attack == Attack.ENCIRCLE && elapsed == 1) pulseRing(px, py);
                if (!isDead() && elapsed >= duration) beginRecovery();
                break;
            case RECOVER:
                if (elapsed >= duration) {
                    if (phasePending()) enter(Stage.SHIFT, 96);
                    else beginOrbit(px, py);
                }
                break;
            case SHIFT:
                if (elapsed >= duration) {
                    blue = true;
                    beginOrbit(px, py);
                }
                break;
            default:
                break;
        }
    }

    private void enter(Stage next, int ticks) {
        stage = next;
        elapsed = 0;
        duration = ticks;
    }

    private void beginOrbit(double px, double py) {
        orbitX = px;
        orbitY = py;
        orbitAngle = Math.atan2(positionY - py, positionX - px);
        orbitRadius = 160 + random.nextDouble() * 96;
        double arc = Math.toRadians(110 + random.nextDouble() * 130);
        int travelTicks = (int) Math.ceil(orbitRadius * arc / MOVE_SPEED);
        int ticks = Math.min(192, Math.max(96 + random.nextInt(45), travelTicks));
        orbitStep = (random.nextBoolean() ? 1 : -1) * arc / ticks;
        enter(Stage.ORBIT, ticks);
    }

    private void orbit(double px, double py) {
        double dx = px - orbitX, dy = py - orbitY;
        double drift = Math.min(1, 1.9 / Math.max(.001, Math.hypot(dx, dy)));
        orbitX += dx * drift;
        orbitY += dy * drift;
        orbitAngle += orbitStep;
        moveToward(orbitX + Math.cos(orbitAngle) * orbitRadius,
            orbitY + Math.sin(orbitAngle) * orbitRadius, MOVE_SPEED);
        if (Math.abs(px - positionX) > 15) facingLeft = px < positionX;
    }

    private void moveToward(double x, double y, double speed) {
        double dx = x - arena.x, dy = y - arena.y;
        double limit = arena.radius - 90;
        double edgeScale = Math.min(1, limit / Math.max(.001, Math.hypot(dx, dy)));
        dx = arena.x + dx * edgeScale - positionX;
        dy = arena.y + dy * edgeScale - positionY;
        double step = Math.min(1, speed / Math.max(.001, Math.hypot(dx, dy)));
        setPosition(positionX + dx * step, positionY + dy * step);
    }

    private void chooseAttack(double px, double py) {
        double distance = Math.hypot(px - positionX, py - positionY);
        boolean bite = previousAttack != Attack.BITE && distance < biteReach() + 32;
        boolean cast = previousAttack != Attack.FIREBALL;
        boolean ring = previousAttack != Attack.ENCIRCLE && sinceEncircle >= 2
            && distance < 295 && arena.distance(px, py) < arena.radius - pulseRadius() - 24;
        if (ring && ((!bite && !cast) || random.nextDouble() < .48)) attack = Attack.ENCIRCLE;
        else if (bite && (!cast || random.nextDouble() < .76)) attack = Attack.BITE;
        else if (cast) attack = Attack.FIREBALL;
        else {
            beginOrbit(px, py);
            return;
        }
        originX = positionX;
        originY = positionY;
        aimAt(px, py);
        if (attack == Attack.ENCIRCLE) {
            ringX = px;
            ringY = py;
            ringGapLeft = ringX > arena.x;
        }
        enter(Stage.WINDUP, attack.windup);
    }

    private void aimAt(double px, double py) {
        double dx = px - originX, dy = py - originY;
        aimAngle = Math.atan2(dy, dx);
        facingLeft = dx < 0;
        biteDistance = biteReach();
    }

    private double biteReach() { return blue ? 208 : 174; }
    private double pulseRadius() { return blue ? BLUE_RING_RADIUS : RING_RADIUS; }
    private double gapAngle() { return ringGapLeft ? Math.PI : 0; }

    private void moveToRing() {
        double side = ringGapLeft ? -1 : 1;
        moveToward(ringX + side * 95, ringY - 55, MOVE_SPEED);
    }

    private void releaseAttack() {
        biteResolved = false;
        enter(Stage.STRIKE, attack.strike);
        if (attack != Attack.FIREBALL) return;
        int count = blue ? 5 : 3;
        double spread = blue ? .27 : .24;
        double startX = positionX + Math.cos(aimAngle) * 71;
        double startY = positionY + Math.sin(aimAngle) * 71;
        Bitmap sheet = resources.bitmap(blue ? R.drawable.boitata_fireball : R.drawable.boitata_fireball_ember, false);
        for (int i = 0; i < count; i++) {
            double angle = aimAngle + (i - (count - 1) / 2.0) * spread;
            fireballs.spawn(new Fireball(startX, startY, angle, blue, sheet));
        }
    }

    private void stepBite(double px, double py) {
        double lunge = Math.sin(Math.PI * Math.min(1, elapsed / 18.0)) * 59;
        moveToward(originX + Math.cos(aimAngle) * lunge, originY + Math.sin(aimAngle) * lunge, MOVE_SPEED * 1.45);
        double dx = px - originX, dy = py - originY;
        if (!biteResolved && elapsed >= 4 && elapsed <= 14 && Math.hypot(dx, dy) <= biteDistance
                && angleDifference(Math.atan2(dy, dx), aimAngle) < BITE_HALF_ANGLE) {
            biteResolved = true;
            hitPlayer(originX, originY, blue ? 3 : 2, DamageType.PHYSICAL, true);
        }
    }

    private void pulseRing(double px, double py) {
        double dx = px - ringX, dy = py - ringY;
        double distance = Math.hypot(dx, dy);
        if (distance >= pulseRadius()) return;
        if (distance >= 32 && angleDifference(Math.atan2(dy, dx), gapAngle()) < GAP_HALF_ANGLE) return;
        hitPlayer(ringX, ringY, blue ? 3 : 2, DamageType.MAGIC, false);
    }

    private static double angleDifference(double a, double b) {
        return Math.abs(Math.IEEEremainder(a - b, Math.PI * 2));
    }

    private void hitPlayer(double x, double y, int amount, DamageType type, boolean canParry) {
        if (player.isDashing()) return;
        if (canParry && player.tryParry(x, y)) {
            parry.onParrySuccess(x, y);
        } else if (player.canTakeContactDamage()) {
            player.receiveHit(new DamageEvent(amount, amount * 14, type));
            player.grantContactInvulnerability(42);
        }
    }

    private void beginRecovery() {
        previousAttack = attack;
        attacksCompleted++;
        sinceEncircle = attack == Attack.ENCIRCLE ? 0 : sinceEncircle + 1;
        enter(Stage.RECOVER, attacksCompleted % 2 == 0 ? 108 : attack.recovery);
    }

    public int projectileCount() { return fireballs.count(); }
    public void drawFireballs(Canvas canvas, GameDisplay display) { fireballs.draw(canvas, display); }

    public double hitQueryRadius() { return hasFormedRing() ? RING_HALF_SIZE * 2 : BODY_HALF_SIZE + 8; }

    public boolean overlapsHit(double x, double y, double hitRadius) {
        if (hasFormedRing()) {
            double dx = x - ringX, dy = y - ringY;
            double dist = Math.hypot(dx, dy);
            return Math.abs(dist - 111) < 26 + hitRadius
                && angleDifference(Math.atan2(dy, dx), gapAngle()) > .43;
        }
        double dx = x - positionX, dy = y - positionY;
        if (Math.hypot(dx, dy) < getRadius() + hitRadius) return true;
        double facing = facingLeft ? -1 : 1;
        return Math.hypot(dx - facing * 91, dy - 12) < 34 + hitRadius
            || Math.hypot(dx + facing * 78, dy + 20) < 24 + hitRadius;
    }

    private boolean hasFormedRing() {
        return attack == Attack.ENCIRCLE
            && (stage == Stage.STRIKE || (stage == Stage.WINDUP && elapsed >= 56));
    }

    public void drawWarning(Canvas canvas, GameDisplay display) {
        if ((stage != Stage.WINDUP && stage != Stage.STRIKE) || attack == null) return;
        boolean release = stage == Stage.STRIKE;
        int rgb = blue ? 0x58D8FF : 0xFF9A49;
        int edge = (release ? 0xEE000000 : 0xB4000000) | rgb;
        int fill = (release ? 0x7A000000 : 0x37000000) | rgb;
        if (attack == Attack.ENCIRCLE) {
            float x = screenX(display, ringX), y = screenY(display, ringY), r = (float) pulseRadius();
            float gap = (float) Math.toDegrees(GAP_HALF_ANGLE);
            float start = (float) Math.toDegrees(gapAngle()) + gap;
            warningSector(canvas, x, y, r, start, 360 - 2 * gap, fill, edge);
            warning.setStyle(Paint.Style.FILL);
            warning.setColor(fill);
            canvas.drawCircle(x, y, 32, warning);
            if (blue) {
                target.inset(14, 14);
                warning.setStyle(Paint.Style.STROKE);
                warning.setStrokeWidth(2);
                warning.setColor(edge);
                canvas.drawArc(target, start, 360 - 2 * gap, false, warning);
            }
        } else if (attack == Attack.BITE) {
            float half = (float) Math.toDegrees(BITE_HALF_ANGLE);
            warningSector(canvas, screenX(display, originX), screenY(display, originY), (float) biteDistance,
                (float) Math.toDegrees(aimAngle) - half, 2 * half, fill, edge);
        } else if (!release) {
            warning.setStyle(Paint.Style.STROKE);
            warning.setStrokeWidth(2);
            warning.setColor(edge);
            float x = screenX(display, originX), y = screenY(display, originY);
            float dx = (float) Math.cos(aimAngle), dy = (float) Math.sin(aimAngle);
            canvas.drawLine(x + dx * 66, y + dy * 66, x + dx * 164, y + dy * 164, warning);
        }
    }

    private void warningSector(Canvas canvas, float x, float y, float r, float start, float sweep, int fill, int edge) {
        target.set(x - r, y - r, x + r, y + r);
        warning.setStyle(Paint.Style.FILL);
        warning.setColor(fill);
        canvas.drawArc(target, start, sweep, true, warning);
        warning.setStyle(Paint.Style.STROKE);
        warning.setStrokeWidth(3);
        warning.setColor(edge);
        canvas.drawArc(target, start, sweep, true, warning);
    }

    @Override public void draw(Canvas canvas, GameDisplay display) {
        double x = positionX, y = positionY;
        float half = BODY_HALF_SIZE;
        boolean flip = facingLeft;
        Clip clip;
        int frame;
        switch (stage) {
            case DORMANT: clip = Clip.AWAKEN; frame = 0; flip = false; break;
            case WAKE:
                clip = elapsed < 66 ? Clip.AWAKEN : Clip.EYES;
                frame = elapsed < 66 ? Math.min(2, elapsed / 22) : Math.min(2, (elapsed - 66) / 20);
                flip = false;
                break;
            case ORBIT: clip = Clip.ORBIT; frame = (elapsed / 14) % 3; break;
            case WINDUP:
            case STRIKE:
                if (attack != Attack.ENCIRCLE) {
                    clip = attack == Attack.BITE ? Clip.BITE : Clip.CAST;
                    frame = stage == Stage.WINDUP ? 0 : elapsed < 16 ? 1 : 2;
                    break;
                }
                clip = Clip.ENCIRCLE;
                frame = stage == Stage.STRIKE ? 2 : Math.min(2, elapsed / 28);
                double settle = stage == Stage.STRIKE ? 1 : Math.min(1, elapsed / 42.0);
                x = originX + (ringX - originX) * settle;
                y = originY + (ringY - originY) * settle;
                half = RING_HALF_SIZE;
                flip = ringGapLeft;
                break;
            case RECOVER:
                if (attack == Attack.ENCIRCLE && elapsed < 26) {
                    clip = Clip.ENCIRCLE;
                    frame = elapsed < 12 ? 1 : 0;
                    double opening = elapsed / 26.0;
                    x = ringX + (positionX - ringX) * opening;
                    y = ringY + (positionY - ringY) * opening;
                    half = (float) (RING_HALF_SIZE + (BODY_HALF_SIZE - RING_HALF_SIZE) * opening);
                    flip = ringGapLeft;
                } else {
                    clip = Clip.RECOVER;
                    frame = elapsed < 14 ? 0 : elapsed < duration - 14 ? 1 : 2;
                }
                break;
            case SHIFT: clip = Clip.SHIFT; frame = Math.min(2, elapsed / 32); flip = false; break;
            default: clip = Clip.DEFEAT; frame = Math.min(2, elapsed / 19); break;
        }
        Rect view = display.getGameRect();
        if (x + half < view.left || x - half > view.right || y + half < view.top || y - half > view.bottom) return;
        Bitmap atlas = sheet(clip);
        if (atlas == null) return;
        float sx = snap(screenX(display, x)), sy = snap(screenY(display, y));
        int size = atlas.getHeight();
        source.set(size * frame, 0, size * (frame + 1), size);
        target.set(sx - half, sy - half, sx + half, sy + half);
        if (flip) {
            canvas.save();
            canvas.scale(-1f, 1f, sx, sy);
        }
        canvas.drawBitmap(atlas, source, target, pixels);
        if (flip) canvas.restore();
    }

    private Bitmap sheet(Clip clip) {
        int phase = blue && clip.blueAsset != clip.emberAsset ? 1 : 0;
        Bitmap atlas = clips[clip.ordinal()][phase];
        if (atlas == null || atlas.isRecycled()) {
            atlas = resources.bitmap(phase == 0 ? clip.emberAsset : clip.blueAsset, false);
            clips[clip.ordinal()][phase] = atlas;
        }
        return atlas;
    }

    private static float screenX(GameDisplay view, double x) { return (float) view.gameToDisplayCoordinatesX(x); }
    private static float screenY(GameDisplay view, double y) { return (float) view.gameToDisplayCoordinatesY(y); }
    private static float snap(float p) { return Math.round(p * .5f) * 2f; }

    private final class Fireball extends EnemyProjectile {
        private final Bitmap atlas;
        private final float angleDegrees;
        private int age;

        Fireball(double x, double y, double angle, boolean hotBlue, Bitmap atlas) {
            super(x, y, Math.cos(angle), Math.sin(angle), hotBlue ? 289 : 260, hotBlue ? 2 : 1);
            this.atlas = atlas;
            angleDegrees = (float) Math.toDegrees(angle);
        }

        @Override public DamageType getDamageType() { return DamageType.MAGIC; }
        @Override public boolean canTravel(double x, double y) { return arena.contains(x, y); }
        @Override public void syncPosition(double x, double y) { super.syncPosition(x, y); age++; }

        @Override public void draw(Canvas canvas, GameDisplay display) {
            if (atlas == null) return;
            int frame = (age / 5) % 3;
            source.set(frame * 48, 0, (frame + 1) * 48, 48);
            float x = snap(screenX(display, positionX)), y = snap(screenY(display, positionY));
            target.set(x - 33, y - 23, x + 13, y + 23);
            canvas.save();
            canvas.rotate(angleDegrees, x, y);
            canvas.drawBitmap(atlas, source, target, pixels);
            canvas.restore();
        }
    }
}
