package com.xueqiya.saf;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");

        View safView = findViewById(R.id.saf);
        safView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            activityResultLauncher.launch(intent);
        });
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    Log.d(TAG, "uri=" + uri.toString());
                    handlerFile(uri);
                }
            });

    private void handlerFile(Uri uri) {
        show();
        Thread thread = new Thread(new FileRunnable(uri));
        thread.start();
    }

    class FileRunnable implements Runnable {
        private final Uri uri;

        public FileRunnable(Uri uri) {
            this.uri = uri;
        }

        @Override
        public void run() {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndex < 0) {
                    Log.e(TAG, "获取columnIndex失败");
                    hide();
                    cursor.close();
                    return;
                }
                String displayName = cursor.getString(columnIndex);
                Log.d(TAG, "文件名=" + displayName);
                copyFile(uri, displayName);
            }
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void copyFile(Uri uri, String displayName) {
        InputStream inputStream;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            File cacheFile = new File(getCacheDir().getAbsolutePath(), displayName);
            FileOutputStream outputStream = new FileOutputStream(cacheFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();
            Log.d(TAG, "拷贝完成 path=" + cacheFile);
            hide();
        } catch (IOException e) {
            hide();
            e.printStackTrace();
        }
    }

    private void show() {
        handler.post(() -> progressDialog.show());
    }

    private void hide() {
        handler.post(() -> progressDialog.hide());
    }
}