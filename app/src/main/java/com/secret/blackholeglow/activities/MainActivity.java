package com.secret.blackholeglow.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.secret.blackholeglow.LoginActivity;
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

    // ╔══════════════════════════════════════════════════════╗
    // ║   🔐 Firebase Auth para gestionar autenticación      ║
    // ╚══════════════════════════════════════════════════════╝
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final String TAG = "MainActivity";

    // ╔════════════════════════════════════════════════════════╗
    // ║ 🌟 Método onCreate: Inicialización de la Activity     ║
    // ╚════════════════════════════════════════════════════════╝
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🎨 Habilitar Edge-to-Edge (borde a borde)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // 0️⃣ Inicializar Firebase Auth y Google Sign-In
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

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

        // 🎨 Aplicar insets al contenedor principal (LinearLayout con Toolbar)
        // para que el Toolbar no quede tapado por la barra de estado del sistema.
        // El LinearLayout es el primer hijo del DrawerLayout (índice 0)
        View mainContainer = drawerLayout.getChildAt(0);
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Solo aplicar padding superior para el Toolbar
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

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

        } else if (id == R.id.nav_logout) {
            // Cerrar sesión
            showLogoutDialog();
            return true;

        } else if (id == R.id.nav_delete_account) {
            // Mostrar diálogo de confirmación para eliminar cuenta
            showDeleteAccountDialog();
            // No cerramos el drawer aquí, se cerrará después del diálogo
            return true;
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

    // ╔═════════════════════════════════════════════════════════════════════╗
    // ║ 🗑️ Método showDeleteAccountDialog: Muestra diálogo de confirmación  ║
    // ╚═════════════════════════════════════════════════════════════════════╝
    /**
     * Muestra un diálogo de confirmación antes de eliminar la cuenta del usuario.
     * Si el usuario confirma, procede a eliminar la cuenta de Firebase y Google.
     */
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar mi cuenta")
                .setMessage("Esta acción es PERMANENTE e IRREVERSIBLE.\n\n" +
                        "Se eliminarán:\n" +
                        "• Tu perfil de usuario\n" +
                        "• Tus fondos favoritos guardados\n" +
                        "• Todas tus preferencias\n\n" +
                        "¿Estás seguro de que deseas continuar?")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                    // Cerrar drawer antes de proceder
                    drawerLayout.closeDrawer(GravityCompat.START);
                    // Proceder a eliminar la cuenta
                    deleteAccount();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // Cerrar drawer
                    drawerLayout.closeDrawer(GravityCompat.START);
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    // ╔═════════════════════════════════════════════════════════════════════╗
    // ║ 🚪 Método showLogoutDialog: Muestra diálogo de cerrar sesión        ║
    // ╚═════════════════════════════════════════════════════════════════════╝
    /**
     * Muestra un diálogo de confirmación antes de cerrar sesión.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?\n\nTendrás que iniciar sesión nuevamente la próxima vez que abras la app.")
                .setPositiveButton("Sí, cerrar sesión", (dialog, which) -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    performLogout();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    // ╔═════════════════════════════════════════════════════════════════════╗
    // ║ 🚪 Método performLogout: Cierra sesión del usuario                  ║
    // ╚═════════════════════════════════════════════════════════════════════╝
    /**
     * Cierra la sesión de Firebase y Google, limpia datos locales y redirige al login.
     */
    private void performLogout() {
        Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();

        // Cerrar sesión de Firebase
        mAuth.signOut();

        // Cerrar sesión de Google
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            // Limpiar datos del UserManager
            UserManager.getInstance(MainActivity.this).logout();

            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

            // Redirigir a LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // ╔═════════════════════════════════════════════════════════════════════╗
    // ║ 🔥 Método deleteAccount: Elimina la cuenta de Firebase              ║
    // ╚═════════════════════════════════════════════════════════════════════╝
    /**
     * Elimina la cuenta del usuario de Firebase Authentication y cierra sesión de Google.
     * Re-autentica automáticamente al usuario antes de eliminar para evitar errores.
     * Después cierra la app completamente.
     */
    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar mensaje de progreso
        Toast.makeText(this, "Eliminando cuenta...", Toast.LENGTH_SHORT).show();

        // Primero, re-autenticar al usuario para evitar el error "requires recent authentication"
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            Toast.makeText(this, "Error: no se pudo obtener la cuenta de Google", Toast.LENGTH_SHORT).show();
            return;
        }

        String idToken = account.getIdToken();
        if (idToken == null) {
            Toast.makeText(this, "Error: no se pudo obtener el token de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear credencial con el token de Google
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        // Re-autenticar al usuario
        user.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        Log.d(TAG, "Re-autenticación exitosa, procediendo a eliminar cuenta...");

                        // Ahora eliminar la cuenta
                        user.delete()
                                .addOnCompleteListener(deleteTask -> {
                                    if (deleteTask.isSuccessful()) {
                                        Log.d(TAG, "Cuenta eliminada exitosamente de Firebase");

                                        // Cerrar sesión de Google
                                        mGoogleSignInClient.signOut().addOnCompleteListener(signOutTask -> {
                                            Log.d(TAG, "Sesión de Google cerrada");

                                            // Limpiar datos del UserManager
                                            UserManager.getInstance(MainActivity.this).logout();

                                            // Mostrar mensaje de éxito
                                            Toast.makeText(MainActivity.this,
                                                    "Cuenta eliminada exitosamente. Cerrando app...",
                                                    Toast.LENGTH_LONG).show();

                                            // Esperar un momento para que se vea el Toast y luego cerrar la app
                                            new android.os.Handler().postDelayed(() -> {
                                                // Cerrar la app completamente
                                                finishAffinity(); // Cierra todas las activities
                                                System.exit(0); // Termina el proceso
                                            }, 2000); // 2 segundos de delay
                                        });
                                    } else {
                                        // Error al eliminar cuenta
                                        Log.e(TAG, "Error al eliminar cuenta", deleteTask.getException());
                                        String errorMsg = "Error al eliminar la cuenta: ";
                                        if (deleteTask.getException() != null) {
                                            errorMsg += deleteTask.getException().getMessage();
                                        }
                                        Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        // Error en la re-autenticación
                        Log.e(TAG, "Error en re-autenticación", reauthTask.getException());
                        Toast.makeText(MainActivity.this,
                                "Error al verificar tu identidad. Por favor, cierra sesión e inicia sesión nuevamente antes de eliminar tu cuenta.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
