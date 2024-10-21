package com.example.midtermpj;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;

public class ImageSliderActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ImageSliderAdapter sliderAdapter;
    private ArrayList<Uri> imageUris;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_slier);

        viewPager = findViewById(R.id.viewPager);

            // Get the image URIs from the intent
        imageUris = getIntent().getParcelableArrayListExtra("imageUris");

        // Check if the imageUris are passed correctly
        if (imageUris != null && !imageUris.isEmpty()) {
            // Set up the adapter
            sliderAdapter = new ImageSliderAdapter(imageUris, this);
            viewPager.setAdapter(sliderAdapter);
        } else {
            // Handle the case where no images are available
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show();
        }

        // Set up the back button functionality
        Button backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        playBackgroundMusic();
    }

    private void playBackgroundMusic() {


        mediaPlayer = MediaPlayer.create(this, R.raw.tadow);

        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);  // To play music continuously
            mediaPlayer.start();  // Start the music
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();  // Pause music when the activity is paused
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();  // Release the MediaPlayer resources
            mediaPlayer = null;
        }
    }

}

