package com.example.amazonMages;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.amazonMages.save.GameSaveRepository;
import com.example.amazonMages.save.PlayerPositionEntity;

import java.text.DateFormat;
import java.util.List;
import java.util.Date;

/** Classic main menu with three independent save slots. */
public class MainActivity extends Activity {
    private Game game;
    private GameSaveRepository menuRepository;
    private boolean resumed;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        menuRepository = new GameSaveRepository(this, 1);
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
        LinearLayout root = menu("AMAZON MAGES");
        button(root, "Jogar", v -> showSlots(false));
        button(root, "Gerenciar jogatinas", v -> showSlots(true));
        button(root, "Sair", v -> finish());
        setContentView(root);
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
        game = new Game(this, slotId);
        setContentView(game);
        if (resumed) game.resume();
    }

    private void returnToMenu() {
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
        if (game != null) game.resume();
    }

    @Override protected void onPause() {
        resumed = false;
        if (game != null) game.pause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (game != null) {
            game.pause();
            game.closeSaveRepository();
            game = null;
        }
        menuRepository.close();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (game != null) {
            new AlertDialog.Builder(this)
                .setTitle("Voltar ao menu?")
                .setMessage("Seu progresso será salvo.")
                .setNegativeButton("Continuar jogando", null)
                .setPositiveButton("Menu", (dialog, which) -> returnToMenu())
                .show();
        } else {
            showMainMenu();
        }
    }
}
