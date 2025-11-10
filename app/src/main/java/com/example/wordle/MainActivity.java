package com.example.wordle;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration; // Import para cambiar idioma
import android.content.res.Resources; // Import para cambiar idioma
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu; // Import para el Menú
import android.view.MenuItem; // Import para el Menú
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog; // Import para Diálogos
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import para el Toolbar
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale; // Import para cambiar idioma

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
    private Toolbar toolbar; // Variable para el Toolbar

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cargar el idioma guardado ANTES de establecer la vista
        cargarIdiomaGuardado();

        setContentView(R.layout.activity_main);

        wordLoader = new WordLoader(this);

        // 1. Inicializar vistas
        initViews();

        // 2. Configurar el Toolbar como barra de acción
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Oculta el título "Wordle"
        }

        // 3. Configurar RecyclerView y Listeners
        setupRecyclerView();
        setupListeners();

        // 4. Configurar música de fondo
        setupMusic();

        // 5. Cargar estado que usa las vistas
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
        // Inicialización del Toolbar
        toolbar = findViewById(R.id.toolbar);

        letra1 = findViewById(R.id.letra1);
        letra2 = findViewById(R.id.letra2);
        letra3 = findViewById(R.id.letra3);
        letra4 = findViewById(R.id.letra4);
        letra5 = findViewById(R.id.letra5);
        tvVidas = findViewById(R.id.tvVidas);
        counter = findViewById(R.id.counter);
        record = findViewById(R.id.record);
        recycler = findViewById(R.id.recycler);

        // Establecer texto inicial usando strings.xml
        counter.setText(R.string.label_score);
        record.setText(R.string.label_record_empty);
    }

    // --- MANEJO DEL MENÚ (PASO 3B) ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Carga el archivo XML del menú (res/menu/main_menu.xml)
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Maneja los clics en los nuevos IDs del menú
        int itemId = item.getItemId();

        if (itemId == R.id.cambiarFondo) {
            mostrarDialogoFondo();
            return true;
        } else if (itemId == R.id.cambiarIdioma) {
            mostrarDialogoIdioma();
            return true;
        } else if (itemId == R.id.cambiarMusica) {
            mostrarDialogoMusica();
            return true;
        } else if (itemId == R.id.creditos) {
            mostrarCreditos();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // --- MÉTODOS DE LAS NUEVAS OPCIONES DEL MENÚ ---

    private void mostrarCreditos() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_credits) // Usa string
                .setMessage(R.string.dialog_message_about) // Usa string
                .setPositiveButton(R.string.dialog_button_close, (dialog, which) -> dialog.dismiss()) // Usa string
                .show();
    }

    private void mostrarDialogoIdioma() {
        // Define los idiomas que ofreces
        final String[] idiomas = {"Español", "English", "Galego"};
        final String[] codigosIdioma = {"es", "en", "gl"};

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_change_language) // Usa string
                .setItems(idiomas, (dialog, which) -> {
                    // 'which' es el índice (0=es, 1=en, 2=gl)
                    String codigoSeleccionado = codigosIdioma[which];
                    cambiarIdioma(codigoSeleccionado);
                })
                .show();
    }

    private void mostrarDialogoMusica() {
        // Lógica para cambiar la música (placeholder)
        showCustomToast("Función 'Cambiar Música' aún no implementada.");
    }

    private void mostrarDialogoFondo() {
        // Lógica para cambiar el fondo (placeholder)
        showCustomToast("Función 'Cambiar Fondo' aún no implementada.");
    }


    // --- LÓGICA DE CAMBIO DE IDIOMA ---

    private void guardarIdioma(String codigoIdioma) {
        SharedPreferences prefs = getSharedPreferences("WORDLE_PREFS", MODE_PRIVATE);
        prefs.edit().putString("idioma", codigoIdioma).apply();
    }

    private void cargarIdiomaGuardado() {
        SharedPreferences prefs = getSharedPreferences("WORDLE_PREFS", MODE_PRIVATE);
        String codigoIdioma = prefs.getString("idioma", "es");
        establecerIdioma(codigoIdioma);
    }

    private void cambiarIdioma(String codigoIdioma) {
        guardarIdioma(codigoIdioma);
        // Necesitamos reiniciar la actividad para que los cambios de idioma surtan efecto
        recreate();
    }

    private void establecerIdioma(String codigoIdioma) {
        Locale locale = new Locale(codigoIdioma);
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }



    private void setupMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.musica_fondo);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } else {
            // Usa string
            showCustomToast(getString(R.string.toast_error_loading_words));
        }
    }

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

    private void setupRecyclerView() {
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttemptAdapter(this, listaIntentos);
        recycler.setAdapter(adapter);
    }

    private final TextView.OnEditorActionListener editorListener = (v, actionId, event) -> {
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
            validaPalabra();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            return true;
        }
        return false;
    };

    private final View.OnKeyListener enterListener = (v, keyCode, event) -> {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            validaPalabra();
            return true;
        }
        return false;
    };

    private void setupListeners() {
        letra1.setOnEditorActionListener(editorListener);
        letra2.setOnEditorActionListener(editorListener);
        letra3.setOnEditorActionListener(editorListener);
        letra4.setOnEditorActionListener(editorListener);
        letra5.setOnEditorActionListener(editorListener);
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
            // Usa string
            showCustomToast(getString(R.string.toast_new_word_loaded));
            intentos = 0;
            vidas = 6;
            actualizarVidas();
            limpiarCasillas();
            actualizarScore();

            bloquearUI(false);
            listaIntentos.clear();
            adapter.notifyDataSetChanged();
        } else {
            // Usa string
            showCustomToast(getString(R.string.toast_error_loading_words));
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
            // Usa string
            showCustomToast(getString(R.string.toast_5_letters_please));
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
        // ... (Esta lógica interna no tiene texto) ...
        List<String> colores = new ArrayList<>(Arrays.asList("", "", "", "", ""));
        char[] secreto = palabraSecreta.toCharArray();
        char[] intentoArray = intento.toCharArray();
        for (int i = 0; i < 5; i++) {
            if (intentoArray[i] == secreto[i]) {
                colores.set(i, "verde");
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
                colores.set(i, encontrada ? "amarillo" : "gris");
            }
        }
        for(int i = 0; i < 5; i++) {
            EditText et = getEditTextPorIndice(i);
            int colorRes = getResources().getColor(
                    colores.get(i).equals("verde") ? R.color.verde :
                            colores.get(i).equals("amarillo") ? R.color.amarillo :
                                    R.color.gris);
            et.setBackgroundColor(colorRes);
        }
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

        // Usa string con formato
        showCustomToast(getString(R.string.toast_correct_guess, intentos));
        bloquearUI(true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_victory) // Usa string
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_button_new_word, (dialog, which) -> { // Usa string
                    cargarNuevaPalabra();
                    bloquearUI(false);
                    dialog.dismiss();
                })
                .show();
    }

    private void mostrarGameOver() {
        bloquearUI(true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_game_over) // Usa string
                .setMessage(getString(R.string.dialog_message_game_over, palabraSecreta)) // Usa string con formato
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_button_new_word, (dialog, which) -> { // Usa string
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
        // Usa string con formato
        counter.setText(getString(R.string.label_score_update, intentos));
    }

    private void actualizarRecord() {
        if (recordIntentos != Integer.MAX_VALUE) {
            // Usa string con formato
            record.setText(getString(R.string.label_record, recordIntentos));
        } else {
            // Usa string
            record.setText(R.string.label_record_empty);
        }
    }

    // ... (El resto de métodos no tienen texto visible) ...
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
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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