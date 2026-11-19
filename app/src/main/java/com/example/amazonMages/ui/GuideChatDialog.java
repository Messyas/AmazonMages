package com.example.amazonMages.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import com.example.amazonMages.dialogue.DialogueLine;
import com.example.amazonMages.dialogue.GuideConversation;
import com.example.amazonMages.gamepanel.AtlasMenuButtonDrawable;
import com.example.amazonMages.gamepanel.AtlasPanelDrawable;
import com.example.androidstudio2dgamedevelopment.R;

/** Portrait and player-facing chat. The developer console is a separate screen. */
public final class GuideChatDialog extends Dialog {
    private final GuideConversation conversation;
    private final java.util.function.Consumer<Boolean> skinSelected;
    private final ScrollView lineScroll;
    private final TextView lineText;
    private final GuidePortraitView portrait;
    private final ImageView hero;
    private final TextView speakerLabel;
    private final Button skinButton;
    private final Button nextButton;
    private final EditText question;
    private final LinearLayout composer;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable speechFrame = this::tickSpeech;
    private DialogueLine speaking;
    private long speechStarted;
    private int visibleCharacters;
    private boolean blue;

    public GuideChatDialog(Activity activity, GuideConversation conversation,
            boolean blue, java.util.function.Consumer<Boolean> skinSelected) {
        super(activity);
        this.conversation = conversation;
        this.blue = blue;
        this.skinSelected = skinSelected;
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout root = new FrameLayout(getContext());
        root.setFocusableInTouchMode(true);

        LinearLayout portraits = new LinearLayout(getContext());
        portraits.setPadding(0, dp(56), 0, 0);
        root.addView(portraits, new FrameLayout.LayoutParams(-1, -1));
        portrait = new GuidePortraitView(getContext());
        portraits.addView(portrait, new LinearLayout.LayoutParams(0, -1, .38f));
        portraits.addView(new View(getContext()), new LinearLayout.LayoutParams(0, 1, .24f));
        hero = new ImageView(getContext());
        hero.setContentDescription("Seu personagem");
        hero.setImageResource(R.drawable.portrait_player);
        hero.setScaleType(ImageView.ScaleType.FIT_END);
        portraits.addView(hero, new LinearLayout.LayoutParams(0, -1, .38f));

        LinearLayout foreground = column();
        foreground.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.addView(foreground, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout controls = new LinearLayout(getContext());
        foreground.addView(controls);
        skinButton = button("");
        controls.addView(skinButton, new LinearLayout.LayoutParams(-2, dp(48)));
        skinButton.setOnClickListener(v -> toggleSkin());
        updateSkin();
        controls.addView(new View(getContext()), new LinearLayout.LayoutParams(0, 1, 1f));
        Button close = button("Fechar");
        controls.addView(close, new LinearLayout.LayoutParams(dp(84), dp(48)));
        close.setOnClickListener(v -> dismiss());

        LinearLayout stage = new LinearLayout(getContext());
        stage.setGravity(Gravity.CENTER_VERTICAL);
        stage.setPadding(0, dp(12), 0, dp(20));
        foreground.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1f));
        stage.addView(new View(getContext()), new LinearLayout.LayoutParams(0, 1, .25f));

        LinearLayout card = column();
        card.setBackground(new AtlasPanelDrawable(getContext()));
        card.setPadding(dp(16), dp(14), dp(16), dp(12));
        stage.addView(card, new LinearLayout.LayoutParams(0, -1, .50f));
        stage.addView(new View(getContext()), new LinearLayout.LayoutParams(0, 1, .25f));

        speakerLabel = text("", 17, 0xFFFFE8A1);
        speakerLabel.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(speakerLabel);

