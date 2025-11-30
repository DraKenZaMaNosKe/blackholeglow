package com.secret.blackholeglow.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.secret.blackholeglow.ClapDetectorService;
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
    // ║ 👏 Variables del Detector de Aplausos                  ║
    // ╚════════════════════════════════════════════════════════╝
    private SwitchCompat clapSwitch;
    private TextView clapStatusText;
    private static final int MICROPHONE_PERMISSION_REQUEST = 100;

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
        // │ 🛠️ 2) Configurar RecyclerView VERTICAL (1 a la vez)│
        // └───────────────────────────────────────────────────┘
        RecyclerView recyclerView = view.findViewById(R.id.wallpaper_recycler_view);

        // LinearLayout vertical: muestra 1 wallpaper a la vez (scroll vertical)
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setInitialPrefetchItemCount(3); // Prefetch 3 items adelante
        recyclerView.setLayoutManager(layoutManager);

        // ⚡ Optimizaciones de rendimiento MEJORADAS
        recyclerView.setHasFixedSize(true); // Tamaño fijo mejora rendimiento
        recyclerView.setItemViewCacheSize(3); // Cache de 3 items (menos que antes, pero más eficiente)

        // RecycledViewPool para reutilizar vistas eficientemente
        androidx.recyclerview.widget.RecyclerView.RecycledViewPool viewPool =
            new androidx.recyclerview.widget.RecyclerView.RecycledViewPool();
        viewPool.setMaxRecycledViews(0, 5); // Pool de 5 vistas del tipo 0
        recyclerView.setRecycledViewPool(viewPool);

        // Habilitar nested scrolling para mejor rendimiento
        recyclerView.setNestedScrollingEnabled(true);

        // ┌───────────────────────────────────────────────────┐
        // │ 🌟 3) Cargar datos simulados                     │
        // └───────────────────────────────────────────────────┘
        wallpaperItems = getWallpapersList();

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
        // │ 👏 5) Configurar Detector de Aplausos             │
        // └───────────────────────────────────────────────────┘
        // DESHABILITADO TEMPORALMENTE - Funcionalidad para versión futura
        // setupClapDetector(view);

        // ┌───────────────────────────────────────────────────┐
        // │ ✅ 6) Retornar vista configurada                  │
        // └───────────────────────────────────────────────────┘
        return view;
    }

    // ╔════════════════════════════════════════════════════════╗
    // ║ 👏 setupClapDetector: Configurar UI y lógica          ║
    // ╚════════════════════════════════════════════════════════╝
    private void setupClapDetector(View view) {
        clapSwitch = view.findViewById(R.id.switch_clap_detector);
        clapStatusText = view.findViewById(R.id.text_clap_status);

        // Configurar listener del switch
        clapSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Usuario quiere activar: verificar permisos
                if (checkMicrophonePermission()) {
                    startClapDetectorService();
                } else {
                    requestMicrophonePermission();
                    clapSwitch.setChecked(false); // Desmarcar hasta obtener permiso
                }
            } else {
                // Usuario quiere desactivar
                stopClapDetectorService();
            }
        });
    }

    // ╔════════════════════════════════════════════════════════╗
    // ║ 🎤 Verificar permiso de micrófono                      ║
    // ╚════════════════════════════════════════════════════════╝
    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    // ╔════════════════════════════════════════════════════════╗
    // ║ 🎤 Solicitar permiso de micrófono                      ║
    // ╚════════════════════════════════════════════════════════╝
    private void requestMicrophonePermission() {
        requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO},
                MICROPHONE_PERMISSION_REQUEST
        );
    }

    // ╔════════════════════════════════════════════════════════╗
    // ║ 🎤 Resultado de solicitud de permisos                 ║
    // ╚════════════════════════════════════════════════════════╝
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MICROPHONE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido: activar servicio
                clapSwitch.setChecked(true);
                startClapDetectorService();
                Toast.makeText(requireContext(),
                        "✅ Permiso concedido. ¡Aplaude 4 veces rápido para probar!",
                        Toast.LENGTH_LONG).show();
            } else {
                // Permiso denegado
                Toast.makeText(requireContext(),
                        "⚠️ Se necesita permiso de micrófono para detectar aplausos",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ╔════════════════════════════════════════════════════════╗
    // ║ 🚀 Iniciar servicio de detección                      ║
    // ╚════════════════════════════════════════════════════════╝
    private void startClapDetectorService() {
        Intent serviceIntent = new Intent(requireContext(), ClapDetectorService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }

        clapStatusText.setText("🟢 Servicio activo - Escuchando aplausos...");
        clapStatusText.setTextColor(0xFF4CAF50); // Verde

        Toast.makeText(requireContext(),
                "👏 Detector activado! Aplaude 4 veces rápido para probar 🔊",
                Toast.LENGTH_LONG).show();
    }

    // ╔════════════════════════════════════════════════════════╗
    // ║ 🛑 Detener servicio de detección                      ║
    // ╚════════════════════════════════════════════════════════╝
    private void stopClapDetectorService() {
        Intent serviceIntent = new Intent(requireContext(), ClapDetectorService.class);
        requireContext().stopService(serviceIntent);

        clapStatusText.setText("⚪ Servicio desactivado");
        clapStatusText.setTextColor(0xFF808080); // Gris

        Toast.makeText(requireContext(),
                "Detector de aplausos desactivado",
                Toast.LENGTH_SHORT).show();
    }

    // ╔════════════════════════════════════════════════════════╗
    // ║ 🔍 getWallpapersList: Generar datos de prueba       ║
    // ╚════════════════════════════════════════════════════════╝
    /**
     * getWallpapersList:
     *   • Crea una lista de WallpaperItem con ejemplos de fondos animados.
     *   • Cada ítem incluye un drawable, un título y una breve descripción.
     *
     * @return Lista mutable de objetos WallpaperItem.
     */
    private List<WallpaperItem> getWallpapersList() {
        List<WallpaperItem> list = new ArrayList<>();

        // ╔════════════════════════════════════════════════════════════╗
        // ║  🎨 CATÁLOGO DE WALLPAPERS - 2 ESCENAS DISPONIBLES        ║
        // ║  Experiencias visuales hipnotizantes en OpenGL            ║
        // ╚════════════════════════════════════════════════════════════╝

        // 1️⃣ UNIVERSO - Sistema solar con planetas y OVNI
        list.add(new WallpaperItem(
                R.drawable.universo03,
                "Universo",
                "Flota entre planetas orbitantes y estrellas danzantes en un universo infinito. Tu avatar explora el cosmos en tiempo real con efectos de brillo pulsante."
        ));

        // 2️⃣ OCEAN PEARL - Fondo del mar con ostra y perla
        list.add(new WallpaperItem(
                R.drawable.agujero_negro,  // TODO: Crear preview del oceano
                "Ocean Pearl",
                "Sumérgete en las profundidades del océano. Una perla mágica brilla dentro de una ostra mientras peces danzan entre rayos de luz solar."
        ));

        // 3️⃣ BATALLA CÓSMICA - Escena modular (igual que Universo pero con nueva arquitectura)
        list.add(new WallpaperItem(
                R.drawable.universo03,
                "Batalla Cósmica",
                "Defiende la Tierra de meteoritos mientras el OVNI patrulla el cosmos. Sistema de combate espacial con escudos y armas láser."
        ));

        return list;
    }
}
