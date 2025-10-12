package com.secret.blackholeglow.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.navigation.NavigationView;
import com.secret.blackholeglow.MusicPermissionActivity;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.UserManager;
import com.secret.blackholeglow.fragments.AnimatedWallpaperListFragment;

/*
╔══════════════════════════════════════════════════════════════════════════════════╗
║                                                                                  ║
║      🌟 MainActivity.java – Actividad Principal con Drawer y Navegación 🌟          ║
║                                                                                  ║
║  ASCII Art:                                                                       ║
║      ┌──────────────────────────────────────────────────────────────────┐        ║
║      │  🏠  ╔══════╗   ☰  ║                                           │        ║
║      │      ║ Menú ║       ║                                               │        ║
║      │      ╚══════╝       ║    [ Fragment Container ]                      │        ║
║      │                     ║                                               │        ║
║      └──────────────────────────────────────────────────────────────────┘        ║
║                                                                                  ║
║   🔍 Descripción General:                                                          ║
║   • Activity que aloja un Toolbar superior y un DrawerLayout (menú lateral).      ║
║   • Permite seleccionar distintas secciones (Home, Animated, Favorites, Settings)║
║     a través de un NavigationView en el Drawer.                                    ║
║   • Al seleccionar un ítem, reemplaza el fragment container con el fragmento      ║
║     correspondiente (por ahora, todos usan AnimatedWallpaperListFragment como      ║
║     placeholder).                                                                 ║
║   • Maneja el comportamiento del botón atrás para cerrar el Drawer si está abierto.║
║                                                                                  ║
╚══════════════════════════════════════════════════════════════════════════════════╝
*/

