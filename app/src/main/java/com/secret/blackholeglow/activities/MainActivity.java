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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.EdgeToEdge;
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
import com.secret.blackholeglow.fragments.DiagnosticFragment;
import com.secret.blackholeglow.systems.AdsManager;


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
        // 🔧 FIX Android 15: Habilitar Edge-to-Edge ANTES de super.onCreate()
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // 0️⃣ Inicializar Firebase Auth y Google Sign-In
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 💰 Inicializar AdsManager (MobileAds) - en background para no bloquear UI
        new Thread(() -> {
            AdsManager.init(this);
            Log.d(TAG, "💰 AdsManager inicializado en background");
        }, "AdsInit").start();

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
        //    ┌────────────────────────────────────────────────────────────────────┐
        //    │ Si no tiene permiso RECORD_AUDIO, lanzar MusicPermissionActivity │
        //    └────────────────────────────────────────────────────────────────────┘
        checkAudioPermission();

        // 6️⃣ Pedir permiso de notificaciones (Android 13+)
        checkNotificationPermission();
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

    /**
     * 🔔 Pide permiso de notificaciones (requerido en Android 13+)
     */
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
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

        } else if (id == R.id.nav_notifications) {
            // 🎵 Abrir configuración de acceso a notificaciones para detectar música
            openNotificationListenerSettings();
            return true;

        } else if (id == R.id.nav_diagnostic) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DiagnosticFragment())
                    .commit();

        } else if (id == R.id.nav_language) {
            drawerLayout.closeDrawer(GravityCompat.START);
            showLanguageDialog();
            return true;

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
                .setTitle("⚠️ Delete my account")
                .setMessage("This action is PERMANENT and IRREVERSIBLE.\n\n" +
                        "The following will be deleted:\n" +
                        "• Your user profile\n" +
                        "• Your saved favorite wallpapers\n" +
                        "• All your preferences\n\n" +
                        "Are you sure you want to continue?")
                .setPositiveButton("Yes, delete", (dialog, which) -> {
                    // Cerrar drawer antes de proceder
                    drawerLayout.closeDrawer(GravityCompat.START);
                    // Proceder a eliminar la cuenta
                    deleteAccount();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
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
     * 🎵 Abre la configuración de acceso a notificaciones
     * Necesario para detectar la música que está reproduciendo el usuario
     *
     * COMPATIBLE CON:
     * - Samsung, Huawei, Xiaomi, Oppo, Vivo, OnePlus, Google Pixel
     * - Android 5.0+ (API 21+)
     */
    private void openNotificationListenerSettings() {
        // Cerrar el drawer primero
        drawerLayout.closeDrawer(GravityCompat.START);

        // Verificar si ya tiene el permiso
        boolean hasPermission = isNotificationListenerEnabled();

        String title, message;
        if (hasPermission) {
            title = "✅ Music Detection Active";
            message = "Music detection is already enabled!\n\n" +
                    "When you play music on Spotify, YouTube Music or another app, " +
                    "you can share it by tapping the heart ♥ on the wallpaper.\n\n" +
                    "Would you like to open settings anyway?";
        } else {
            title = "🎵 Set Up Music Detection";
            message = "To share the song you\'re listening to, we need access to notifications.\n\n" +
                    "📱 On the next screen:\n" +
                    "1. Look for \"Orbix iA Live Wallpapers\"\n" +
                    "2. Toggle the switch on\n" +
                    "3. Confirm in the dialog that appears";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(hasPermission ? "Open Settings" : "Set Up", (dialog, which) -> {
                    openNotificationAccessSettings();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * 🔍 Verifica si el permiso de NotificationListener está habilitado
     */
    private boolean isNotificationListenerEnabled() {
        String packageName = getPackageName();
        String flat = android.provider.Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");

        if (flat != null && !flat.isEmpty()) {
            String[] listeners = flat.split(":");
            for (String listener : listeners) {
                if (listener.contains(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 📱 Abre la configuración de acceso a notificaciones
     * Con fallbacks para diferentes dispositivos y versiones de Android
     */
    private void openNotificationAccessSettings() {
        // Intentar abrir la configuración de notification listeners
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        } catch (android.content.ActivityNotFoundException e) {
            android.util.Log.w("MainActivity", "ACTION_NOTIFICATION_LISTENER_SETTINGS no disponible");
        }

        // Fallback 1: Intentar abrir configuración de notificaciones de la app
        try {
            android.content.Intent intent = new android.content.Intent();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", getPackageName());
                intent.putExtra("app_uid", getApplicationInfo().uid);
            }
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        } catch (android.content.ActivityNotFoundException e) {
            android.util.Log.w("MainActivity", "APP_NOTIFICATION_SETTINGS no disponible");
        }

        // Fallback 2: Abrir configuración general de la aplicación
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // Mostrar instrucciones adicionales
            android.widget.Toast.makeText(this,
                    "Look for 'Notifications' or 'Notification access' on this screen",
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        } catch (android.content.ActivityNotFoundException e) {
            android.util.Log.w("MainActivity", "APPLICATION_DETAILS_SETTINGS no disponible");
        }

        // Fallback 3: Abrir configuración general del sistema
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.provider.Settings.ACTION_SETTINGS);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            android.widget.Toast.makeText(this,
                    "Go to Apps → Orbix iA → Notifications → Notification access",
                    android.widget.Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(this,
                    "Could not open settings. Please do it manually from Settings.",
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Muestra un diálogo para seleccionar el idioma de la app.
     */
    private void showLanguageDialog() {
        String[] languages = {
                getString(R.string.language_english),
                getString(R.string.language_spanish)
        };
        String[] tags = {"en", "es"};

        new AlertDialog.Builder(this)
                .setTitle(R.string.language_dialog_title)
                .setItems(languages, (dialog, which) -> {
                    LocaleListCompat locales = LocaleListCompat.forLanguageTags(tags[which]);
                    AppCompatDelegate.setApplicationLocales(locales);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Muestra un diálogo de confirmación antes de cerrar sesión.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?\n\nYou will need to sign in again the next time you open the app.")
                .setPositiveButton("Yes, log out", (dialog, which) -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    performLogout();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
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
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

        // Cerrar sesión de Firebase
        mAuth.signOut();

        // Cerrar sesión de Google
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            // Limpiar datos del UserManager
            UserManager.getInstance(MainActivity.this).logout();

            Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();

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
            Toast.makeText(this, "No authenticated user", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar mensaje de progreso
        Toast.makeText(this, "Deleting account...", Toast.LENGTH_SHORT).show();

        // Primero, re-autenticar al usuario para evitar el error "requires recent authentication"
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            Toast.makeText(this, "Error: could not get Google account", Toast.LENGTH_SHORT).show();
            return;
        }

        String idToken = account.getIdToken();
        if (idToken == null) {
            Toast.makeText(this, "Error: could not get authentication token", Toast.LENGTH_SHORT).show();
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
                                                    "Account deleted successfully. Closing app...",
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
                                        String errorMsg = "Error deleting account: ";
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
                                "Error verifying your identity. Please log out and sign in again before deleting your account.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
