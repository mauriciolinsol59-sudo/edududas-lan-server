package com.moodify.planetas;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.CookieManager;
import android.widget.Toast;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String ASSET_HOST = "appassets.androidplatform.net";
    private static final String HOME = "https://" + ASSET_HOST + "/assets/index.html";
    private WebView webView;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 16, 32));
        getWindow().setNavigationBarColor(Color.rgb(11, 16, 32));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11, 16, 32));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equals(uri.getScheme()) && ASSET_HOST.equals(uri.getHost())) {
                    if ("/assets/index.html".equals(uri.getPath())) {
                        try {
                            InputStream content = getAssets().open("index.html");
                            return new WebResourceResponse("text/html", "UTF-8", content);
                        } catch (IOException e) {
                            return new WebResourceResponse("text/plain", "UTF-8",
                                new ByteArrayInputStream("No se pudo cargar Moodify".getBytes()));
                        }
                    }
                    return new WebResourceResponse("text/plain", "UTF-8",
                        new ByteArrayInputStream(new byte[0]));
                }
                return null;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                if (HOME.equals(uri.toString())) return false;
                openExternal(uri.toString());
                return true;
            }
        });
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface public void openExternal(String url) {
                runOnUiThread(() -> MainActivity.this.openExternal(url));
            }
        }, "MoodifyAndroid");
        setContentView(webView);
        webView.loadUrl(HOME);
    }
    private void openExternal(String url) {
        try {
            Uri uri = Uri.parse(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return;
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!host.equals("open.spotify.com") && !host.equals("youtube.com")
                && !host.equals("www.youtube.com") && !host.equals("m.youtube.com")) return;
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir el enlace musical.", Toast.LENGTH_LONG).show();
        }
    }
    @Override public void onBackPressed() {
        if (webView == null) { super.onBackPressed(); return; }
        webView.evaluateJavascript("window.moodifyHandleBack && window.moodifyHandleBack()", value -> {
            if (!"true".equals(value)) MainActivity.super.onBackPressed();
        });
    }
    @Override protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("MoodifyAndroid");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
