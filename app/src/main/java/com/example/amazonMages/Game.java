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
        }).execute(command);
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
    private final QuestJournal questJournal = new QuestJournal();
    private final PlayerProgression progression = new PlayerProgression();
    private final AttributesPanel attributesPanel = new AttributesPanel();
    private final com.example.amazonMages.ui.ExplorationPanel explorationPanel =
        new com.example.amazonMages.ui.ExplorationPanel();
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
        forestHud = new com.example.amazonMages.gamepanel.ForestHud(context);
        coconutStormFx = new com.example.amazonMages.gamepanel.CoconutStormFx(context);
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
        player = new Player(context, joystick, 2*500, 500, 32, animator);
        saves = new GameSaveRepository(context, slotId);
        effectTarget = new com.example.amazonMages.effects.PlayerEffectTarget(player, progression);
        input = new InputController(player, joystick, aimStick, weapons, this::activateCoconutStorm,
            this::activatePlayerParry);


        // Initialize display and center it around the player
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        gameDisplay = new GameDisplay(displayMetrics.widthPixels, displayMetrics.heightPixels, player);

        // Initialize Tilemap
        tilemap = new Tilemap(spriteSheet);
        enemyManager = new EnemyManager(context, player, tilemap);
        renderer = new GameRenderer(tilemap, player, enemyManager, projectileWorld, pickupWorld, gameDisplay);
        // Decode/upload the ghost atlases and build the first map chunks off the game thread, so the
        // first enemy spawn and the first frames do not stall on PNG decoding or chunk rendering.
        final Context appContext = context.getApplicationContext();
        final Tilemap warmTilemap = tilemap;
        final double warmX = player.getPositionX(), warmY = player.getPositionY();
        final int warmWidth = displayMetrics.widthPixels, warmHeight = displayMetrics.heightPixels;
        Thread warmUp = new Thread(() -> {
            com.example.amazonMages.graphics.GhostAtlasRenderer.preload(appContext);
            warmTilemap.prefetchAround(warmX, warmY, warmWidth, warmHeight);
        }, "asset-warmup");
        warmUp.setDaemon(true);
        warmUp.start();
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
                        Math.max(100, Math.min(6300, player.getPositionX()+300)), Math.max(100, Math.min(4380, player.getPositionY()+200)));
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

        setFocusable(true);
        saves.loadSnapshot((position, stats, quests, items, equipment, pickups) -> {
            if (position != null && Double.isFinite(position.x) && Double.isFinite(position.y)) {
                player.restorePosition(position.x, position.y);
                gameDisplay.update();
                tilemap.prefetchAround(position.x, position.y,
                    gameDisplay.DISPLAY_RECT.width(), gameDisplay.DISPLAY_RECT.height());
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
        if (player.getHealthPoint() <= 0) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (gameOver.isRespawnPressed(event.getX(), event.getY())) {
                    respawnPlayer();
                }
            }
            return true;
        }
        if (!states.acceptsInput()) return true;
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            resetInput();
            return true;
        }
        if (explorationPanel.isOpen()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN &&
                explorationPanel.touch(event.getX(), event.getY(), getWidth(), getHeight())) {
                if (!explorationPanel.isOpen()) states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
            }
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
                states.modal(com.example.amazonMages.state.GameStateManager.State.ATTRIBUTES);
                resetInput();
                return true;
            } else if (nav == com.example.amazonMages.gamepanel.ForestHud.NAV_INVENTORY) {
                resetInput(); inventoryPanel.open();
                states.modal(com.example.amazonMages.state.GameStateManager.State.INVENTORY);
                return true;
            } else if (nav == com.example.amazonMages.gamepanel.ForestHud.NAV_MAP) {
                explorationPanel.openMap();
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
                    if (!attributesPanel.isOpen()) states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
                    saveNow();
                }
            }
            return true;
        }
        return input.touch(event, getWidth(), getHeight()) || super.onTouchEvent(event);
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
        aimGuide.draw(canvas, gameDisplay, player, aimStick.getIsPressed(),
            aimStick.magnitude() >= InputController.AIM_DEADZONE);
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
        forestHud.draw(canvas, getWidth(), getHeight(), player, progression, weapons,
            weapons.specialCooldown() == 0 && progression.mana >= 35,
            player.getParryCooldownTicks(), hasDeveloperChat());
        forestHud.drawAimKnob(canvas, aimStick.getKnobX(), aimStick.getKnobY(),
            aimStick.getKnobRadius() * 2f);
        drawBossHud(canvas);
        attributesPanel.draw(canvas, getWidth(), getHeight(), progression, forestHud);
        inventoryPanel.draw(canvas, getWidth(), getHeight(), inventory, weapons, inventoryNotice, forestHud);
        explorationPanel.draw(canvas, getWidth(), getHeight(), tilemap, player, questJournal,
            INTRO_QUEST, SENTINEL_QUEST);
        if (!inventoryPanel.isOpen() && inventoryNotice != null)
            canvasUi.text(canvas, inventoryNotice, getWidth()*.04f, getHeight()*.39f, Math.min(getWidth(),getHeight())*.026f, 0xFFFFD17C);

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
        joystick.update();
        aimStick.update();
        player.update();
        tilemap.update();
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
        coconutStormFx.update((x, y, damageMultiplier) -> {
            enemyManager.coconutImpact(x, y, 45, damageMultiplier);
            sound("coconut_storm_hit");
        });

        // Coordinate AI updates, enemy population and staggered ranged attacks.
        enemyManager.update();
        if (player.getHealthPoint() <= 0) { resetInput(); states.gameOver(); return; }

        // Atualiza o estado de todos os feitiços
        while (input.consumeShot()) {
            if (weapons.basic(progression.castSpeed())) {
                sound("attack");
                player.playAction(com.example.amazonMages.graphics.AnimationAction.ATTACK);
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

    private void activateCoconutStorm() {
        if (progression.mana < 35 || !weapons.special()) return;
        player.playAction(com.example.amazonMages.graphics.AnimationAction.SPECIAL);
        progression.spendMana(35);
        sound("coconut_storm_cast");
        saveNow();
        coconutStormTicks = 150;
        coconutStormFx.clear();
        double dx = player.getAttackDirectionX(), dy = player.getAttackDirectionY();
        if (dx == 0 && dy == 0) dy = 1;
        stormX = player.getPositionX() + dx * 210;
        stormY = player.getPositionY() + dy * 210;
    }

    private void activatePlayerParry() {
        player.beginParry();
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
            "\nInimigos " + enemyManager.getEnemyCount() +
            " | projeteis inimigos " + enemyManager.getProjectileCount() +
            " | magias " + projectileWorld.count() +
            "\nJogador " + (int) player.getPositionX() +
            "," + (int) player.getPositionY() +
            "\n" + deviceMonitor.snapshot();
    }

    private void drawDebugOverlay(Canvas canvas) {
        String[] lines = diagnostics().split("\\n");
        float density = getResources().getDisplayMetrics().density;
        float lineHeight = 17 * density;
        float left = 10 * density, top = 68 * density;
        float right = Math.min(getWidth() - left, left + 390 * density);
        float bottom = top + (lines.length + 1) * lineHeight + 12 * density;
        forestHud.drawPanel(canvas, left, top, right, bottom);
        hudPaint.setColor(0xFFFFE09B);
        hudPaint.setTextSize(12 * density);
        hudPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        canvas.drawText("DESEMPENHO - AO VIVO", left + 10 * density,
            top + 17 * density, hudPaint);
        hudPaint.setTypeface(android.graphics.Typeface.create("monospace", 0));
        hudPaint.setColor(0xFFCCF0D2);
        hudPaint.setTextSize(10.5f * density);
        for (int i = 0; i < lines.length; i++)
            canvas.drawText(lines[i], left + 10 * density,
                top + (i + 2) * lineHeight, hudPaint);
        hudPaint.setTypeface(null);

    }

    /** Explicit save action used by the in-game pause menu. */
    public synchronized void saveNow() { saveCoordinator.save(); }

    /** Revives the player at their current location with full health and mana. */
    public synchronized void respawnPlayer() {
        player.setHealthPoint(player.getMaximumHealth());
        progression.mana = progression.maxMana;
        enemyManager.onPlayerRespawn();
        states.revive();
        states.modal(com.example.amazonMages.state.GameStateManager.State.PLAYING);
        resetInput();
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
        saves.close();
    }

}
