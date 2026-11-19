package com.example.amazonMages.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/** Optional narration using only an installed offline Portuguese voice. */
final class GuideVoice {
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech engine;
    private Runnable finished;
    private int serial;
    private boolean ready;

    GuideVoice(Context context, Consumer<Boolean> onReady) {
        engine = new TextToSpeech(context.getApplicationContext(),
            status -> main.post(() -> initialize(status, onReady)));
    }

    private void initialize(int status, Consumer<Boolean> onReady) {
        if (engine == null) return;
        if (status != TextToSpeech.SUCCESS) {
            onReady.accept(false);
            return;
        }
        Set<Voice> offered = engine.getVoices();
        Voice candidate = null;
        if (offered != null) {
            for (Voice voice : offered) {
                Locale language = voice.getLocale();
                if (voice.isNetworkConnectionRequired() || !"pt".equals(language.getLanguage())
                        || voice.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
                if (candidate == null || "BR".equals(language.getCountry())
                        && !"BR".equals(candidate.getLocale().getCountry())) {
                    candidate = voice;
                }
            }
        }
        ready = candidate != null && engine.setVoice(candidate) == TextToSpeech.SUCCESS;
        if (ready) engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) { complete(id); }
            @Override public void onError(String id) { complete(id); }
        });
        onReady.accept(ready);
    }

    boolean speak(String text, boolean guide, Runnable onEnd) {
        if (!ready || engine == null) return false;
        String id = Integer.toString(++serial);
        finished = onEnd;
        engine.setPitch(guide ? 1.1f : .90f);
        engine.setSpeechRate(.96f);
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.SUCCESS) return true;
        finished = null;
        return false;
    }

    private void complete(String id) {
        main.post(() -> {
            if (!Integer.toString(serial).equals(id) || finished == null) return;
            Runnable callback = finished;
            finished = null;
            callback.run();
        });
    }

    void stop() {
        serial++;
        finished = null;
        if (engine != null) engine.stop();
    }

    void close() {
        stop();
        if (engine != null) engine.shutdown();
        engine = null;
    }
}
