package com.example.amazonMages.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;
import com.example.androidstudio2dgamedevelopment.R;
import java.util.HashMap;
import java.util.Map;

/** Activity-owned audio: music, short effects, preferences and pause lifecycle. */
public final class GameAudioManager {
    private static final String TAG = "GameAudio";
    private final Context context;
    private final SharedPreferences prefs;
    private final AudioManager audioManager;
    private final SoundPool pool;
    private final Map<String, Integer> effects = new HashMap<>();
    private final Map<Integer, Boolean> loaded = new HashMap<>();
    private final AudioManager.OnAudioFocusChangeListener focusListener = change -> {
        if (change == AudioManager.AUDIOFOCUS_GAIN) {
            focusHeld = true;
            updateMusic();
        } else if (change == AudioManager.AUDIOFOCUS_LOSS) {
            focusHeld = false;
            stopMusic();
        } else {
            focusHeld = false;
            stopMusic();
        }
    };
    private MediaPlayer music;
    private boolean active;
    private boolean focusHeld;
    private boolean released;
    private float musicVolume;
    private float effectsVolume;

    public GameAudioManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE);
        audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        musicVolume = clamp(prefs.getFloat("music", .55f));
        effectsVolume = clamp(prefs.getFloat("effects", .8f));
        pool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener((soundPool, id, status) -> loaded.put(id, status == 0));
        registerEffect("attack", "sfx_attack");
        registerEffect("hit", "sfx_hit");
        registerEffect("ghost_defeat", "sfx_ghost_defeat");
        registerEffect("storm", "sfx_storm");
        registerEffect("dialogue", "sfx_dialogue");
        registerEffect("level_up", "sfx_level_up");
        registerEffect("menu", "sfx_menu");
    }

    private void registerEffect(String key, String resourceName) {
        int id = context.getResources().getIdentifier(resourceName, "raw", context.getPackageName());
        if (id != 0) effects.put(key, pool.load(context, id, 1));
    }

    private static float clamp(float v) {
        return Float.isFinite(v) ? Math.max(0, Math.min(1, v)) : .5f;
    }
    public float musicVolume() { return musicVolume; }
    public float effectsVolume() { return effectsVolume; }

    public void setMusicVolume(float value) {
        musicVolume = clamp(value);
        prefs.edit().putFloat("music", musicVolume).apply();
        updateMusic();
    }
    public void setEffectsVolume(float value) {
        effectsVolume = clamp(value);
        prefs.edit().putFloat("effects", effectsVolume).apply();
    }

    public void play(String effect) {
        if (released || !active || effectsVolume <= 0 || !focusHeld) return;
        Integer id = effects.get(effect);
        if (id != null && Boolean.TRUE.equals(loaded.get(id)))
            pool.play(id, effectsVolume, effectsVolume, 1, 0, 1f);
    }

    public void resume() {
        if (released) return;
        active = true;
        if (audioManager != null) {
            int result = audioManager.requestAudioFocus(focusListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else focusHeld = true;
        pool.autoResume();
        updateMusic();
    }

    public void pause() {
        active = false;
        stopMusic();
        pool.autoPause();
        if (audioManager != null) audioManager.abandonAudioFocus(focusListener);
        focusHeld = false;
    }

    private void updateMusic() {
        if (released) return;
        if (!active || !focusHeld || musicVolume <= 0) { stopMusic(); return; }
        if (music == null) {
            music = MediaPlayer.create(context, R.raw.videoplayback);
            if (music == null) { Log.w(TAG, "Music resource could not be opened"); return; }
            music.setLooping(true);
            music.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "Music playback error: " + what + "/" + extra);
                stopMusic();
                return true;
            });
        }
        music.setVolume(musicVolume, musicVolume);
        try { if (!music.isPlaying()) music.start(); }
        catch (IllegalStateException e) { Log.w(TAG, "Music playback unavailable", e); }
    }

    private void stopMusic() {
        if (music != null) {
            try { if (music.isPlaying()) music.pause(); }
            catch (IllegalStateException e) { Log.w(TAG, "Cannot pause music", e); }
        }
    }

    public void release() {
        if (released) return;
        pause();
        released = true;
        pool.release();
        if (music != null) { music.release(); music = null; }
    }
}
