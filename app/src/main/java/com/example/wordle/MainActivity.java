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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private AttemptAdapter adapter;
    private ArrayList<Attempt> listaIntentos = new ArrayList<>();

    private String palabraSecreta = "";
    private Button nuevaPalabra, validar;
    private WordLoader wordLoader;

    private EditText letra1, letra2, letra3, letra4, letra5;

    private int intentos = 0;
    private int recordIntentos = Integer.MAX_VALUE;

    private TextView counter, record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wordLoader = new WordLoader(this);

        letra1 = findViewById(R.id.letra1);
        letra2 = findViewById(R.id.letra2);
        letra3 = findViewById(R.id.letra3);
        letra4 = findViewById(R.id.letra4);
        letra5 = findViewById(R.id.letra5);

        recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttemptAdapter(this, listaIntentos);
        recycler.setAdapter(adapter);

        letra1.setOnKeyListener(enterListener);
        letra2.setOnKeyListener(enterListener);
        letra3.setOnKeyListener(enterListener);
        letra4.setOnKeyListener(enterListener);
        letra5.setOnKeyListener(enterListener);

        setupAutoFocus();

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
            Toast.makeText(this, "Nueva palabra cargada", Toast.LENGTH_SHORT).show();
            limpiarCasillas();
            intentos = 0;
            actualizarScore();
        } else {
            Toast.makeText(this, "Error cargando palabras", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiarCasillas() {
        EditText[] letras = {letra1, letra2, letra3, letra4, letra5};

        for (EditText txt : letras) {
            txt.setText("");
            txt.setBackgroundColor(getResources().getColor(android.R.color.white));
        }

        letra1.requestFocus();
    }

    private void validaPalabra() {
        vibrar(100);
        if (palabraSecreta == null || palabraSecreta.length() != 5) {
            Toast.makeText(this, "Primero pulsa 'Nueva Palabra'", Toast.LENGTH_SHORT).show();
            intentos--; // Para no contar intento inválido
            actualizarScore();
            return;
        }

        String intento = (letra1.getText().toString() +
                letra2.getText().toString() +
                letra3.getText().toString() +
                letra4.getText().toString() +
                letra5.getText().toString()).toUpperCase();

        if (intento.length() != 5) {
            Toast.makeText(this, "Escribe 5 letras", Toast.LENGTH_SHORT).show();
            return;
        }

        intentos++;
        actualizarScore();

        List<String> colores = new ArrayList<>(Arrays.asList("", "", "", "", ""));

        char[] secreto = palabraSecreta.toCharArray();
        char[] intentoArray = intento.toCharArray();

        // VERDES
        for (int i = 0; i < 5; i++) {
            if (intentoArray[i] == secreto[i]) {
                getEditTextPorIndice(i).setBackgroundColor(getResources().getColor(R.color.verde));
                colores.set(i, "verde");
                secreto[i] = '-';
                intentoArray[i] = '*';
            }
        }

        // AMARILLOS Y GRISES
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
                    colores.set(i, "amarillo");
                } else {
                    getEditTextPorIndice(i).setBackgroundColor(getResources().getColor(R.color.gris));
                    colores.set(i, "gris");
                }
            }
        }

        Attempt attemptObj = new Attempt(intento, colores);
        listaIntentos.add(0, attemptObj);
        adapter.notifyItemInserted(0);
        recycler.scrollToPosition(0);

        if (intento.equals(palabraSecreta)) {
            if (intentos < recordIntentos) {
                recordIntentos = intentos;
                actualizarRecord();
            }

            Toast.makeText(this, "¡Correcto en " + intentos + " intentos!", Toast.LENGTH_LONG).show();
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
        counter.setText("SCORE: " + intentos);
    }

    private void actualizarRecord() {
        if (recordIntentos != Integer.MAX_VALUE) {
            record.setText("RECORD: " + recordIntentos);
        }
    }

    private void setupAutoFocus() {
        setAutoMove(letra1, letra2);
        setAutoMove(letra2, letra3);
        setAutoMove(letra3, letra4);
        setAutoMove(letra4, letra5);
    }

    private void setAutoMove(EditText actual, EditText siguiente) {
        actual.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) siguiente.requestFocus();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    View.OnKeyListener enterListener = (v, keyCode, event) -> {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            validaPalabra();
            return true;
        }
        return false;
    };

    private void vibrar(int ms) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null)
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
    }
}
