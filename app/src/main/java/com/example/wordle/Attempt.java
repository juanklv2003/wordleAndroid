package com.example.wordle;

import java.util.List;

public class Attempt {
    private String palabra;
    private List<String> colores;

    public Attempt(String palabra, List<String> colores) {
        this.palabra = palabra;
        this.colores = colores;
    }

    public String getPalabra() {
        return palabra;
    }

    public List<String> getColores() {
        return colores;
    }
}
