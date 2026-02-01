package com.secret.blackholeglow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import java.util.ArrayList;
import java.util.List;

import com.secret.blackholeglow.activities.MainActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * LoginActivity - Pantalla de inicio de sesión con Google
 *
 * Permite al usuario autenticarse con su cuenta de Google
 * y guarda su información para usar en la app y wallpaper
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_PERMISSIONS = 9002;

    // Firebase
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // UI
    private Button btnSignIn;
    private Button btnSkip;
    private ProgressBar progressBar;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ╔════════════════════════════════════════════════════════════╗
        // ║  🎨 CAMBIAR DE SPLASH THEME A TEMA NORMAL                 ║
        // ║  Esto permite que el contenido de la Activity se vea bien ║
        // ╚════════════════════════════════════════════════════════════╝
        // El splash con fondo negro y logo ya se mostró (instantáneamente)
        // Ahora cambiamos al tema normal para el contenido de la Activity
        setTheme(R.style.Theme_Blackholeglow);

        // 🔧 FIX Android 15: Habilitar Edge-to-Edge ANTES de super.onCreate()
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Verificar si el usuario ya tiene sesión iniciada
        UserManager userManager = UserManager.getInstance(this);
        if (userManager.isLoggedIn()) {
            Log.d(TAG, "✓ Usuario ya tiene sesión iniciada, redirigiendo a MainActivity");
            goToMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        // 🎨 Aplicar insets para que el contenido no quede tapado por las barras del sistema
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar vistas
        btnSignIn = findViewById(R.id.btn_sign_in);
        btnSkip = findViewById(R.id.btn_skip);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);

        // Configurar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configurar Google Sign-In
        // NOTA: Si requestIdToken falla, significa que necesitas configurar
        // el OAuth 2.0 Client ID en Firebase Console
        GoogleSignInOptions gso;
        try {
            String webClientId = getString(getResources().getIdentifier(
                    "default_web_client_id", "string", getPackageName()));
            gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
        } catch (Exception e) {
            // Fallback: Solo solicitar email (sin Firebase Auth)
            Log.w(TAG, "⚠ No se encontró default_web_client_id, usando modo básico");
            gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
        }

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Botón de inicio de sesión
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        // Botón skip - continuar sin login
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Skip login - continuando sin autenticacion");
                Toast.makeText(LoginActivity.this, "Continuando sin login", Toast.LENGTH_SHORT).show();
                goToMainActivity();
            }
        });

        Log.d(TAG, "LoginActivity creada");
    }

    /**
     * Inicia el flujo de Google Sign-In
     */
    private void signIn() {
        showLoading(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Inicio de sesión exitoso, autenticar con Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "✓ Google Sign-In exitoso: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Inicio de sesión falló
                Log.e(TAG, "✗ Google Sign-In falló: " + e.getStatusCode(), e);
                showLoading(false);
                Toast.makeText(this, "Error al iniciar sesión: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Autentica con Firebase usando el token de Google
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Autenticación exitosa
                        Log.d(TAG, "✓ Firebase Auth exitoso");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        // Autenticación falló
                        Log.e(TAG, "✗ Firebase Auth falló", task.getException());
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "Autenticación fallida", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    /**
     * Actualiza la UI después del login
     */
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // Guardar datos del usuario
            String userId = user.getUid();
            String userName = user.getDisplayName();
            String userEmail = user.getEmail();
            String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

            UserManager userManager = UserManager.getInstance(this);
            userManager.saveUserData(userId, userName, userEmail, photoUrl);
            userManager.printUserInfo();

            // Login completo, ir a MainActivity
            showLoading(false);
            Toast.makeText(this, "¡Bienvenido, " + userName + "!", Toast.LENGTH_SHORT).show();
            goToMainActivity();
        }
    }

    /**
     * Navega a MainActivity después de verificar permisos
     */
    private void goToMainActivity() {
        // Solicitar permisos necesarios antes de ir a MainActivity
        requestAppPermissions();
    }

    /**
     * Solicita los permisos necesarios para la app
     */
    private void requestAppPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // 🎵 Permiso para audio (ecualizador)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        // 📷 Permiso de galería REMOVIDO - función deshabilitada para Play Store

        if (!permissionsNeeded.isEmpty()) {
            Log.d(TAG, "📋 Solicitando " + permissionsNeeded.size() + " permisos: " + permissionsNeeded);
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    RC_PERMISSIONS);
        } else {
            Log.d(TAG, "✓ Todos los permisos ya están concedidos");
            navigateToMain();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_PERMISSIONS) {
            // Verificar qué permisos fueron concedidos
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "✓ Permiso concedido: " + permissions[i]);
                } else {
                    Log.w(TAG, "✗ Permiso denegado: " + permissions[i]);
                }
            }
            // Continuar a MainActivity aunque algunos permisos sean denegados
            navigateToMain();
        }
    }

    /**
     * Navega a MainActivity (llamado después de permisos)
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Muestra/oculta el indicador de carga
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSignIn.setEnabled(!show);
        tvStatus.setText(show ? "Iniciando sesión..." : "");
    }
}
