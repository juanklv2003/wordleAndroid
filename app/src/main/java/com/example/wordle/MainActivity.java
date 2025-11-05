package com.example.wordle;

import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private String palabraSecreta = "";
    private Button nuevaPalabra, validar;
    private WordLoader wordLoader;

    private EditText letra1, letra2, letra3, letra4, letra5;

    // Score y record
    private int intentos = 0;
    private int recordIntentos = Integer.MAX_VALUE;

    private TextView counter, record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wordLoader = new WordLoader(this);

        //encontrar por id
        letra1 = findViewById(R.id.letra1);
        letra2 = findViewById(R.id.letra2);
        letra3 = findViewById(R.id.letra3);
        letra4 = findViewById(R.id.letra4);
        letra5 = findViewById(R.id.letra5);

        //validar con la tecla enter
        letra1.setOnKeyListener(enterListener);
        letra2.setOnKeyListener(enterListener);
        letra3.setOnKeyListener(enterListener);
        letra4.setOnKeyListener(enterListener);
        letra5.setOnKeyListener(enterListener);

        setupAutoFocus();//inicializar el salto automatico

        counter = findViewById(R.id.counter);
        record = findViewById(R.id.record);

        nuevaPalabra = findViewById(R.id.change);
        validar = findViewById(R.id.button);

        nuevaPalabra.setOnClickListener(v -> cargarNuevaPalabra());
        validar.setOnClickListener(v -> validaPalabra());
    }

    private void cargarNuevaPalabra() {
        palabraSecreta = wordLoader.getRandomWord();
        if (palabraSecreta != null) {
            Toast.makeText(this, "Palabra cambiada", Toast.LENGTH_SHORT).show();
            limpiarCasillas();
            intentos = 0;
            actualizarScore();
        } else {
            Toast.makeText(this, "Error cargando palabras", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiarCasillas() {
        letra1.setText(""); letra2.setText(""); letra3.setText(""); letra4.setText(""); letra5.setText("");
        letra1.setBackgroundColor(getResources().getColor(android.R.color.white));
        letra2.setBackgroundColor(getResources().getColor(android.R.color.white));
        letra3.setBackgroundColor(getResources().getColor(android.R.color.white));
        letra4.setBackgroundColor(getResources().getColor(android.R.color.white));
        letra5.setBackgroundColor(getResources().getColor(android.R.color.white));
    }

    private void validaPalabra() {
        vibrar(100);//llamamos al metodo vibrar
        intentos++;
        actualizarScore();

        String l1 = letra1.getText().toString().toUpperCase();
        String l2 = letra2.getText().toString().toUpperCase();
        String l3 = letra3.getText().toString().toUpperCase();
        String l4 = letra4.getText().toString().toUpperCase();
        String l5 = letra5.getText().toString().toUpperCase();

        String intento = l1 + l2 + l3 + l4 + l5;

        if (intento.length() != 5) {
            Toast.makeText(this, "Escribe 5 letras", Toast.LENGTH_SHORT).show();
            intentos--; // No contar si no escribió 5 letras
            actualizarScore();
            return;
        }

        char[] secreto = palabraSecreta.toCharArray();
        char[] intentoArray = intento.toCharArray();

        for (int i = 0; i < 5; i++) {
            if (intentoArray[i] == secreto[i]) {
                getEditTextPorIndice(i).setBackgroundColor(getResources().getColor(R.color.verde));
                secreto[i] = '-';
                intentoArray[i] = '*';
            }
        }

        for (int i = 0; i < 5; i++) {
            if (intentoArray[i] != '*') {
                boolean encontrada = false;
                for (int j = 0; j < 5; j++) {
                    if (intentoArray[i] == secreto[j]) {
                        encontrada = true;
                        secreto[j] = '-';
                        break;
                    }
                }
                if (encontrada) {
                    getEditTextPorIndice(i).setBackgroundColor(getResources().getColor(R.color.amarillo));
                } else {
                    getEditTextPorIndice(i).setBackgroundColor(getResources().getColor(R.color.gris));
                }
            }
        }

        // Palabra correcta
        if (intento.equals(palabraSecreta)) {
            if (intentos < recordIntentos) {
                recordIntentos = intentos;
                actualizarRecord();
            }
            Toast.makeText(this, "Adivinaste en" + intentos + " intentos!", Toast.LENGTH_LONG).show();
            intentos = 0;
            actualizarScore();
        }
    }

    private EditText getEditTextPorIndice(int i) {
        switch (i) {
            case 0: return letra1;
            case 1: return letra2;
            case 2: return letra3;
            case 3: return letra4;
            default: return letra5;
        }
    }

    private void actualizarScore() {
        counter.setText("SCORE:  " + intentos);
    }

    private void actualizarRecord() {
        if (recordIntentos != Integer.MAX_VALUE) {
            record.setText("RECORD:  " + recordIntentos);
        }
    }

    private void setupAutoFocus(){
        setAutoMove(letra1,letra2);
        setAutoMove(letra2,letra3);
        setAutoMove(letra3,letra4);
        setAutoMove(letra4,letra5);

    }
    //metodo para ir saltando automaticamete de editText en editText
    private void setAutoMove(EditText actual, EditText siguiente) {
        actual.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    siguiente.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    //metodo para validar con la tecla enter con KeyEvent
    View.OnKeyListener enterListener = (v, keyCode, event) -> {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            validaPalabra();
            return true;
        }
        return false;
    };

    //funcion para vibrar al validar palabra con vibrator
    private void vibrar(int milisegundos) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
