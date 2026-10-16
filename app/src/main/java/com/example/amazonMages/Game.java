package com.example.amazonMages;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.example.amazonMages.combat.WeaponLoadout;
import com.example.amazonMages.combat.WeaponDefinition;
import com.example.amazonMages.combat.WeaponType;
import com.example.amazonMages.combat.DamageEvent;
import com.example.amazonMages.dev.DevConsole;
import com.example.amazonMages.map.MapLayout;
import com.example.amazonMages.audio.GameAudioManager;
import java.util.Random;
import java.util.Arrays;
import com.example.amazonMages.dialogue.DialogueLine;
import com.example.amazonMages.dialogue.DialogueSequence;
import com.example.amazonMages.dialogue.DialogueManager;
import com.example.amazonMages.dialogue.DialogueRenderer;
import com.example.amazonMages.quest.QuestDefinition;
import com.example.amazonMages.quest.QuestJournal;
import com.example.amazonMages.quest.QuestState;
import com.example.amazonMages.progression.PlayerProgression;
import com.example.amazonMages.progression.AttributesPanel;
import com.example.amazonMages.combat.DamageType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.amazonMages.gameobject.Circle;
import com.example.amazonMages.ai.EnemyManager;
import com.example.amazonMages.ai.EnemyTemplate;
import com.example.amazonMages.ai.EnemyTemplates;
import com.example.amazonMages.gameobject.Enemy;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gameobject.Spell;
import com.example.amazonMages.gamepanel.GameOver;
import com.example.amazonMages.gamepanel.Joystick;
import com.example.amazonMages.gamepanel.Performance;
import com.example.amazonMages.graphics.SpriteAnimator;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.amazonMages.map.Tilemap;
import com.example.amazonMages.save.GameSaveRepository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A classe Game gerencia todos os objetos do jogo e é responsável por atualizar todos os estados
 * e renderizar todos os objetos na tela.
 */
class Game extends SurfaceView implements SurfaceHolder.Callback {

    private GameAudioManager audio;
    public void setAudioManager(GameAudioManager manager) { this.audio = manager; }
    private void sound(String event) { if (audio != null) audio.play(event); }

