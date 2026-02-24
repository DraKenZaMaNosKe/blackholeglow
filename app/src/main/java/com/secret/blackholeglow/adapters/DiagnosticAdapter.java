package com.secret.blackholeglow.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.diagnostic.DiagnosticData;
import com.secret.blackholeglow.models.SceneWeight;

import java.util.List;

/**
 * Adapter para la lista de compatibilidad de wallpapers en el panel diagnostico.
 */
public class DiagnosticAdapter extends RecyclerView.Adapter<DiagnosticAdapter.CompatViewHolder> {

    private final List<DiagnosticData.WallpaperCompat> items;

    public DiagnosticAdapter(List<DiagnosticData.WallpaperCompat> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public CompatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diagnostic_wallpaper, parent, false);
        return new CompatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CompatViewHolder h, int position) {
        DiagnosticData.WallpaperCompat item = items.get(position);

        // Nombre
        h.name.setText(item.nombre);

        // Punto de color segun compatibilidad
        int dotColor;
        switch (item.level) {
            case OPTIMAL:        dotColor = 0xFF4CAF50; break; // verde
            case MODERATE:       dotColor = 0xFFFFC107; break; // amarillo
            case NOT_RECOMMENDED: dotColor = 0xFFF44336; break; // rojo
            default:             dotColor = 0xFF9E9E9E; break;
        }
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(dotColor);
        h.dot.setBackground(dot);

        // Badge de peso
        String weightLabel;
        int weightColor;
        switch (item.weight) {
            case LIGHT:
                weightLabel = "LIGERO";
                weightColor = 0xFF4CAF50;
                break;
            case HEAVY:
                weightLabel = "PESADO";
                weightColor = 0xFFF44336;
                break;
            default:
                weightLabel = "MEDIO";
                weightColor = 0xFFFFC107;
                break;
        }
        h.weightBadge.setText(weightLabel);
        h.weightBadge.setTextColor(weightColor);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CompatViewHolder extends RecyclerView.ViewHolder {
        final View dot;
        final TextView name;
        final TextView weightBadge;

        CompatViewHolder(@NonNull View itemView) {
            super(itemView);
            dot = itemView.findViewById(R.id.compat_dot);
            name = itemView.findViewById(R.id.wallpaper_name);
            weightBadge = itemView.findViewById(R.id.weight_badge);
        }
    }
}
