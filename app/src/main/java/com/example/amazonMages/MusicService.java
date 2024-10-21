package com.example.amazonMages;

import android.media.MediaPlayer;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.example.androidstudio2dgamedevelopment.R;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MusicService", "Service criado");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Libera o MediaPlayer se já estiver em uso
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        // Cria e inicia o MediaPlayer com o recurso de áudio
        mediaPlayer = MediaPlayer.create(this, R.raw.videoplayback); // Troque pelo seu arquivo de música
        mediaPlayer.setLooping(true); // Faz a música tocar em loop
        mediaPlayer.start(); // Começa a tocar a música
        Log.d("MusicService", "Música começou a tocar");

        return START_STICKY; // Garante que o serviço será reiniciado caso o Android o mate
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Log.d("MusicService", "Música parada e MediaPlayer liberado");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Não necessário para este caso
    }
}