    /** Commands run only while the game is paused in a debuggable build. */
    public String runDevCommand(String input) {
        if ((getContext().getApplicationInfo().flags &
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0)
            return "Console indisponível em builds de produção.";
        return new DevConsole(new DevConsole.Commands() {
            @Override public String position() {
                return "x=" + (int) player.getPositionX() + " y=" + (int) player.getPositionY();
            }
            @Override public String teleport(double x, double y) {
                double maxX = MapLayout.NUMBER_OF_COLUMN_TILES * MapLayout.TILE_WIDTH_PIXELS;
                double maxY = MapLayout.NUMBER_OF_ROW_TILES * MapLayout.TILE_HEIGHT_PIXELS;
                if (!Double.isFinite(x) || !Double.isFinite(y) ||
                    x < 0 || y < 0 || x >= maxX || y >= maxY) return "Coordenadas fora do mapa.";
                player.restorePosition(x, y);
                gameDisplay.update();
                return position();
            }
            @Override public String god(boolean enabled) {
                player.setInvulnerable(enabled);
                return "Imortalidade " + (enabled ? "ativada" : "desativada");
            }
            @Override public String heal() {
                player.setHealthPoint(player.getMaximumHealth());
                progression.mana = progression.maxMana;
                return "Vida e mana restauradas.";
            }
            @Override public String give(String item) {
                if ("coco".equals(item)) {
                    weapons.equip(0, WeaponLoadout.COCONUT);
                    return "Domínio dos Cocos equipado no slot 1.";
                }
                if ("cajado".equals(item)) {
                    weapons.equip(1, new WeaponDefinition("Cajado de Teste",
                        WeaponType.INTRINSIC, new DamageEvent(1, 10, DamageType.MAGIC)));
                    return "Cajado de Teste equipado no slot 2. Use TROCA.";
                }
                return "Item desconhecido: coco, cajado. Inventário ainda não existe.";
            }
            @Override public String spawn(String species, int count) {
                EnemyTemplate template;
                if ("fantasma".equals(species)) template = EnemyTemplates.ACID_GHOST;
                else if ("sentinela".equals(species)) template = EnemyTemplates.SWAMP_SENTINEL;
                else if ("espectro".equals(species)) template = EnemyTemplates.ANCIENT_WRAITH;
                else return "Espécie desconhecida: fantasma, sentinela, espectro.";
                int spawned = 0;
                for (int i = 0; i < count; i++) {
                    double x = player.getPositionX() + 150 + (i % 4) * 75;
                    double y = player.getPositionY() + 100 + (i / 4) * 75;
                    boolean ok = template.isBossEncounter() ?
                        enemyManager.spawnEncounter(template, x, y) != null :
                        enemyManager.spawnNormalForDebug(x, y);
                    if (ok) spawned++;
                }
                return "Invocados: " + spawned + "/" + count;
            }
            @Override public String clearEnemies() {
                enemyManager.clearForDebug();
                return "Inimigos e projéteis removidos sem recompensas.";
            }
            @Override public String level(int amount) {
                int gained = 0;
                while (gained < amount && progression.level < 100) {
                    progression.gainExperience(PlayerProgression.experienceToNextLevel(progression.level));
                    gained++;
                }
                return "Nível " + progression.level + " | pontos " + progression.attributePoints;
            }
        }).execute(input);
    }

    private static final String INTRO_QUEST = "first_ghosts";
    private final DialogueManager dialogue = new DialogueManager();
    private final DialogueRenderer dialogueRenderer = new DialogueRenderer();
    private final QuestJournal questJournal = new QuestJournal();
    private final PlayerProgression progression = new PlayerProgression();
    private final AttributesPanel attributesPanel = new AttributesPanel();
    private String lastRewardNotice;
    private final Tilemap tilemap;
    private final GameSaveRepository saves;
    private volatile boolean saveLoaded = false;
    private int updatesSinceSave = 0;
    private int joystickPointerId = 0;
    private final Joystick joystick;
    private final Player player;
    private GameLoop gameLoop;
    private volatile boolean activityResumed = false;
    private final EnemyManager enemyManager;
    private final List<Spell> spellList = new ArrayList<>();
    private int numberOfSpellsToCast = 0;
    private final WeaponLoadout weapons = new WeaponLoadout();
    private final Random coconutRandom = new Random();
    private int coconutStormTicks;
    private double stormX, stormY;
    private final Paint hudPaint = new Paint(3);
    private int attackPointerId = -1;
    private final GameOver gameOver;
    private final Performance performance;
    private final GameDisplay gameDisplay;

    /**
     * Construtor da classe Game.
     *
     * @param context O contexto da aplicação, que é passado para a Superclasse SurfaceView.
     */
    public Game(Context context, int slotId) {
        super(context);

        // Get surface holder and add callback
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        gameLoop = new GameLoop(this, surfaceHolder);

        // Initialize game panels
        performance = new Performance(context, gameLoop);
        gameOver = new GameOver(context);
        joystick = new Joystick(275, 700, 70, 40);

        // Initialize game objects
        SpriteSheet spriteSheet = new SpriteSheet(context);
        SpriteAnimator animator = new SpriteAnimator(spriteSheet.getPlayerAnimations());
        player = new Player(context, joystick, 2*500, 500, 32, animator);
        saves = new GameSaveRepository(context, slotId);


        // Initialize display and center it around the player
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        gameDisplay = new GameDisplay(displayMetrics.widthPixels, displayMetrics.heightPixels, player);

        // Initialize Tilemap
        tilemap = new Tilemap(spriteSheet);
        enemyManager = new EnemyManager(context, player, tilemap);
        questJournal.register(new QuestDefinition(INTRO_QUEST, "Ecos do Pantano",
            "enemy.acid_ghost.defeated", 3, "unlock.arcane_lore_intro"));
        enemyManager.setKillListener(event -> {
            questJournal.onEvent(event);
            sound("ghost_defeat");
            int reward = 0;
            if (EnemyTemplates.ACID_GHOST.defeatEvent.equals(event)) reward = EnemyTemplates.ACID_GHOST.experienceReward;
            else if (EnemyTemplates.SWAMP_SENTINEL.defeatEvent.equals(event)) reward = EnemyTemplates.SWAMP_SENTINEL.experienceReward;
            else if (EnemyTemplates.ANCIENT_WRAITH.defeatEvent.equals(event)) reward = EnemyTemplates.ANCIENT_WRAITH.experienceReward;
            if (reward > 0) progression.gainExperience(reward);
            saveQuestProgress();
            if (saveLoaded) saves.saveStats(progression.snapshot(saves.getSlotId()));
        });
        dialogue.setEventListener(this::handleDialogueEvent);


        setFocusable(true);
        saves.loadPosition(position -> {
            if (position != null && Double.isFinite(position.x) && Double.isFinite(position.y)) {
                player.restorePosition(position.x, position.y);
                gameDisplay.update();
            }
            saves.loadQuests(rows -> {
                questJournal.restore(rows);
                saves.loadStats(stats -> {
                    progression.restore(stats);
                    applyProgressionEffects();
                    if (position != null) player.setHealthPoint(position.health);
                    saveLoaded = true;
                    startIfReady();
                });
            });
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (attributesPanel.isOpen()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (attributesPanel.touch(event.getX(), event.getY(), getWidth(), getHeight(), progression)) {
                    applyProgressionEffects();
                    if (saveLoaded) saves.saveStats(progression.snapshot(saves.getSlotId()));
                }
            }
            return true;
        }
        if (!dialogue.isActive() && event.getActionMasked() == MotionEvent.ACTION_DOWN &&
            event.getX() < getWidth() * .38f && event.getY() < getHeight() * .18f) {
            attributesPanel.toggle();
            joystick.setIsPressed(false);
            joystick.resetActuator();
            attackPointerId = -1;
            numberOfSpellsToCast = 0;
            return true;
        }
        if (dialogue.isActive()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) dialogue.advance();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN &&
            event.getX() > getWidth() * 0.38f && event.getX() < getWidth() * 0.70f &&
            event.getY() < getHeight() * 0.22f) {
            startElderDialogue();
            sound("dialogue");
            return true;
        }
        // Lida com ações de eventos de toque do usuário
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                int downIndex = event.getActionIndex();
                float touchX = event.getX(downIndex), touchY = event.getY(downIndex);
                if (touchX > getWidth() * 0.72f && touchY > getHeight() * 0.55f) {
                    if (touchY < getHeight() * 0.75f) {
                        weapons.next();
                    } else if (touchX < getWidth() * 0.85f) {
                        activateCoconutStorm();
                    } else {
                        attackPointerId = event.getPointerId(downIndex);
                        numberOfSpellsToCast++;
                    }
                } else if (joystick.getIsPressed()) {
                    // O joystick estava pressionado antes deste evento -> lançar feitiço
                    numberOfSpellsToCast++;
                } else if (joystick.isPressed(touchX, touchY)) {
                    // O joystick está pressionado neste evento -> setIsPressed(true) e armazena o id do ponteiro
                    joystickPointerId = event.getPointerId(event.getActionIndex());
                    joystick.setIsPressed(true);
                } else {
                    // O joystick não estava pressionado anteriormente e não está pressionado neste evento -> lançar feitiço
                    numberOfSpellsToCast++;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (attackPointerId >= 0) {
                    int attackIndex = event.findPointerIndex(attackPointerId);
                    if (attackIndex >= 0) player.aim(event.getX(attackIndex) - getWidth() * 0.90,
                        event.getY(attackIndex) - getHeight() * 0.86);
                }
                if (joystick.getIsPressed()) {
                    // O joystick foi pressionado anteriormente e agora está sendo movido
                    int pointerIndex = event.findPointerIndex(joystickPointerId);
                    if (pointerIndex >= 0) joystick.setActuator(event.getX(pointerIndex), event.getY(pointerIndex));
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (attackPointerId == event.getPointerId(event.getActionIndex())) attackPointerId = -1;
                if (joystickPointerId == event.getPointerId(event.getActionIndex())) {
                    // O ponteiro do joystick foi solto -> setIsPressed(false) e resetActuator()
                    joystick.setIsPressed(false);
                    joystick.resetActuator();
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.d("Game.java", "surfaceCreated()");
        startIfReady();
    }

    /** Resume only if a surface exists; never restart an already-running Thread. */
    public synchronized void resume() {
        activityResumed = true;
        startIfReady();
    }

    private synchronized void startIfReady() {
        if (!saveLoaded || !activityResumed || !getHolder().getSurface().isValid()) return;
        if (gameLoop.getState() == Thread.State.TERMINATED) {
            gameLoop = new GameLoop(this, getHolder());
            performance.setGameLoop(gameLoop);
        }
        if (gameLoop.getState() == Thread.State.NEW) {
            gameLoop.startLoop();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.d("Game.java", "surfaceChanged()");
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.d("Game.java", "surfaceDestroyed()");
        gameLoop.stopLoop();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Desenha o Tilemap
        tilemap.draw(canvas, gameDisplay);

        // Desenha os objetos do jogo
        player.draw(canvas, gameDisplay);

        enemyManager.draw(canvas, gameDisplay);
        for (Spell spell : spellList) spell.draw(canvas, gameDisplay);

        // Desenha os painéis do jogo
        joystick.draw(canvas);
        performance.draw(canvas);
        drawCombatHud(canvas);
        drawBossHud(canvas);
        if (!dialogue.isActive()) drawTalkPrompt(canvas);
        dialogueRenderer.draw(canvas, dialogue, getWidth(), getHeight(),
            getResources().getDisplayMetrics().density);
        if (!dialogue.isActive() && !attributesPanel.isOpen()) {
            hudPaint.setColor(0xFFDDCCA1);
            hudPaint.setTextSize(17 * getResources().getDisplayMetrics().density);
            canvas.drawText("ATRIBUTOS", 16, getHeight() * .11f, hudPaint);
        }
        attributesPanel.draw(canvas, getWidth(), getHeight(), progression);

        // Desenha "Game Over" se o jogador estiver morto
        if (player.getHealthPoint() <= 0) {
            gameOver.draw(canvas);
        }
    }


    public void update() {
        if (attributesPanel.isOpen()) return;
        if (dialogue.isActive()) { dialogue.update(); return; }
        // Para de atualizar o jogo se o jogador estiver morto
        if (player.getHealthPoint() <= 0) {
            return;
        }

        // Atualiza o estado do jogo
        joystick.update();
        player.update();
        weapons.update();
        progression.regenerateMana();
        updateCoconutStorm();

        // Coordinate AI updates, enemy population and staggered ranged attacks.
        enemyManager.update();

        // Atualiza o estado de todos os feitiços
        while (numberOfSpellsToCast > 0) {
            if (weapons.basic(progression.castSpeed())) {
                sound("attack");
                boolean coconut = weapons.current() == WeaponLoadout.COCONUT;
                spellList.add(new Spell(getContext(), player, coconut,
                    coconut ? progression.physicalMultiplier() : progression.magicMultiplier()));
            }
            numberOfSpellsToCast--;
        }
        Iterator<Spell> activeSpells = spellList.iterator();
        while (activeSpells.hasNext()) {
            Spell spell = activeSpells.next();
            spell.update();
            // Remove projectiles after they leave the playable world.
            if (spell.getPositionX() < -64 || spell.getPositionY() < -64 ||
                spell.getPositionX() > 100 * 64 + 64 || spell.getPositionY() > 70 * 64 + 64) {
                activeSpells.remove();
            }
        }

        enemyManager.collideWithPlayerSpells(spellList);

        // Atualiza o gameDisplay para que seu centro seja definido para o novo centro das
        // coordenadas do jogador no jogo
        gameDisplay.update();
        if (++updatesSinceSave >= 600) {
            updatesSinceSave = 0;
            saveNow();
        }
    }


    private void startElderDialogue() {
        QuestState state = questJournal.state(INTRO_QUEST);
        if (state == QuestState.LOCKED) {
            dialogue.start(new DialogueSequence("elder_intro", Arrays.asList(
                new DialogueLine("elder", "Os fantasmas acidos estao agitados. Voce pode nos ajudar?", "neutral", "quest.offer.first_ghosts"),
                new DialogueLine("player", "Vou enfrentar tres deles. Me espere aqui!", "neutral", "quest.accept.first_ghosts"))));
        } else if (state == QuestState.COMPLETED) {
            dialogue.start(new DialogueSequence("elder_reward", Arrays.asList(
                new DialogueLine("elder", "Voce conseguiu! O pantano esta mais tranquilo.", "happy", "quest.claim.first_ghosts"),
                new DialogueLine("player", "Mais um misterio da floresta resolvido!", "neutral", null))));
        } else if (state == QuestState.REWARDED) {
            dialogue.start(new DialogueSequence("elder_done", Arrays.asList(
                new DialogueLine("elder", "Obrigado pela ajuda, jovem mago.", "neutral", null))));
        } else {
            dialogue.start(new DialogueSequence("elder_progress", Arrays.asList(
                new DialogueLine("elder", "Fantasmas enfrentados: " + questJournal.progress(INTRO_QUEST) + " de 3.", "neutral", null))));
        }
    }

    private void applyProgressionEffects() {
        // Small provisional resistance bonuses; physical strength never scales magic.
        player.getCombatStats().setResistance(DamageType.PHYSICAL,
            Math.min(.40, (progression.resistencia - 1) * .025));
        player.getCombatStats().setResistance(DamageType.ACID,
            Math.min(.25, (progression.arcano - 1) * .015));
        player.setMaximumHealth(progression.maximumHealth());
        progression.maxMana = progression.manaCapacity();
        progression.mana = Math.min(progression.mana, progression.maxMana);
    }

    private void saveQuestProgress() {
        if (saveLoaded) saves.saveQuests(questJournal.snapshot(saves.getSlotId()));
    }

    private void handleDialogueEvent(String event) {
        if ("quest.offer.first_ghosts".equals(event)) questJournal.offer(INTRO_QUEST);
        else if ("quest.accept.first_ghosts".equals(event)) questJournal.accept(INTRO_QUEST);
        else if ("quest.claim.first_ghosts".equals(event)) {
            String unlock = questJournal.claimReward(INTRO_QUEST);
            if (unlock != null) lastRewardNotice = "Historia arcana descoberta";
        }
        saveQuestProgress();
    }

    private void drawTalkPrompt(Canvas canvas) {
        hudPaint.setColor(0xCC2B493C);
        float left = getWidth() * 0.38f, right = getWidth() * 0.70f;
        float top = getHeight() * 0.07f, bottom = getHeight() * 0.19f;
        canvas.drawRoundRect(left, top, right, bottom, 18, 18, hudPaint);
        hudPaint.setColor(0xFFF5E9CE);
        hudPaint.setTextSize(17 * getResources().getDisplayMetrics().density);
        canvas.drawText("FALAR (NPC)", left + 12, (top + bottom) / 2, hudPaint);
        QuestState state = questJournal.state(INTRO_QUEST);
        if (state == QuestState.ACTIVE || state == QuestState.COMPLETED) {
            canvas.drawText("Missao: " + questJournal.progress(INTRO_QUEST) + "/3",
                left, bottom + 28, hudPaint);
        }
        if (lastRewardNotice != null) canvas.drawText(lastRewardNotice, left, bottom + 55, hudPaint);
        hudPaint.setTextSize(14 * getResources().getDisplayMetrics().density);
        canvas.drawText("Nv " + progression.level + "  XP " + progression.experience + "/" +
            PlayerProgression.experienceToNextLevel(progression.level) +
            "  Pontos " + progression.attributePoints, left, bottom + 83, hudPaint);
    }

    private void activateCoconutStorm() {
        if (progression.mana < 35 || !weapons.special()) return;
        progression.spendMana(35);
        sound("storm");
        if (saveLoaded) saves.saveStats(progression.snapshot(saves.getSlotId()));
        coconutStormTicks = 150;
        double dx = player.getAttackDirectionX(), dy = player.getAttackDirectionY();
        if (dx == 0 && dy == 0) dy = 1;
        stormX = player.getPositionX() + dx * 210;
        stormY = player.getPositionY() + dy * 210;
    }

    private void updateCoconutStorm() {
        if (coconutStormTicks <= 0) return;
        coconutStormTicks--;
        if (coconutStormTicks % 18 == 0) {
            double angle = coconutRandom.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(coconutRandom.nextDouble()) * 125;
            enemyManager.coconutImpact(stormX + Math.cos(angle) * distance,
                stormY + Math.sin(angle) * distance, 45, progression.physicalMultiplier());
        }
    }

    /** Called by future quests, arena triggers or scripted encounters. */
    public boolean startBossEncounter(EnemyTemplate template, double x, double y) {
        return enemyManager.spawnEncounter(template, x, y) != null;
    }

    private void drawBossHud(Canvas canvas) {
        Enemy boss = enemyManager.activeBoss();
        if (boss == null) return;
        float width = getWidth() * .7f;
        float left = (getWidth() - width) / 2;
        float top = getHeight() * .89f;
        hudPaint.setColor(0xDD18241F);
        canvas.drawRoundRect(left, top, left + width, top + 24, 8, 8, hudPaint);
        hudPaint.setColor(0xFFD4A46C);
        canvas.drawRoundRect(left, top, left + width * (float) boss.getHealthRatio(),
            top + 24, 8, 8, hudPaint);
        hudPaint.setColor(0xFFFFFFFF);
        hudPaint.setTextSize(15 * getResources().getDisplayMetrics().density);
        canvas.drawText(boss.getTemplate().displayName + " - Fase " + (boss.getPhase() + 1),
            left, top - 12, hudPaint);
    }

    private void drawCombatHud(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        hudPaint.setTextSize(24 * getResources().getDisplayMetrics().density);
        hudPaint.setColor(0xCCF2E8D0);
        canvas.drawText("Mana: " + (int) progression.mana + "/" + (int) progression.maxMana,
            w * .52f, h * .53f, hudPaint);
        canvas.drawText("Arma: " + (weapons.selectedSlot() + 1) + "  " + weapons.current().name,
            w * 0.52f, h * 0.59f, hudPaint);
        hudPaint.setColor(0xAA38574C);
        canvas.drawCircle(w * 0.90f, h * 0.86f, 54, hudPaint);
        canvas.drawCircle(w * 0.77f, h * 0.86f, 43, hudPaint);
        canvas.drawCircle(w * 0.88f, h * 0.66f, 38, hudPaint);
        hudPaint.setColor(0xFFFFFFFF);
        hudPaint.setTextSize(22 * getResources().getDisplayMetrics().density);
        canvas.drawText("ATK", w * 0.865f, h * 0.87f, hudPaint);
        canvas.drawText(weapons.specialCooldown() == 0 ? "ESP" : "" + (weapons.specialCooldown() / 60 + 1),
            w * 0.73f, h * 0.87f, hudPaint);
        canvas.drawText("TROCA", w * 0.81f, h * 0.67f, hudPaint);
        if (coconutStormTicks > 0) {
            hudPaint.setColor(0x66D9B86C);
            canvas.drawCircle((float) gameDisplay.gameToDisplayCoordinatesX(stormX),
                (float) gameDisplay.gameToDisplayCoordinatesY(stormY), 125, hudPaint);
        }
    }

    /** Explicit save action used by the in-game pause menu. */
    public void saveNow() {
        if (!saveLoaded) return;
        saves.savePosition(player.getPositionX(), player.getPositionY(), player.getHealthPoint());
        saveQuestProgress();
        saves.saveStats(progression.snapshot(saves.getSlotId()));
    }

    public synchronized void pause() {
        activityResumed = false;
        gameLoop.stopLoop();
        saveNow();
    }

    public void closeSaveRepository() {
        saves.close();
    }

}
