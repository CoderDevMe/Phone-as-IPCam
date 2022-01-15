package com.example.httpclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE
    };

    OkHttpClient client = new OkHttpClient();
    EditText serverIP;
    TextView tx;
    ImageView imageView;
    Timer receiveTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        Button btn = findViewById(R.id.button);
        tx = findViewById(R.id.textView);
        imageView = findViewById(R.id.imageView);
        serverIP = findViewById(R.id.serverIP);


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                receiveTimer.schedule(new timerTask(), 10);
            }
        });
    }

    class timerTask extends TimerTask {
        @Override
        public void run() {
            new RetrieveFeedTask().execute();
            receiveTimer.schedule(new timerTask(), 70);
        }
    }

    class RetrieveFeedTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            long start = System.currentTimeMillis();
            Request request = new Request.Builder()
                    .url("http://" + serverIP.getText() + ":8080/")
                    .build();
            try (Response response = client.newCall(request).execute()) {

                try {
                    final byte[] bytes = Objects.requireNonNull(response.body()).bytes();


                    //استقبال البيانات وتحويلها إلى صورة
                    Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (image == null)
                        return null;

                    //إعادة الصورة إلى حجمها الطبيعي
//                    Bitmap bitmapBigSize = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
//                    bitmapBigSize = Bitmap.createScaledBitmap(image, bitmapBigSize.getWidth(),
//                            bitmapBigSize.getHeight(), false);

                    //تجهيز الصورة لعرضها
                    Bitmap bitmapToShow = Bitmap.createBitmap(image);

                    long dt = System.currentTimeMillis() - start;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tx.setText("rc length = " + bytes.length + "\ndt = " + dt);
//                            Bitmap image = Bitmap.createBitmap(imageRGB, width, height, Bitmap.Config.ARGB_8888);
                            imageView.setImageBitmap(bitmapToShow);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        }
        else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                break;
        }
    }
}