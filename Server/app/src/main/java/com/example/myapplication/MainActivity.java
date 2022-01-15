package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    ServerConnection serverConnection;

    FrameLayout preview;
    Timer sendTimer = new Timer();
    TextView txtDebug;
    ImageView imageView;
    boolean isSaveImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendTimer.schedule(new timerTask(),1000);
        txtDebug = findViewById(R.id.txtDebug);
        imageView = findViewById(R.id.imageView);
        try {
            serverConnection = new ServerConnection(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);

        Log.d("SaveTime", "");
        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSaveImage = true;
            }
        });
    }

    class timerTask extends TimerTask {
        @Override
        public void run() {
            isSaveImage = true;
            sendTimer.schedule(new timerTask(), 70);
        }
    }

    Camera.PreviewCallback cameraPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            if (!isSaveImage)
                return;

            try {
                long start = System.currentTimeMillis();
                // من أجل إرسال البيانات القادمة (الصورة) كما هي data[] ذلك يستغرق 800ms أي حوالي 1.2fps
                // من أجل حل المشكلة قمت بتشكيل الصورة التي تمثلها تلك البيانات ثم قمت بعمل resize للصورة من أجل تقليل الحجم
                //ثم قمت بتحويل الصورة من جديد إلى بايتات و حفظها في الهاتف وذلك أعطى حجم أقل بكثير للبايتات المرسلة بمعدل 10fps


                //تحديد أبعاد صورة الكاميرا
                int width = camera.getParameters().getPreviewSize().width;
                int height = camera.getParameters().getPreviewSize().height;

                //تحويل البيانات إلى صورة (علما أن الصورة تكون بشكل YUV وليس RGB)
                ByteArrayOutputStream outstr = new ByteArrayOutputStream();
                Rect rect = new Rect(0, 0, width, height);
                YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);


                //تحويل من IUV --> RGB  50ms
                yuvimage.compressToJpeg(rect, 50, outstr);
                Bitmap bmpJpeg = BitmapFactory.decodeByteArray(outstr.toByteArray(), 0, outstr.size());


                //تصغير الصورة 4 مرات
                Bitmap bitmapSmallSize = Bitmap.createBitmap(width / 4, height / 4, Bitmap.Config.ARGB_8888);
                bitmapSmallSize = Bitmap.createScaledBitmap(bmpJpeg, bitmapSmallSize.getWidth(),
                        bitmapSmallSize.getHeight(), false);
                imageView.setImageBitmap(bitmapSmallSize);


                //تحويل الصورة إلى بايتات من جديد 15ms
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmapSmallSize.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                //حفظ الصورة في الهاتف من أجل إرسالها عند الطلب
                FileOutputStream fos = new FileOutputStream(getOutputMediaFile());
                fos.write(byteArray);
                fos.flush();
                long dt = System.currentTimeMillis() - start;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtDebug.setText("dt = "+dt);
                    }
                });
                isSaveImage = false;
            } catch (Exception e) {
                e.printStackTrace();
                txtDebug.setText("image Error\n" + e.getMessage());
//                    Log.d("SAVING", "image Error");
            }
        }
    };


    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open(1);
            camera.setPreviewCallback(cameraPreviewCallback);

        } catch (Exception e) {
            // cannot get camera or does not exist
        }
        return camera;
    }


    public static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "1.jpg");

        return mediaFile;
    }

    @Override
    protected void onPause() {
        mCamera.release();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(this, mCamera);
        preview.addView(mCameraPreview);
        super.onResume();
    }
}