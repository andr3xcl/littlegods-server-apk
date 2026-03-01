package com.example.statsweb;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;

public class ApiDocsActivity extends AppCompatActivity {

    private WaveView waveView;
    private LinearLayout apiContent;
    private View statusDot;
    private TextView statusText;
    private View statusContainer;
    private boolean isNightMode = true;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String HEALTH_CHECK_URL = "https://web.littlegods.space/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive/edge-to-edge setup
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        setContentView(R.layout.activity_api_docs);

        waveView = findViewById(R.id.waveView);
        apiContent = findViewById(R.id.apiContent);
        statusDot = findViewById(R.id.statusDot);
        statusText = findViewById(R.id.statusText);
        statusContainer = findViewById(R.id.statusContainer);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Get theme from intent or system
        isNightMode = getIntent().getBooleanExtra("night_mode", true);
        
        populateEndpoints();
        updateTheme(isNightMode);
        checkApiStatus();
    }

    private void checkApiStatus() {
        Request request = new Request.Builder()
                .url(HEALTH_CHECK_URL)
                .build();

        statusText.setText("Comprobando...");
        statusDot.setBackground(createDotDrawable(0xFF808080));

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> {
                    statusText.setText("Desconectado");
                    statusDot.setBackground(createDotDrawable(0xFFFF4444));
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handler.post(() -> {
                    if (response.isSuccessful()) {
                        statusText.setText("En línea");
                        statusDot.setBackground(createDotDrawable(0xFF00C853));
                    } else {
                        statusText.setText("Error de servidor");
                        statusDot.setBackground(createDotDrawable(0xFFFFBB33));
                    }
                });
            }
        });
        
        // Refresh every 30 seconds
        handler.postDelayed(this::checkApiStatus, 30000);
    }

    private GradientDrawable createDotDrawable(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        return gd;
    }

    private void updateTheme(boolean isNight) {
        if (waveView != null) waveView.setNightMode(isNight);

        int bgColor = isNight ? 0xFF0F0F0F : 0xFFE0F2F7;
        int titleColor = isNight ? 0xFFFFFFFF : 0xFF1A1A1A;
        int subtitleColor = isNight ? 0x99FFFFFF : 0x99000000;
        int sectionColor = isNight ? 0x4DFFFFFF : 0x4D000000;

        findViewById(R.id.scrollView).setBackgroundColor(bgColor);
        
        ((TextView)findViewById(R.id.apiTitle)).setTextColor(titleColor);
        ((TextView)findViewById(R.id.apiSubtitle)).setTextColor(subtitleColor);
        
        // Update status container
        updateCardStyle(statusContainer, isNight);
        statusText.setTextColor(titleColor);

        // Update items if they exist
        for (int i = 0; i < apiContent.getChildCount(); i++) {
            View v = apiContent.getChildAt(i);
            if (v instanceof TextView) { // Section title
                ((TextView) v).setTextColor(sectionColor);
            } else if (v instanceof LinearLayout) { // Card
                updateCardStyle(v, isNight);
            }
        }
    }

    private void updateCardStyle(View card, boolean isNight) {
        int bgColor = isNight ? 0x1AFFFFFF : 0xFFFFFFFF;
        int strokeColor = isNight ? 0x20FFFFFF : 0x1A000000;
        float elevation = isNight ? 0f : 8f;

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dpToPx(24));
        gd.setStroke(dpToPx(1), strokeColor);
        card.setBackground(gd);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dpToPx(elevation));
        }
        
        // Update nested texts
        updateNestedTexts((ViewGroup) card, isNight);
    }

    private void updateNestedTexts(ViewGroup group, boolean isNight) {
        int titleColor = isNight ? 0xFFFFFFFF : 0xFF1A1A1A;
        int subtitleColor = isNight ? 0x99FFFFFF : 0x99000000;

        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if (tv.getId() == R.id.path) {
                    tv.setTextColor(titleColor);
                } else if (tv.getId() == R.id.description) {
                    tv.setTextColor(subtitleColor);
                }
            } else if (child instanceof ViewGroup) {
                updateNestedTexts((ViewGroup) child, isNight);
            }
        }
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void populateEndpoints() {
        // Auth & Identity
        addSection("🔐 Autenticación e Identidad");
        addEndpoint("GET", "/discord", "Inicia el inicio de sesión con Discord OAuth2.");
        addEndpoint("GET", "/discord/callback", "Callback para el inicio de sesión de Discord.");
        addEndpoint("GET", "/logout", "Cierra la sesión del usuario actual.");
        addEndpoint("GET", "/me", "Devuelve el perfil del usuario (datos de Discord).");
        addEndpoint("GET", "/link-status", "Devuelve el estado de vinculación y cuentas de Plutonium.");
        addEndpoint("POST", "/link-web", "Vincula una cuenta de juego (playerName, guid, password).");
        addEndpoint("POST", "/switch-account", "Cambia la cuenta activa de Plutonium.");
        addEndpoint("POST", "/unlink", "Desvincula una o todas las cuentas de juego.");

        // Players & Stats
        addSection("👤 Jugadores y Estadísticas");
        addEndpoint("GET", "/search", "Admin: Busca jugadores por nombre o GUID.");
        addEndpoint("GET", "/search-public", "Búsqueda pública de jugadores (mín. 3 caracteres).");
        addEndpoint("GET", "/:identifier/overall", "Estadísticas, récords de mapas y partidas.");

        // Economy & Rewards
        addSection("💰 Economía y Recompensas");
        addEndpoint("GET", "/play/live-status", "Estado actual en el juego del jugador.");
        addEndpoint("POST", "/play/mystery-box", "Mystery box web (950 pts).");
        addEndpoint("POST", "/play/coin-flip", "Apuesta puntos a cara o cruz.");
        addEndpoint("POST", "/play/mystery-box-online", "Solicitud de envío de arma (2000 pts).");
        addEndpoint("POST", "/play/wunderfizz-online", "Solicitud de envío de ventaja (2000 pts).");
        addEndpoint("POST", "/play/pack-a-punch-online", "Mejora el arma actual en el juego (8000 pts).");
        addEndpoint("POST", "/transfer", "Transfiere puntos a otro jugador.");
        addEndpoint("POST", "/claim-daily", "Reclama la recompensa diaria de $5,000.");
        addEndpoint("POST", "/welcome-reward", "Reclama el regalo de bienvenida único de $30,000.");
        addEndpoint("GET", "/penalty-status", "Comprueba fondos perdidos por inactividad.");
        addEndpoint("POST", "/penalty-claim", "Recupera fondos de penalización.");
        
        // Missions
        addSection("🎯 Misiones");
        addEndpoint("GET", "/missions/global", "Lista misiones diarias, semanales y mensuales.");
        addEndpoint("POST", "/missions/:type/claim", "Reclama recompensa por misión completada.");
        addEndpoint("POST", "/missions/incentive/claim", "Reclama incentivo por actividad en Discord.");

        // Maps
        addSection("🗺️ Mapas");
        addEndpoint("GET", "/available", "Lista todos los mapas compatibles y sus metadatos.");
        addEndpoint("GET", "/current", "Mapa seleccionado actualmente en el perfil.");
        addEndpoint("POST", "/select", "Actualiza el mapa seleccionado para el usuario.");

        // GSC Integration
        addSection("🎮 Integración GSC (En el juego)");
        addEndpoint("POST", "/heartbeat", "Actualiza el estado en tiempo real de un GUID.");
        addEndpoint("GET", "/request/:guid", "El cliente en el juego consulta comandos.");
        addEndpoint("GET", "/bank/balance/:guid", "Respuesta simple de saldo (Texto/Plano).");
        addEndpoint("POST", "/bank/transaction", "Maneja transacciones bancarias en el juego.");

        // Bot & Registry
        addSection("🤖 Bot y Registro");
        addEndpoint("GET", "/registry/data", "Ajustes actuales del bot de Discord.");
        addEndpoint("POST", "/registry/data", "Actualiza ajustes del bot de Discord.");
        addEndpoint("GET", "/bot/roles", "Lista roles de Discord disponibles.");
        addEndpoint("GET", "/bot/channels", "Lista canales de Discord disponibles.");
        addEndpoint("POST", "/bot/send-embed", "Envía un embed personalizado a Discord.");
        addEndpoint("POST", "/bot/edit-embed", "Edita un embed personalizado existente.");
        addEndpoint("GET", "/bot/sent-embeds", "Historial de embeds enviados.");

        // Admin Panel
        addSection("🛠️ Panel de Administración");
        addEndpoint("POST", "/reset-daily", "Resetea el cooldown diario para un ID de Discord.");
        addEndpoint("GET", "/missions/config", "Ver configuración de la cola de misiones.");
        addEndpoint("POST", "/missions/set", "Establece una nueva misión.");
        addEndpoint("POST", "/complete-mission", "Fuerza la finalización de una misión.");
        addEndpoint("POST", "/api/economy/admin/adjust-bank", "Ajuste bancario de administrador.");

        // GitHub Webhooks
        addSection("🔄 Webhooks de GitHub");
        addEndpoint("POST", "/webhook", "Recibe eventos de GitHub y envía notificaciones.");
    }

    private void addSection(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        int sectionColor = isNightMode ? 0x4DFFFFFF : 0x4D000000;
        tv.setTextColor(sectionColor);
        tv.setTextSize(12);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(32), 0, dpToPx(16));
        tv.setLayoutParams(params);
        apiContent.addView(tv);
    }

    private void addEndpoint(String method, String path, String description) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_api_endpoint, apiContent, false);
        
        TextView methodTv = view.findViewById(R.id.method);
        TextView pathTv = view.findViewById(R.id.path);
        TextView descTv = view.findViewById(R.id.description);

        methodTv.setText(method);
        pathTv.setText(path);
        descTv.setText(description);

        // Styling based on method
        int methodColor = 0xFF00FFCC;
        if (method.equals("POST")) methodColor = 0xFFFFCC00;
        
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(adjustAlpha(methodColor, 0.2f));
        gd.setCornerRadius(dpToPx(4));
        methodTv.setBackground(gd);
        methodTv.setTextColor(methodColor);

        // updateCardStyle(view, isNightMode); // Handled in updateTheme if initially empty
        apiContent.addView(view);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(android.graphics.Color.alpha(color) * factor);
        int red = android.graphics.Color.red(color);
        int green = android.graphics.Color.green(color);
        int blue = android.graphics.Color.blue(color);
        return android.graphics.Color.argb(alpha, red, green, blue);
    }
}
