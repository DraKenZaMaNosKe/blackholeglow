package com.secret.blackholeglow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.adapters.WallpaperAdapter;
import com.secret.blackholeglow.models.WallpaperItem;

import java.util.ArrayList;
import java.util.List;

public class AnimatedWallpaperListFragment extends Fragment {

    private List<WallpaperItem> wallpaperItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_animated_wallpapers, container, false);

        // 1. Referencia del RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.wallpaper_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. Cargar wallpapers (simulados)
        wallpaperItems = getSampleWallpapers();

        // 3. Crear adaptador
        WallpaperAdapter adapter = new WallpaperAdapter(getContext(), wallpaperItems, item -> {
            // Aquí lanzamos la vista previa
            // En el siguiente paso lo programaremos
        });

        recyclerView.setAdapter(adapter);

        return view;
    }

    // Devuelve una lista de prueba de wallpapers
    private List<WallpaperItem> getSampleWallpapers() {
        List<WallpaperItem> list = new ArrayList<>();
        list.add(new WallpaperItem(R.drawable.aurora_boreal, "Aurora Boreal", "Luces del norte en movimiento."));
        list.add(new WallpaperItem(R.drawable.star_glow, "Estrellas", "Cielo lleno de partículas brillantes."));
        list.add(new WallpaperItem(R.drawable.estrellas_fugaces, "Fugaces", "Estrellas fugaces cruzando el cielo."));
        return list;
    }
}