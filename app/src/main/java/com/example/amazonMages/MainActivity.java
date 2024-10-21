package com.example.amazonMages;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * MainActivity é o ponto de entrada principal da aplicação.
 */
public class MainActivity extends Activity {

    // Atributo que representa o jogo
    private Game game;

    /**
     * O método `onCreate` é chamado quando a atividade é criada.
     * Ele inicializa o jogo e define o layout da tela.
     *
     * @param savedInstanceState Contém o estado salvo anterior da atividade, se houver.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Registra uma mensagem no log para indicar que o método onCreate foi chamado
        Log.d("MainActivity.java", "onCreate()");
        super.onCreate(savedInstanceState);

        // Inicializa a classe Game e define o layout da tela para o jogo, permitindo que os objetos
        // da classe Game sejam renderizados na tela
        game = new Game(this);
        setContentView(game);
    }

    /**
     * O método `onStart` é chamado quando a atividade se torna visível para o usuário.
     */
    @Override
    protected void onStart() {
        Log.d("MainActivity.java", "onStart()");
        super.onStart();
    }

    /**
     * O método `onResume` é chamado quando a atividade começa a interagir com o usuário.
     */
    @Override
    protected void onResume() {
        Log.d("MainActivity.java", "onResume()");
        super.onResume();
    }

    /**
     * O método `onPause` é chamado quando a atividade é parcialmente visível (por exemplo, quando
     * outra atividade é iniciada ou a tela é bloqueada). Aqui, o jogo é pausado.
     */
    @Override
    protected void onPause() {
        Log.d("MainActivity.java", "onPause()");
        game.pause();  // Pausa o jogo
        super.onPause();
    }

    /**
     * O método `onStop` é chamado quando a atividade não é mais visível para o usuário.
     */
    @Override
    protected void onStop() {
        Log.d("MainActivity.java", "onStop()");
        super.onStop();
    }

    /**
     * O método `onDestroy` é chamado antes da destruição da atividade.
     */
    @Override
    protected void onDestroy() {
        Log.d("MainActivity.java", "onDestroy()");
        super.onDestroy();
    }

    /**
     * O método `onBackPressed` é sobrescrito para desabilitar o botão "Voltar" do Android.
     * O comportamento padrão é comentado, evitando que a atividade feche quando o botão for pressionado.
     */
    @Override
    public void onBackPressed() {
        // Comentando "super.onBackPressed()" para desabilitar o botão "Voltar"
        //super.onBackPressed();
    }
}
