package com.eightbittrade.typecraft;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.widget.Toast;
import java.io.OutputStream;

public final class MainActivity extends Activity {
    private static final int SELECT_PHOTO_OR_PROJECT = 30;
    private static final int SAVE_FILE = 31;
    private WebView view;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingData;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xff0d0f17);
        getWindow().setNavigationBarColor(0xff0d0f17);
        view = new WebView(this);
        view.setBackgroundColor(0xff0d0f17);
        view.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                return !"file".equals(request.getUrl().getScheme());
            }
        });
        view.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), SELECT_PHOTO_OR_PROJECT);
                    return true;
                } catch (Exception err) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "Cannot open file picker", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        view.addJavascriptInterface(new Object() {
            @JavascriptInterface public void saveFile(String dataUrl, String fileName, String mime) {
                try {
                    String part = dataUrl.substring(dataUrl.indexOf(',') + 1);
                    pendingData = Base64.decode(part, Base64.DEFAULT);
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType(mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
                    i.putExtra(Intent.EXTRA_TITLE, fileName);
                    runOnUiThread(() -> startActivityForResult(i, SAVE_FILE));
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Export failed", Toast.LENGTH_SHORT).show());
                }
            }
        }, "TypeCraftAndroid");
        setContentView(view);
        view.loadUrl("file:///android_asset/index.html");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PHOTO_OR_PROJECT) {
            if (fileCallback != null) {
                fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                fileCallback = null;
            }
        } else if (requestCode == SAVE_FILE) {
            final byte[] bytes = pendingData;
            pendingData = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && bytes != null) {
                final Uri uri = data.getData();
                new Thread(() -> {
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        if (out == null) throw new Exception("Cannot write file");
                        out.write(bytes);
                        runOnUiThread(() -> Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show());
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this, "Save failed", Toast.LENGTH_LONG).show());
                    }
                }).start();
            }
        }
    }

    @Override public void onBackPressed() {
        if (view != null && view.canGoBack()) view.goBack();
        else super.onBackPressed();
    }
    @Override public void onDestroy() {
        if (view != null) view.destroy();
        super.onDestroy();
    }
}
