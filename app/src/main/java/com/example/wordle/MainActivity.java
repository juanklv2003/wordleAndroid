package com.example.wordle;

import android.content.SharedPreferences;
import android.graphics.Color;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private AttemptAdapter adapter;
    private final ArrayList<Attempt> listaIntentos = new ArrayList<>();

    private String palabraSecreta = "";
    private Button nuevaPalabra, validar;
    private WordLoader wordLoader;

    private EditText letra1, letra2, letra3, letra4, letra5;

    private int intentos = 0;
    private int recordIntentos = Integer.MAX_VALUE;
    private int vidas = 6;
    private TextView tvVidas;
    private TextView counter, record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wordLoader = new WordLoader(this);

        // Inicialización de vistas
        initViews();
        setupRecyclerView();
        setupListeners();

        // Cargar datos guardados previamente
        cargarRecord();
        cargarEstado();

        // Si no hay palabra cargada, se inicia una nueva
        if (palabraSecreta == null || palabraSecreta.isEmpty()) {
            cargarNuevaPalabra();
        } else {
            actualizarScore();
            actualizarVidas();
        }
    }

    // Inicializa todos los elementos de la interfaz de usuario
    private void initViews() {
        letra1 = findViewById(R.id.letra1);
        letra2 = findViewById(R.id.letra2);
        letra3 = findViewById(R.id.letra3);
        letra4 = findViewById(R.id.letra4);
        letra5 = findViewById(R.id.letra5);
        tvVidas = findViewById(R.id.tvVidas);
        counter = findViewById(R.id.counter);
        record = findViewById(R.id.record);
        nuevaPalabra = findViewById(R.id.change);
        validar = findViewById(R.id.button);
        recycler = findViewById(R.id.recycler);
    }

    // Configura el RecyclerView y su adaptador
    private void setupRecyclerView() {
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttemptAdapter(this, listaIntentos);
        recycler.setAdapter(adapter);
    }

    // Configura los Click Listeners y Key Listeners
    private void setupListeners() {
        letra1.setOnKeyListener(enterListener);
        letra2.setOnKeyListener(enterListener);
        letra3.setOnKeyListener(enterListener);
        letra4.setOnKeyListener(enterListener);
        letra5.setOnKeyListener(enterListener);

        setupAutoFocus();

        nuevaPalabra.setOnClickListener(v -> cargarNuevaPalabra());
        validar.setOnClickListener(v -> validaPalabra());
    }

    private void actualizarVidas() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(i < vidas ? "❤️" : "🤍");
        }
        tvVidas.setText(sb.toString());
    }

    private void cargarNuevaPalabra() {
        palabraSecreta = wordLoader.getRandomWord();

        if (palabraSecreta != null && !palabraSecreta.isEmpty()) {
            Toast.makeText(this, "Nueva palabra cargada", Toast.LENGTH_SHORT).show();
            intentos = 0;
            vidas = 6;
            actualizarVidas();
            limpiarCasillas();
            actualizarScore();

            bloquearUI(false);
            listaIntentos.clear();
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Error cargando palabras", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiarCasillas() {
        EditText[] letras = {letra1, letra2, letra3, letra4, letra5};

        for (EditText txt : letras) {
            txt.setText("");
            txt.setBackgroundColor(Color.WHITE);
        }

        letra1.requestFocus();
    }

    private void validaPalabra() {
        vibrar(100);

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
        vidas--;
        actualizarVidas();
        actualizarScore();

        if (intento.equals(palabraSecreta)) {
            mostrarVictoria();
            return;
        }

        if (vidas <= 0) {
            mostrarGameOver();
            return;
        }

        procesarIntento(intento);
        limpiarCasillas(); // Limpiar después de procesar
    }

    // Lógica principal de comparación de Wordle (Verde, Amarillo, Gris)
    private void procesarIntento(String intento) {
        List<String> colores = new ArrayList<>(Arrays.asList("", "", "", "", ""));
        char[] secreto = palabraSecreta.toCharArray();
        char[] intentoArray = intento.toCharArray();

        // 1. Marcar VERDES
        for (int i = 0; i < 5; i++) {
            if (intentoArray[i] == secreto[i]) {
                colores.set(i, "verde");
                secreto[i] = '-'; // Marcar como usado
                intentoArray[i] = '*'; // Marcar como usado
            }
        }

        // 2. Marcar AMARILLOS y GRISES
        for (int i = 0; i < 5; i++) {
            if (intentoArray[i] != '*') { // Si no es verde
                boolean encontrada = false;
                for (int j = 0; j < 5; j++) {
                    if (intentoArray[i] == secreto[j]) {
                        encontrada = true;
                        secreto[j] = '-'; // Marcar como usado
                        break;
                    }
                }
                colores.set(i, encontrada ? "amarillo" : "gris");
            }
        }

        // Mostrar en el RecyclerView
        Attempt attemptObj = new Attempt(intento, colores);
        listaIntentos.add(0, attemptObj);
        adapter.notifyItemInserted(0);
        recycler.scrollToPosition(0);

        // Actualizar colores en las casillas del intento actual (opcional, pero se mantiene la lógica)
        for(int i = 0; i < 5; i++) {
            EditText et = getEditTextPorIndice(i);
            int colorRes = getResources().getColor(
                    colores.get(i).equals("verde") ? R.color.verde :
                            colores.get(i).equals("amarillo") ? R.color.amarillo :
                                    R.color.gris);
            et.setBackgroundColor(colorRes);
        }
    }

    private void mostrarVictoria() {
        if (intentos < recordIntentos) {
            recordIntentos = intentos;
            guardarRecord();
            actualizarRecord();
        }

        Toast.makeText(this, "¡Correcto en " + intentos + " intentos!", Toast.LENGTH_LONG).show();
        bloquearUI(true);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🎉 GRANDE ACERTASTE!!!! 🎉")
                .setCancelable(false)
                .setPositiveButton("NEW WORD", (dialog, which) -> {
                    cargarNuevaPalabra();
                    bloquearUI(false);
                    dialog.dismiss();
                })
                .show();
    }

    private void mostrarGameOver() {
        bloquearUI(true);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("GAME OVER ❌")
                .setMessage("\nLa palabra era: " + palabraSecreta)
                .setCancelable(false)
                .setPositiveButton("NEW WORD", (dialog, which) -> {
                    cargarNuevaPalabra();
                    bloquearUI(false);
                    dialog.dismiss();
                })
                .show();
    }

    // Habilita o deshabilita los elementos de la UI
    private void bloquearUI(boolean bloquear) {
        letra1.setEnabled(!bloquear);
        letra2.setEnabled(!bloquear);
        letra3.setEnabled(!bloquear);
        letra4.setEnabled(!bloquear);
        letra5.setEnabled(!bloquear);
        validar.setEnabled(!bloquear);
        nuevaPalabra.setEnabled(!bloquear);
    }

    // Devuelve el EditText correspondiente al índice
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
        } else {
            record.setText("RECORD: --");
        }
    }

    // Configura el movimiento automático de foco al escribir una letra
    private void setAutoMove(EditText actual, EditText siguiente) {
        actual.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) siguiente.requestFocus();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // Listener para validar al presionar Enter en cualquier casilla
    private final View.OnKeyListener enterListener = (v, keyCode, event) -> {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            validaPalabra();
            return true;
        }
        return false;
    };

    private void vibrar(int ms) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    // Configura el movimiento automático de foco y retroceso
    private void setupAutoFocus() {
        setAutoMove(letra1, letra2);
        setAutoMove(letra2, letra3);
        setAutoMove(letra3, letra4);
        setAutoMove(letra4, letra5);

        setAutoBackspace(letra2, letra1);
        setAutoBackspace(letra3, letra2);
        setAutoBackspace(letra4, letra3);
        setAutoBackspace(letra5, letra4);
    }

    // Configura el retroceso de foco al borrar la primera letra de una casilla
    private void setAutoBackspace(EditText actual, EditText anterior) {
        actual.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (actual.getText().length() == 0) {
                    anterior.requestFocus();
                    anterior.setText("");
                    return true;
                }
            }
            return false;
        });
    }

    // --- Persistencia de datos (SharedPreferences) ---

    private void guardarRecord() {
        getSharedPreferences("WORDLE_PREFS", MODE_PRIVATE)
                .edit()
                .putInt("record", recordIntentos)
                .apply();
    }

    private void cargarRecord() {
        recordIntentos = getSharedPreferences("WORDLE_PREFS", MODE_PRIVATE)
                .getInt("record", Integer.MAX_VALUE);
        actualizarRecord();
    }

    @Override
    protected void onPause() {
        super.onPause();
        guardarEstado();
    }

    private void guardarEstado() {
        SharedPreferences prefs = getSharedPreferences("WORDLE_DATA", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("palabraSecreta", palabraSecreta);
        editor.putInt("puntuacion", intentos);
        editor.putInt("vidas", vidas);

        Gson gson = new Gson();
        String jsonIntentos = gson.toJson(listaIntentos);
        editor.putString("intentos", jsonIntentos);

        editor.apply();
    }

    private void cargarEstado() {
        SharedPreferences prefs = getSharedPreferences("WORDLE_DATA", MODE_PRIVATE);

        palabraSecreta = prefs.getString("palabraSecreta", null);
        intentos = prefs.getInt("puntuacion", 0);
        vidas = prefs.getInt("vidas", 6);

        String jsonIntentos = prefs.getString("intentos", null);
        if (jsonIntentos != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Attempt>>() {}.getType();
            ArrayList<Attempt> cargados = gson.fromJson(jsonIntentos, type);

            listaIntentos.clear();
            listaIntentos.addAll(cargados);
            // El adaptador se inicializa en onCreate, si ya está cargado, lo actualizamos.
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }
}