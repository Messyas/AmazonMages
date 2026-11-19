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
import com.example.amazonMages.ecs.LifetimeComponent;
import com.example.amazonMages.items.ItemRegistry;
import com.example.amazonMages.items.ItemCatalog;
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
import com.example.amazonMages.gameobject.BoitataBoss;
import com.example.amazonMages.gameobject.SanctuaryGuide;
import com.example.amazonMages.dialogue.GuideConversation;
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
    private Runnable developerChatListener;
    private final DevicePerformanceMonitor deviceMonitor = new DevicePerformanceMonitor(getContext());
    private volatile boolean debugOverlay;
    public void setAudioManager(GameAudioManager manager) { this.audio = manager; }
    public void setDeveloperChatListener(Runnable listener) { developerChatListener = listener; }
    public boolean hasDeveloperChat() { return isDebuggable() && developerChatListener != null; }
    public void togglePerformanceOverlay() {
        debugOverlay = !debugOverlay;
        if (debugOverlay) deviceMonitor.sampleIfDue();
    }
    public boolean isPerformanceOverlayEnabled() { return debugOverlay; }
    /** Simulation ticks between presented frames; used by the game loop to cap the frame rate. */
    int getFrameInterval() { return frameInterval; }
    public float getRenderScale() { return renderScale; }

    /** Applies a new resolution / frame-cap choice immediately (UI thread). */
    public void applyGraphics(com.example.amazonMages.graphics.GraphicsSettings settings) {
        renderScale = settings.scale();
        frameInterval = settings.frameInterval();
        applyRenderResolution();
        requestFrameRate(getHolder());
    }

    /**
     * Sizes the surface buffer to the chosen fraction of the view. Only the buffer shrinks: the
     * view, touch input and game coordinates stay native, and the compositor upscales the result.
     */
    private void applyRenderResolution() {
        int width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        float scale = renderScale;
        if (scale >= 0.999f) {
            getHolder().setSizeFromLayout();
        } else {
            getHolder().setFixedSize(Math.max(1, Math.round(width * scale)),
                Math.max(1, Math.round(height * scale)));
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        applyRenderResolution();
    }

    public synchronized double cycleZoom() {
        double zoom = Math.round(gameDisplay.getZoom()) + 1; // 1x -> 2x -> 3x -> back to 1x
        if (zoom > 3) zoom = 1;
        gameDisplay.setZoom(zoom);
        return zoom;
    }
    public synchronized void setCameraZoom(double zoom) {
        if (gameDisplay != null) gameDisplay.setZoom(zoom);
    }
    private void sound(String event) { if (audio != null) audio.play(event); }

    /** Commands run only while the game is paused in a debuggable build. */
    public String runDevCommand(String input) {
        if ((getContext().getApplicationInfo().flags &
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0)
            return "Console indisponível em builds de produção.";
        if (input == null) return "Digite /help para ver os comandos.";
        String command = input.trim();
        if (command.startsWith("/")) command = command.substring(1);
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
                if (!tilemap.world().isWalkable(x, y, player.getRadius())) return "Destino bloqueado pelo terreno.";
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
                states.revive();
                return "Vida e mana restauradas.";
            }
            @Override public String give(String item) {
                if ("coco".equals(item)) {
                    weapons.equip(0, WeaponLoadout.COCONUT);
                    int accepted = inventory.add("coco", 5);
                    saveInventory();
                    return "Dominio dos Cocos (+" + accepted + ", total " + inventory.count("coco") + ").";
                }
                if ("cajado".equals(item)) {
                    inventory.add("cajado", 1);
                    weapons.equip(1, com.example.amazonMages.items.ItemCatalog.STAFF);
                    arsenal.unlockLeafFromStaff();
                    saveNow();
                    return "Cajado do Pantano equipado no slot 2. Use TROCA.";
                }
                if ("erva".equals(item)) {
                    int accepted = inventory.add("erva", 3);
                    saveInventory();
                    return "Ervas: " + inventory.count("erva") + " (+" + accepted + ")";
                }
                if ("mana".equals(item)) {
                    int accepted = inventory.add("mana", 3);
                    saveInventory();
                    return "Essencias de Mana: " + inventory.count("mana") + " (+" + accepted + ")";
                }
                if ("bencao".equals(item)) {
                    int accepted = inventory.add("bencao", 2);
                    saveInventory();
                    return "Bencaos: " + inventory.count("bencao") + " (+" + accepted + ")";
                }
                if ("elixir".equals(item)) {
                    int accepted = inventory.add("elixir", 2);
                    saveInventory();
                    return "Elixires: " + inventory.count("elixir") + " (+" + accepted + ")";
                }
                if ("bomba".equals(item)) {
                    int accepted = inventory.add("bomba_coco", 3);
                    saveInventory();
                    return "Bombas de Pororoca: " + inventory.count("bomba_coco") + " (+" + accepted + ")";
                }
                if ("tonico".equals(item)) {
                    int accepted = inventory.add("tonico", 2);
                    saveInventory();
                    return "Tonicos: " + inventory.count("tonico") + " (+" + accepted + ")";
                }
                if ("runa".equals(item)) {
                    int accepted = inventory.add("runa_ancestral", 1);
                    saveInventory();
                    return "Runas Ancestrais: " + inventory.count("runa_ancestral") + " (+" + accepted + ")";
                }
                if ("kit".equals(item)) {
                    inventory.add("coco", 10);
                    inventory.add("erva", 5);
                    inventory.add("mana", 5);
                    inventory.add("bencao", 2);
                    saveInventory();
                    return "Kit Alquimia adicionado!";
                }
                return "Item desconhecido: coco, cajado, erva, mana, bencao, elixir, bomba, tonico, runa, kit.";
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
                states.revive();
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
                if ("morcego".equals(species)) template = EnemyTemplates.SWARM_BAT;
                else if ("fantasma".equals(species)) template = EnemyTemplates.ACID_GHOST;
                else if ("diabrete".equals(species)) template = EnemyTemplates.CURUPIRA_IMP;
                else if ("golem".equals(species)) template = EnemyTemplates.MOSS_GOLEM;
                else if ("xama".equals(species)) template = EnemyTemplates.VINE_SHAMAN;
                else if ("sentinela".equals(species)) template = EnemyTemplates.SWAMP_SENTINEL;
                else if ("espectro".equals(species)) template = EnemyTemplates.ANCIENT_WRAITH;
                else return "Especie desconhecida: morcego, fantasma, diabrete, golem, xama, sentinela, espectro.";
                int spawned = 0;
                for (int i = 0; i < count; i++) {
                    double x = player.getPositionX() + 150 + (i % 4) * 75;
                    double y = player.getPositionY() + 100 + (i / 4) * 75;
                    boolean ok = template.isBossEncounter() ?
                        enemyManager.spawnEncounter(template, x, y) != null :
                        enemyManager.spawnDebugSpecies(template, x, y);
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
        }).execute(command);
    }

    private static final String INTRO_QUEST = "first_ghosts";
    private static final String SENTINEL_QUEST = "sentinel_encounter";
    private static final String BOITATA_ENCOUNTER = "boitata_encounter";
    private final GameEventBus eventBus = new GameEventBus();
    private final World pickupWorld = new World();
    private final com.example.amazonMages.ecs.LifetimeSystem pickupLifetime = new com.example.amazonMages.ecs.LifetimeSystem();
    private final PickupSystem pickupSystem = new PickupSystem();
    private final ItemRegistry itemRegistry = com.example.amazonMages.items.ItemCatalog.create();
    private final com.example.amazonMages.ui.InventoryPanel inventoryPanel = new com.example.amazonMages.ui.InventoryPanel(itemRegistry);
    private String inventoryNotice;
    private int inventoryNoticeTicks;
    private final Inventory inventory = new Inventory(itemRegistry);
    private final com.example.amazonMages.effects.PlayerEffectTarget effectTarget;
    private final ConsumableService consumables = new ConsumableService();
    private final GameRenderer renderer;
    private final com.example.amazonMages.save.SaveCoordinator saveCoordinator;
    private final QuestJournal questJournal = new QuestJournal();
    private final PlayerProgression progression = new PlayerProgression();
    private final com.example.amazonMages.progression.SkillTree skillTree =
        new com.example.amazonMages.progression.SkillTree();
    private final com.example.amazonMages.items.WorldForagingSystem foragingSystem =
        new com.example.amazonMages.items.WorldForagingSystem();
    private final GameRenderer.CombatFxState combatFxState = new GameRenderer.CombatFxState();
    private final AttributesPanel attributesPanel = new AttributesPanel();
    private final com.example.amazonMages.ui.ExplorationPanel explorationPanel =
        new com.example.amazonMages.ui.ExplorationPanel();
    private java.util.function.Consumer<Boolean> worldControlsVisibility;

    void setWorldControlsVisibility(java.util.function.Consumer<Boolean> listener) {
        worldControlsVisibility = listener;
    }

    private void showWorldControls(boolean visible) {
        if (worldControlsVisibility != null) worldControlsVisibility.accept(visible);
    }
    private final com.example.amazonMages.ui.SurvivorUpgradePanel upgradePanel;
    private final com.example.amazonMages.combat.SurvivorArsenal arsenal =
        new com.example.amazonMages.combat.SurvivorArsenal();
    private final com.example.amazonMages.combat.XpCrystalPool xpCrystals =
        new com.example.amazonMages.combat.XpCrystalPool();
    private int lastObservedLevel = 1;
    private String hudBanner;
    private int hudBannerTicks;
    private final com.example.amazonMages.gamepanel.ForestHud forestHud;
    private final com.example.amazonMages.gamepanel.CoconutStormFx coconutStormFx;
    private final Tilemap tilemap;
    private final GameSaveRepository saves;
    private volatile boolean saveLoaded = false;
    // Render resolution x frame cap (see GraphicsSettings); read by the loop thread.
    private volatile float renderScale = 1f;
    private volatile int frameInterval = 1;
    private int updatesSinceSave = 0;
    private final InputController input;
    private final Joystick joystick;
    private final Joystick aimStick;
    private final com.example.amazonMages.gamepanel.AimGuide aimGuide =
        new com.example.amazonMages.gamepanel.AimGuide();
    private final Player player;
    private final SanctuaryGuide guide;
    private final GuideConversation guideConversation = new GuideConversation();
    private Runnable guideChatListener;
    private GameLoop gameLoop;
    private volatile boolean activityResumed = false;
    private final EnemyManager enemyManager;
    private final ProjectileWorld projectileWorld = new ProjectileWorld(
        -128, -128,
        com.example.amazonMages.map.WorldDefinition.COLUMNS * 64 + 128,
        com.example.amazonMages.map.WorldDefinition.ROWS * 64 + 128);
    private final WeaponLoadout weapons = new WeaponLoadout();
    private final Random coconutRandom = new Random();
    private int coconutStormTicks;
    private double stormX, stormY;
    private double stormPowerMultiplier = 1.0;
    private int manualComboStep;
    private int manualComboResetTicks;
    private int vortexTicks;
    private double vortexX, vortexY, vortexRadius = 155.0;
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
        forestHud = new com.example.amazonMages.gamepanel.ForestHud(context);
        coconutStormFx = new com.example.amazonMages.gamepanel.CoconutStormFx(context);
        upgradePanel = new com.example.amazonMages.ui.SurvivorUpgradePanel(context);
        states.loading();

        com.example.amazonMages.graphics.GraphicsSettings graphics =
            com.example.amazonMages.graphics.GraphicsSettings.load(context);
        renderScale = graphics.scale();
        frameInterval = graphics.frameInterval();

        // Get surface holder and add callback
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        gameLoop = new GameLoop(this, surfaceHolder);

        // Initialize game panels
        performance = new Performance(context, gameLoop);
        gameOver = new GameOver(context);
        joystick = new Joystick(275, 700, 70, 40, context);
        aimStick = new Joystick(0, 0, 55, 30, context);

        // Initialize game objects
        SpriteSheet spriteSheet = new SpriteSheet(context);
        SpriteAnimator animator = new SpriteAnimator(spriteSheet.getPlayerAnimations());
        com.example.amazonMages.map.WorldDefinition world = new com.example.amazonMages.map.WorldDefinition();
        player = new Player(context, joystick, world.spawn.worldX(), world.spawn.worldY(), 32, animator);
        guide = new SanctuaryGuide(context, world, player);
        player.setWorldGeometry(world);
        saves = new GameSaveRepository(context, slotId);
        effectTarget = new com.example.amazonMages.effects.PlayerEffectTarget(player, progression);
        attributesPanel.attachSkillTree(context, skillTree);
        inventoryPanel.attachContext(context, skillTree);
        explorationPanel.attachContext(context);
        arsenal.setSkillTree(skillTree);
        input = new InputController(player, joystick, aimStick, weapons,
            this::activateEquippedSpecial, this::activatePlayerParry,
            this::activatePlayerDash, this::activateQuickItem);


        // Initialize display and center it around the player
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        gameDisplay = new GameDisplay(displayMetrics.widthPixels, displayMetrics.heightPixels, player);

        // Initialize Tilemap
        tilemap = new Tilemap(spriteSheet, world);
        enemyManager = new EnemyManager(context, player, tilemap);
        renderer = new GameRenderer(tilemap, player, enemyManager, projectileWorld, pickupWorld, gameDisplay);
        renderer.attachGuide(guide);
        renderer.attachSurvivorFx(context, arsenal, xpCrystals);
        renderer.attachCombatFx(combatFxState);
        // Decode/upload the enemy atlases and build the first map chunks off the game thread.
        final Context appContext = context.getApplicationContext();
        final Tilemap warmTilemap = tilemap;
        final double warmX = player.getPositionX(), warmY = player.getPositionY();
        final int warmWidth = displayMetrics.widthPixels, warmHeight = displayMetrics.heightPixels;
        Thread warmUp = new Thread(() -> {
            com.example.amazonMages.graphics.EnemySpriteRenderer.preload(appContext);
            warmTilemap.prefetchAround(warmX, warmY, warmWidth, warmHeight);
        }, "asset-warmup");
        warmUp.setDaemon(true);
        warmUp.start();
        saveCoordinator = new com.example.amazonMages.save.SaveCoordinator(
            saves, inventory, progression, questJournal, player, pickupWorld, weapons, itemRegistry);
        saveCoordinator.setWorldGeometry(world);
        saveCoordinator.setSkillTree(skillTree);
        questJournal.register(new QuestDefinition(INTRO_QUEST, "Ecos do Pantano",
            "enemy.acid_ghost.defeated", 3, "unlock.arcane_lore_intro"));
        questJournal.register(new QuestDefinition(SENTINEL_QUEST, "Sentinela do Pantano",
            EnemyTemplates.SWAMP_SENTINEL.defeatEvent, 1, "sentinel.defeated"));
        questJournal.register(new QuestDefinition(BOITATA_ENCOUNTER, "Boitatá",
            EnemyTemplates.BOITATA.defeatEvent, 1, "boitata.defeated"));
        questJournal.offer(BOITATA_ENCOUNTER);
        questJournal.accept(BOITATA_ENCOUNTER);
        eventBus.subscribe(event -> questJournal.onEvent(event.id));
        eventBus.subscribe(event -> {
            if ("encounter.swamp_sentinel.requested".equals(event.id)) {
                if (questJournal.state(SENTINEL_QUEST) == QuestState.LOCKED) {
                    questJournal.offer(SENTINEL_QUEST); questJournal.accept(SENTINEL_QUEST);
                }
                if (questJournal.state(SENTINEL_QUEST) == QuestState.ACTIVE) {
                    int tileSize = com.example.amazonMages.map.WorldDefinition.TILE_SIZE;
                    startBossEncounter(EnemyTemplates.SWAMP_SENTINEL, 58 * tileSize, 124 * tileSize);
                }
            }
        });
        eventBus.subscribe(event -> {
            if (event.id.startsWith("enemy.")) sound("ghost_defeat");
        });
        pickupSystem.setListener((item,id,quantity) -> eventBus.publish(new GameEvent("item."+item+".collected",id)));
        enemyManager.setDropListener((template, x, y) -> {
            if (template == EnemyTemplates.BOITATA) {
                dropBoitataLoot(x, y);
                return;
            }
            int xp = template == null ? 15 : template.experienceReward;
            int tier = template == null ? 0 : template.xpTier;
            double danger = tilemap.world().dangerFactorAt(x, y);
            if (danger >= 0.90) {
                xp = (int) Math.round(xp * 1.35);
            }
            xpCrystals.spawn(x, y, xp, tier);
            com.example.amazonMages.items.DropTable.forEnemy(template == null ? "acid_ghost" : template.id)
                .roll(coconutRandom, (item, quantity) -> spawnPickup(item, quantity, x, y));
            if (danger >= 1.05 && coconutRandom.nextFloat() < 0.25f) {
                com.example.amazonMages.items.DropTable.forEnemy(template == null ? "acid_ghost" : template.id)
                    .roll(coconutRandom, (item, quantity) -> spawnPickup(item, quantity, x, y));
            }
        });
        enemyManager.setDetailedKillListener((event, id) -> {
            eventBus.publish(new GameEvent(event, id));
            if (EnemyTemplates.BOITATA.defeatEvent.equals(event)) saveNow();
        });
        enemyManager.setParryListener(this::onParryRiposte);

        setFocusable(true);
        saves.loadSnapshot((position, stats, quests, items, equipment, pickups) -> {
            boolean legacyWorld = position != null && position.worldRevision < com.example.amazonMages.map.WorldDefinition.REVISION;
            if (position != null) {
                com.example.amazonMages.map.WorldDefinition.Position restored = tilemap.world().safePosition(
                    legacyWorld ? world.spawn.worldX() : position.x,
                    legacyWorld ? world.spawn.worldY() : position.y, player.getRadius());
                player.restorePosition(restored.x, restored.y);
                gameDisplay.update();
                tilemap.prefetchAround(restored.x, restored.y,
                    gameDisplay.DISPLAY_RECT.width(), gameDisplay.DISPLAY_RECT.height());
            }
            questJournal.restore(quests);
            if (questJournal.state(BOITATA_ENCOUNTER) == QuestState.ACTIVE) enemyManager.placeBoitata();
            skillTree.restore(quests);
            progression.restore(stats);
            lastObservedLevel = progression.level;
            applyProgressionEffects();
            if (position != null) player.setHealthPoint(position.health);
            Map<String, Integer> loaded = new LinkedHashMap<>();
            for (InventoryItemEntity row : items) loaded.put(row.itemId, row.quantity);
            boolean freshSave = loaded.isEmpty();
            inventory.restore(loaded);
            if (freshSave) {
                inventory.add("coco", 10);
                inventory.add("erva", 3);
                inventory.add("mana", 2);
            } else if (inventory.count("coco") == 0) {
                inventory.add("coco", 1);
            }
            saveCoordinator.restoreWorld(equipment, pickups, legacyWorld);
            if (inventory.count("cajado") > 0) arsenal.unlockLeafFromStaff();
            if (questJournal.state(SENTINEL_QUEST) == QuestState.ACTIVE)
                eventBus.publish(new GameEvent("encounter.swamp_sentinel.requested",0));
            saveCoordinator.ready();
            if (legacyWorld) saveCoordinator.save();
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
        if (player.getHealthPoint() <= 0) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (gameOver.isRespawnPressed(event.getX(), event.getY())) {
                    respawnPlayer();
                }
            }
            return true;
        }
        if (!states.acceptsInput() || states.state() == com.example.amazonMages.state.GameStateManager.State.DIALOGUE) {
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            resetInput();
            return true;
        }
        if (upgradePanel.isOpen()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                com.example.amazonMages.combat.SurvivorArsenal.UpgradeOption choice =
                    upgradePanel.touch(event.getX(), event.getY(), getWidth(), getHeight());
                if (choice != null) {
                    int heal = arsenal.apply(choice.kind, progression);
                    if (choice.kind == com.example.amazonMages.combat.SurvivorArsenal.UpgradeKind.LEAF
                            && inventory.count("cajado") == 0) {
                        inventory.add("cajado", 1);
                        weapons.equip(1, com.example.amazonMages.items.ItemCatalog.STAFF);
                    }
                    applyProgressionEffects();
                    if (heal > 0) player.heal(heal);
                    upgradePanel.advance(arsenal, coconutRandom);
                    if (!upgradePanel.isOpen()) states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
                    saveNow();
                }
            }
            return true;
        }
        if (explorationPanel.isOpen()) {
            explorationPanel.onTouch(event, getWidth(), getHeight());
            if (!explorationPanel.isOpen()) {
                showWorldControls(true);
                states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
            }
            return true;
        }
        if (inventoryPanel.isOpen()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                int action = inventoryPanel.touch(event.getX(), event.getY(), getWidth(), getHeight());
                if (action == com.example.amazonMages.ui.InventoryPanel.ACTION_CLOSE) {
                    states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
                } else if (action == com.example.amazonMages.ui.InventoryPanel.ACTION_CRAFT) {
                    com.example.amazonMages.items.CraftingSystem.Recipe recipe = inventoryPanel.selectedRecipe();
                    int made = inventoryPanel.craftSelected(inventory);
                    if (made > 0) {
                        sound("coconut_storm_cast");
                        saveNow();
                        inventoryNotice = "Criado: " + recipe.title + " x" + made + "!";
                    } else {
                        inventoryNotice = "Recursos insuficientes ou mochila cheia.";
                    }
                    inventoryNoticeTicks = 120;
                } else {
                    ItemDefinition item = inventoryPanel.selected();
                    if (action == com.example.amazonMages.ui.InventoryPanel.ACTION_USE) {
                        boolean applied = consumables.useInCombat(
                            inventory, item, player, progression, skillTree, coconutRandom);
                        if (applied) {
                            if ("bomba_coco".equals(item.id)) detonatePororocaBomb();
                            applyProgressionEffects();
                            saveNow();
                            inventoryNotice = "runa_ancestral".equals(item.id)
                                ? "Runa usada! +1 Ponto de Habilidade!"
                                : "Item utilizado.";
                        } else {
                            inventoryNotice = "O item nao pode ser usado agora.";
                        }
                        inventoryNoticeTicks = 120;
                    } else if (action >= 0 && item.weapon != null && inventory.count(item.id) > 0) {
                        weapons.equip(action, item.weapon);
                        weapons.select(action);
                        saveNow();
                        inventoryNotice = "Arma equipada no Slot " + (action + 1) + ".";
                        inventoryNoticeTicks = 120;
                    }
                }
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && !attributesPanel.isOpen()) {
            int nav = forestHud.navigationAt(event.getX(), event.getY(), getWidth(), hasDeveloperChat());
            if (nav == com.example.amazonMages.gamepanel.ForestHud.NAV_CHAT) {
                resetInput();
                post(() -> {
                    if (developerChatListener != null) developerChatListener.run();
                });
                return true;
            }
            if (nav == com.example.amazonMages.gamepanel.ForestHud.NAV_ATTRIBUTES) {
                attributesPanel.toggle();
                showWorldControls(false);
                states.modal(com.example.amazonMages.state.GameStateManager.State.ATTRIBUTES);
                resetInput();
                return true;
            } else if (nav == com.example.amazonMages.gamepanel.ForestHud.NAV_INVENTORY) {
                resetInput(); inventoryPanel.open();
                states.modal(com.example.amazonMages.state.GameStateManager.State.INVENTORY);
                return true;
            } else if (nav == com.example.amazonMages.gamepanel.ForestHud.NAV_CRAFT) {
                resetInput(); inventoryPanel.openCraft();
                states.modal(com.example.amazonMages.state.GameStateManager.State.INVENTORY);
                return true;
            } else if (nav == com.example.amazonMages.gamepanel.ForestHud.NAV_MAP) {
                explorationPanel.openMap();
                showWorldControls(false);
                states.modal(com.example.amazonMages.state.GameStateManager.State.INVENTORY);
                resetInput();
                return true;
            } else if (nav == com.example.amazonMages.gamepanel.ForestHud.NAV_QUESTS) {
                explorationPanel.openQuests();
                states.modal(com.example.amazonMages.state.GameStateManager.State.INVENTORY);
                resetInput();
                return true;
            }
            int slot = forestHud.hotbarSlotAt(event.getX(), event.getY(), getHeight());
            if (slot >= 0) {
                weapons.select(slot);
                return true;
            }
        }
        if (attributesPanel.isOpen()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (attributesPanel.touch(event.getX(), event.getY(), getWidth(), getHeight(), progression)) {
                    applyProgressionEffects();
                    if (!attributesPanel.isOpen()) {
                        showWorldControls(true);
                        states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
                    }
                    saveNow();
                }
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && guideChatListener != null
                && guide.canTalkAt(event.getX(), event.getY(), gameDisplay, getWidth(), getHeight())) {
            resetInput();
            guide.facePlayer();
            states.modal(com.example.amazonMages.state.GameStateManager.State.DIALOGUE);
            showWorldControls(false);
            sound("dialogue");
            guideChatListener.run();
            return true;
        }
        return input.touch(event, getWidth(), getHeight()) || super.onTouchEvent(event);
    }

    void setGuideChatListener(Runnable listener) { guideChatListener = listener; }
    GuideConversation guideConversation() { return guideConversation; }
    synchronized void setGuideBlue(boolean blue) { guide.setBlue(blue); }

    synchronized void closeGuideChat() {
        states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
        showWorldControls(true);
        resetInput();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.d("Game.java", "surfaceCreated()");
        requestFrameRate(holder);
        startIfReady();
    }

    /**
     * The loop simulates at 60 Hz and presents at 60 or 30 FPS. On a 90/120 Hz panel (e.g. Moto G52) frames then
     * alternate between 1 and 2 refresh periods, which reads as stutter; telling the compositor the
     * content is 60 fps lets the display pick a matching refresh rate.
     */
    private void requestFrameRate(SurfaceHolder holder) {
        if (android.os.Build.VERSION.SDK_INT >= 30 && holder.getSurface().isValid()) {
            holder.getSurface().setFrameRate(60f / frameInterval,
                android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
        }
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
        requestFrameRate(holder);
        // With a reduced render resolution the callback reports the (smaller) buffer; the game
        // lays out and receives touches in view coordinates.
        int viewWidth = getWidth() > 0 ? getWidth() : width;
        int viewHeight = getHeight() > 0 ? getHeight() : height;
        joystick.layout(viewWidth,viewHeight);
        aimStick.layoutAim(viewWidth,viewHeight);
        gameDisplay.resize(viewWidth,viewHeight);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.d("Game.java", "surfaceDestroyed()");
        gameLoop.stopLoop();
        synchronized (this) { states.pause(); resetInput(); }
    }

    @Override
    public synchronized void draw(Canvas canvas) {
        super.draw(canvas);

        int worldSave = canvas.save();
        float zoom = (float) gameDisplay.getZoom();
        canvas.scale(zoom, zoom, getWidth() * .5f, getHeight() * .5f);
        renderer.draw(canvas);
        float chargeRatio = input.currentAimHoldTicks() / (float) Math.max(1, skillTree.chargeTicksRequired());
        aimGuide.draw(canvas, gameDisplay, player, aimStick.getIsPressed(),
            aimStick.magnitude() >= InputController.AIM_DEADZONE, chargeRatio);
        coconutStormFx.draw(canvas, gameDisplay);
        if (coconutStormTicks > 0) {
            hudPaint.setColor(0x66D9B86C);
            canvas.drawCircle((float) gameDisplay.gameToDisplayCoordinatesX(stormX),
                (float) gameDisplay.gameToDisplayCoordinatesY(stormY), 125, hudPaint);
        }
        canvas.restoreToCount(worldSave);

        // Desenha os painéis do jogo
        joystick.draw(canvas);
        aimStick.drawBase(canvas);
        if (debugOverlay && isDebuggable()) drawDebugOverlay(canvas);
        int quickItemCount = inventory.count("erva") + inventory.count("mana")
            + inventory.count("bencao") + inventory.count("elixir")
            + inventory.count("tonico") + inventory.count("bomba_coco")
            + Math.max(0, inventory.count("coco") - 1);
        boolean dashUnlocked = skillTree.rank("passo_curupira") > 0 || progression.level >= 2;
        boolean specialUnlocked = progression.level >= 3 || skillTree.rank("furia_castanheira") > 0;
        forestHud.draw(canvas, getWidth(), getHeight(), player, progression, weapons,
            weapons.specialCooldown() == 0 && progression.mana >= 30 && weapons.current() != null,
            player.getParryCooldownTicks(), hasDeveloperChat(),
            enemyManager.waveDirector().survivalTicks(),
            enemyManager.waveDirector().waveNumber(),
            enemyManager.getKillCount(), hudBanner,
            inventory.count("coco"), quickItemCount,
            dashUnlocked, specialUnlocked);
        forestHud.drawAimKnob(canvas, aimStick.getKnobX(), aimStick.getKnobY(),
            aimStick.getKnobRadius() * 2f);
        drawBossHud(canvas);
        attributesPanel.draw(canvas, getWidth(), getHeight(), progression, forestHud);
        inventoryPanel.draw(canvas, getWidth(), getHeight(), inventory, weapons, inventoryNotice, forestHud);
        explorationPanel.draw(canvas, getWidth(), getHeight(), tilemap, player, questJournal,
            INTRO_QUEST, SENTINEL_QUEST, forestHud);
        upgradePanel.draw(canvas, getWidth(), getHeight(), progression.level, forestHud);
        if (!inventoryPanel.isOpen() && !upgradePanel.isOpen()
                && !attributesPanel.isOpen() && !explorationPanel.isOpen()
                && inventoryNotice != null) {
            float nx = Math.max(getWidth() * .055f, forestHud.pixelScale() * 34f);
            float nSize = Math.min(getWidth(), getHeight()) * .026f;
            forestHud.uiKit().drawTextShadow(canvas, inventoryNotice, nx, getHeight() * .39f,
                nSize, 0xFFFFF1B0, true, Paint.Align.LEFT);
        }

        // Desenha "Game Over" se o jogador estiver morto
        if (player.getHealthPoint() <= 0) {
            gameOver.draw(canvas, getWidth(), getHeight());
        }
    }


    public synchronized void update() {
        if (debugOverlay && isDebuggable()) deviceMonitor.sampleIfDue();

        // Para de atualizar o jogo se o jogador estiver morto
        if (player.getHealthPoint() <= 0) {
            player.advanceDeathAnimation();
            states.gameOver();
            return;
        }
        if (!states.updatesWorld()) {
            return;
        }

        // Atualiza o estado do jogo
        if (hudBannerTicks > 0 && --hudBannerTicks == 0) hudBanner = null;
        if (inventoryNoticeTicks > 0 && --inventoryNoticeTicks == 0) inventoryNotice = null;
        if (manualComboResetTicks > 0 && --manualComboResetTicks == 0) manualComboStep = 0;
        input.updateAimHold();
        joystick.update();
        aimStick.update();
        player.update();
        guide.update();
        if (player.pollAegisBurst()) {
            double burstDmg = 5.0 * Math.max(progression.magicMultiplier(), progression.physicalMultiplier());
            enemyManager.parryShockwave(player.getPositionX(), player.getPositionY(), 190.0, burstDmg, 48);
            progression.mana = Math.min(progression.maxMana, progression.mana + 15.0);
            showBanner("MANTO DA CASTANHEIRA!", 90);
            sound("coconut_storm_hit");
        }
        tilemap.update();
        pickupLifetime.update(pickupWorld,1);
        foragingSystem.update(player.getPositionX(), player.getPositionY(),
            player.getHealthPoint(), player.getMaximumHealth(),
            inventory, progression, skillTree, pickupWorld, tilemap.world(), coconutRandom,
            this::spawnPickup);

        int collected = pickupSystem.collect(pickupWorld, player.getPositionX(),
            player.getPositionY(), player.getRadius(), inventory);
        if (collected > 0) {
            if (inventory.count("cajado") > 0) arsenal.unlockLeafFromStaff();
            if (skillTree.hasFloraSymphony()) {
                arsenal.reduceAllCooldowns(24);
                weapons.reduceSpecialCooldown(24);
            }
            inventoryNotice = "Itens coletados: " + collected;
            inventoryNoticeTicks = 120;
            saveInventory();
        }
        int transmuted = foragingSystem.transmuteFullStackPickups(pickupWorld,
            player.getPositionX(), player.getPositionY(), player.getRadius(), inventory, itemRegistry);
        if (transmuted > 0) {
            progression.mana = Math.min(progression.maxMana, progression.mana + transmuted * 12.0);
            progression.gainExperience(transmuted * 8L);
            inventoryNotice = "Mana absorvida!";
            inventoryNoticeTicks = 90;
        } else if (pickupSystem.blockedByCapacity()) {
            inventoryNotice = "Inventario cheio para este item.";
            inventoryNoticeTicks = 120;
        }

        xpCrystals.updateAndCollect(player.getPositionX(), player.getPositionY(), player.getRadius(),
            arsenal.magnetRadius(progression), (xp, tier) -> {
                progression.gainExperience(xp);
                progression.mana = Math.min(progression.maxMana, progression.mana + 4.0);
                if (tier >= 2) player.heal(1);
                if (skillTree.hasFloraSymphony()) {
                    arsenal.reduceAllCooldowns(12);
                    weapons.reduceSpecialCooldown(12);
                }
            });

        weapons.update();
        progression.regenerateMana();
        if (tilemap != null && tilemap.world().isInsideSanctuary(player.getPositionX(), player.getPositionY())) {
            int ticks = enemyManager.waveDirector().survivalTicks();
            if (ticks % 300 == 0 && player.getHealthPoint() < player.getMaximumHealth()) {
                player.setHealthPoint(player.getHealthPoint() + 1);
            }
            progression.mana = Math.min(progression.maxMana, progression.mana + 0.04);
        }
        updateCoconutStorm();
        updatePororocaVortex();
        updateCurupiraEmbers();
        coconutStormFx.update((x, y, damageMultiplier) -> {
            enemyManager.coconutImpact(x, y, 48, damageMultiplier * stormPowerMultiplier);
            sound("coconut_storm_hit");
        });

        // Coordinate wave director, AI updates, horde separation, melee/parry and staggered ranged attacks.
        enemyManager.update();
        String announcement = enemyManager.waveDirector().pollAnnouncement();
        if (announcement != null) showBanner(announcement, 150);
        if (player.getHealthPoint() <= 0) { resetInput(); states.gameOver(); return; }

        // Vampire Survivors smart auto-aim:
        // Level 1: front-facing only (manual stick or player direction).
        // Level 2: adaptive forward arc.
        // Level 3+: omni 360-degree auto-aim against nearest target.
        Enemy nearestTarget = enemyManager.findNearestEnemy(player.getPositionX(), player.getPositionY(), 640);
        if (!aimStick.getIsPressed()) {
            if (arsenal.coconutLevel() >= 3 && nearestTarget != null) {
                player.aim(nearestTarget.getPositionX() - player.getPositionX(),
                    nearestTarget.getPositionY() - player.getPositionY());
            } else if (arsenal.coconutLevel() == 2 && nearestTarget != null) {
                double dx = nearestTarget.getPositionX() - player.getPositionX();
                double dy = nearestTarget.getPositionY() - player.getPositionY();
                double dot = dx * player.getDirectionX() + dy * player.getDirectionY();
                if (dot > 0) {
                    player.aim(dx, dy);
                } else {
                    player.aim(player.getDirectionX(), player.getDirectionY());
                }
            } else {
                player.aim(player.getDirectionX(), player.getDirectionY());
            }
        }
        arsenal.update(player.getPositionX(), player.getPositionY(),
            nearestTarget != null || aimStick.getIsPressed(), progression,
            new com.example.amazonMages.combat.SurvivorArsenal.AutoCastSink() {
                @Override public void fireCoconutFan(int count, double damageMultiplier) {
                    double expRadius = arsenal.pollExplosiveVolley() ? arsenal.activeExplosiveRadius() : 0.0;
                    double reserveBonus = inventory.count("coco") > 1 ? 1.10 : 1.0;
                    spawnFanProjectiles(true, count, 1, damageMultiplier * reserveBonus, 0.22, expRadius);
                    sound("attack");
                }
                @Override public void fireLeafBurst(int count, int pierce, double damageMultiplier) {
                    spawnFanProjectiles(false, count, pierce, damageMultiplier, 0.16, 0.0);
                    sound("attack");
                }
                @Override public void pulseOrbDamage(double orbX, double orbY, double radius, double damageMultiplier) {
                    enemyManager.applyAreaDamage(orbX, orbY, radius,
                        new DamageEvent(damageMultiplier, 12, DamageType.MAGIC), 7.5, 0);
                }
                @Override public void strikeLightningTarget(double damageMultiplier, double areaRadius) {
                    Enemy target = enemyManager.findHighestHealthEnemy(player.getPositionX(), player.getPositionY(), 520);
                    if (target == null) return;
                    arsenal.addStrikeFx(target.getPositionX(), target.getPositionY());
                    enemyManager.applyAreaDamage(target.getPositionX(), target.getPositionY(), areaRadius,
                        new DamageEvent(damageMultiplier, 30, DamageType.MAGIC), 3.0, 36);
                    sound("coconut_storm_hit");
                }
            });

        // Manual quick-shots support 3-hit combos and Charged Aim release volleys.
        while (input.consumeShot()) {
            arsenal.onManualShotFired();
            int heldTicks = input.consumeLastHoldTicks();
            boolean charged = heldTicks >= skillTree.chargeTicksRequired();
            if (charged || weapons.basic(progression.castSpeed())) {
                sound("attack");
                player.playAction(com.example.amazonMages.graphics.AnimationAction.ATTACK);
                boolean coconut = weapons.current() == WeaponLoadout.COCONUT;
                double reserveBonus = coconut && inventory.count("coco") > 1 ? 1.15 : 1.0;
                double baseMult = (coconut ? progression.physicalMultiplier() : progression.magicMultiplier()) * reserveBonus;
                if (charged) {
                    manualComboStep = 0;
                    int count = 3 + skillTree.chargedBonusProjectiles();
                    double expRadius = coconut ? Math.max(68.0, skillTree.explosiveCoconutRadius()) : 0.0;
                    spawnFanProjectiles(coconut, count, coconut ? 2 : 4, baseMult * 1.65, 0.20, expRadius);
                    showBanner("DISPARO CARREGADO!", 45);
                } else {
                    manualComboStep = (manualComboStep % 3) + 1;
                    manualComboResetTicks = 55;
                    boolean finisher = manualComboStep == 3;
                    double stepMult = finisher ? skillTree.comboFinisherBonus() : (manualComboStep == 2 ? 1.08 : 1.0);
                    int count = finisher ? (coconut ? 3 : 2) : 1;
                    int pierce = finisher ? (coconut ? 2 : 4) : (coconut ? 1 : 2);
                    double expRadius = finisher && coconut ? skillTree.explosiveCoconutRadius() : 0.0;
                    spawnFanProjectiles(coconut, count, pierce, baseMult * stepMult, 0.18, expRadius);
                }
            }
        }
        projectileWorld.update();
        enemyManager.collideWithPlayerProjectiles(projectileWorld);

        if (progression.level > lastObservedLevel && !upgradePanel.isOpen() && !enemyManager.isBoitataFalling()) {
            int gained = progression.level - lastObservedLevel;
            lastObservedLevel = progression.level;
            resetInput();
            upgradePanel.queueLevelUps(gained, arsenal, coconutRandom);
            states.modal(com.example.amazonMages.state.GameStateManager.State.ATTRIBUTES);
        }

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
        com.example.amazonMages.items.WorldForagingSystem.recycleIfCrowded(
            pickupWorld, player.getPositionX(), player.getPositionY());
        if (pickupWorld.size() >= 64) {
            inventoryNotice = "Limite de itens no chao atingido.";
            inventoryNoticeTicks = 90;
            return;
        }
        placePickup(itemId, quantity, x, y, new LifetimeComponent(36000));
    }

    private void dropBoitataLoot(double x, double y) {
        String[] items = {ItemCatalog.BOITATA_SKIN, ItemCatalog.BOITATA_FANG, ItemCatalog.BOITATA_EYE};
        for (int i = 0; i < items.length; i++) {
            placePickup(items[i], 1, x + (i - 1) * 57, y + 26,
                new LifetimeComponent(LifetimeComponent.PERMANENT, BoitataBoss.DEFEAT_TICKS));
        }
        progression.gainExperience(EnemyTemplates.BOITATA.experienceReward);
    }

    private void placePickup(String itemId, int quantity, double x, double y, LifetimeComponent life) {
        com.example.amazonMages.map.WorldDefinition.Position safe = tilemap.world().safePosition(x, y, 13);
        pickupWorld.create()
            .add(TransformComponent.class, new TransformComponent(safe.x, safe.y))
            .add(ColliderComponent.class, new ColliderComponent(13))
            .add(PickupComponent.class, new PickupComponent(itemId, quantity))
            .add(LifetimeComponent.class, life);
    }

    private void applyProgressionEffects() {
        player.getCombatStats().setResistance(DamageType.PHYSICAL,
            Math.min(.40, (progression.resistencia - 1) * .025));
        player.getCombatStats().setResistance(DamageType.ACID,
            Math.min(.25, (progression.arcano - 1) * .015));
        player.setMaximumHealth(progression.maximumHealth());
        progression.maxMana = progression.manaCapacity();
        progression.mana = Math.min(progression.mana, progression.maxMana);
        player.configureDefense(
            skillTree.bonusParryTicks(),
            skillTree.guardReduction(),
            skillTree.bonusPosture(),
            skillTree.dashCooldownTicks(),
            skillTree.hasLivingAegis()
        );
    }

    private void saveInventory() { saveNow(); }

    private void saveQuestProgress() { saveNow(); }

    private void spawnFanProjectiles(boolean coconut, int count, int pierce,
                                     double damageMultiplier, double spreadStep, double explosiveRadius) {
        double dx = player.getAttackDirectionX(), dy = player.getAttackDirectionY();
        if (dx == 0 && dy == 0) dy = 1;
        double baseAngle = Math.atan2(dy, dx);
        double half = (count - 1) * 0.5;
        for (int i = 0; i < count; i++) {
            double angle = baseAngle + (i - half) * spreadStep;
            projectileWorld.spawn(new Spell(getContext(), player, coconut, damageMultiplier,
                Math.cos(angle), Math.sin(angle), pierce, explosiveRadius));
        }
    }

    private void onParryRiposte(double sourceX, double sourceY) {
        progression.mana = Math.min(progression.maxMana, progression.mana + arsenal.parryManaRestore());
        weapons.reduceSpecialCooldown(90);
        enemyManager.parryShockwave(player.getPositionX(), player.getPositionY(),
            arsenal.parryRadius(), arsenal.parryDamage(progression), arsenal.parryStunTicks());
        double dx = sourceX - player.getPositionX(), dy = sourceY - player.getPositionY();
        double len = Math.hypot(dx, dy);
        double baseAngle = len > 0.01 ? Math.atan2(dy, dx) : Math.atan2(player.getAttackDirectionY(), player.getAttackDirectionX());
        if (arsenal.parryLevel() >= 2) {
            for (int i = -1; i <= 1; i++) {
                double a = baseAngle + i * 0.22;
                projectileWorld.spawn(new Spell(getContext(), player, false,
                    progression.magicMultiplier() * 1.4, Math.cos(a), Math.sin(a), 4));
            }
        } else {
            projectileWorld.spawn(new Spell(getContext(), player, false,
                progression.magicMultiplier() * 1.5, Math.cos(baseAngle), Math.sin(baseAngle), 3));
        }
        showBanner("PARRY DE FOLHA! +MANA", 80);
        sound("coconut_storm_hit");
    }

    private void showBanner(String text, int durationTicks) {
        hudBanner = text;
        hudBannerTicks = durationTicks;
    }

    private void activateEquippedSpecial() {
        if (progression.level < 3 && skillTree.rank("furia_castanheira") <= 0) {
            showBanner("ESPECIAL BLOQUEADO (Requer Nv. 3 ou Furia)", 45);
            return;
        }
        if (progression.mana < 30 || weapons.current() == null) return;
        if (!weapons.castEquippedSpecial()) return;
        player.playAction(com.example.amazonMages.graphics.AnimationAction.SPECIAL);
        progression.spendMana(30);
        sound("coconut_storm_cast");

        double dx = player.getAttackDirectionX(), dy = player.getAttackDirectionY();
        if (dx == 0 && dy == 0) dy = 1;
        double durMult = skillTree.specialDurationMultiplier();

        if (weapons.current() == WeaponLoadout.COCONUT) {
            boolean boosted = inventory.count("coco") > 1 && inventory.remove("coco", 1);
            stormPowerMultiplier = boosted ? 1.30 : 1.0;
            coconutStormTicks = (int) Math.round(150 * durMult);
            coconutStormFx.clear();
            stormX = player.getPositionX() + dx * 180;
            stormY = player.getPositionY() + dy * 180;
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                projectileWorld.spawn(new Spell(getContext(), player, false,
                    progression.magicMultiplier() * 1.35, Math.cos(a), Math.sin(a), 3));
            }
            showBanner(boosted ? "TEMPESTADE DE COCOS!" : "TEMPESTADE!", 70);
        } else {
            vortexX = player.getPositionX() + dx * 170;
            vortexY = player.getPositionY() + dy * 170;
            vortexRadius = 155.0;
            vortexTicks = (int) Math.round(180 * durMult);
            combatFxState.vortexX = vortexX;
            combatFxState.vortexY = vortexY;
            combatFxState.vortexRadius = vortexRadius;
            combatFxState.vortexTicks = vortexTicks;
            showBanner("VORTICE DAS AGUAS!", 75);
        }
        saveNow();
    }

    private void activatePlayerParry() {
        player.beginParry();
    }

    private void activatePlayerDash() {
        if (skillTree.rank("passo_curupira") <= 0 && progression.level < 2) {
            showBanner("DASH BLOQUEADO (Requer Nv. 2 ou Passo do Curupira)", 45);
            return;
        }
        if (!player.beginDash()) return;
        sound("attack");
        if (skillTree.hasCurupiraTrail()) {
            double ux = player.getAttackDirectionX(), uy = player.getAttackDirectionY();
            if (Math.hypot(joystick.getActuatorX(), joystick.getActuatorY()) >= 0.15) {
                double len = Math.hypot(joystick.getActuatorX(), joystick.getActuatorY());
                ux = joystick.getActuatorX() / len;
                uy = joystick.getActuatorY() / len;
            } else if (ux == 0 && uy == 0) {
                uy = 1;
            }
            for (int i = 0; i < 3; i++) {
                combatFxState.embers.add(new GameRenderer.DashEmber(
                    player.getPositionX() - ux * i * 36.0,
                    player.getPositionY() - uy * i * 36.0,
                    90
                ));
            }
        }
    }

    private void activateQuickItem() {
        String feedback = consumables.quickUse(
            inventory, itemRegistry, player, progression, skillTree, coconutRandom);
        if (feedback != null) {
            if ("BOMBA DE POROROCA!".equals(feedback)) detonatePororocaBomb();
            applyProgressionEffects();
            saveNow();
            showBanner(feedback, 65);
            sound("attack");
        } else {
            showBanner("Sem item necessario agora.", 45);
        }
    }

    private void detonatePororocaBomb() {
        double dmg = 9.0 * Math.max(progression.magicMultiplier(), progression.physicalMultiplier());
        enemyManager.parryShockwave(player.getPositionX(), player.getPositionY(), 240.0, dmg, 60);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            projectileWorld.spawn(new Spell(getContext(), player, true,
                progression.physicalMultiplier() * 1.45, Math.cos(a), Math.sin(a), 2, 92.0));
        }
        weapons.reduceSpecialCooldown(WeaponLoadout.SPECIAL_COOLDOWN_TICKS);
        showBanner("BOMBA DE POROROCA!", 75);
        sound("coconut_storm_hit");
    }

    private void updatePororocaVortex() {
        if (vortexTicks <= 0) {
            combatFxState.vortexTicks = 0;
            return;
        }
        vortexTicks--;
        combatFxState.vortexTicks = vortexTicks;
        DamageEvent pulse = (vortexTicks % 20 == 0)
            ? new DamageEvent(progression.magicMultiplier() * 1.15, 22, DamageType.MAGIC)
            : null;
        int root = pulse != null ? 16 : 0;
        enemyManager.pullAndDamageVortex(vortexX, vortexY, vortexRadius, 4.2, pulse, root);
        if (pulse != null) sound("coconut_storm_hit");
    }

    private void updateCurupiraEmbers() {
        if (combatFxState.embers.isEmpty()) return;
        double mult = progression.magicMultiplier() * (1.0 + skillTree.curupiraTrailRank() * 0.35);
        for (int i = combatFxState.embers.size() - 1; i >= 0; i--) {
            GameRenderer.DashEmber ember = combatFxState.embers.get(i);
            ember.ticksRemaining--;
            if (ember.ticksRemaining % 18 == 0) {
                enemyManager.applyAreaDamage(ember.x, ember.y, 48.0,
                    new DamageEvent(mult * 0.85, 14, DamageType.MAGIC), 1.5, 18);
            }
            if (ember.ticksRemaining <= 0) combatFxState.embers.remove(i);
        }
    }

    private void updateCoconutStorm() {
        if (coconutStormTicks <= 0) return;
        coconutStormTicks--;
        if (coconutStormTicks % 18 == 0) {
            double angle = coconutRandom.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(coconutRandom.nextDouble()) * 125;
            coconutStormFx.schedule(stormX + Math.cos(angle) * distance,
                stormY + Math.sin(angle) * distance, progression.physicalMultiplier());
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
        forestHud.drawBossBar(canvas, left, top, width, (float) boss.getHealthRatio());
        hudPaint.setColor(0xFFFFFFFF);
        hudPaint.setTextSize(15 * getResources().getDisplayMetrics().density);
        String phase = boss instanceof BoitataBoss
            ? (boss.getPhase() == 0 ? "Chamas de brasa" : "Fogo azul")
            : "Fase " + (boss.getPhase() + 1);
        canvas.drawText(boss.getTemplate().displayName + "  ·  " + phase, left, top - 12, hudPaint);
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
            "\nInimigos " + enemyManager.getEnemyCount() +
            " | projeteis inimigos " + enemyManager.getProjectileCount() +
            " | magias " + projectileWorld.count() +
            "\nJogador " + (int) player.getPositionX() +
            "," + (int) player.getPositionY() +
            "\nCenario em cache: " + tilemap.cachedTerrainBytes() / (1024 * 1024) + "/36 MiB" +
            "\n" + deviceMonitor.snapshot();
    }

    private void drawDebugOverlay(Canvas canvas) {
        String[] lines = diagnostics().split("\\n");
        float density = getResources().getDisplayMetrics().density;
        float lineHeight = 16 * density;
        float left = Math.max(10 * density, forestHud.pixelScale() * 34f), top = 74 * density;
        float right = Math.min(getWidth() - left, left + 390 * density);
        float bottom = top + (lines.length + 1) * lineHeight + 12 * density;
        android.graphics.RectF box = new android.graphics.RectF(left, top, right, bottom);
        forestHud.uiKit().drawPerfOverlay(canvas, box, 224);
        forestHud.uiKit().drawTextShadow(canvas, "DESEMPENHO - AO VIVO",
            left + 10 * density, top + 16 * density, 12.5f * density, 0xFFFFE066, true, Paint.Align.LEFT);
        for (int i = 0; i < lines.length; i++) {
            forestHud.uiKit().drawTextShadow(canvas, lines[i],
                left + 10 * density, top + (i + 2) * lineHeight, 10.5f * density, 0xFFCCF0D2, false, Paint.Align.LEFT);
        }
    }

    /** Explicit save action used by the in-game pause menu. */
    public synchronized void saveNow() { saveCoordinator.save(); }

    /** Revives the player in the Sanctuary courtyard safe zone with full health and mana. */
    public synchronized void respawnPlayer() {
        player.setHealthPoint(player.getMaximumHealth());
        progression.mana = progression.maxMana;
        double spawnX = tilemap.world().spawn.worldX();
        double spawnY = tilemap.world().spawn.worldY();
        player.restorePosition(spawnX, spawnY);
        gameDisplay.update();
        tilemap.prefetchAround(spawnX, spawnY,
            gameDisplay.DISPLAY_RECT.width(), gameDisplay.DISPLAY_RECT.height());
        enemyManager.onPlayerRespawnAtSanctuary(spawnX, spawnY,
            com.example.amazonMages.map.WorldDefinition.SAFE_RADIUS * 64.0);
        states.revive();
        states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
        resetInput();
        showBanner("RENASCIDO NO SANTUARIO DAS FOLHAS!", 120);
        saveNow();
    }

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
        tilemap.release();
        saves.close();
    }

}
