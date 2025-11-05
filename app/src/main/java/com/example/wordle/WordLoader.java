package com.example.wordle;

import android.content.Context;

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

    public WordLoader(Context context) {
        loadWordsFromAssets(context);
    }

    private void loadWordsFromAssets(Context context) {
        try {
            InputStream is = context.getAssets().open("palabras.json");
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
        }
    }

    public String getRandomWord() {
        if (palabras.isEmpty()) return null;
        int index = random.nextInt(palabras.size());
        return palabras.get(index);
    }

    public List<String> getAllWords() {
        return palabras;
    }
}


