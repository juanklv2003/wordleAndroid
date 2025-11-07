package com.example.wordle;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttemptAdapter extends RecyclerView.Adapter<AttemptAdapter.ViewHolder> {

    private Context context;
    private List<Attempt> listaIntentos;

    public AttemptAdapter(Context context, List<Attempt> listaIntentos) {
        this.context = context;
        this.listaIntentos = listaIntentos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_attempt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attempt attempt = listaIntentos.get(position);

        // Mostrar letras
        String palabra = attempt.getPalabra();
        holder.cell1.setText(String.valueOf(palabra.charAt(0)));
        holder.cell2.setText(String.valueOf(palabra.charAt(1)));
        holder.cell3.setText(String.valueOf(palabra.charAt(2)));
        holder.cell4.setText(String.valueOf(palabra.charAt(3)));
        holder.cell5.setText(String.valueOf(palabra.charAt(4)));

        // Mostrar colores
        setColor(holder.cell1, attempt.getColores().get(0));
        setColor(holder.cell2, attempt.getColores().get(1));
        setColor(holder.cell3, attempt.getColores().get(2));
        setColor(holder.cell4, attempt.getColores().get(3));
        setColor(holder.cell5, attempt.getColores().get(4));
    }

    @Override
    public int getItemCount() {
        return listaIntentos.size();
    }

    public void updateList(List<Attempt> nuevaLista) {
        this.listaIntentos.clear();
        this.listaIntentos.addAll(nuevaLista);
        notifyDataSetChanged();
    }

    private void setColor(View cell, String colorName) {
        int color;

        switch (colorName) {
            case "verde":
                color = context.getResources().getColor(R.color.verde);
                break;
            case "amarillo":
                color = context.getResources().getColor(R.color.amarillo);
                break;
            case "gris":
            default:
                color = context.getResources().getColor(R.color.gris);
                break;
        }

        cell.setBackgroundColor(color);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView cell1, cell2, cell3, cell4, cell5;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cell1 = itemView.findViewById(R.id.cell1);
            cell2 = itemView.findViewById(R.id.cell2);
            cell3 = itemView.findViewById(R.id.cell3);
            cell4 = itemView.findViewById(R.id.cell4);
            cell5 = itemView.findViewById(R.id.cell5);
        }
    }
}
