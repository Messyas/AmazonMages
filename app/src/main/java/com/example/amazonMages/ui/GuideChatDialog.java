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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.amazonMages.dialogue.DialogueLine;
import com.example.amazonMages.dialogue.GuideConversation;
import com.example.amazonMages.gamepanel.AtlasMenuButtonDrawable;
import com.example.amazonMages.gamepanel.AtlasPanelDrawable;
import com.example.androidstudio2dgamedevelopment.R;

/** Portrait and player-facing chat. The developer console is a separate screen. */
public final class GuideChatDialog extends Dialog {
    private final GuideConversation conversation;
    private final java.util.function.Consumer<Boolean> skinSelected;
    private final ScrollView historyScroll;
    private final LinearLayout history;
    private final GuidePortraitView portrait;
    private final ImageView hero;
    private final TextView speakerLabel;
    private final Button skinButton;
    private final Button nextButton;
    private final Button voiceButton;
    private final EditText question;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable speechFrame = this::tickSpeech;
    private DialogueLine speaking;
    private TextView speakingText;
    private long speechStarted;
    private int visibleCharacters;
    private GuideVoice voice;
    private boolean voiceEnabled, voicePlaying, voiceRequested;
    private boolean blue;

    public GuideChatDialog(Activity activity, GuideConversation conversation,
            boolean blue, java.util.function.Consumer<Boolean> skinSelected) {
        super(activity);
        this.conversation = conversation;
        this.blue = blue;
        this.skinSelected = skinSelected;
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.setBackground(new AtlasPanelDrawable(getContext()));
        root.setFocusableInTouchMode(true);

        LinearLayout profile = column();
        profile.setPadding(0, 0, dp(8), 0);
        root.addView(profile, new LinearLayout.LayoutParams(0, -1, .42f));
        speakerLabel = text("Iara e você", 16, 0xFFFFE8A1);
        speakerLabel.setGravity(Gravity.CENTER);
        speakerLabel.setTypeface(Typeface.DEFAULT_BOLD);
        profile.addView(speakerLabel);
        LinearLayout portraits = new LinearLayout(getContext());
        portraits.setGravity(Gravity.BOTTOM);
        profile.addView(portraits, new LinearLayout.LayoutParams(-1, 0, 1f));
        portrait = new GuidePortraitView(getContext());
        portraits.addView(portrait, new LinearLayout.LayoutParams(0, -1, .61f));
        hero = new ImageView(getContext());
        hero.setContentDescription("Seu personagem");
        hero.setImageResource(R.drawable.portrait_player);
        hero.setScaleType(ImageView.ScaleType.FIT_END);
        portraits.addView(hero, new LinearLayout.LayoutParams(0, -1, .39f));
        skinButton = button("");
        profile.addView(skinButton, new LinearLayout.LayoutParams(-1, dp(43)));
        skinButton.setOnClickListener(v -> toggleSkin());
        updateSkin();

        LinearLayout chat = column();
        root.addView(chat, new LinearLayout.LayoutParams(0, -1, .58f));
        LinearLayout header = new LinearLayout(getContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        chat.addView(header);
        TextView title = text("Conversa com a guia", 16, 0xFFFFE8A1);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = button("Fechar");
        header.addView(close, new LinearLayout.LayoutParams(dp(84), dp(43)));
        close.setOnClickListener(v -> dismiss());

        historyScroll = new ScrollView(getContext());
        historyScroll.setFillViewport(true);
        historyScroll.setBackground(panel(0xFF11251C));
        history = column();
        history.setPadding(dp(8), dp(6), dp(8), dp(6));
        historyScroll.addView(history);
        chat.addView(historyScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout scene = new LinearLayout(getContext());
        chat.addView(scene);
        nextButton = button("Continuar");
        scene.addView(nextButton, new LinearLayout.LayoutParams(0, dp(40), 1f));
        nextButton.setOnClickListener(v -> advanceScene());
        voiceButton = button("Ativar voz");
        scene.addView(voiceButton, new LinearLayout.LayoutParams(dp(103), dp(40)));
        voiceButton.setOnClickListener(v -> toggleVoice());

        LinearLayout topics = new LinearLayout(getContext());
        chat.addView(topics);
        addTopic(topics, "Início", "Como começo?");
        addTopic(topics, "Combate", "Como funciona o combate?");
        addTopic(topics, "Mapa", "Como uso o mapa?");

        LinearLayout composer = new LinearLayout(getContext());
        composer.setGravity(Gravity.CENTER_VERTICAL);
        chat.addView(composer);
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
        renderHistory();
        updateSceneButton();
    }

    @Override protected void onStart() {
        super.onStart();
        if (conversation.lines().isEmpty()) play(conversation.nextIntroLine());
    }

    @Override protected void onStop() {
        pauseSpeech();
        if (voice != null) {
            voice.close();
            voice = null;
        }
        voiceEnabled = false;
        super.onStop();
    }

    public void pauseSpeech() {
        voiceRequested = false;
        finishSpeech(true);
    }

    @Override public void show() {
        super.show();
        Window window = getWindow();
        if (window == null) return;
        android.util.DisplayMetrics m = getContext().getResources().getDisplayMetrics();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        window.setLayout(Math.min(dp(780), m.widthPixels - dp(24)), (int) (m.heightPixels * .89f));
    }

    private void addTopic(LinearLayout row, String label, String prompt) {
        Button topic = button(label);
        row.addView(topic, new LinearLayout.LayoutParams(0, dp(40), 1f));
        topic.setOnClickListener(v -> ask(prompt));
    }

    private void sendQuestion() {
        String value = question.getText().toString().trim();
        if (value.isEmpty()) return;
        question.setText("");
        ask(value);
    }

    private void ask(String value) {
        play(conversation.ask(value));
    }

    private void advanceScene() {
        if (speaking != null && visibleCharacters < speaking.text.length()) {
            finishSpeech(true);
            return;
        }
        play(conversation.hasIntroLine() ? conversation.nextIntroLine() : conversation.replayIntro());
    }

    private void play(DialogueLine line) {
        if (line == null) return;
        finishSpeech(true);
        speaking = line;
        visibleCharacters = 0;
        speechStarted = SystemClock.uptimeMillis();
        renderHistory();
        updateSceneButton();
        if (voiceEnabled) read(line);
        main.post(speechFrame);
    }

    private void tickSpeech() {
        if (speaking == null) return;
        long elapsed = SystemClock.uptimeMillis() - speechStarted;
        int count = Math.min(speaking.text.length(), (int) (elapsed / 23));
        if (speakingText != null && count != visibleCharacters) {
            visibleCharacters = count;
            speakingText.setText(speaking.text.substring(0, count));
            scrollHistoryToEnd();
            updateSceneButton();
        }
        if (count >= speaking.text.length() && !voicePlaying) {
            finishSpeech(false);
            return;
        }
        boolean mouthOpen = elapsed % 390 >= 110 && elapsed % 390 < 265;
        showSpeaker(speaking, mouthOpen, true);
        main.postDelayed(speechFrame, 32);
    }

    private void finishSpeech(boolean stopVoice) {
        main.removeCallbacks(speechFrame);
        if (stopVoice && voice != null) voice.stop();
        voicePlaying = false;
        if (speaking != null && speakingText != null) speakingText.setText(speaking.text);
        speaking = null;
        speakingText = null;
        showSpeaker(lastLine(), false, false);
        scrollHistoryToEnd();
        updateSceneButton();
    }

    private void updateSceneButton() {
        if (speaking != null && visibleCharacters < speaking.text.length()) nextButton.setText("Mostrar fala");
        else nextButton.setText(conversation.hasIntroLine() ? "Continuar" : "Rever diálogo");
    }

    private DialogueLine lastLine() {
        java.util.List<DialogueLine> lines = conversation.lines();
        return lines.isEmpty() ? null : lines.get(lines.size() - 1);
    }

    private void showSpeaker(DialogueLine line, boolean mouthOpen, boolean playing) {
        boolean guideSpeaking = line != null && isGuide(line);
        portrait.pose(guideSpeaking ? line.expression : "neutral", blue, guideSpeaking && mouthOpen);
        portrait.setAlpha(playing && !guideSpeaking ? .68f : 1f);
        hero.setAlpha(playing && guideSpeaking ? .68f : 1f);
        hero.setTranslationY(playing && !guideSpeaking && mouthOpen ? -dp(2) : 0);
        speakerLabel.setText(line == null ? "Iara e você" : line.speakerId);
    }

    private static boolean isGuide(DialogueLine line) {
        return GuideConversation.NAME.equals(line.speakerId);
    }

    private void toggleVoice() {
        if (voiceEnabled) {
            voiceEnabled = voicePlaying = false;
            if (voice != null) voice.stop();
            voiceButton.setText("Ativar voz");
            return;
        }
        if (voice != null) {
            enableVoice();
            return;
        }
        voiceButton.setEnabled(false);
        voiceRequested = true;
        voice = new GuideVoice(getContext(), ready -> {
            voiceButton.setEnabled(true);
            if (!ready) {
                if (voice != null) voice.close();
                voice = null;
                if (voiceRequested && isShowing()) {
                    Toast.makeText(getContext(), "Nenhuma voz offline em português está instalada.", Toast.LENGTH_SHORT).show();
                }
            } else if (voiceRequested && isShowing()) enableVoice();
            voiceRequested = false;
        });
    }

    private void enableVoice() {
        voiceEnabled = true;
        voiceButton.setText("Desligar voz");
        DialogueLine line = speaking != null ? speaking : lastLine();
        if (line == null) return;
        if (speaking == null) {
            speaking = line;
            visibleCharacters = line.text.length();
            speechStarted = SystemClock.uptimeMillis() - visibleCharacters * 23L;
            main.post(speechFrame);
        }
        read(line);
    }

    private void read(DialogueLine line) {
        voicePlaying = voice != null && voice.speak(line.text, isGuide(line), () -> voicePlaying = false);
    }

    private void renderHistory() {
        history.removeAllViews();
        speakingText = null;
        java.util.List<DialogueLine> lines = conversation.lines();
        for (int index = 0; index < lines.size(); index++) {
            DialogueLine line = lines.get(index);
            boolean guide = isGuide(line);
            LinearLayout bubble = column();
            bubble.setPadding(dp(8), dp(4), dp(8), dp(6));
            bubble.setBackground(panel(guide ? 0xFF254233 : 0xFF38432E));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(5);
            if (guide) params.rightMargin = dp(20); else params.leftMargin = dp(20);
            TextView speaker = text(line.speakerId, 12, 0xFFEAD69D);
            speaker.setTypeface(Typeface.DEFAULT_BOLD);
            bubble.addView(speaker);
            if (index == lines.size() - 1 && speaking == line) {
                speakingText = text("", 14, 0xFFF1EDD9);
                bubble.addView(speakingText);
            } else bubble.addView(text(line.text, 14, 0xFFF1EDD9));
            history.addView(bubble, params);
        }
        showSpeaker(lastLine(), false, speaking != null);
        scrollHistoryToEnd();
    }

    private void scrollHistoryToEnd() {
        historyScroll.post(() -> historyScroll.scrollTo(0, history.getHeight()));
    }

    private void updateSkin() {
        portrait.pose("neutral", blue, false);
        skinButton.setText(blue ? "Visual: Azul" : "Visual: Branco");
    }

    private void toggleSkin() {
        blue = !blue;
        updateSkin();
        showSpeaker(lastLine(), false, speaking != null);
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
