package com.example.vishika;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;

public class AutoUpdateHelper {

    private static long downloadId = -1;

    public static void checkForUpdates(final Activity activity) {
        String updateNode = MainActivity.IS_VISHAL ? "app_update/vishal" : "app_update/rashika";
        DatabaseReference updateRef = FirebaseDatabase.getInstance().getReference(updateNode);
        updateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer latestVersionCode = snapshot.child("latest_version_code").getValue(Integer.class);
                    String downloadUrl = snapshot.child("download_url").getValue(String.class);
                    Long updateTimestamp = snapshot.child("update_timestamp").getValue(Long.class);

                    if (latestVersionCode != null && downloadUrl != null) {
                        int currentVersionCode = 1;
                        try {
                            android.content.pm.PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                            currentVersionCode = pInfo.versionCode;
                        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }

                        if (latestVersionCode > currentVersionCode) {
                            showUpdateDialog(activity, downloadUrl, "A new version of Vishika is available!", updateTimestamp != null ? updateTimestamp : 0);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore errors
            }
        });
    }

    private static void showUpdateDialog(final Activity activity, final String downloadUrl, String releaseNotes, final long updateTimestamp) {
        if (activity.isFinishing()) return;
        if (releaseNotes == null) releaseNotes = "A new version of Vishika is available!";

        new AlertDialog.Builder(activity)
                .setTitle("Update Available")
                .setMessage(releaseNotes)
                .setCancelable(false)
                .setPositiveButton("Update Now", (dialog, which) -> {
                    startDownload(activity, downloadUrl, updateTimestamp);
                })
                .setNegativeButton("Later", (dialog, which) -> {
                    dialog.dismiss();
                    activity.finishAffinity();
                })
                .show();
    }

    private static void startDownload(final Activity activity, String downloadUrl, final long updateTimestamp) {
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(activity);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (20 * activity.getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(progressBar);

        AlertDialog progressDialog = new AlertDialog.Builder(activity)
                .setTitle("Downloading Update")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton("Install", null)
                .create();

        progressDialog.show();
        
        // Initially disable install button
        android.widget.Button installBtn = progressDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        installBtn.setEnabled(false);

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                java.net.URL url = new java.net.URL(downloadUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.connect();
                
                int fileLength = connection.getContentLength();
                
                java.io.InputStream input = new java.io.BufferedInputStream(url.openStream());
                File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
                if (file.exists()) file.delete();
                
                java.io.OutputStream output = new java.io.FileOutputStream(file);

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        activity.runOnUiThread(() -> progressBar.setProgress(progress));
                    }
                    output.write(data, 0, count);
                }
                
                output.flush();
                output.close();
                input.close();

                activity.runOnUiThread(() -> {
                    progressDialog.setTitle("Download Complete");
                    progressBar.setProgress(100);
                    installBtn.setEnabled(true);
                    installBtn.setOnClickListener(v -> {
                        android.content.SharedPreferences prefs = activity.getSharedPreferences("vishika_prefs", Context.MODE_PRIVATE);
                        prefs.edit().putLong("installed_update_timestamp", updateTimestamp).apply();
                        installApk(activity);
                        progressDialog.dismiss();
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> {
                    progressDialog.setTitle("Download Failed");
                    progressDialog.setCancelable(true);
                });
            }
        });
    }

    private static void installApk(Activity activity) {
        File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
        if (file.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(file);
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                activity.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(activity, "Failed to install update.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
