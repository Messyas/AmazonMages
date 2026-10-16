package com.example.amazonMages.gameobject;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import androidx.core.content.ContextCompat;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.combat.DamageEvent;
import com.example.amazonMages.combat.DamageType;
import com.example.amazonMages.GameLoop;
import com.example.androidstudio2dgamedevelopment.R;

public class Spell extends Circle {
    public static final double SPEED_PIXELS_PER_SECOND = 800.0;
    private static final double MAX_SPEED = SPEED_PIXELS_PER_SECOND / GameLoop.MAX_UPS;
    private static Bitmap spellBitmap;
    private final DamageEvent hit;
    private final boolean coconut;

    public DamageEvent getHit() { return hit; }

    public Spell(Context context, Player spellcaster) {
        this(context, spellcaster, false);
    }

    public Spell(Context context, Player spellcaster, boolean coconut) {
        this(context, spellcaster, coconut, 1.0);
    }

    public Spell(Context context, Player spellcaster, boolean coconut, double multiplier) {
        super(
                ContextCompat.getColor(context, R.color.spell),
                spellcaster.getPositionX(),
                spellcaster.getPositionY(),
                25
        );

        this.coconut = coconut;
        this.hit = coconut ? new DamageEvent(Math.max(0, multiplier), 12, DamageType.PHYSICAL) : new DamageEvent(Math.max(0, multiplier), 10, DamageType.MAGIC);
        // Carrega a imagem do feitiço
        if (spellBitmap == null) {
            spellBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.energyball);
        }

        // Define a direção do feitiço com base na posição do jogador
        double dx = spellcaster.getAttackDirectionX(), dy = spellcaster.getAttackDirectionY();
        if (dx == 0 && dy == 0) { dx = 0; dy = 1; }
        velocityX = dx * MAX_SPEED;
        velocityY = dy * MAX_SPEED;
    }

    /** ECS motion is authoritative; legacy object only mirrors the transform for Canvas/collision. */
    public void syncPosition(double x, double y) {
        positionX = x;
        positionY = y;
    }

    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }

    @Override
    public void update() {
        positionX += velocityX;
        positionY += velocityY;
    }

    @Override
    public void draw(Canvas canvas, GameDisplay gameDisplay) {
        // Converte as coordenadas para o display
        float displayX = (float) gameDisplay.gameToDisplayCoordinatesX(positionX);
        float displayY = (float) gameDisplay.gameToDisplayCoordinatesY(positionY);

        // Desenha o Bitmap do feitiço centralizado
        if (coconut) {
            android.graphics.Paint husk = new android.graphics.Paint(3);
            husk.setColor(0xFF79502D);
            canvas.drawCircle(displayX, displayY, 17, husk);
            husk.setColor(0xFFB88C50);
            canvas.drawCircle(displayX - 4, displayY - 5, 5, husk);
        } else if (spellBitmap != null) {
            canvas.drawBitmap(
                    spellBitmap,
                    displayX - (float) spellBitmap.getWidth() / 2, // Centraliza o bitmap no eixo X
                    displayY - (float) spellBitmap.getHeight() / 2, // Centraliza o bitmap no eixo Y
                    null
            );
        }
    }
}
