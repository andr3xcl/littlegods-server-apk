package com.example.statsweb;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.util.Xml;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.StringReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String STATS_URL = "http://68.211.73.62:3000/";
    private static final String ADMIN_URL = "http://68.211.73.62:1624/";

    private String currentUrl = STATS_URL;

    private WebView webView;
    private NestedScrollView dashboard;
    private FrameLayout webContainer;
    private FrameLayout loadingOverlay;
    private LinearLayout errorOverlay;
    private TextView errorMessage;
    private View updatePill;
    private boolean hasError = false;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final String GITHUB_RELEASE_URL = "https://api.github.com/repos/andr3xcl/littlegods-server-apk/releases/latest";
    private final String YOUTUBE_RSS_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=UCKBugJznx8oCjEgl304pNvQ";
    private final String YOUTUBE_CHANNEL_URL = "https://www.youtube.com/@Littlegods_cl";
    private GitHubRelease latestRelease;
    
    private LinearLayout youtubeContainer;
    private View youtubeProgress;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive/edge-to-edge setup
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Window window = getWindow();
        
        // Use a more modern way to achieve transparency and full screen
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        // For newer APIs, ensure content flows behind system bars
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        setContentView(R.layout.activity_main);

        // Find views
        dashboard = findViewById(R.id.dashboard);
        webContainer = findViewById(R.id.webContainer);
        webView = findViewById(R.id.webView);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        errorOverlay = findViewById(R.id.errorOverlay);
        errorMessage = findViewById(R.id.errorMessage);
        
        View btnStats = findViewById(R.id.btnStats);
        View btnAdmin = findViewById(R.id.btnAdmin);
        ImageButton btnBackToHome = findViewById(R.id.btnBackToHome);
        Button retryButton = findViewById(R.id.retryButton);

        // Dashboard buttons
        btnStats.setOnClickListener(v -> openWebsite(STATS_URL));
        btnAdmin.setOnClickListener(v -> openWebsite(ADMIN_URL));

        // Home button in WebView
        btnBackToHome.setOnClickListener(v -> showDashboard());

        retryButton.setOnClickListener(v -> {
            errorOverlay.setVisibility(View.GONE);
            loadingOverlay.setVisibility(View.VISIBLE);
            hasError = false;
            webView.loadUrl(currentUrl);
        });

        // Configure WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setTextZoom(100);

        // User agent: identify as mobile browser
        String defaultUA = settings.getUserAgentString();
        settings.setUserAgentString(defaultUA);

        // Handle page navigation inside the WebView
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                hasError = false;
                loadingOverlay.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!hasError) {
                    loadingOverlay.setVisibility(View.GONE);
                    errorOverlay.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    hasError = true;
                    loadingOverlay.setVisibility(View.GONE);
                    errorOverlay.setVisibility(View.VISIBLE);
                    errorMessage.setText("No se pudo conectar al servidor\nError: " + error.getDescription());
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.setBackgroundColor(0xFF000000);

        updatePill = findViewById(R.id.updatePill);
        updatePill.setOnClickListener(v -> showUpdateDialog());

        youtubeContainer = findViewById(R.id.youtubeContainer);
        youtubeProgress = findViewById(R.id.youtubeProgress);
        findViewById(R.id.btnYoutubeChannel).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_CHANNEL_URL));
            startActivity(intent);
        });

        checkUpdates();
        fetchYoutubeVideos();
    }

    private void openWebsite(String url) {
        currentUrl = url;
        dashboard.setVisibility(View.GONE);
        webContainer.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void showDashboard() {
        webView.stopLoading();
        webView.loadUrl("about:blank");
        webContainer.setVisibility(View.GONE);
        dashboard.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (webContainer.getVisibility() == View.VISIBLE) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                showDashboard();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    private void checkUpdates() {
        Request request = new Request.Builder()
                .url(GITHUB_RELEASE_URL)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    latestRelease = new Gson().fromJson(json, GitHubRelease.class);
                    
                    if (latestRelease != null && latestRelease.tagName != null) {
                        String currentVersion = getAppVersion();
                        if (isNewerVersion(latestRelease.tagName, currentVersion)) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                updatePill.setVisibility(View.VISIBLE);
                            });
                        }
                    }
                }
            }
        });
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    private boolean isNewerVersion(String latest, String current) {
        // Simple version comparison (e.g. "1.2" vs "1.1")
        // Basic implementation, can be improved to handle "v1.2" etc.
        String l = latest.replace("v", "").trim();
        String c = current.replace("v", "").trim();
        return l.compareTo(c) > 0;
    }

    private void showUpdateDialog() {
        if (latestRelease == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update, null);
        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
                .setView(dialogView)
                .create();

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView version = dialogView.findViewById(R.id.versionName);
        TextView changelog = dialogView.findViewById(R.id.changelogText);
        Button btnUpdate = dialogView.findViewById(R.id.btnUpdateNow);
        View btnLater = dialogView.findViewById(R.id.btnLater);

        title.setText("Nueva Versión " + latestRelease.tagName);
        version.setText("Versión " + latestRelease.tagName + " disponible");
        changelog.setText(latestRelease.body);

        btnUpdate.setOnClickListener(v -> {
            if (latestRelease.assets != null && !latestRelease.assets.isEmpty()) {
                downloadAndInstall(latestRelease.assets.get(0).browserDownloadUrl);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "No se encontró el archivo de instalación", Toast.LENGTH_SHORT).show();
            }
        });

        btnLater.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void downloadAndInstall(String url) {
        Toast.makeText(this, "Descargando actualización...", Toast.LENGTH_LONG).show();
        
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(MainActivity.this, "Error al descargar", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                File apkFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(apkFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                    fos.flush();
                }

                new Handler(Looper.getMainLooper()).post(() -> installApk(apkFile));
            }
        });
    }

    private void installApk(File file) {
        Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void fetchYoutubeVideos() {
        Request request = new Request.Builder().url(YOUTUBE_RSS_URL).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> youtubeProgress.setVisibility(View.GONE));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String xml = response.body().string();
                    List<YoutubeVideo> videos = parseYoutubeRss(xml);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        youtubeProgress.setVisibility(View.GONE);
                        displayYoutubeVideos(videos);
                    });
                }
            }
        });
    }

    private List<YoutubeVideo> parseYoutubeRss(String xml) {
        List<YoutubeVideo> videos = new ArrayList<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(xml));

            int eventType = parser.getEventType();
            YoutubeVideo currentVideo = null;

            while (eventType != XmlPullParser.END_DOCUMENT && videos.size() < 2) {
                String name = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("entry".equals(name)) {
                            currentVideo = new YoutubeVideo();
                        } else if (currentVideo != null) {
                            if ("title".equals(name)) {
                                currentVideo.title = parser.nextText();
                            } else if ("yt:videoId".equals(name)) {
                                currentVideo.id = parser.nextText();
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("entry".equals(name) && currentVideo != null) {
                            videos.add(currentVideo);
                            currentVideo = null;
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videos;
    }

    private void displayYoutubeVideos(List<YoutubeVideo> videos) {
        youtubeContainer.removeAllViews();
        for (YoutubeVideo video : videos) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_youtube_video, youtubeContainer, false);
            TextView title = itemView.findViewById(R.id.videoTitle);
            ImageView thumbnail = itemView.findViewById(R.id.videoThumbnail);

            title.setText(video.title);
            String thumbUrl = "https://i.ytimg.com/vi/" + video.id + "/hqdefault.jpg";
            Glide.with(this).load(thumbUrl).into(thumbnail);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + video.id));
                startActivity(intent);
            });

            youtubeContainer.addView(itemView);
        }
    }

    private static class YoutubeVideo {
        String title;
        String id;
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    // Gson data classes
    private static class GitHubRelease {
        @SerializedName("tag_name") String tagName;
        @SerializedName("body") String body;
        @SerializedName("assets") List<Asset> assets;
    }

    private static class Asset {
        @SerializedName("browser_download_url") String browserDownloadUrl;
    }
}
