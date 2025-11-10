package com.example.wordle;

import android.content.Context;
import android.content.res.AssetManager; // Import para Assets
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordLoader {

    private List<String> palabras = new ArrayList<>();
    private Random random = new Random();

    // CAMBIO 1: El constructor AHORA ACEPTA el código de idioma
    public WordLoader(Context context, String langCode) {
        loadWordsFromAssets(context, langCode);
    }

    // CAMBIO 2: Nuevo método para elegir el archivo JSON
    private String getFileNameForLanguage(String langCode) {
        switch (langCode) {
            case "en":
                return "words_en.json";
            case "gl":
                return "words_gl.json";
            case "es":
            default:
                return "words_es.json";
        }
    }

    // CAMBIO 3: El método ahora usa el langCode para abrir el archivo correcto
    private void loadWordsFromAssets(Context context, String langCode) {

        // Obtiene el nombre del archivo (ej: "words_en.json")
        String fileName = getFileNameForLanguage(langCode);
        AssetManager assetManager = context.getAssets();

        try {
            InputStream is = assetManager.open(fileName); // Usa el nombre de archivo dinámico
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, "UTF-8");

            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonObject.getJSONArray("palabras");

            for (int i = 0; i < jsonArray.length(); i++) {
                palabras.add(jsonArray.getString(i).toUpperCase()); // Guardamos en mayúsculas
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            palabras.add("ERROR"); // Añade una palabra de fallback
        }
    }

    // CAMBIO 4: getRandomWord() actualizado para ser más seguro
    public String getRandomWord() {
        if (palabras.isEmpty() || palabras.get(0).equals("ERROR")) {
            return "FALLO"; // Devuelve un String, no null
        }
        int index = random.nextInt(palabras.size());
        return palabras.get(index);
    }

    public List<String> getAllWords() {
        return palabras;
    }
}