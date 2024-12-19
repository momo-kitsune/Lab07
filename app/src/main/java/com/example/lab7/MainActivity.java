package com.example.lab7;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.lab7.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_AUDIO_PICK = 3;
    private static final int REQUEST_VIDEO_PICK = 4;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private VideoView videoView;
    private ImageView imageView;
    private Button captureButton;
    private Button selectAudioButton;
    private Button selectVideoButton;
    private Button playPauseAudioButton;
    private String currentPhotoPath;
    private MediaPlayer mediaPlayer;
    private boolean isAudioPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        imageView = findViewById(R.id.imageView);
        captureButton = findViewById(R.id.captureButton);
        selectAudioButton = findViewById(R.id.selectAudioButton);
        selectVideoButton = findViewById(R.id.selectVideoButton);
        playPauseAudioButton = findViewById(R.id.playPauseAudioButton);

        captureButton.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent();
            } else {
                requestCameraPermission();
            }
        });

        selectAudioButton.setOnClickListener(v -> selectAudioFile());
        selectVideoButton.setOnClickListener(v -> selectVideoFile());

        playPauseAudioButton.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (isAudioPlaying) {
                    mediaPlayer.pause();
                    playPauseAudioButton.setText("Воспроизвести аудио");
                } else {
                    mediaPlayer.start();
                    playPauseAudioButton.setText("Пауза");
                }
                isAudioPlaying = !isAudioPlaying;
            } else {
                Toast.makeText(this, "Сначала выберите аудиофайл", Toast.LENGTH_SHORT).show();
            }
        });

        playPauseAudioButton.setVisibility(View.GONE);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Ошибка при создании файла изображения", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void selectAudioFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_AUDIO_PICK);
    }

    private void selectVideoFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, REQUEST_VIDEO_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageView.setImageURI(Uri.parse(currentPhotoPath));
            imageView.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
            playPauseAudioButton.setVisibility(View.GONE);
        } else if (requestCode == REQUEST_AUDIO_PICK && resultCode == RESULT_OK) {
            Uri audioUri = data.getData();
            prepareAudioPlayer(audioUri);
            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.GONE);
        } else if (requestCode == REQUEST_VIDEO_PICK && resultCode == RESULT_OK) {
            Uri videoUri = data.getData();
            videoView.setVideoURI(videoUri);
            videoView.setMediaController(new MediaController(this));
            videoView.start();
            videoView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            playPauseAudioButton.setVisibility(View.GONE);
        }
    }

    private void prepareAudioPlayer(Uri audioUri) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getApplicationContext(), audioUri);
            mediaPlayer.prepare();
            playPauseAudioButton.setVisibility(View.VISIBLE);
            playPauseAudioButton.setText("Воспроизвести аудио");
            isAudioPlaying = false;
            Toast.makeText(this, "Аудиофайл готов к воспроизведению", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка при подготовке аудиофайла", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}