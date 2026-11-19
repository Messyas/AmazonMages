package com.example.amazonMages.gameobject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.graphics.Direction;
import com.example.amazonMages.map.WorldDefinition;
import com.example.amazonMages.resources.ResourceManager;
import com.example.androidstudio2dgamedevelopment.R;

/** The guide follows a walking route wholly inside the sanctuary. */
public final class SanctuaryGuide extends GameObject {
    public static final double TALK_DISTANCE = 176;
    private static final int FRAME_WIDTH = 64;
    private static final int FRAME_HEIGHT = 72;
    private static final double WALK_STEP = 1.65;
    private static final double[][] ROUTE = {{133, 26}, {234, 1}, {291, 86}, {187, 164}, {82, 132}};

    private final WorldDefinition world;
    private final Player player;
    private final Bitmap whiteSprites, blueSprites;
    private final Rect frame = new Rect();
    private final RectF destination = new RectF();
    private final Paint pixels = new Paint();
    private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Direction facing = Direction.SOUTH;
    private int nextStop = 1, waitTicks = 95, walkTicks, blockedTicks;
    private boolean blue;

    public SanctuaryGuide(Context context, WorldDefinition world, Player player) {
        super(world.spawn.worldX() + ROUTE[0][0], world.spawn.worldY() + ROUTE[0][1]);
        this.world = world;
        this.player = player;
        ResourceManager resources = ResourceManager.shared(context);
        whiteSprites = resources.bitmap(R.drawable.guide_walk, false);
        blueSprites = resources.bitmap(R.drawable.guide_walk_blue, false);
        pixels.setFilterBitmap(false);
        marker.setTextAlign(Paint.Align.CENTER);
        marker.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        marker.setTextSize(14f);
    }

    public void setBlue(boolean blue) { this.blue = blue; }

    public void facePlayer() {
        facing = Direction.fromVector(player.getPositionX() - positionX,
            player.getPositionY() - positionY, facing);
        walkTicks = 0;
        waitTicks = Math.max(waitTicks, 36);
    }

    private boolean isNearby() {
        return Math.hypot(player.getPositionX() - positionX, player.getPositionY() - positionY)
            <= TALK_DISTANCE;
    }

    public boolean canTalkAt(float x, float y, GameDisplay view, int width, int height) {
        if (!isNearby()) return false;
        double zoom = view.getZoom();
        double px = width * .5 + (view.gameToDisplayCoordinatesX(positionX) - width * .5) * zoom;
        double py = height * .5 + (view.gameToDisplayCoordinatesY(positionY) - height * .5) * zoom;
        return Math.abs(x - px) <= 28 * zoom && y >= py - 58 * zoom && y <= py + 32 * zoom;
    }

    @Override public void update() {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        double dx = world.spawn.worldX() + ROUTE[nextStop][0] - positionX;
        double dy = world.spawn.worldY() + ROUTE[nextStop][1] - positionY;
        double distance = Math.hypot(dx, dy);
        if (distance <= WALK_STEP || blockedTicks >= 30) {
            waitTicks = 65 + nextStop * 13;
            nextStop = (nextStop + 1) % ROUTE.length;
            walkTicks = blockedTicks = 0;
            return;
        }
        WorldDefinition.Position next = world.resolveMovement(positionX, positionY,
            dx / distance * WALK_STEP, dy / distance * WALK_STEP, 14);
        double movedX = next.x - positionX, movedY = next.y - positionY;
        if (!world.isInsideSanctuary(next.x, next.y) || Math.hypot(movedX, movedY) < .1) {
            blockedTicks++;
            walkTicks = 0;
            return;
        }
        blockedTicks = 0;
        facing = Direction.fromVector(movedX, movedY, facing);
        setPosition(next.x, next.y);
        walkTicks = walkTicks % 40 + 1;
    }

    @Override public void draw(Canvas canvas, GameDisplay view) {
        Bitmap atlas = blue ? blueSprites : whiteSprites;
        Rect visible = view.getGameRect();
        if (atlas == null || positionX < visible.left - 38 || positionX > visible.right + 38
                || positionY < visible.top - 32 || positionY > visible.bottom + 58) return;
        int step = walkTicks == 0 ? 0 : (walkTicks / 10) % 4;
        int col = step == 1 ? 1 : step == 3 ? 2 : 0;
        int row = facing.ordinal();
        frame.set(col * FRAME_WIDTH, row * FRAME_HEIGHT,
            (col + 1) * FRAME_WIDTH, (row + 1) * FRAME_HEIGHT);
        float x = Math.round(view.gameToDisplayCoordinatesX(positionX));
        float y = Math.round(view.gameToDisplayCoordinatesY(positionY));
        destination.set(x - FRAME_WIDTH / 2f, y - 40, x + FRAME_WIDTH / 2f, y + 32);
        canvas.drawBitmap(atlas, frame, destination, pixels);
        if (isNearby()) {
            marker.setColor(0xED14271E);
            canvas.drawRoundRect(x - 38, y - 58, x + 38, y - 41, 4, 4, marker);
            marker.setColor(0xFFFFEDAD);
            canvas.drawText("Conversar", x, y - 45, marker);
        }
    }
}
