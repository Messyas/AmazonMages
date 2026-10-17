package com.example.amazonMages;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
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
import com.example.amazonMages.audio.GameAudioManager;

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

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        menuRepository = new GameSaveRepository(this, 1);
        audio = new GameAudioManager(this);
        showMainMenu();
    }

    private LinearLayout menu(String heading) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.rgb(18, 25, 23));
        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        return root;
    }

    private void button(LinearLayout root, String label, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(click);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 20;
        root.addView(button, params);
    }

    private void showMainMenu() {
        states.menu();
        LinearLayout root = menu("AMAZON MAGES");
        button(root, "Jogar", v -> showSlots(false));
        button(root, "Áudio", v -> showAudioSettings());
        button(root, "Gerenciar jogatinas", v -> showSlots(true));
        button(root, "Sair", v -> finish());
        setContentView(root);
    }

    private void showAudioSettings() {
        LinearLayout root = menu("CONFIGURAÇÕES DE ÁUDIO");
        addVolumeSlider(root, "Música", audio.musicVolume(), true);
        addVolumeSlider(root, "Efeitos sonoros", audio.effectsVolume(), false);
        button(root, "Voltar", v -> {
            audio.play("menu");
            if (game == null) showMainMenu();
            else showPauseMenu();
        });
        setContentView(root);
    }

    private void addVolumeSlider(LinearLayout root, String label, float value, boolean music) {
        TextView text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setText(label + ": " + Math.round(value * 100) + "%");
        root.addView(text);
        SeekBar slider = new SeekBar(this);
        slider.setMax(100);
        slider.setProgress(Math.round(value * 100));
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

    private void showSlots(boolean managing) {
        LinearLayout root = menu(managing ? "Gerenciar jogatinas" : "Escolher jogatina");
        TextView loading = new TextView(this);
        loading.setText("Carregando saves...");
        loading.setTextColor(Color.WHITE);
        root.addView(loading);
        button(root, "Voltar", v -> showMainMenu());
        setContentView(root);
        menuRepository.loadSlots(slots -> {
            if (game != null || getWindow().getDecorView().getRootView() == null) return;
            root.removeView(loading);
            for (int id = 1; id <= 3; id++) {
                final int slot = id;
                PlayerPositionEntity found = null;
                for (PlayerPositionEntity item : slots) {
                    if (item.slotId == slot) { found = item; break; }
                }
                String label = found == null ? "Jogatina " + id + " — Nova" :
                    "Jogatina " + id + " — Vida " + found.health + " — " +
                    DateFormat.getDateTimeInstance().format(new Date(found.updatedAt));
                if (managing) {
                    if (found != null) button(root, "Excluir " + label, v ->
                        new AlertDialog.Builder(this)
                            .setTitle("Excluir jogatina " + slot + "?")
                            .setMessage("O progresso será perdido permanentemente.")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Excluir", (dialog, which) ->
                                menuRepository.deleteSlot(slot, () -> showSlots(true)))
                            .show());
                } else {
                    button(root, label, v -> startGame(slot));
                }
            }
        });
    }

    private void startGame(int slotId) {
        game = new Game(this, slotId, states);
        game.setAudioManager(audio);
        FrameLayout root = new FrameLayout(this);
        root.addView(game, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        Button pauseButton = new Button(this);
        pauseButton.setText("Ⅱ");
        pauseButton.setContentDescription("Pausar jogo");
        pauseButton.setTextColor(Color.WHITE);
        pauseButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(29, 53, 43)));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            (int)(56 * getResources().getDisplayMetrics().density),
            (int)(56 * getResources().getDisplayMetrics().density),
            Gravity.TOP | Gravity.END);
        int margin = (int)(12 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        root.addView(pauseButton, params);
        pauseButton.setOnClickListener(v -> showPauseMenu());
        setContentView(root);
        if (resumed) game.resume();
    }

    private void showPauseMenu() {
        if (game == null || pauseDialog != null) return;
        game.pause();
        LinearLayout panel = menu("JOGO PAUSADO");
        button(panel, "Continuar", v -> pauseDialog.dismiss());
        button(panel, "Configurações de áudio", v -> showPauseAudioSettings());
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            button(panel, "Console de desenvolvimento", v -> showDevConsole());
        button(panel, "Salvar progresso", v -> {
            if (game != null) {
                game.saveNow();
                Toast.makeText(this, "Salvamento solicitado", Toast.LENGTH_SHORT).show();
            }
        });
        button(panel, "Voltar ao menu principal", v -> {
            pauseDialog.setOnDismissListener(null);
            pauseDialog.dismiss();
            pauseDialog = null;
            returnToMenu();
        });
        pauseDialog = new AlertDialog.Builder(this)
            .setView(panel)
            .create();
        pauseDialog.setOnDismissListener(dialog -> {
            pauseDialog = null;
            if (game != null && resumed) game.resume();
        });
        pauseDialog.show();
    }

    private void showDevConsole() {
        if (game == null || pauseDialog == null ||
            (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return;

        ScrollView scrollView = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(32, 24, 32, 32);
        panel.setBackgroundColor(Color.rgb(18, 25, 23));
        scrollView.addView(panel);

        // Header com título e botão de fechar [✕]
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("CONSOLE DEV");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button closeX = new Button(this);
        closeX.setText("✕");
        closeX.setTextColor(Color.WHITE);
        closeX.setTextSize(18);
        closeX.setBackgroundColor(Color.rgb(139, 37, 0));
        header.addView(closeX, new LinearLayout.LayoutParams(
            (int)(48 * getResources().getDisplayMetrics().density),
            (int)(42 * getResources().getDisplayMetrics().density)));
        panel.addView(header);

        TextView output = new TextView(this);
        output.setTextColor(Color.WHITE);
        output.setText("Digite 'help' para comandos ou 'sair' para fechar.");
        output.setPadding(0, 16, 0, 16);
        panel.addView(output);

        EditText command = new EditText(this);
        command.setSingleLine(true);
        command.setHint("tp inicio | god on | spawn fantasma | sair");
        command.setTextColor(Color.WHITE);
        command.setHintTextColor(Color.GRAY);
        panel.addView(command);

        AlertDialog console = new AlertDialog.Builder(this)
            .setView(scrollView)
            .setCancelable(true)
            .setNegativeButton("Fechar / Sair do Console", (dialog, which) -> dialog.dismiss())
            .create();

        closeX.setOnClickListener(v -> console.dismiss());

        Runnable executeAction = () -> {
            String text = command.getText().toString().trim();
            if (text.equalsIgnoreCase("sair") || text.equalsIgnoreCase("exit") ||
                text.equalsIgnoreCase("fechar") || text.equalsIgnoreCase("quit") ||
                text.equalsIgnoreCase("close")) {
                console.dismiss();
                return;
            }
            String result = game.runDevCommand(text);
            if ("close".equals(result)) {
                console.dismiss();
                return;
            }
            output.setText(result);
            command.selectAll();
        };

        button(panel, "Executar", v -> executeAction.run());
        button(panel, "Fechar / Sair", v -> console.dismiss());

        command.setOnEditorActionListener((v, actionId, event) -> {
            executeAction.run();
            return true;
        });

        console.show();
    }

    private void showPauseAudioSettings() {
        LinearLayout panel = menu("ÁUDIO");
        addVolumeSlider(panel, "Música", audio.musicVolume(), true);
        addVolumeSlider(panel, "Efeitos sonoros", audio.effectsVolume(), false);
        AlertDialog settings = new AlertDialog.Builder(this).setView(panel).create();
        button(panel, "Voltar", v -> settings.dismiss());
        settings.show();
    }

    private void returnToMenu() {
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

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (audio != null) audio.resume();
        if (game != null && pauseDialog == null) game.resume();
    }

    @Override protected void onPause() {
        resumed = false;
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
        if (game != null) showPauseMenu();
        else showMainMenu();
    }
}
