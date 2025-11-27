package com.secret.blackholeglow.activities;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.secret.blackholeglow.GeminiService;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.UserManager;

/**
 * GeminiChatActivity - Chat flotante con Gemini AI
 *
 * Se muestra como overlay transparente sobre el wallpaper
 * Permite chatear con Orbix directamente desde la pantalla de inicio
 */
public class GeminiChatActivity extends AppCompatActivity {

    private EditText etMessage;
    private TextView tvResponse;
    private TextView tvUserMessage;
    private ImageButton btnSend;
    private ImageButton btnClose;
    private ProgressBar progressBar;
    private View chatContainer;

    private GeminiService geminiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hacer la Activity flotante y transparente
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(0.7f);

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_gemini_chat);

        // Hacer que la ventana sea más pequeña y centrada
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER;
        getWindow().setAttributes(params);

        // Inicializar vistas
        etMessage = findViewById(R.id.et_message);
        tvResponse = findViewById(R.id.tv_response);
        tvUserMessage = findViewById(R.id.tv_user_message);
        btnSend = findViewById(R.id.btn_send);
        btnClose = findViewById(R.id.btn_close);
        progressBar = findViewById(R.id.progress_bar);
        chatContainer = findViewById(R.id.chat_container);

        // Inicializar Gemini
        geminiService = GeminiService.getInstance();
        UserManager userManager = UserManager.getInstance(this);
        if (userManager.isLoggedIn()) {
            geminiService.setUserName(userManager.getFirstName());
        }

        // Botón enviar
        btnSend.setOnClickListener(v -> sendMessage());

        // Enter para enviar
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Botón cerrar
        btnClose.setOnClickListener(v -> finish());

        // Mostrar saludo inicial de Orbix
        showInitialGreeting();
    }

    private void showInitialGreeting() {
        tvResponse.setText("¡Hola! Soy Orbix, tu asistente cósmico. ¿En qué puedo ayudarte?");
        tvUserMessage.setVisibility(View.GONE);
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (message.isEmpty()) return;

        // Mostrar mensaje del usuario
        tvUserMessage.setText(message);
        tvUserMessage.setVisibility(View.VISIBLE);

        // Limpiar input y mostrar loading
        etMessage.setText("");
        showLoading(true);
        tvResponse.setText("Pensando...");

        // Enviar a Gemini
        geminiService.chat(message, new GeminiService.GeminiCallback() {
            @Override
            public void onResponse(String response) {
                showLoading(false);
                tvResponse.setText(response);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                tvResponse.setText("Oops, las estrellas están desalineadas. Intenta de nuevo.");
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!show);
        etMessage.setEnabled(!show);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
