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
        // │ 🛠️ 2) Configurar RecyclerView VERTICAL (1 a la vez)│
        // └───────────────────────────────────────────────────┘
        RecyclerView recyclerView = view.findViewById(R.id.wallpaper_recycler_view);
        // LinearLayout vertical: muestra 1 wallpaper a la vez (scroll vertical)
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // ⚡ Optimizaciones de rendimiento
        recyclerView.setHasFixedSize(true); // Tamaño fijo mejora rendimiento
        recyclerView.setItemViewCacheSize(4); // Cache de 4 items
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(android.view.View.DRAWING_CACHE_QUALITY_HIGH);

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
        // │ ✅ 5) Retornar vista configurada                  │
        // └───────────────────────────────────────────────────┘
        return view;
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
        // ║  🎨 11 WALLPAPERS TEMÁTICOS - CATÁLOGO VISUAL ÉPICO      ║
        // ║  Cada tema es único con efectos OpenGL hipnotizantes     ║
        // ╚════════════════════════════════════════════════════════════╝

        // 🪩 DISCO BALL - Visualizador musical interactivo ⭐ NUEVO!
        list.add(new WallpaperItem(
                R.drawable.agujero_negro,  // Placeholder - usaremos textura oscura
                "DiscoBall",
                "Bola disco con cuadritos espejo que gira hipnóticamente. Reacciona a tu música con efectos pulsantes y colores rainbow. La fiesta nunca termina! 🎵✨"
        ));

        // 1️⃣ ESPACIO - Universo con sistema solar completo
        list.add(new WallpaperItem(
                R.drawable.universo03,
                "Universo",
                "Flota entre planetas orbitantes y estrellas danzantes en un universo infinito. Tu avatar explora el cosmos en tiempo real con efectos de brillo pulsante."
        ));

        // 2️⃣ BOSQUE MÁGICO - Naturaleza encantada con luciérnagas
        list.add(new WallpaperItem(
                R.drawable.textura_roninplaneta,
                "🌲 Bosque Encantado",
                "Árboles místicos brillan con luciérnagas danzantes. Neblina mágica flota entre las ramas mientras la luna ilumina el camino. Efecto: partículas de luz flotando."
        ));

        // 3️⃣ CIUDAD CYBERPUNK - Metrópolis futurista nocturna
        list.add(new WallpaperItem(
                R.drawable.agujero_negro,
                "🏙️ Neo Tokyo 2099",
                "Rascacielos neón se elevan al cielo nocturno. Luces holográficas parpadean y vehículos vuelan entre edificios. Atmósfera: lluvia cyberpunk con reflejos."
        ));

        // 4️⃣ PLAYA TROPICAL - Atardecer en paraíso
        list.add(new WallpaperItem(
                R.drawable.textura_sol,
                "🏖️ Paraíso Dorado",
                "El sol se funde con el océano en un atardecer de ensueño. Olas brillantes acarician la arena mientras palmeras se mecen. Colores cálidos que hipnotizan."
        ));

        // 5️⃣ SAFARI SALVAJE - Animalitos en movimiento
        list.add(new WallpaperItem(
                R.drawable.textura_asteroide,
                "🦁 Safari Salvaje",
                "Leones, elefantes y jirafas deambulan bajo el sol africano. Siluetas de animales se mueven grácilmente en la sabana dorada. Vida en cada píxel."
        ));

        // 6️⃣ LLUVIA NOCTURNA - Bosque con tormenta
        list.add(new WallpaperItem(
                R.drawable.universo03,
                "🌧️ Lluvia Mística",
                "Gotas de lluvia caen entre árboles oscuros. Relámpagos iluminan el cielo mientras truenos resuenan. Ambiente: tormentoso y relajante a la vez."
        ));

        // 7️⃣ MUNDO RETRO - Videojuegos pixel art 8-bit
        list.add(new WallpaperItem(
                R.drawable.fondo_transparente,
                "🎮 Pixel Quest",
                "Personajes de 8-bit saltan y corren en un mundo retro. Monedas giran, bloques parpadean y enemigos patrullan. Nostalgia gaming en movimiento."
        ));

        // 8️⃣ AGUJERO NEGRO - Portal al vacío cósmico
        list.add(new WallpaperItem(
                R.drawable.agujero_negro,
                "🕳️ Portal Infinito",
                "Un agujero negro devora la luz con su disco de acreción brillante. El espacio-tiempo se curva ante tus ojos. Efecto: distorsión gravitacional hipnótica."
        ));

        // 9️⃣ JARDÍN ZEN - Serenidad con flores de cerezo
        list.add(new WallpaperItem(
                R.drawable.textura_roninplaneta,
                "🌸 Jardín Zen",
                "Pétalos de sakura flotan sobre un estanque tranquilo. Koi nadan en círculos mientras bambú se mece. Paz absoluta en cada fotograma."
        ));

        // 🔟 TORMENTA ELÉCTRICA - Poder de la naturaleza
        list.add(new WallpaperItem(
                R.drawable.textura_sol,
                "⚡ Furia Celestial",
                "Rayos fractales iluminan nubes tormentosas. Energía pura danza en el cielo mientras relámpagos explotan. El poder elemental al máximo."
        ));

        return list;
    }
}
