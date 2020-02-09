package com.example.driveralertness;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioAttributes;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.TextureView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;


import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.Timer;


import static android.provider.ContactsContract.Directory.PACKAGE_NAME;


public class MainActivity extends AppCompatActivity {

    Preview preview;
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    TextureView textureView;
    boolean[] closed = new boolean[3];
    int counter = 0;

        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainscreen);

        Button startButton = findViewById(R.id.startbutton);
        Button infoButton = findViewById(R.id.information);
        infoButton.setOnClickListener(new View.OnClickListener(){
            @Override
                    public void onClick(View v) {
                setContentView(R.layout.infopage);
            }
        });

        FirebaseApp.initializeApp(this);
        String PACKAGE_NAME = getApplicationContext().getPackageName();


        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNotificationChannel();
                setContentView(R.layout.activity_main);
                textureView = findViewById(R.id.view_finder);
                startCamera();
                start();
            }
        });


    }

    private Timer timer;
    private TimerTask timerTask = new TimerTask() {

        @Override
        public void run() {
            startScan();
        }
    };
    public void start() {
        /*if(timer != null) {
            return;
        }*/
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 100, 300);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "notification";
            String description = "stay awake";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("poggers", name, importance);
            channel.setDescription(description);
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public Preview startCamera() {


        CameraX.unbindAll();


        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen


        PreviewConfig pConfig = new PreviewConfig.Builder().setLensFacing(CameraX.LensFacing.FRONT).setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        preview = new Preview(pConfig);


        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY).setLensFacing(CameraX.LensFacing.FRONT)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        CameraX.bindToLifecycle((LifecycleOwner) this, preview, imgCap);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);
                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();


                    }
                });

        return preview;
    }

    public boolean checkAll() {
        int count = 0;
        for(int i = 0; i < 3; i++) {
            if (closed[i]) {
                count++;
            }
        }
        if (count == 3) {
            return true;
        } else {
            return false;
        }
    }

    public void startScan(){
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "poggers")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("ALERT: PLEASE FOCUS ON THE ROAD")
                .setContentText("Take a break every so often if you are tired!!!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        Notification notification = builder.build();


        final FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .enableTracking()
                        .build();



        Bitmap pic = textureView.getBitmap();
        /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pic.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        pic.recycle();*/

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(pic);

        Task<List<FirebaseVisionFace>> result = detector
                .detectInImage(image)
                .addOnSuccessListener(new
                                              OnSuccessListener<List<FirebaseVisionFace>>() {
                                                  @Override
                                                  public void onSuccess(List<FirebaseVisionFace> faces) {



                                                      for (FirebaseVisionFace face : faces) {
                                                              Log.d("tag", "****************************");
                                                              Log.d("tag", "face [" + face + "]");
                                                              Log.d("tag", "Smiling Prob [" + face.getSmilingProbability() + "]");
                                                              Log.d("tag", "Left eye open [" + face.getLeftEyeOpenProbability() + "]");
                                                              Log.d("tag", "Right eye open [" + face.getRightEyeOpenProbability() + "]");
                                                          boolean sleeping = false;
                                                          if ((face.getLeftEyeOpenProbability() < 0.4) && (face.getRightEyeOpenProbability() < 0.4)) {
                                                              if (counter == 3) {
                                                                  counter = 0;
                                                              }
                                                              closed[counter] = true;
                                                              counter++;
                                                              sleeping = checkAll();

                                                          } else {
                                                              if (counter == 3) {
                                                                  counter = 0;
                                                              }
                                                              closed[counter] = false;
                                                              counter++;
                                                          }

                                                          if (sleeping){
                                                              Log.d("tag","SLEEPING!!!");
                                                              notificationManager.notify(69, builder.build());
                                                          }

                                                      }

                                                  }
                                              });
        pic.recycle();






    }



    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();

        switch(rotation){
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setTransform(mx);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            startCamera();
        }
    }



}



