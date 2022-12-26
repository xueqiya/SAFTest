package com.xueqiya.saf;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                    try {
                        handlerFile(uri);
                    } catch (IOException e) {
                        Log.e(TAG, "文件处理失败:" + e);
                        e.printStackTrace();
                    }
                }
            });

    private void handlerFile(Uri uri) throws IOException {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (columnIndex < 0) {
                Log.e(TAG, "获取columnIndex失败");
                cursor.close();
            }
            String displayName = cursor.getString(columnIndex);
            Log.d(TAG, "文件名=" + displayName);
            copyFile(uri, displayName);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private void copyFile(Uri uri, String displayName) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
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
    }
}