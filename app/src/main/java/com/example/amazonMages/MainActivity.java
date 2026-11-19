package com.example.amazonMages;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.res.ResourcesCompat;
import com.example.androidstudio2dgamedevelopment.R;
import com.example.amazonMages.gamepanel.AtlasMenuButtonDrawable;
import com.example.amazonMages.gamepanel.AtlasPanelDrawable;
import com.example.amazonMages.gamepanel.HudZoomDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.EditText;
import android.widget.ScrollView;
import android.content.pm.ApplicationInfo;
import android.content.SharedPreferences;
import com.example.amazonMages.audio.GameAudioManager;
import com.example.amazonMages.ui.MainMenuView;
import com.example.amazonMages.ui.GuideChatDialog;

import com.example.amazonMages.save.GameSaveRepository;
import com.example.amazonMages.save.PlayerPositionEntity;

import java.text.DateFormat;
import java.util.List;
import java.util.Date;

/** Classic main menu with three independent save slots. */
public class MainActivity extends Activity {
    private Game game;
    private final com.example.amazonMages.state.GameStateManager states = new com.example.amazonMages.state.GameStateManager();
    private GameAudioManager audio;
    private GameSaveRepository menuRepository;
    private boolean resumed;
    private AlertDialog pauseDialog;
    private AlertDialog chatDialog;
    private GuideChatDialog guideDialog;
    private int menuGeneration;
    private SharedPreferences gamePreferences;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        menuRepository = new GameSaveRepository(this, 1);
        audio = new GameAudioManager(this);
        gamePreferences = getSharedPreferences("game_settings", MODE_PRIVATE);
        showMainMenu();
    }

    private LinearLayout menu(String heading) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int widthDp = Math.round(getResources().getDisplayMetrics().widthPixels /
            getResources().getDisplayMetrics().density);
        int side = Math.max(28, (widthDp - 560) / 2);
        int pad = dp(16);
        root.setPadding(side, pad, side, pad);
        root.setBackgroundColor(Color.rgb(9, 21, 16));
        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextColor(Color.rgb(255, 241, 176));
        title.setTextSize(26);
        title.setTypeface(pixelTypeface(true));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(12));
        root.addView(title);
        return root;
    }

    private LinearLayout dialogMenu(String heading) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(16));
        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextColor(Color.rgb(255, 241, 176));
        title.setTextSize(20);
        title.setTypeface(pixelTypeface(true));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(6));
        root.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private Typeface pixelTypeface(boolean bold) {
        Typeface font = ResourcesCompat.getFont(this, R.font.pixelify_sans);
        if (font == null) return Typeface.create("monospace", bold ? Typeface.BOLD : Typeface.NORMAL);
        return bold ? Typeface.create(font, Typeface.BOLD) : font;
    }

    private Drawable atlasButtonBackground() {
        AtlasMenuButtonDrawable bg = new AtlasMenuButtonDrawable(this);
        bg.setPixelScale(Math.max(2, Math.min(4, Math.round(getResources().getDisplayMetrics().density))));
        return bg;
    }

    private Button button(LinearLayout root, String label, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.rgb(244, 235, 200));
        button.setTextSize(17);
        button.setAllCaps(false);
        button.setTypeface(pixelTypeface(true));
        button.setMinHeight(dp(54));
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setBackground(atlasButtonBackground());
        button.setOnClickListener(click);
        int screenWidthDp = Math.round(getResources().getDisplayMetrics().widthPixels /
            getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            dp(Math.min(420, screenWidthDp - 72)),
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = dp(8);
        root.addView(button, params);
        return button;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable greenButtonBackground() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(39, 75, 53));
        shape.setCornerRadius(dp(5));
        shape.setStroke(dp(1.5f), Color.rgb(157, 178, 105));
        return shape;
    }

    private Drawable menuPanelBackground() {
        return new AtlasPanelDrawable(this);
    }

    private void showMainMenu() {
        states.menu();
        menuGeneration++;
        setContentView(new MainMenuView(this,
            v -> { audio.play("menu"); showSlots(); },
            v -> { audio.play("menu"); showAudioSettings(); },
            v -> finish()));
    }

    private void showAudioSettings() {
        LinearLayout root = menu("CONFIGURAÇÕES");
        addSectionLabel(root, "ÁUDIO");
        addVolumeSlider(root, "Música", audio.musicVolume(), true);
        addVolumeSlider(root, "Efeitos sonoros", audio.effectsVolume(), false);
        addSectionLabel(root, "CÂMERA");
        TextView zoomValue = new TextView(this);
        zoomValue.setTextColor(Color.rgb(220, 235, 212));
        zoomValue.setTextSize(15);
        int savedZoom = savedZoomFactor() - 1;
        zoomValue.setText("Zoom inicial: " + zoomLabel(savedZoom));
        root.addView(zoomValue);
        SeekBar zoomSlider = new SeekBar(this);
        zoomSlider.setMax(2);
        zoomSlider.setProgress(savedZoom);
        styleSlider(zoomSlider);
        root.addView(zoomSlider);
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                gamePreferences.edit().putInt("camera_zoom_factor", progress + 1).apply();
                zoomValue.setText("Zoom inicial: " + zoomLabel(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        TextView zoomHint = new TextView(this);
        zoomHint.setText("Aplicado ao iniciar uma jogatina. Também é possível mudar durante o jogo.");
        zoomHint.setTextColor(Color.rgb(180, 207, 163));
        zoomHint.setTextSize(13);
        root.addView(zoomHint);
        addSectionLabel(root, "GRÁFICOS");
        addGraphicsControls(root);
        button(root, "Voltar", v -> {
            audio.play("menu");
            if (game == null) showMainMenu();
            else showPauseMenu();
        });
        setMenuContent(root);
    }

    private void addSectionLabel(LinearLayout root, String label) {
        TextView section = new TextView(this);
        section.setText(label);
        section.setTextColor(Color.rgb(229, 206, 142));
        section.setTextSize(14);
        section.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        section.setPadding(0, dp(12), 0, dp(2));
        root.addView(section);
    }

    private String zoomLabel(int step) {
        String[] labels = {"1× · afastado", "2×", "3× · próximo"};
        return labels[Math.max(0, Math.min(labels.length - 1, step))];
    }

    private String formatZoom(double zoom) {
        return Math.round(zoom) + "×";
    }

    /** Saved start-of-game camera zoom: 1, 2 or 3 (times). Defaults to 2x for crisp GBA pixel art. */
    private int savedZoomFactor() {
        return Math.max(1, Math.min(3, gamePreferences.getInt("camera_zoom_factor", 2)));
    }

    private void styleSlider(SeekBar slider) {
        slider.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(137, 194, 105)));
        slider.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(50, 78, 56)));
        slider.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.rgb(226, 203, 132)));
    }

    private void showPauseGraphicsSettings() {
        LinearLayout panel = dialogMenu("GRÁFICOS");
        addGraphicsControls(panel);
        AlertDialog settings = new AlertDialog.Builder(this).setView(panel).create();
        button(panel, "Voltar", v -> settings.dismiss());
        styleDialog(settings);
        settings.show();
    }

    /**
     * Resolution x frame-rate trade-off. A lower render resolution shrinks only the GPU buffer
     * (the screen upscales it), so it is the main lever on GPU-bound devices; 30 FPS halves the
     * frames drawn. Choices are saved and applied immediately when a game is running.
     */
    private void addGraphicsControls(LinearLayout root) {
        com.example.amazonMages.graphics.GraphicsSettings settings =
            com.example.amazonMages.graphics.GraphicsSettings.load(this);
        TextView info = new TextView(this);
        info.setTextColor(Color.rgb(203, 238, 207));
        info.setTextSize(14);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, dp(4), 0, dp(4));
        root.addView(info);
        Button resolution = button(root, "", null);
        Button frameRate = button(root, "", null);
        TextView hint = new TextView(this);
        hint.setText("Resolução menor = mais FPS, com imagem um pouco mais suave. " +
            "30 FPS economiza bateria e esquenta menos.");
        hint.setTextColor(Color.rgb(180, 207, 163));
        hint.setTextSize(13);
        hint.setPadding(0, dp(8), 0, dp(2));
        root.addView(hint);

        Runnable refresh = () -> {
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int percent = settings.resolutionPercent();
            resolution.setText("Resolução: " + percent + "%" + (percent == 100 ? " (nativa)" : ""));
            frameRate.setText("Taxa de quadros: " + settings.fpsCap() + " FPS");
            info.setText("Buffer de render: " + Math.round(metrics.widthPixels * settings.scale()) +
                " × " + Math.round(metrics.heightPixels * settings.scale()) + " px");
        };
        resolution.setOnClickListener(v -> {
            settings.cycleResolution();
            if (game != null) game.applyGraphics(settings);
            refresh.run();
        });
        frameRate.setOnClickListener(v -> {
            settings.cycleFps();
            if (game != null) game.applyGraphics(settings);
            refresh.run();
        });
        refresh.run();
    }

    private void addVolumeSlider(LinearLayout root, String label, float value, boolean music) {
        TextView text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setText(label + ": " + Math.round(value * 100) + "%");
        root.addView(text);
        SeekBar slider = new SeekBar(this);
        slider.setMax(100);
        slider.setProgress(Math.round(value * 100));
        styleSlider(slider);
        root.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                float level = progress / 100f;
                if (music) audio.setMusicVolume(level);
                else audio.setEffectsVolume(level);
                text.setText(label + ": " + progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {
                if (!music) audio.play("menu");
            }
        });
    }

    private void showSlots() {
        LinearLayout root = menu("SUAS JOGATINAS");
        TextView intro = new TextView(this);
        intro.setText("Escolha uma sessão ou apague o progresso para começar de novo.");
        intro.setTextColor(Color.rgb(180, 207, 163));
        intro.setTextSize(14);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(0, 0, 0, dp(8));
        root.addView(intro);
        TextView loading = new TextView(this);
        loading.setText("Carregando saves...");
        loading.setTextColor(Color.rgb(220, 235, 212));
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(12), 0, dp(12));
        root.addView(loading);
        setMenuContent(root);
        int requestGeneration = menuGeneration;
        menuRepository.loadSlots(slots -> {
            if (game != null || menuGeneration != requestGeneration) return;
            root.removeView(loading);
            for (int id = 1; id <= 3; id++) {
                final int slot = id;
                PlayerPositionEntity found = null;
                for (PlayerPositionEntity item : slots) {
                    if (item.slotId == slot) { found = item; break; }
                }
                addSlotRow(root, slot, found);
            }
            button(root, "Voltar", v -> showMainMenu());
        });
    }

    private void addSlotRow(LinearLayout root, int slot, PlayerPositionEntity save) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackground(menuPanelBackground());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            Math.min(dp(500), Math.max(dp(240), getResources().getDisplayMetrics().widthPixels - dp(72))),
            LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER_HORIZONTAL;
        cardParams.topMargin = dp(7);

        TextView title = new TextView(this);
        title.setText("Jogatina " + slot + (save == null ? "  ·  NOVA" : "  ·  SALVA"));
        title.setTextColor(Color.rgb(255, 241, 176));
        title.setTextSize(17);
        title.setTypeface(pixelTypeface(true));
        card.addView(title);

        TextView detail = new TextView(this);
        if (save == null) {
            detail.setText("Espaço livre para uma nova aventura");
        } else {
            detail.setText("Vida " + save.health + "  ·  Atualizada " +
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(save.updatedAt)));
        }
        detail.setTextColor(Color.rgb(180, 207, 163));
        detail.setTextSize(13);
        detail.setTypeface(pixelTypeface(false));
        detail.setPadding(0, dp(2), 0, dp(8));
        card.addView(detail);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Button play = slotActionButton(save == null ? "Começar" : "Continuar", false,
            v -> startGame(slot));
        actions.addView(play, new LinearLayout.LayoutParams(0, dp(48), 1));
        if (save != null) {
            Button delete = slotActionButton("Apagar", true, v -> confirmDeleteSlot(slot));
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(104), dp(48));
            deleteParams.leftMargin = dp(8);
            actions.addView(delete, deleteParams);
        }
        card.addView(actions);
        root.addView(card, cardParams);
    }

    private Button slotActionButton(String label, boolean destructive, View.OnClickListener click) {
        Button action = new Button(this);
        action.setText(label);
        action.setTextColor(Color.rgb(244, 235, 200));
        action.setTextSize(15);
        action.setAllCaps(false);
        action.setTypeface(pixelTypeface(true));
        action.setMinHeight(dp(48));
        action.setPadding(dp(8), 0, dp(8), 0);
        if (destructive) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.rgb(122, 42, 34));
            background.setCornerRadius(dp(2));
            background.setStroke(dp(2), Color.rgb(11, 26, 18));
            action.setBackground(background);
        } else action.setBackground(atlasButtonBackground());
        action.setOnClickListener(click);
        return action;
    }

    private void confirmDeleteSlot(int slot) {
        AlertDialog confirm = new AlertDialog.Builder(this)
            .setTitle("Apagar jogatina " + slot + "?")
            .setMessage("O progresso salvo será apagado. Esta ação não pode ser desfeita.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Apagar", (dialog, which) ->
                menuRepository.deleteSlot(slot, this::showSlots))
            .create();
        styleDialog(confirm);
        confirm.show();
    }

    private void setMenuContent(LinearLayout root) {
        menuGeneration++;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Color.rgb(9, 21, 16));
        scroll.addView(root, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    private void startGame(int slotId) {
        game = new Game(this, slotId, states);
        game.setGuideBlue(gamePreferences.getBoolean("guide_blue", false));
        game.setGuideChatListener(this::showGuideChat);
        game.setAudioManager(audio);
        double initialZoom = savedZoomFactor();
        game.setCameraZoom(initialZoom);
        FrameLayout root = new FrameLayout(this);
        root.addView(game, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        Button pauseButton = new Button(this);
        pauseButton.setText("");
        pauseButton.setContentDescription("Pausar jogo");
        pauseButton.setPadding(0, 0, 0, 0);
        pauseButton.setBackground(new com.example.amazonMages.gamepanel.HudMenuDrawable(this));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            (int)(56 * getResources().getDisplayMetrics().density),
            (int)(56 * getResources().getDisplayMetrics().density),
            Gravity.TOP | Gravity.END);
        int margin = (int)(12 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        root.addView(pauseButton, params);
        pauseButton.setOnClickListener(v -> showPauseMenu());
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            game.setDeveloperChatListener(this::showDevChat);
        }
        HudZoomDrawable zoomDrawable = new HudZoomDrawable(this, formatZoom(initialZoom));
        Button zoomButton = new Button(this);
        zoomButton.setText("");
        zoomButton.setContentDescription("Alternar zoom da câmera");
        zoomButton.setPadding(0, 0, 0, 0);
        zoomButton.setBackground(zoomDrawable);
        FrameLayout.LayoutParams zoomParams = new FrameLayout.LayoutParams(
            dp(56), dp(62), Gravity.TOP | Gravity.END);
        zoomParams.setMargins(0, dp(74), dp(12), 0);
        root.addView(zoomButton, zoomParams);
        zoomButton.setOnClickListener(v -> {
            double zoom = game.cycleZoom();
            zoomDrawable.setZoomLabel(formatZoom(zoom));
            gamePreferences.edit().putInt("camera_zoom_factor", (int) Math.round(zoom)).apply();
        });
        game.setWorldControlsVisibility(visible -> {
            int visibility = visible ? View.VISIBLE : View.INVISIBLE;
            pauseButton.setVisibility(visibility);
            zoomButton.setVisibility(visibility);
        });
        setContentView(root);
        if (resumed) game.resume();
    }

    private void showPauseMenu() {
        if (game == null || pauseDialog != null) return;
        game.pause();
        LinearLayout panel = dialogMenu("JOGO PAUSADO");
        Button[] actions = {
            button(panel, "Continuar", v -> pauseDialog.dismiss()),
            button(panel, "Configurações de áudio", v -> showPauseAudioSettings()),
            button(panel, "Gráficos (resolução e FPS)", v -> showPauseGraphicsSettings()),
            button(panel, "Salvar progresso", v -> {
                if (game != null) {
                    game.saveNow();
                    Toast.makeText(this, "Salvamento solicitado", Toast.LENGTH_SHORT).show();
                }
            }),
            button(panel, "Voltar ao menu principal", v -> {
                pauseDialog.setOnDismissListener(null);
                pauseDialog.dismiss();
                pauseDialog = null;
                returnToMenu();
            })
        };
        for (Button action : actions) {
            action.setMinHeight(dp(44));
            action.setTextSize(15);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) action.getLayoutParams();
            lp.topMargin = dp(5);
            action.setLayoutParams(lp);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(panel);
        pauseDialog = new AlertDialog.Builder(this)
            .setView(scroll)
            .create();
        styleDialog(pauseDialog);
        pauseDialog.setOnDismissListener(dialog -> {
            pauseDialog = null;
            if (game != null && resumed) game.resume();
        });
        pauseDialog.show();
        pauseDialog.getWindow().setLayout(
            Math.min(dp(500), getResources().getDisplayMetrics().widthPixels - dp(48)),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void showGuideChat() {
        if (game == null || guideDialog != null) return;
        Game session = game;
        guideDialog = new GuideChatDialog(this, session.guideConversation(),
            gamePreferences.getBoolean("guide_blue", false), blue -> {
                session.setGuideBlue(blue);
                gamePreferences.edit().putBoolean("guide_blue", blue).apply();
            });
        guideDialog.setOnDismissListener(dialog -> {
            guideDialog = null;
            if (game == session) session.closeGuideChat();
        });
        guideDialog.show();
    }

    private void showDevChat() {
        if (game == null || pauseDialog != null || chatDialog != null ||
            (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return;
        game.pause();

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(14));
        GradientDrawable panelBackground = new GradientDrawable();
        panelBackground.setColor(Color.rgb(14, 31, 21));
        panelBackground.setCornerRadius(dp(6));
        panelBackground.setStroke(dp(2), Color.rgb(130, 160, 91));
        panel.setBackground(panelBackground);
        ScrollView chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        chatScroll.addView(panel);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button monitor = new Button(this);
        monitor.setText("FPS");
        monitor.setTextColor(Color.rgb(236, 244, 221));
        monitor.setTextSize(13);
        monitor.setAllCaps(false);
        monitor.setBackground(greenButtonBackground());
        header.addView(monitor, new LinearLayout.LayoutParams(dp(64), dp(46)));
        TextView title = new TextView(this);
        title.setText("CHAT DE COMANDOS");
        title.setTextColor(Color.rgb(229, 206, 142));
        title.setTextSize(18);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        panel.addView(header);

        TextView history = new TextView(this);
        history.setText("Digite /help para listar os comandos de teste.\n");
        history.setTextColor(Color.rgb(203, 238, 207));
        history.setTextSize(13);
        history.setTypeface(android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL));
        history.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable historyBackground = new GradientDrawable();
        historyBackground.setColor(Color.rgb(7, 19, 12));
        historyBackground.setCornerRadius(dp(4));
        historyBackground.setStroke(dp(1), Color.rgb(68, 108, 71));
        history.setBackground(historyBackground);
        ScrollView historyScroll = new ScrollView(this);
        historyScroll.setBackgroundColor(Color.TRANSPARENT);
        historyScroll.addView(history);
        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(128));
        historyParams.topMargin = dp(8);
        panel.addView(historyScroll, historyParams);

        EditText command = new EditText(this);
        command.setSingleLine(true);
        command.setHint("/god on, /spawn fantasma 1, /heal...");
        command.setTextColor(Color.rgb(245, 244, 221));
        command.setHintTextColor(Color.rgb(161, 194, 157));
        command.setPadding(dp(12), dp(8), dp(12), dp(8));
        command.setBackground(greenButtonBackground());
        LinearLayout.LayoutParams commandParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        commandParams.topMargin = dp(8);
        panel.addView(command, commandParams);

        AlertDialog chat = new AlertDialog.Builder(this)
            .setView(chatScroll)
            .setCancelable(true)
            .create();
        monitor.setOnClickListener(v -> {
            game.togglePerformanceOverlay();
            monitor.setText(game.isPerformanceOverlayEnabled() ? "FPS ✓" : "FPS");
        });

        Runnable executeAction = () -> {
            if (game == null) { chat.dismiss(); return; }
            String text = command.getText().toString().trim();
            String normalized = text.startsWith("/") ? text.substring(1).trim() : text;
            if (normalized.equalsIgnoreCase("sair") || normalized.equalsIgnoreCase("exit") ||
                normalized.equalsIgnoreCase("fechar") || normalized.equalsIgnoreCase("quit") ||
                normalized.equalsIgnoreCase("close")) {
                chat.dismiss();
                return;
            }
            String result = game.runDevCommand(text);
            history.append("> " + text + "\n" + result + "\n\n");
            historyScroll.post(() -> historyScroll.fullScroll(View.FOCUS_DOWN));
            command.setText("");
        };
        button(panel, "Enviar comando", v -> executeAction.run());
        button(panel, "Fechar chat", v -> chat.dismiss());
        command.setOnEditorActionListener((v, actionId, event) -> {
            executeAction.run();
            return true;
        });
        chatDialog = chat;
        chat.setOnDismissListener(dialog -> {
            chatDialog = null;
            if (game != null && resumed) game.resume();
        });
        chat.show();
        chat.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        int maxWidth = Math.min(dp(720), getResources().getDisplayMetrics().widthPixels - dp(28));
        int maxHeight = Math.min(dp(420), getResources().getDisplayMetrics().heightPixels - dp(32));
        chat.getWindow().setLayout(maxWidth, maxHeight);
    }

    private void showPauseAudioSettings() {
        LinearLayout panel = dialogMenu("ÁUDIO");
        addVolumeSlider(panel, "Música", audio.musicVolume(), true);
        addVolumeSlider(panel, "Efeitos sonoros", audio.effectsVolume(), false);
        AlertDialog settings = new AlertDialog.Builder(this).setView(panel).create();
        button(panel, "Voltar", v -> settings.dismiss());
        styleDialog(settings);
        settings.show();
    }

    private void styleDialog(AlertDialog dialog) {
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(menuPanelBackground());
                android.view.WindowManager.LayoutParams windowParams = dialog.getWindow().getAttributes();
                windowParams.dimAmount = 0f;
                dialog.getWindow().setAttributes(windowParams);
            }
            TextView title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
            if (title != null) {
                title.setTextColor(Color.rgb(255, 241, 176));
                title.setTypeface(pixelTypeface(true));
            }
            TextView message = dialog.findViewById(android.R.id.message);
            if (message != null) {
                message.setTextColor(Color.rgb(220, 235, 212));
                message.setTypeface(pixelTypeface(false));
            }
            for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE,
                                      AlertDialog.BUTTON_NEUTRAL}) {
                Button action = dialog.getButton(which);
                if (action == null) continue;
                action.setTextColor(Color.rgb(244, 235, 200));
                action.setTypeface(pixelTypeface(true));
                action.setAllCaps(false);
                action.setBackground(atlasButtonBackground());
            }
        });
    }

    private void returnToMenu() {
        closeGuideDialog();
        if (pauseDialog != null) {
            pauseDialog.setOnDismissListener(null);
            pauseDialog.dismiss();
            pauseDialog = null;
        }
        if (game != null) {
            game.pause();
            game.closeSaveRepository();
            game = null;
        }
        showMainMenu();
    }

    @Override protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0
                && game != null && intent != null) {
            String cmd = intent.getStringExtra("dev_cmd");
            if (cmd != null && !cmd.trim().isEmpty()) {
                android.util.Log.i("DevCmd", cmd + " -> " + game.runDevCommand(cmd));
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (audio != null) audio.resume();
        if (game != null && pauseDialog == null && chatDialog == null) game.resume();
    }

    @Override protected void onPause() {
        resumed = false;
        if (guideDialog != null) guideDialog.pauseSpeech();
        if (game != null) game.pause();
        if (audio != null) audio.pause();
        super.onPause();
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
            com.example.amazonMages.resources.ResourceManager.clearShared();
    }

    @Override protected void onDestroy() {
        closeGuideDialog();
        if (game != null) {
            game.pause();
            game.closeSaveRepository();
            game = null;
        }
        menuRepository.close();
        if (audio != null) audio.release();
        com.example.amazonMages.resources.ResourceManager.clearShared();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (guideDialog != null) guideDialog.dismiss();
        else if (chatDialog != null) chatDialog.dismiss();
        else if (game != null) showPauseMenu();
        else showMainMenu();
    }

    private void closeGuideDialog() {
        if (guideDialog == null) return;
        guideDialog.dismiss();
        guideDialog = null;
    }
}
