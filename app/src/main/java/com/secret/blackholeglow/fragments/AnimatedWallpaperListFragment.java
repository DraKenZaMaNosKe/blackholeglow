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

/*
╔════════════════════════════════════════════════════════════════╗
║                     🌌 AnimatedWallpaperListFragment.java      ║
║           «Saga de Géminis – Dualidad Cósmica»                ║
╚════════════════════════════════════════════════════════════════╝
║ 🔭 Descripción:                                              ║
║   • Fragmento que despliega la lista de wallpapers animados. ║
║   • Actúa como la pantalla principal: aquí el usuario elige   ║
║     qué fondo de pantalla desea previsualizar o aplicar.     ║
║ 🔗 Función básica: Inflar layout, cargar datos y enlazar     ║
║   RecyclerView con WallpaperAdapter.                         ║
╚════════════════════════════════════════════════════════════════╝
*/
public class AnimatedWallpaperListFragment extends Fragment {

    // ╔════════════════════════════════════════════════════════╗
    // ║ 📜 Variables Miembro: Lista de Wallpapers              ║
    // ╚════════════════════════════════════════════════════════╝
    /**
     * wallpaperItems:
     *   • Lista de objetos WallpaperItem (drawable, título, descripción).
     *   • Representa las "estrellas gemelas" de Géminis que guían
     *     el RecyclerView.
     */
    private List<WallpaperItem> wallpaperItems;

    // ╔════════════════════════════════════════════════════════╗
    // ║ ⚙️ onCreateView: Inflar y Configurar UI                ║
    // ╚════════════════════════════════════════════════════════╝
    /**
     * onCreateView:
     *   • Se invoca para construir la vista del fragmento.
     *   • Infla fragment_animated_wallpapers.xml.
     *   • Configura RecyclerView con LinearLayoutManager.
     *   • Carga datos de prueba y enlaza el adaptador.
     *
     * @param inflater           Convierte XML en objetos View.
     * @param container          ViewGroup padre (puede ser null).
     * @param savedInstanceState Estado previo (puede ser null).
     * @return Vista raíz ya inflada y preparada.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        // ┌───────────────────────────────────────────────────┐
        // │ 🎨 1) Inflar layout: fragment_animated_wallpapers │
        // └───────────────────────────────────────────────────┘
        View view = inflater.inflate(
                R.layout.fragment_animated_wallpapers,
                container,
                false
        );

        // ┌───────────────────────────────────────────────────┐
        // │ 🛠️ 2) Configurar RecyclerView                   │
        // └───────────────────────────────────────────────────┘
        RecyclerView recyclerView = view.findViewById(R.id.wallpaper_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // ┌───────────────────────────────────────────────────┐
        // │ 🌟 3) Cargar datos simulados                     │
        // └───────────────────────────────────────────────────┘
        wallpaperItems = getSampleWallpapers();

        // ┌───────────────────────────────────────────────────┐
        // │ 🔌 4) Crear y asignar adaptador                   │
        // └───────────────────────────────────────────────────┘
        WallpaperAdapter adapter = new WallpaperAdapter(
                getContext(),
                wallpaperItems,
                item -> {
                    // ➤ Callback al pulsar: lanzar previsualización
                    //   (implementación en siguiente etapa).
                }
        );
        recyclerView.setAdapter(adapter);

        // ┌───────────────────────────────────────────────────┐
        // │ ✅ 5) Retornar vista configurada                  │
        // └───────────────────────────────────────────────────┘
        return view;
    }

    // ╔════════════════════════════════════════════════════════╗
    // ║ 🔍 getSampleWallpapers: Generar datos de prueba       ║
    // ╚════════════════════════════════════════════════════════╝
    /**
     * getSampleWallpapers:
     *   • Crea una lista de WallpaperItem con ejemplos de fondos animados.
     *   • Cada ítem incluye un drawable, un título y una breve descripción.
     *
     * @return Lista mutable de objetos WallpaperItem.
     */
    private List<WallpaperItem> getSampleWallpapers() {
        List<WallpaperItem> list = new ArrayList<>();

        // • Ejemplo 1: Aurora Boreal
        list.add(new WallpaperItem(
                R.drawable.aurora_boreal,
                "FondoNulo",
                "Luces del norte en movimiento cósmico."
        ));

        // • Ejemplo 2: Estrellas
        list.add(new WallpaperItem(
                R.drawable.star_glow,
                "Fondoxx",
                "Cielo lleno de partículas brillantes."
        ));

        // • Ejemplo 3: Estrellas Fugaces
        list.add(new WallpaperItem(
                R.drawable.estrellas_fugaces,
                "Fondoxxx",
                "Estrellas fugaces cruzando el cielo."
        ));

        return list;
    }
}
