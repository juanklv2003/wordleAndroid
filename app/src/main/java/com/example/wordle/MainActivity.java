package com.example.wordle;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Importaciones de JSON (ya las tenías en tu WordLoader)
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Importaciones de GSON (las usas para guardar el estado, así que las dejamos)
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private AttemptAdapter adapter;
    private final ArrayList<Attempt> listaIntentos = new ArrayList<>();

    private String palabraSecreta = "";
    private WordLoader wordLoader; // Se inicializa después de cargar el idioma

    private EditText letra1, letra2, letra3, letra4, letra5;

    private int intentos = 0;
    private int recordIntentos = Integer.MAX_VALUE;
    private int vidas = 6;
    private TextView tvVidas;
    private TextView counter, record;
    private Toolbar toolbar;

    private MediaPlayer mediaPlayer;

    // Variable para guardar el idioma actual
    private String currentLangCode = "es";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Cargar el idioma guardado ANTES de todo
        cargarIdiomaGuardado();

        setContentView(R.layout.activity_main);


        wordLoader = new WordLoader(this, currentLangCode);

        // 3. Inicializar vistas
        initViews();

        // 4. Configurar el Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Oculta el título
        }

        setupRecyclerView();
        setupListeners();

        setupMusic();

        cargarRecord();
        cargarEstado();

        if (palabraSecreta == null || palabraSecreta.isEmpty()) {
            cargarNuevaPalabra();
        } else {
            actualizarScore();
            actualizarVidas();
        }
    }

    private void initViews() {
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

        counter.setText(R.string.label_score);
        record.setText(R.string.label_record_empty);
    }

    // --- MANEJO DEL MENÚ ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
                .setTitle(R.string.menu_credits)
                .setMessage(R.string.dialog_message_about)
                .setPositiveButton(R.string.dialog_button_close, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void mostrarDialogoIdioma() {
        final String[] idiomas = {"Español", "English", "Galego"};
        final String[] codigosIdioma = {"es", "en", "gl"};

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_change_language)
                .setItems(idiomas, (dialog, which) -> {
                    String codigoSeleccionado = codigosIdioma[which];
                    // Comprobar si el idioma seleccionado es diferente al actual
                    if (!codigoSeleccionado.equals(currentLangCode)) {
                        cambiarIdioma(codigoSeleccionado);
                    }
                })
                .show();
    }

    private void mostrarDialogoMusica() {
        showCustomToast("Función 'Cambiar Música' aún no implementada.");
    }

    private void mostrarDialogoFondo() {
        showCustomToast("Función 'Cambiar Fondo' aún no implementada.");
    }

    // --- LÓGICA DE CAMBIO DE IDIOMA ---

    private void guardarIdioma(String codigoIdioma) {
        SharedPreferences prefs = getSharedPreferences("WORDLE_PREFS", MODE_PRIVATE);
        prefs.edit().putString("idioma", codigoIdioma).apply();
    }

    private void cargarIdiomaGuardado() {
        SharedPreferences prefs = getSharedPreferences("WORDLE_PREFS", MODE_PRIVATE);
        currentLangCode = prefs.getString("idioma", "es"); // Actualiza la variable de clase
        establecerIdioma(currentLangCode);
    }

    private void cambiarIdioma(String codigoIdioma) {
        guardarIdioma(codigoIdioma);
        // Actualiza la variable de clase antes de reiniciar
        currentLangCode = codigoIdioma;
        recreate(); // Reinicia la actividad para aplicar el idioma y cargar el nuevo JSON
    }

    private void establecerIdioma(String codigoIdioma) {
        Locale locale = new Locale(codigoIdioma);
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    // --- MÉTODOS DEL JUEGO (TRADUCIDOS) ---

    private void setupMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.musica_fondo);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } else {
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
        // wordLoader ya fue inicializado con el idioma correcto en onCreate
        palabraSecreta = wordLoader.getRandomWord();

        if (palabraSecreta != null && !palabraSecreta.isEmpty() && !palabraSecreta.equals("FALLO") && !palabraSecreta.equals("ERROR")) {
            showCustomToast(getString(R.string.toast_new_word_loaded));
            intentos = 0;
            vidas = 6;
            actualizarVidas();
            limpiarCasillas();
            actualizarScore();

            bloquearUI(false);
            listaIntentos.clear();
            if (adapter != null) { // Comprobación de nulidad
                adapter.notifyDataSetChanged();
            }
        } else {
            showCustomToast(getString(R.string.toast_error_loading_words));
            palabraSecreta = "ERROR"; // Pone una palabra de fallback
        }
    }

    private void limpiarCasillas() {
        EditText[] letras = {letra1, letra2, letra3, letra4, letra5};
        for (EditText txt : letras) {
            txt.setText("");
            txt.setBackgroundColor(Color.WHITE);
        }
        if (letra1 != null) {
            letra1.requestFocus();
        }
    }

    private void validaPalabra() {
        vibrar(100);

        String intento = (letra1.getText().toString() +
                letra2.getText().toString() +
                letra3.getText().toString() +
                letra4.getText().toString() +
                letra5.getText().toString()).toUpperCase();

        if (intento.length() != 5) {
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

        if (adapter != null) { // Comprobación de nulidad
            adapter.notifyItemInserted(0);
            recycler.scrollToPosition(0);
        }
    }

    private void mostrarVictoria() {
        if (intentos < recordIntentos) {
            recordIntentos = intentos;
            guardarRecord();
            actualizarRecord();
        }
        showCustomToast(getString(R.string.toast_correct_guess, intentos));
        bloquearUI(true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_victory)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_button_new_word, (dialog, which) -> {
                    cargarNuevaPalabra();
                    bloquearUI(false);
                    dialog.dismiss();
                })
                .show();
    }

    private void mostrarGameOver() {
        bloquearUI(true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_game_over)
                .setMessage(getString(R.string.dialog_message_game_over, palabraSecreta))
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_button_new_word, (dialog, which) -> {
                    cargarNuevaPalabra();
                    bloquearUI(false);
                    dialog.dismiss();
                })
                .show();
    }

    private void bloquearUI(boolean bloquear) {
        if (letra1 == null) return; // Comprobación de seguridad
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
        if (counter != null) { // Comprobación de nulidad
            counter.setText(getString(R.string.label_score_update, intentos));
        }
    }

    private void actualizarRecord() {
        if (record != null) { // Comprobación de nulidad
            if (recordIntentos != Integer.MAX_VALUE) {
                record.setText(getString(R.string.label_record, recordIntentos));
            } else {
                record.setText(R.string.label_record_empty);
            }
        }
    }

    private void setAutoMove(EditText actual, EditText siguiente) {
        actual.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && siguiente != null) siguiente.requestFocus();
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
                if (actual.getText().length() == 0 && anterior != null) {
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

        // No cargamos la palabra secreta si el idioma ha cambiado
        String idiomaGuardado = prefs.getString("idioma_estado", currentLangCode);
        if (idiomaGuardado.equals(currentLangCode)) {
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
            }
        } else {
            // Si el idioma cambió, no cargamos el estado anterior, forzamos una nueva palabra
            palabraSecreta = null;
            intentos = 0;
            vidas = 6;
            listaIntentos.clear();
        }

        // Actualizamos el idioma guardado
        prefs.edit().putString("idioma_estado", currentLangCode).apply();


        // Actualizamos la UI
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        actualizarVidas();
        actualizarScore();
    }
}