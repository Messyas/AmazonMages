package com.example.amazonMages.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import com.example.androidstudio2dgamedevelopment.R;

public class SpriteSheet {
    private static final int SPRITE_WIDTH_PIXELS = 64; // Largura de cada sprite em pixels
    private static final int SPRITE_HEIGHT_PIXELS = 64; // Altura de cada sprite em pixels
    private final Bitmap bitmap; // Bitmap que contém a sprite sheet
    private final Bitmap turnaround;
    private final Context context;

    public SpriteSheet(Context context) {
        this.context = context.getApplicationContext();
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inScaled = false; // Desativa a escala automática da imagem
        bitmap = com.example.amazonMages.resources.ResourceManager.shared(context).bitmap(R.drawable.sprites, false); // Carrega a sprite sheet
        Bitmap model = com.example.amazonMages.resources.ResourceManager.shared(context)
            .bitmap(R.drawable.player_turnaround, false);
        turnaround = model != null && model.getWidth() == 8 * SPRITE_WIDTH_PIXELS &&
            model.getHeight() == SPRITE_HEIGHT_PIXELS ? model : null;
    }

    /** Extract any grid-aligned frame without hardcoded pixel rectangles. */
    public Sprite frame(int column, int row) {
        return frame(column, row, SPRITE_WIDTH_PIXELS, SPRITE_HEIGHT_PIXELS);
    }

    public Sprite frame(int column, int row, int width, int height) {
        if (column < 0 || row < 0 || width <= 0 || height <= 0)
            throw new IllegalArgumentException("Invalid sprite grid coordinates");
        int left = column * width;
        int top = row * height;
        if (left + width > bitmap.getWidth() || top + height > bitmap.getHeight())
            throw new IllegalArgumentException("Sprite frame outside sheet");
        return new Sprite(this, new Rect(left, top, left + width, top + height));
    }

    /** Extract a frame from one of the standalone world-tile atlases. */
    public Sprite tileFrame(int resourceId, int column, int cellWidth, int cellHeight) {
        if (column < 0 || cellWidth <= 0 || cellHeight <= 0)
            throw new IllegalArgumentException("Invalid tile frame coordinates");
        Bitmap atlas = com.example.amazonMages.resources.ResourceManager.shared(context)
            .bitmap(resourceId, false);
        if (atlas == null || (column + 1) * cellWidth > atlas.getWidth() ||
                cellHeight > atlas.getHeight())
            throw new IllegalArgumentException("Tile frame outside atlas");
        return new Sprite(atlas, new Rect(column * cellWidth, 0,
            (column + 1) * cellWidth, cellHeight));
    }

    public Sprite[] frames(int startColumn, int row, int count) {
        if (count <= 0) throw new IllegalArgumentException("Empty animation");
        Sprite[] result = new Sprite[count];
        for (int i = 0; i < count; i++) result[i] = frame(startColumn + i, row);
        return result;
    }

    /** Load optional 8-row action atlases, falling back to the approved turnaround poses. */
    public AnimationSet getPlayerAnimations() {
        AnimationSet directional = new AnimationSet();
        for (AnimationAction action : AnimationAction.values()) {
            int columns = columns(action);
            Bitmap atlas = loadPlayerAtlas(action);
            if (!validAtlas(atlas, columns)) continue;
            int ticks = ticksPerFrame(action);
            boolean looping = action == AnimationAction.IDLE || action == AnimationAction.MOVE;
            for (Direction direction : Direction.values()) {
                Sprite[] frames = new Sprite[columns];
                for (int frame = 0; frame < columns; frame++) {
                    int left = frame * SPRITE_WIDTH_PIXELS;
                    int top = direction.ordinal() * SPRITE_HEIGHT_PIXELS;
                    frames[frame] = new Sprite(atlas, new Rect(left, top,
                        left + SPRITE_WIDTH_PIXELS, top + SPRITE_HEIGHT_PIXELS));
                }
                directional.put(action, direction, new AnimationClip(frames, ticks, looping));
            }
        }
        if (turnaround != null) {
            for (Direction direction : Direction.values()) {
                int column = direction.ordinal();
                Sprite frame = new Sprite(turnaround, new Rect(column * SPRITE_WIDTH_PIXELS, 0,
                    (column + 1) * SPRITE_WIDTH_PIXELS, SPRITE_HEIGHT_PIXELS));
                AnimationClip pose = new AnimationClip(new Sprite[]{frame}, 1, true);
                if (loadPlayerAtlas(AnimationAction.IDLE) == null)
                    directional.put(AnimationAction.IDLE, direction, pose);
                if (loadPlayerAtlas(AnimationAction.MOVE) == null)
                    directional.put(AnimationAction.MOVE, direction, pose);
            }
            return directional;
        }
        Sprite[] frames = frames(0, 0, 3);
        return new AnimationSet()
            .putFallback(AnimationAction.IDLE, new AnimationClip(new Sprite[]{frames[0]}, 1, true))
            .putFallback(AnimationAction.MOVE, new AnimationClip(new Sprite[]{frames[1], frames[2]}, 5, true));
    }