        lineScroll = new ScrollView(getContext());
        lineScroll.setFillViewport(true);
        card.addView(lineScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        lineText = text("", 16, 0xFFFFF3D5);
        lineText.setGravity(Gravity.CENTER_VERTICAL);
        lineText.setPadding(0, dp(4), 0, dp(4));
        lineScroll.addView(lineText);
        lineText.setOnClickListener(v -> advanceScene());

        LinearLayout actions = new LinearLayout(getContext());
        card.addView(actions);
        Button questions = button("Perguntar");
        addAction(actions, questions);
        questions.setOnClickListener(v -> showTopics(questions));
        nextButton = button("Continuar");
        addAction(actions, nextButton);
        nextButton.setOnClickListener(v -> advanceScene());

        composer = new LinearLayout(getContext());
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setVisibility(View.GONE);
        foreground.addView(composer);
        question = new EditText(getContext());
        question.setSingleLine(true);
        question.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        question.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});
        question.setTextSize(14);
        question.setTextColor(0xFFFFF3D5);
        question.setHintTextColor(0xFFC3D5B9);
        question.setHint("Pergunte à Iara");
        question.setBackground(panel(0xFF182E23));
        question.setPadding(dp(8), 0, dp(8), 0);
        question.setImeOptions(EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        composer.addView(question, new LinearLayout.LayoutParams(0, dp(44), 1f));
        Button cancel = button("Cancelar");
        composer.addView(cancel, new LinearLayout.LayoutParams(-2, dp(48)));
        cancel.setOnClickListener(v -> closeComposer());
        Button send = button("Enviar");
        composer.addView(send, new LinearLayout.LayoutParams(dp(84), dp(44)));
        send.setOnClickListener(v -> sendQuestion());
        question.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            sendQuestion();
            return true;
        });
        setContentView(root);
        root.requestFocus();
        showLine(lastLine(), false);
    }

    @Override protected void onStart() {
        super.onStart();
        if (conversation.lines().isEmpty()) play(conversation.nextIntroLine());
    }

    @Override protected void onStop() {
        finishSpeech();
        super.onStop();
    }

    @Override public void show() {
        super.show();
        Window window = getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        window.setLayout(-1, -1);
    }

    private void addAction(LinearLayout row, Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
        if (row.getChildCount() > 0) params.leftMargin = dp(8);
        row.addView(button, params);
    }

    private void showTopics(View anchor) {
        PopupMenu menu = new PopupMenu(getContext(), anchor);
        addTopic(menu, "Como come?o?");
        addTopic(menu, "Como funciona o combate?");
        addTopic(menu, "Como uso o mapa?");
        menu.getMenu().add("Escrever pergunta…").setOnMenuItemClickListener(item -> {
            composer.setVisibility(View.VISIBLE);
            question.requestFocus();
            question.post(() -> {
                if (question.hasFocus()) keyboard().showSoftInput(question, 0);
            });
            return true;
        });
        menu.show();
    }

    private void addTopic(PopupMenu menu, String prompt) {
        menu.getMenu().add(prompt).setOnMenuItemClickListener(item -> {
            closeComposer();
            ask(prompt);
            return true;
        });
    }

    private android.view.inputmethod.InputMethodManager keyboard() {
        return getContext().getSystemService(android.view.inputmethod.InputMethodManager.class);
    }

    private void closeComposer() {
        keyboard().hideSoftInputFromWindow(question.getWindowToken(), 0);
        question.clearFocus();
        composer.setVisibility(View.GONE);
    }

    private void sendQuestion() {
        String value = question.getText().toString().trim();
        if (value.isEmpty()) return;
        question.setText("");
        closeComposer();
        ask(value);
    }

    private void ask(String value) {
        play(conversation.ask(value));
    }

    private void advanceScene() {
        if (speaking != null && visibleCharacters < speaking.text.length()) {
            finishSpeech();
            return;
        }
        if (conversation.hasReply()) play(conversation.nextReply());
        else play(conversation.hasIntroLine() ? conversation.nextIntroLine() : conversation.replayIntro());
    }

    private void play(DialogueLine line) {
        if (line == null) return;
        finishSpeech();
        speaking = line;
        visibleCharacters = 0;
        speechStarted = SystemClock.uptimeMillis();
        showLine(line, true);
        main.post(speechFrame);
    }

    private void tickSpeech() {
        if (speaking == null) return;
        long elapsed = SystemClock.uptimeMillis() - speechStarted;
        int count = Math.min(speaking.text.length(), (int) (elapsed / 23));
        if (count != visibleCharacters) {
            visibleCharacters = count;
            lineText.setText(speaking.text.substring(0, count));
        }
        if (count >= speaking.text.length()) {
            finishSpeech();
            return;
        }
        boolean mouthOpen = elapsed % 390 >= 110 && elapsed % 390 < 265;
        showSpeaker(speaking, mouthOpen);
        main.postDelayed(speechFrame, 32);
    }

    private void finishSpeech() {
        main.removeCallbacks(speechFrame);
        if (speaking != null) lineText.setText(speaking.text);
        speaking = null;
        showSpeaker(lastLine(), false);
        updateSceneButton();
    }

    private void showLine(DialogueLine line, boolean animate) {
        speakerLabel.setText(line == null ? "" : line.speakerId);
        lineText.setText(animate || line == null ? "" : line.text);
        lineScroll.scrollTo(0, 0);
        showSpeaker(line, false);
        updateSceneButton();
    }

    private void updateSceneButton() {
        if (speaking != null) nextButton.setText("Mostrar fala");
        else if (conversation.hasReply() || conversation.hasIntroLine()) nextButton.setText("Continuar");
        else nextButton.setText("Rever diálogo");
    }

    private DialogueLine lastLine() {
        java.util.List<DialogueLine> lines = conversation.lines();
        return lines.isEmpty() ? null : lines.get(lines.size() - 1);
    }

    private void showSpeaker(DialogueLine line, boolean mouthOpen) {
        boolean guideSpeaking = line != null && GuideConversation.NAME.equals(line.speakerId);
        portrait.setVisibility(guideSpeaking ? View.VISIBLE : View.INVISIBLE);
        hero.setVisibility(line != null && !guideSpeaking ? View.VISIBLE : View.INVISIBLE);
        if (guideSpeaking) portrait.pose(line.expression, blue, mouthOpen);
    }

    private void updateSkin() {
        skinButton.setText(blue ? "Visual: Azul" : "Visual: Branco");
    }

    private void toggleSkin() {
        blue = !blue;
        updateSkin();
        showSpeaker(lastLine(), false);
        skinSelected.accept(blue);
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(String value, int size, int color) {
        TextView label = new TextView(getContext());
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        return label;
    }

    private Button button(String value) {
        Context context = getContext();
        Button action = new Button(context);
        action.setAllCaps(false);
        action.setText(value);
        action.setTextColor(0xFFF4EBC8);
        action.setTextSize(13);
        action.setPadding(dp(5), 0, dp(5), 0);
        action.setBackground(new AtlasMenuButtonDrawable(context));
        return action;
    }

    private GradientDrawable panel(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(3));
        return background;
    }

    private int dp(float value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