/**
 * MainActivity – Actividad principal de la aplicación que contiene:
 *   • Toolbar personalizable como ActionBar.
 *   • DrawerLayout con íconos de navegación (NavigationView).
 *   • Un contenedor de fragmentos para mostrar distintos contenidos.
 *
 * ❤️ Bebé, aquí organizamos el menú lateral, el toolbar y la lógica de navegación.
 *
 * 📋 Buenas Prácticas:
 *   - Separar la lógica de UI (Toolbar, Drawer) de la lógica de navegación (FragmentManager).
 *   - Evitar duplicar código: los fragmentos temporales pueden sustituirse por instancias
 *     de sus propios fragments en lugar de repetir AnimatedWallpaperListFragment.
 *   - Manejar el estado al rotar la pantalla para no recargar innecesariamente el fragment.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // ╔══════════════════════════════════════════════════════╗
    // ║   🔧 Referencia al DrawerLayout para abrir/ cerrar el menú lateral ║
    // ╚══════════════════════════════════════════════════════╝
    private DrawerLayout drawerLayout;

    // ╔════════════════════════════════════════════════════════╗
    // ║ 🌟 Método onCreate: Inicialización de la Activity     ║
    // ╚════════════════════════════════════════════════════════╝
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // 1️⃣ Configurar Toolbar como ActionBar
        //    ┌───────────────────────────────────────────┐
        //    │ el Toolbar está definido en activity_main.xml │
        //    └───────────────────────────────────────────┘
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // ❤️ Ahora el Toolbar funciona como barra de acción principal

        // 2️⃣ Configurar DrawerLayout (menú lateral deslizable)
        //    ┌──────────────────────────────────────────────┐
        //    │ El DrawerLayout contiene el NavigationView  │
        //    └──────────────────────────────────────────────┘
        drawerLayout = findViewById(R.id.drawer_layout);

        // 2a. Asociar el NavigationView al listener de selección
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // ❤️ Bebé, "this" Activity implementa OnNavigationItemSelectedListener

        // 2b. Cargar avatar del usuario en el header del NavigationDrawer
        loadUserAvatar(navigationView);

        // 3️⃣ Configurar el botón “hamburguesa” que abre/cierra el Drawer
        //    ┌───────────────────────────────────────────────────┐
        //    │ ActionBarDrawerToggle se encarga de la animación │
        //    │ del ícono y sincroniza el estado del Drawer.      │
        //    └───────────────────────────────────────────────────┘
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,                  // Contexto—esta Activity
                drawerLayout,          // DrawerLayout a controlar
                toolbar,               // Toolbar que mostrará el ícono
                R.string.navigation_drawer_open,   // Texto para “Drawer abierto” (accesibilidad)
                R.string.navigation_drawer_close   // Texto para “Drawer cerrado”
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        // ❤️ Listo, el ícono de “☰” ya abre y cierra el Drawer sin problemas

        // 4️⃣ Selección por defecto al iniciar (solo si es la primera vez)
        //    ┌────────────────────────────────────────────────────────────────┐
        //    │ Evitamos recargar el fragment si venimos de rotación u otra  │
        //    │ configuración; solo cargamos el fragment inicial si saved   │
        //    │ instance es null.                                            │
        //    └────────────────────────────────────────────────────────────────┘
        if (savedInstanceState == null) {
            // Marcar el ítem "nav_animated" como seleccionado en el NavigationView
            navigationView.setCheckedItem(R.id.nav_home);

            // Reemplazar el fragment_container con AnimatedWallpaperListFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new AnimatedWallpaperListFragment()
                    )
                    .commit();
            // ❤️ Bebé, aquí mostramos por primera vez la lista de fondos animados
        }

        // 5️⃣ Verificar permisos de audio para music visualizer
        //    ┌────────────────────────────────────────────────────────────────┐
        //    │ Si no tiene permiso RECORD_AUDIO, lanzar MusicPermissionActivity │
        //    └────────────────────────────────────────────────────────────────┘
        checkAudioPermission();
    }

    /**
     * Verifica si tiene permiso de audio, y si no, lanza MusicPermissionActivity
     */
    private void checkAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                // No tiene permiso, lanzar MusicPermissionActivity
                Intent intent = new Intent(this, MusicPermissionActivity.class);
                startActivity(intent);
            }
        }
    }

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║ 🌈 Método onNavigationItemSelected: Maneja clicks en el menú lateral ║
    // ╚════════════════════════════════════════════════════════════════════╝
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Obtener el ID del ítem seleccionado
        int id = item.getItemId();

        // ❤️ Según el ítem, reemplazamos el fragment_container con el fragment correspondiente
        if (id == R.id.nav_home) {
            // Por ahora, usamos AnimatedWallpaperListFragment como placeholder para “Home”
            getSupportFragmentManager().beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new AnimatedWallpaperListFragment()  // temporal
                    )
                    .commit();

        } else if (id == R.id.nav_micuenta) {
            // Fragmento para la sección Animated (lista de wallpapers animados)
            getSupportFragmentManager().beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new AnimatedWallpaperListFragment()
                    )
                    .commit();

        }

        // Cerrar el Drawer después de seleccionar un ítem
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;  // devolvemos true porque el evento se manejó correctamente
    }

    // ╔═════════════════════════════════════════════════════════════════════╗
    // ║ 📲 Método onBackPressed: Cierra el Drawer si está abierto, sino sale ║
    // ╚═════════════════════════════════════════════════════════════════════╝
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            // Si el Drawer está abierto, cerrarlo en lugar de salir de la Activity
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Si no está abierto, realizar el comportamiento por defecto ("ir atrás")
            super.onBackPressed();
        }
    }

    /**
     * Carga el avatar del usuario en el header del NavigationDrawer
     */
    private void loadUserAvatar(NavigationView navigationView) {
        // Obtener UserManager
        UserManager userManager = UserManager.getInstance(this);

        // Verificar si el usuario está logueado
        if (!userManager.isLoggedIn()) {
            return; // No hay usuario logueado
        }

        // Obtener el header del NavigationView
        View headerView = navigationView.getHeaderView(0);

        // Referencias a las vistas del header
        ImageView ivAvatar = headerView.findViewById(R.id.iv_user_avatar);
        TextView tvName = headerView.findViewById(R.id.tv_user_name);
        TextView tvEmail = headerView.findViewById(R.id.tv_user_email);

        // Obtener datos del usuario
        String userName = userManager.getUserName();
        String userEmail = userManager.getUserEmail();
        String photoUrl = userManager.getUserPhotoUrl();

        // Actualizar textos
        tvName.setText(userName);
        tvEmail.setText(userEmail);

        // Cargar avatar con Glide
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(ivAvatar);
        }
    }
}