    private Bitmap loadPlayerAtlas(AnimationAction action) {
        int id = context.getResources().getIdentifier("player_" + action.name().toLowerCase(java.util.Locale.ROOT),
            "drawable", context.getPackageName());
        return id == 0 ? null : com.example.amazonMages.resources.ResourceManager.shared(context).bitmap(id, false);
    }

    private static boolean validAtlas(Bitmap atlas, int columns) {
        return atlas != null && atlas.getWidth() == columns * SPRITE_WIDTH_PIXELS &&
            atlas.getHeight() == 8 * SPRITE_HEIGHT_PIXELS;
    }

    private static int columns(AnimationAction action) {
        switch (action) {
            case IDLE: return 4;
            case MOVE: return 6;
            case ATTACK: return 6;
            case HURT: return 3;
            case DEATH: return 8;
            case SPECIAL: return 8;
            case PARRY: return 4;
            case DODGE: return 5;
            default: throw new IllegalArgumentException("Unsupported player action: " + action);
        }
    }

    private static int ticksPerFrame(AnimationAction action) {
        switch (action) {
            case IDLE: return 14;
            case MOVE: return 6;
            case ATTACK: return 5;
            case HURT: return 6;
            case DEATH: return 8;
            case SPECIAL: return 6;
            case PARRY: return 4;
            case DODGE: return 4;
            default: throw new IllegalArgumentException("Unsupported player action: " + action);
        }
    }

    public Sprite[] getPlayerSpriteArray() {
        Sprite[] spriteArray = new Sprite[3]; // Cria um array de sprites para o jogador
        spriteArray[0] = frame(0, 0); // Primeiro sprite
        spriteArray[1] = frame(1, 0); // Segundo sprite
        spriteArray[2] = frame(2, 0); // Terceiro sprite
        return spriteArray; // Retorna o array de sprites do jogador
    }

    public Bitmap getBitmap() {
        return bitmap; // Retorna o bitmap da sprite sheet
    }

    public Sprite getWaterSprite() {
        return tileFrame(R.drawable.tile_water, 0, 64, 64); // Retorna o sprite da água
    }

    public Sprite getLavaSprite() {
        return tileFrame(R.drawable.tile_lava, 0, 64, 64); // Retorna o sprite da lava
    }

    public Sprite getGroundSprite() {
        return tileFrame(R.drawable.tile_ground, 0, 64, 64); // Retorna o sprite do chão
    }

    public Sprite getGrassSprite() {
        return tileFrame(R.drawable.tile_grass, 0, 64, 64); // Retorna o sprite da grama
    }

    public Sprite getTreeSprite() {
        return getSpriteByIndex(4); // Retorna o sprite da árvore
    }

     // isso aqui é interessante, tenta entender depois
    private Sprite getSpriteByIndex(int idxCol) {
        // Retorna um sprite baseado no índice da coluna fornecido
        return frame(idxCol, 1);
    }
}
