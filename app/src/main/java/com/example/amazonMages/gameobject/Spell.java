package com.example.amazonMages.gameobject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.core.content.ContextCompat;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.GameLoop;
import com.example.amazonMages.combat.DamageEvent;
import com.example.amazonMages.combat.DamageType;
import com.example.amazonMages.graphics.Direction;
import com.example.androidstudio2dgamedevelopment.R;

public class Spell extends Circle {
    public static final double SPEED_PIXELS_PER_SECOND = 800.0;
    private static final double MAX_SPEED = SPEED_PIXELS_PER_SECOND / GameLoop.MAX_UPS;
    private static final int LEAF_CELL = 48;
    private static final int COCONUT_CELL = 32;
    private static final Rect SOURCE = new Rect();
    private static final RectF DESTINATION = new RectF();
    private static final Paint PIXEL_PAINT = new Paint();
    static {
        PIXEL_PAINT.setAntiAlias(false);
        PIXEL_PAINT.setFilterBitmap(false);
        PIXEL_PAINT.setDither(false);
    }

    private final Bitmap spellBitmap;
    private final Bitmap castBitmap;
    private final Bitmap impactBitmap;
    private final Bitmap coconutBitmap;
    private final Bitmap coconutCastBitmap;
    private final Bitmap coconutImpactBitmap;
    private final DamageEvent hit;
    private final boolean coconut;
    private final int directionRow;
    private final double castX, castY;
    private int ageTicks;
    private int impactTicks;

    public DamageEvent getHit() { return hit; }

    public Spell(Context context, Player spellcaster) { this(context, spellcaster, false); }
    public Spell(Context context, Player spellcaster, boolean coconut) {
        this(context, spellcaster, coconut, 1.0);
    }
    public Spell(Context context, Player spellcaster, boolean coconut, double multiplier) {
        super(ContextCompat.getColor(context, R.color.spell), spellcaster.getPositionX(),
            spellcaster.getPositionY(), 25);
        this.coconut = coconut;
        hit = coconut
            ? new DamageEvent(Math.max(0, multiplier), 12, DamageType.PHYSICAL)
            : new DamageEvent(Math.max(0, multiplier), 10, DamageType.MAGIC);

        double dx = spellcaster.getAttackDirectionX(), dy = spellcaster.getAttackDirectionY();
        if (dx == 0 && dy == 0) { dx = 0; dy = 1; }
        Direction direction = Direction.fromVector(dx, dy, Direction.SOUTH);
        directionRow = direction.ordinal();
        castX = positionX + dx * 16;
        castY = positionY + dy * 16;
        velocityX = dx * MAX_SPEED;
        velocityY = dy * MAX_SPEED;

        com.example.amazonMages.resources.ResourceManager resources =
            com.example.amazonMages.resources.ResourceManager.shared(context);
        spellBitmap = coconut ? null : resources.bitmap(R.drawable.leaf_projectile, false);
        castBitmap = coconut ? null : resources.bitmap(R.drawable.leaf_cast_flash, false);
        impactBitmap = coconut ? null : resources.bitmap(R.drawable.leaf_impact, false);
        coconutBitmap = coconut ? resources.bitmap(R.drawable.coconut_projectile, false) : null;
        coconutCastBitmap = coconut ? resources.bitmap(R.drawable.coconut_summon, false) : null;
        coconutImpactBitmap = coconut ? resources.bitmap(R.drawable.coconut_impact, false) : null;
    }

    /** ECS motion is authoritative; the legacy object mirrors its transform for Canvas/collision. */
    public void syncPosition(double x, double y) {
        positionX = x;
        positionY = y;
        ageTicks++;
    }

    public boolean beginImpact() {
        if (impactTicks > 0) return false;
        impactTicks = 1;
        return true;
    }

    public boolean updateImpact() {
        if (impactTicks <= 0) return false;
        impactTicks++;
        return impactTicks <= 24;
    }

    public void drawImpact(Canvas canvas, GameDisplay gameDisplay) {
        if (impactTicks <= 0) return;
        if (coconut && coconutImpactBitmap != null) {
            drawAtlasFrame(canvas, gameDisplay, coconutImpactBitmap,
                Math.min(5, (impactTicks - 1) / 4), 0, 48, positionX, positionY);
        } else if (impactBitmap != null) {
            drawAtlasFrame(canvas, gameDisplay, impactBitmap,
                Math.min(5, (impactTicks - 1) / 4), 0, 64, positionX, positionY);
        }
    }

    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }

    @Override public void update() {
        positionX += velocityX;
        positionY += velocityY;
        ageTicks++;
    }

    @Override public void draw(Canvas canvas, GameDisplay gameDisplay) {
        float displayX = (float) gameDisplay.gameToDisplayCoordinatesX(positionX);
        float displayY = (float) gameDisplay.gameToDisplayCoordinatesY(positionY);
        if (impactTicks > 0) return;
        if (coconut && coconutBitmap != null) {
            int frame = (ageTicks / 3) % 4;
            if (ageTicks < 15 && coconutCastBitmap != null) {
                drawAtlasFrame(canvas, gameDisplay, coconutCastBitmap, Math.min(4, ageTicks / 3),
                    0, COCONUT_CELL, castX, castY);
            }
            drawAtlasFrame(canvas, gameDisplay, coconutBitmap, frame, 0,
                COCONUT_CELL, positionX, positionY);
        } else if (spellBitmap != null) {
            int frame = (ageTicks / 4) % 4;
            drawAtlasFrame(canvas, gameDisplay, spellBitmap, frame, directionRow,
                LEAF_CELL, positionX, positionY);
            if (ageTicks < 20 && castBitmap != null) {
                drawAtlasFrame(canvas, gameDisplay, castBitmap, (ageTicks / 4) % 5,
                    0, 48, castX, castY);
            }
        }
    }

    private static void drawAtlasFrame(Canvas canvas, GameDisplay display, Bitmap bitmap,
            int column, int row, int cellSize, double gameX, double gameY) {
        float x = (float) display.gameToDisplayCoordinatesX(gameX);
        float y = (float) display.gameToDisplayCoordinatesY(gameY);
        SOURCE.set(column * cellSize, row * cellSize,
            (column + 1) * cellSize, (row + 1) * cellSize);
        float half = cellSize / 2f;
        DESTINATION.set(x-half, y-half, x+half, y+half);
        canvas.drawBitmap(bitmap, SOURCE, DESTINATION, PIXEL_PAINT);
    }
}
