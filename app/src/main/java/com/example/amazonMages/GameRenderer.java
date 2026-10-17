package com.example.amazonMages;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.example.amazonMages.ai.EnemyManager;
import com.example.amazonMages.ecs.*;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.map.Tilemap;

/** Owns world rendering and its reusable drawing state; never mutates simulation data. */
final class GameRenderer {
    private final java.util.List<Entity> visiblePickups = new java.util.ArrayList<>();
    private final Tilemap tiles;
    private final Player player;
    private final EnemyManager enemies;
    private final ProjectileWorld projectiles;
    private final World pickups;
    private final GameDisplay display;
    private final Paint pickupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    GameRenderer(Tilemap tiles, Player player, EnemyManager enemies,
            ProjectileWorld projectiles, World pickups, GameDisplay display) {
        this.tiles = tiles;
        this.player = player;
        this.enemies = enemies;
        this.projectiles = projectiles;
        this.pickups = pickups;
        this.display = display;
        pickupPaint.setColor(0xFF8CD87B);
    }

    void draw(Canvas canvas) {
        tiles.draw(canvas, display);
        tiles.drawTrees(canvas, display, player.getPositionY(), true);
        player.draw(canvas, display);
        tiles.drawTrees(canvas, display, player.getPositionY(), false);
        enemies.draw(canvas, display);
        projectiles.draw(canvas, display);
        pickups.queryInto(visiblePickups,PickupComponent.class,TransformComponent.class);
        for (Entity entity : visiblePickups) {
            TransformComponent position = entity.get(TransformComponent.class);
            canvas.drawCircle((float) display.gameToDisplayCoordinatesX(position.x),
                (float) display.gameToDisplayCoordinatesY(position.y), 13, pickupPaint);
        }
    }
}
