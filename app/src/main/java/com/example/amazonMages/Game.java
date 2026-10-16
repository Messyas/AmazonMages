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
import com.example.amazonMages.ecs.ProjectileWorld;
import com.example.amazonMages.ecs.World;
import com.example.amazonMages.ecs.Entity;
import com.example.amazonMages.ecs.TransformComponent;
import com.example.amazonMages.ecs.ColliderComponent;
import com.example.amazonMages.ecs.PickupComponent;
import com.example.amazonMages.ecs.PickupSystem;
import com.example.amazonMages.items.ItemRegistry;
import com.example.amazonMages.items.ItemDefinition;
import com.example.amazonMages.items.Inventory;
import com.example.amazonMages.items.ConsumableService;
import com.example.amazonMages.save.InventoryItemEntity;
import java.util.Map;
import java.util.LinkedHashMap;
import com.example.amazonMages.events.GameEvent;
import com.example.amazonMages.events.GameEventBus;
import com.example.amazonMages.dev.DevicePerformanceMonitor;
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
    private final com.example.amazonMages.state.GameStateManager states;

    private GameAudioManager audio;
    private final DevicePerformanceMonitor deviceMonitor = new DevicePerformanceMonitor(getContext());
    private volatile boolean debugOverlay;
    public void setAudioManager(GameAudioManager manager) { this.audio = manager; }
    private void sound(String event) { if (audio != null) audio.play(event); }

    /** Commands run only while the game is paused in a debuggable build. */
    public String runDevCommand(String input) {
        if ((getContext().getApplicationInfo().flags &
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0)
            return "Console indisponível em builds de produção.";
        return new DevConsole(new DevConsole.Commands() {
            @Override public String debug(String mode) {
                if ("on".equals(mode)) {
                    debugOverlay = true;
                    return "Monitor ligado. Feche o console e retome o jogo.";
                }
                if ("off".equals(mode)) {
                    debugOverlay = false;
                    return "Monitor desligado.";
                }
                deviceMonitor.sampleIfDue();
                return diagnostics();
            }
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
                    inventory.add("cajado", 1);
                    weapons.equip(1, com.example.amazonMages.items.ItemCatalog.STAFF);
                    saveNow();
                    return "Cajado de Teste equipado no slot 2. Use TROCA.";
                }
                if ("erva".equals(item)) {
                    int accepted = inventory.add("erva", 1);
                    saveInventory();
                    return "Ervas: " + inventory.count("erva") + " (+" + accepted + ")";
                }
                return "Item desconhecido: coco, cajado, erva.";
            }
            @Override public String inventory() {
                return "Inventario: " + inventory.snapshot();
            }
            @Override public String use(String item) {
                if (!"erva".equals(item)) return "Consumivel desconhecido: erva.";
                int before = player.getHealthPoint();
                int after = consumables.useHealingHerb(inventory, before, player.getMaximumHealth());
                if (after == before) return "Sem erva ou vida ja esta cheia.";
                player.setHealthPoint(after);
                saveInventory();
                return "Erva utilizada. Vida: " + after + "/" + player.getMaximumHealth();
            }
            @Override public String spawn(String species, int count) {
                if ("erva".equals(species)) {
                    for (int i = 0; i < count; i++)
                        spawnPickup("erva", 1, player.getPositionX() + 75 + (i % 4) * 28,
                            player.getPositionY() + (i / 4) * 28);
                    return "Ervas no chao: " + count;
                }
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
    private static final String SENTINEL_QUEST = "sentinel_encounter";
    private final GameEventBus eventBus = new GameEventBus();
    private final World pickupWorld = new World();
    private final com.example.amazonMages.ecs.LifetimeSystem pickupLifetime = new com.example.amazonMages.ecs.LifetimeSystem();
    private final PickupSystem pickupSystem = new PickupSystem();
    private final ItemRegistry itemRegistry = com.example.amazonMages.items.ItemCatalog.create();
    private final com.example.amazonMages.ui.InventoryPanel inventoryPanel = new com.example.amazonMages.ui.InventoryPanel(itemRegistry);
    private String inventoryNotice;
    private final Inventory inventory = new Inventory(itemRegistry);
    private final com.example.amazonMages.effects.PlayerEffectTarget effectTarget;
    private final ConsumableService consumables = new ConsumableService();
    private final GameRenderer renderer;
    private final com.example.amazonMages.save.SaveCoordinator saveCoordinator;
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
    private final InputController input;
    private final Joystick joystick;
    private final Player player;
    private GameLoop gameLoop;
    private volatile boolean activityResumed = false;
    private final EnemyManager enemyManager;
    private final ProjectileWorld projectileWorld = new ProjectileWorld(-64, -64, 100 * 64 + 64, 70 * 64 + 64);
    private final WeaponLoadout weapons = new WeaponLoadout();
    private final Random coconutRandom = new Random();
    private int coconutStormTicks;
    private double stormX, stormY;
    private final com.example.amazonMages.ui.CanvasUi canvasUi = new com.example.amazonMages.ui.CanvasUi();
    private final Paint hudPaint = new Paint(3);
    private final GameOver gameOver;
    private final Performance performance;
    private final GameDisplay gameDisplay;

    /**
     * Construtor da classe Game.
     *
     * @param context O contexto da aplicação, que é passado para a Superclasse SurfaceView.
     */
    public Game(Context context, int slotId) {
        this(context, slotId, new com.example.amazonMages.state.GameStateManager());
    }

    public Game(Context context, int slotId, com.example.amazonMages.state.GameStateManager states) {
        super(context);
        this.states = states;
        states.loading();

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
        effectTarget = new com.example.amazonMages.effects.PlayerEffectTarget(player, progression);
        input = new InputController(player, joystick, weapons, this::activateCoconutStorm);


        // Initialize display and center it around the player
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        gameDisplay = new GameDisplay(displayMetrics.widthPixels, displayMetrics.heightPixels, player);

        // Initialize Tilemap
        tilemap = new Tilemap(spriteSheet);
        enemyManager = new EnemyManager(context, player, tilemap);
        renderer = new GameRenderer(tilemap, player, enemyManager, projectileWorld, pickupWorld, gameDisplay);
        saveCoordinator = new com.example.amazonMages.save.SaveCoordinator(
            saves, inventory, progression, questJournal, player, pickupWorld, weapons, itemRegistry);
        questJournal.register(new QuestDefinition(INTRO_QUEST, "Ecos do Pantano",
            "enemy.acid_ghost.defeated", 3, "unlock.arcane_lore_intro"));
        questJournal.register(new QuestDefinition(SENTINEL_QUEST, "Sentinela do Pantano",
            EnemyTemplates.SWAMP_SENTINEL.defeatEvent, 1, "sentinel.defeated"));
        eventBus.subscribe(event -> questJournal.onEvent(event.id));
        eventBus.subscribe(event -> {
            if ("encounter.swamp_sentinel.requested".equals(event.id)) {
                if (questJournal.state(SENTINEL_QUEST) == QuestState.LOCKED) {
                    questJournal.offer(SENTINEL_QUEST); questJournal.accept(SENTINEL_QUEST);
                }
                if (questJournal.state(SENTINEL_QUEST) == QuestState.ACTIVE)
                    startBossEncounter(EnemyTemplates.SWAMP_SENTINEL,
                        Math.min(6300, player.getPositionX()+300), Math.min(4380, player.getPositionY()+200));
            }
        });
        eventBus.subscribe(event -> {
            if (event.id.startsWith("enemy.")) sound("ghost_defeat");
        });
        eventBus.subscribe(event -> {
            int reward = 0;
            if (EnemyTemplates.ACID_GHOST.defeatEvent.equals(event.id)) reward = EnemyTemplates.ACID_GHOST.experienceReward;
            else if (EnemyTemplates.SWAMP_SENTINEL.defeatEvent.equals(event.id)) reward = EnemyTemplates.SWAMP_SENTINEL.experienceReward;
            else if (EnemyTemplates.ANCIENT_WRAITH.defeatEvent.equals(event.id)) reward = EnemyTemplates.ANCIENT_WRAITH.experienceReward;
            if (reward > 0) { progression.gainExperience(reward); saveQuestProgress(); }
        });
        pickupSystem.setListener((item,id,quantity) -> eventBus.publish(new GameEvent("item."+item+".collected",id)));
        enemyManager.setDropListener((template, x, y) ->
            com.example.amazonMages.items.DropTable.forEnemy(template == null ? "acid_ghost" : template.id)
                .roll(coconutRandom, (item, quantity) -> spawnPickup(item, quantity, x, y)));
        enemyManager.setDetailedKillListener((event,id) -> eventBus.publish(new GameEvent(event, id)));

        dialogue.setEventListener(this::handleDialogueEvent);


        setFocusable(true);
        saves.loadSnapshot((position, stats, quests, items, equipment, pickups) -> {
            if (position != null && Double.isFinite(position.x) && Double.isFinite(position.y)) {
                player.restorePosition(position.x, position.y);
                gameDisplay.update();
            }
            questJournal.restore(quests);
            progression.restore(stats);
            applyProgressionEffects();
            if (position != null) player.setHealthPoint(position.health);
            Map<String, Integer> loaded = new LinkedHashMap<>();
            for (InventoryItemEntity row : items) loaded.put(row.itemId, row.quantity);
            inventory.restore(loaded);
            if (inventory.count("coco") == 0) inventory.add("coco", 1);
            saveCoordinator.restoreWorld(equipment, pickups);
            if (questJournal.state(SENTINEL_QUEST) == QuestState.ACTIVE)
                eventBus.publish(new GameEvent("encounter.swamp_sentinel.requested",0));
            saveCoordinator.ready();
            saveLoaded = true;
            states.loaded();
            startIfReady();
        }, () -> android.widget.Toast.makeText(getContext(),
            "Falha ao carregar save. Volte ao menu e tente novamente.",
            android.widget.Toast.LENGTH_LONG).show());
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public synchronized boolean onTouchEvent(MotionEvent event) {
        if (!states.acceptsInput() || player.getHealthPoint() <= 0) return true;
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            resetInput();
            return true;
        }
        if (inventoryPanel.isOpen()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                int action = inventoryPanel.touch(event.getX(), event.getY(), getWidth(), getHeight());
                if (action == -2) states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
                ItemDefinition item = inventoryPanel.selected();
                if (action == -1) {
                    boolean applied = consumables.use(inventory, item, effectTarget);
                    if (applied) saveNow();
                    inventoryNotice = applied ? "Item utilizado." : "O item nao pode ser usado agora.";
                } else if (action >= 0 && item.weapon != null && inventory.count(item.id) > 0) {
                    weapons.equip(action, item.weapon); weapons.select(action); saveNow();
                    inventoryNotice = "Arma equipada.";
                }
            }
            return true;
        }
        if (!dialogue.isActive() && !attributesPanel.isOpen() && event.getActionMasked() == MotionEvent.ACTION_DOWN &&
                event.getX() > getWidth() * .70f && event.getY() > getHeight() * .20f && event.getY() < getHeight() * .36f) {
            resetInput(); inventoryPanel.open(); states.modal(com.example.amazonMages.state.GameStateManager.State.INVENTORY); return true;
        }
        if (attributesPanel.isOpen()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (attributesPanel.touch(event.getX(), event.getY(), getWidth(), getHeight(), progression)) {
                    applyProgressionEffects();
                    if (!attributesPanel.isOpen()) states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
                    saveNow();
                }
            }
            return true;
        }
        if (!dialogue.isActive() && event.getActionMasked() == MotionEvent.ACTION_DOWN &&
            event.getX() < getWidth() * .38f && event.getY() < getHeight() * .18f) {
            attributesPanel.toggle();
            states.modal(com.example.amazonMages.state.GameStateManager.State.ATTRIBUTES);
            resetInput();
            return true;
        }
        if (dialogue.isActive()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                dialogue.advance();
                if (!dialogue.isActive()) states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN &&
            event.getX() > getWidth() * 0.38f && event.getX() < getWidth() * 0.70f &&
            event.getY() < getHeight() * 0.22f) {
            resetInput();
            startElderDialogue();
            sound("dialogue");
            return true;
        }
        return input.touch(event, getWidth(), getHeight()) || super.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.d("Game.java", "surfaceCreated()");
        startIfReady();
    }

    /** Resume only if a surface exists; never restart an already-running Thread. */
    public synchronized void resume() {
        activityResumed = true;
        states.resume();
        startIfReady();
    }

    private synchronized void startIfReady() {
        if (!saveLoaded || !activityResumed || !getHolder().getSurface().isValid()) return;
        states.resume();
        if (gameLoop.getState() == Thread.State.TERMINATED) {
            gameLoop = new GameLoop(this, getHolder());
            performance.setGameLoop(gameLoop);
        }
        if (gameLoop.getState() == Thread.State.NEW) {
            gameLoop.startLoop();
        }
    }

    @Override
    public synchronized void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.d("Game.java", "surfaceChanged()");
        resetInput();
        joystick.layout(width,height);
        gameDisplay.resize(width,height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.d("Game.java", "surfaceDestroyed()");
        gameLoop.stopLoop();
        synchronized (this) { resetInput(); }
    }

    @Override
    public synchronized void draw(Canvas canvas) {
        super.draw(canvas);

        renderer.draw(canvas);

        // Desenha os painéis do jogo
        joystick.draw(canvas);
        if (debugOverlay && isDebuggable()) drawDebugOverlay(canvas);
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
        hudPaint.setColor(0xFFF5EACD); hudPaint.setTextSize(getHeight() * .035f);
        canvas.drawText("INVENTARIO", getWidth() * .72f, getHeight() * .29f, hudPaint);
        inventoryPanel.draw(canvas, getWidth(), getHeight(), inventory, weapons, inventoryNotice);
        if (!inventoryPanel.isOpen() && inventoryNotice != null)
            canvasUi.text(canvas, inventoryNotice, getWidth()*.04f, getHeight()*.39f, Math.min(getWidth(),getHeight())*.026f, 0xFFFFD17C);

        // Desenha "Game Over" se o jogador estiver morto
        if (player.getHealthPoint() <= 0) {
            gameOver.draw(canvas);
        }
    }


    public synchronized void update() {
        if (debugOverlay && isDebuggable()) deviceMonitor.sampleIfDue();

        // Para de atualizar o jogo se o jogador estiver morto
        if (player.getHealthPoint() <= 0) { states.gameOver(); return; }
        if (!states.updatesWorld()) {
            if (states.state() == com.example.amazonMages.state.GameStateManager.State.DIALOGUE) dialogue.update();
            return;
        }

        // Atualiza o estado do jogo
        joystick.update();
        player.update();
        pickupLifetime.update(pickupWorld,1);
        int collected = pickupSystem.collect(pickupWorld, player.getPositionX(),
            player.getPositionY(), player.getRadius(), inventory);
        if (collected > 0) {
            inventoryNotice = "Itens coletados: " + collected;
            saveInventory();
        }
        if (pickupSystem.blockedByCapacity()) inventoryNotice = "Inventario cheio para este item.";
        weapons.update();
        progression.regenerateMana();
        updateCoconutStorm();

        // Coordinate AI updates, enemy population and staggered ranged attacks.
        enemyManager.update();
        if (player.getHealthPoint() <= 0) { resetInput(); states.gameOver(); return; }

        // Atualiza o estado de todos os feitiços
        while (input.consumeShot()) {
            if (weapons.basic(progression.castSpeed())) {
                sound("attack");
                boolean coconut = weapons.current() == WeaponLoadout.COCONUT;
                projectileWorld.spawn(new Spell(getContext(), player, coconut,
                    coconut ? progression.physicalMultiplier() : progression.magicMultiplier()));
            }
        }
        projectileWorld.update();
        enemyManager.collideWithPlayerProjectiles(projectileWorld);

        // Atualiza o gameDisplay para que seu centro seja definido para o novo centro das
        // coordenadas do jogador no jogo
        gameDisplay.update();
        if (++updatesSinceSave >= 600) {
            updatesSinceSave = 0;
            saveNow();
        }
    }


    private void spawnPickup(String itemId, int quantity, double x, double y) {
        if (itemRegistry.get(itemId) == null || quantity < 1 || !Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("item");
        if (pickupWorld.size() >= 64) { inventoryNotice = "Limite de itens no chao atingido."; return; }
        pickupWorld.create()
            .add(TransformComponent.class, new TransformComponent(x, y))
            .add(ColliderComponent.class, new ColliderComponent(13))
            .add(PickupComponent.class, new PickupComponent(itemId, quantity))
            .add(com.example.amazonMages.ecs.LifetimeComponent.class, new com.example.amazonMages.ecs.LifetimeComponent(36000));
    }

    private void startElderDialogue() {
        states.modal(com.example.amazonMages.state.GameStateManager.State.DIALOGUE);
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

    private void saveInventory() { saveNow(); }

    private void saveQuestProgress() { saveNow(); }

    private void handleDialogueEvent(String event) {
        if ("quest.offer.first_ghosts".equals(event)) questJournal.offer(INTRO_QUEST);
        else if ("quest.accept.first_ghosts".equals(event)) questJournal.accept(INTRO_QUEST);
        else if ("quest.claim.first_ghosts".equals(event)) {
            String unlock = questJournal.claimReward(INTRO_QUEST);
            if (unlock != null) {
                lastRewardNotice = "Historia arcana descoberta. A Sentinela se aproxima!";
                eventBus.publish(new GameEvent("encounter.swamp_sentinel.requested",0));
            }
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
        saveNow();
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

    private boolean isDebuggable() {
        return (getContext().getApplicationInfo().flags &
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private String diagnostics() {
        return String.format(java.util.Locale.US,
            "FPS %.1f | UPS %.1f | update %.2f ms | draw %.2f ms",
            gameLoop.getAverageFPS(), gameLoop.getAverageUPS(),
            gameLoop.getAverageUpdateMs(), gameLoop.getAverageDrawMs()) +
            "\\nInimigos " + enemyManager.getEnemyCount() +
            " | projeteis inimigos " + enemyManager.getProjectileCount() +
            " | magias " + projectileWorld.count() +
            "\\nJogador " + (int) player.getPositionX() +
            "," + (int) player.getPositionY() +
            "\\n" + deviceMonitor.snapshot();
    }

    private void drawDebugOverlay(Canvas canvas) {
        String[] lines = diagnostics().split("\\n");
        float density = getResources().getDisplayMetrics().density;
        float lineHeight = 17 * density;
        float left = 8 * density, top = getHeight() * .23f;
        hudPaint.setColor(0xD916211C);
        canvas.drawRoundRect(left, top, getWidth() - left,
            top + (lines.length + 1) * lineHeight, 8, 8, hudPaint);
        hudPaint.setColor(0xFFBDF1CB);
        hudPaint.setTextSize(12 * density);
        for (int i = 0; i < lines.length; i++)
            canvas.drawText(lines[i], left + 8 * density,
                top + (i + 1) * lineHeight, hudPaint);
    }

    private void drawCombatHud(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        canvasUi.bar(canvas, w*.52f, h*.47f, w*.85f, h*.49f,
            progression.mana / Math.max(1,progression.maxMana), 0xFF638ECF);
        canvasUi.bar(canvas, w*.72f, h*.94f, w*.94f, h*.96f,
            1-weapons.specialCooldown()/1080.0, 0xFFD3AE64);
        hudPaint.setTextSize(Math.min(w,h)*.035f);
        hudPaint.setColor(0xCCF2E8D0);
        canvas.drawText("Mana: " + (int) progression.mana + "/" + (int) progression.maxMana,
            w * .52f, h * .53f, hudPaint);
        canvas.drawText("Arma: " + (weapons.selectedSlot() + 1) + "  " + weapons.current().name,
            w * 0.52f, h * 0.59f, hudPaint);
        hudPaint.setColor(0xAA38574C);
        canvas.drawCircle(w * 0.90f, h * 0.86f, Math.min(w,h)*.085f, hudPaint);
        canvas.drawCircle(w * 0.77f, h * 0.86f, Math.min(w,h)*.07f, hudPaint);
        canvas.drawCircle(w * 0.88f, h * 0.66f, Math.min(w,h)*.06f, hudPaint);
        hudPaint.setColor(0xFFFFFFFF);
        hudPaint.setTextSize(Math.min(w,h)*.04f);
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
    public synchronized void saveNow() { saveCoordinator.save(); }

    private void resetInput() { input.reset(); }

    public void pause() {
        activityResumed = false;
        states.pause();
        // Never hold the Game monitor while joining a loop that needs it.
        gameLoop.stopLoop();
        synchronized (this) { resetInput(); saveNow(); }
    }

    public void closeSaveRepository() {
        pause();
        saves.close();
    }

}
