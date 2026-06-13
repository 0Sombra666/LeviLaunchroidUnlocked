package org.levimc.launcher.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.levimc.launcher.R;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GithubReleaseUpdater {
    private static final String TAG = "GithubReleaseUpdater";
    private static final String GITHUB_LATEST_API = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String APK_ASSET_KEYWORD = ".apk";
    private static final String PREF_IGNORED_VERSION = "update_ignored_version";
    private final Activity activity;
    private final String owner = "0Sombra666";
    private final String repo = "LeviLaunchroidUnlocked";
    private final OkHttpClient client = new OkHttpClient();
    private ActivityResultLauncher<Intent> permissionResultLauncher;

    public GithubReleaseUpdater(Activity activity, String owner, String repo,
                                ActivityResultLauncher<Intent> permissionResultLauncher) {
        this.activity = activity;
        this.permissionResultLauncher = permissionResultLauncher;
    }

    public static int compareVersion(String a, String b) {
        a = a.startsWith("v") ? a.substring(1) : a;
        b = b.startsWith("v") ? b.substring(1) : b;
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        int len = Math.max(x.length, y.length);
        for (int i = 0; i < len; i++) {
            int vi = i < x.length ? Integer.parseInt(x[i]) : 0;
            int vj = i < y.length ? Integer.parseInt(y[i]) : 0;
            if (vi > vj) return 1;
            if (vi < vj) return -1;
        }
        return 0;
    }

    public void checkUpdate() {
        String url = String.format(GITHUB_LATEST_API, owner, repo);
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    String latestVersion = json.getString("tag_name");
                    JSONArray assets = json.getJSONArray("assets");
                    String downloadUrl = null;
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.getString("name");
                        if (name.endsWith(APK_ASSET_KEYWORD)) {
                            downloadUrl = asset.getString("browser_download_url");
                            break;
                        }
                    }
                    if (downloadUrl == null) {
                        Log.e(TAG, "No APK asset found in release.");
                        return;
                    }
                    String localVersion = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
                    if (compareVersion(latestVersion, localVersion) > 0) {
                        showUpdateDialog(latestVersion, downloadUrl);
                    } else {
                        activity.runOnUiThread(() ->
                                Toast.makeText(activity, activity.getString(R.string.already_latest_version, localVersion), Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse error: " + e.getMessage());
                }
            }
        });
    }

    public void checkUpdateOnLaunch() {
        String url = String.format(GITHUB_LATEST_API, owner, repo);
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    String latestVersion = json.getString("tag_name");
                    JSONArray assets = json.getJSONArray("assets");
                    String downloadUrl = null;
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.getString("name");
                        if (name.endsWith(APK_ASSET_KEYWORD)) {
                            downloadUrl = asset.getString("browser_download_url");
                            break;
                        }
                    }
                    if (downloadUrl == null) {
                        Log.e(TAG, "No APK asset found in release.");
                        return;
                    }
                    String localVersion = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
                    SharedPreferences prefs = activity.getSharedPreferences("no_update_version", Context.MODE_PRIVATE);
                    String ignoredVersion = prefs.getString(PREF_IGNORED_VERSION, "");

                    if (compareVersion(latestVersion, localVersion) > 0
                            && !ignoredVersion.equals(latestVersion)) {
                        showUpdateDialogWithIgnore(latestVersion, downloadUrl);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse error: " + e.getMessage());
                }
            }
        });
    }

    private void showUpdateDialog(String version, String url) {
        activity.runOnUiThread(() -> {
            CustomAlertDialog dialog = new CustomAlertDialog(activity);
            dialog.setTitleText(activity.getString(R.string.new_version_found, version));
            dialog.setMessage(activity.getString(R.string.update_question));
            dialog.setPositiveButton(activity.getString(R.string.download_update), (d) -> downloadApk(url));
            dialog.setNegativeButton(activity.getString(R.string.cancel), null);
            dialog.show();
        });
    }

    private void showUpdateDialogWithIgnore(String version, String url) {
        activity.runOnUiThread(() -> {
            CustomAlertDialog dialog = new CustomAlertDialog(activity);
            dialog.setTitleText(activity.getString(R.string.new_version_found, version));
            dialog.setMessage(activity.getString(R.string.update_question));
            dialog.setPositiveButton(activity.getString(R.string.download_update), (d) -> downloadApk(url));
            dialog.setNegativeButton(activity.getString(R.string.cancel), null);
            dialog.setNeutralButton(activity.getString(R.string.ignore_this_version), (d) -> ignoreThisVersion(version));
            dialog.show();
        });
    }

    private void ignoreThisVersion(String version) {
        SharedPreferences prefs = activity.getSharedPreferences("no_update_version", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_IGNORED_VERSION, version).apply();
        activity.runOnUiThread(() -> Toast.makeText(activity, activity.getString(R.string.version_ignored), Toast.LENGTH_SHORT).show());
    }

    private void downloadApk(String url) {
        activity.runOnUiThread(() -> Toast.makeText(activity, activity.getString(R.string.downloading_update), Toast.LENGTH_SHORT).show());
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, activity.getString(R.string.update_failed, e.getMessage()), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    byte[] buf = new byte[4096];
                    int len;
                    long downloaded = 0;
                    long total = response.body().contentLength();
                    is = response.body().byteStream();
                    File apkFile = new File(activity.getExternalCacheDir(), "update_apk.apk");
                    fos = new FileOutputStream(apkFile);

                    long lastToastTime = 0;
                    while ((len = is.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                        downloaded += len;

                        long now = System.currentTimeMillis();
                        if (now - lastToastTime > 500 && total > 0) {
                            int percent = (int) (downloaded * 100 / total);
                            activity.runOnUiThread(() ->
                                    Toast.makeText(activity, activity.getString(R.string.update_progress, percent), Toast.LENGTH_SHORT).show());
                            lastToastTime = now;
                        }
                    }
                    fos.flush();
                    installApk(apkFile);
                } catch (Exception e) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(activity, activity.getString(R.string.update_failed, e.getMessage()), Toast.LENGTH_LONG).show());
                } finally {
                    try {
                        if (is != null) is.close();
                    } catch (Exception ignored) {
                    }
                    try {
                        if (fos != null) fos.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    private void installApk(File apkFile) {
        PermissionsHandler handler = PermissionsHandler.getInstance();
        handler.setActivity(activity, permissionResultLauncher);
        handler.requestPermission(PermissionsHandler.PermissionType.UNKNOWN_SOURCES,
                new PermissionsHandler.PermissionResultCallback() {
                    @Override
                    public void onPermissionGranted(PermissionsHandler.PermissionType type) {
                        activity.runOnUiThread(() -> {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                Uri apkUri;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    apkUri = FileProvider.getUriForFile(
                                            activity,
                                            activity.getPackageName() + ".fileprovider",
                                            apkFile
                                    );
                                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                                } else {
                                    apkUri = Uri.fromFile(apkFile);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                }
                                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                                activity.startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(activity, activity.getString(R.string.install_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onPermissionDenied(PermissionsHandler.PermissionType type, boolean permanentlyDenied) {
                        activity.runOnUiThread(() ->
                                Toast.makeText(activity, activity.getString(R.string.unknown_sources_permission_message), Toast.LENGTH_LONG).show()
                        );
                    }
                }
        );
    }
}