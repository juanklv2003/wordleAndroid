// El código Java ha sido actualizado para eliminar referencias a los botones de CHECK y CHANGE WORD
// y utiliza showCustomToast() en lugar de Toast.makeText().

package com.example.wordle;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer; // Nuevo import
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
    private WordLoader wordLoader;

    private EditText letra1, letra2, letra3, letra4, letra5;

    private int intentos = 0;
    private int recordIntentos = Integer.MAX_VALUE;
    private int vidas = 6;
    private TextView tvVidas;
    private TextView counter, record;

    // Objeto MediaPlayer para la música de fondo
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wordLoader = new WordLoader(this);

        // 1. Inicializar vistas (DEBE SER PRIMERO para evitar NullPointer en la carga)
        initViews();
        setupRecyclerView();
        setupListeners();

        // 2. Configurar música de fondo
        setupMusic();

        // 3. Cargar estado que usa las vistas
        cargarRecord();
        cargarEstado();

        // Lógica de inicio
        if (palabraSecreta == null || palabraSecreta.isEmpty()) {
            cargarNuevaPalabra();
        } else {
            actualizarScore();
            actualizarVidas();
        }
    }

    private void initViews() {
        letra1 = findViewById(R.id.letra1);
        letra2 = findViewById(R.id.letra2);
        letra3 = findViewById(R.id.letra3);
        letra4 = findViewById(R.id.letra4);
        letra5 = findViewById(R.id.letra5);
        tvVidas = findViewById(R.id.tvVidas);
        counter = findViewById(R.id.counter);
        record = findViewById(R.id.record);
        recycler = findViewById(R.id.recycler);
    }

    // Nuevo método para configurar la música
    private void setupMusic() {
        // Asegúrate de que tienes un archivo llamado musica_fondo.mp3 en res/raw/
        mediaPlayer = MediaPlayer.create(this, R.raw.musica_fondo);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true); // Repetir la música indefinidamente
            mediaPlayer.start();
        } else {
            // Manejo de errores si el recurso no se encuentra
            showCustomToast("Error: No se encontró el archivo de música.");
        }
    }

    private void setupRecyclerView() {
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttemptAdapter(this, listaIntentos);
        recycler.setAdapter(adapter);
    }

    // Método para mostrar el Toast personalizado (usa res/layout/custom_toast.xml)
    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast,
                findViewById(R.id.custom_toast_container));

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }


    // Listener de acción del editor (Enter/Next/Done del teclado virtual)
    private final TextView.OnEditorActionListener editorListener = (v, actionId, event) -> {
        // Verificar si la acción es Next o Done (depende de la configuración XML)
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
            validaPalabra();

            // Ocultar el teclado después de validar para una mejor UX
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            return true; // El evento ha sido manejado
        }
        return false;
    };

    // Listener de tecla (Aún se mantiene para teclados físicos o ciertos comportamientos)
    private final View.OnKeyListener enterListener = (v, keyCode, event) -> {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            validaPalabra();
            return true;
        }
        return false;
    };

    private void setupListeners() {
        // Aplicar OnEditorActionListener (teclado virtual) a TODAS las casillas
        letra1.setOnEditorActionListener(editorListener);
        letra2.setOnEditorActionListener(editorListener);
        letra3.setOnEditorActionListener(editorListener);
        letra4.setOnEditorActionListener(editorListener);
        letra5.setOnEditorActionListener(editorListener);

        // Mantenemos OnKeyListener (para teclados físicos o fallbacks)
        letra1.setOnKeyListener(enterListener);
        letra2.setOnKeyListener(enterListener);
        letra3.setOnKeyListener(enterListener);
        letra4.setOnKeyListener(enterListener);
        letra5.setOnKeyListener(enterListener);

        setupAutoFocus();
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
            showCustomToast("Nueva palabra cargada");
            intentos = 0;
            vidas = 6;
            actualizarVidas();
            limpiarCasillas();
            actualizarScore();

            bloquearUI(false);
            listaIntentos.clear();
            adapter.notifyDataSetChanged();
        } else {
            showCustomToast("Error cargando palabras");
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
            showCustomToast("Escribe 5 letras");
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
        limpiarCasillas();
    }

    private void procesarIntento(String intento) {
        List<String> colores = new ArrayList<>(Arrays.asList("", "", "", "", ""));
        char[] secreto = palabraSecreta.toCharArray();
        char[] intentoArray = intento.toCharArray();

        // 1. Marcar VERDES (Posición y letra correctas)
        for (int i = 0; i < 5; i++) {
            if (intentoArray[i] == secreto[i]) {
                colores.set(i, "verde");
                secreto[i] = '-';
                intentoArray[i] = '*';
            }
        }

        // 2. Marcar AMARILLOS (Letra correcta, posición incorrecta) y GRISES (Letra incorrecta)
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
                colores.set(i, encontrada ? "amarillo" : "gris");
            }
        }

        // Mostrar colores en las casillas del intento actual
        for(int i = 0; i < 5; i++) {
            EditText et = getEditTextPorIndice(i);
            int colorRes = getResources().getColor(
                    colores.get(i).equals("verde") ? R.color.verde :
                            colores.get(i).equals("amarillo") ? R.color.amarillo :
                                    R.color.gris);
            et.setBackgroundColor(colorRes);
        }

        // Mostrar en el RecyclerView
        Attempt attemptObj = new Attempt(intento, colores);
        listaIntentos.add(0, attemptObj);
        adapter.notifyItemInserted(0);
        recycler.scrollToPosition(0);
    }

    private void mostrarVictoria() {
        if (intentos < recordIntentos) {
            recordIntentos = intentos;
            guardarRecord();
            actualizarRecord();
        }

        showCustomToast("¡Correcto en " + intentos + " intentos!");
        bloquearUI(true);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("GRANDE ACERTASTE!!!!")
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
                .setTitle("GAME OVER")
                .setMessage("\nLa palabra era: " + palabraSecreta)
                .setCancelable(false)
                .setPositiveButton("NEW WORD", (dialog, which) -> {
                    cargarNuevaPalabra();
                    bloquearUI(false);
                    dialog.dismiss();
                })
                .show();
    }

    private void bloquearUI(boolean bloquear) {
        letra1.setEnabled(!bloquear);
        letra2.setEnabled(!bloquear);
        letra3.setEnabled(!bloquear);
        letra4.setEnabled(!bloquear);
        letra5.setEnabled(!bloquear);
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
        } else {
            record.setText("RECORD: --");
        }
    }

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

    private void vibrar(int ms) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

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
        // Pausar música
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reanudar música
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Liberar recursos
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
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
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }
}