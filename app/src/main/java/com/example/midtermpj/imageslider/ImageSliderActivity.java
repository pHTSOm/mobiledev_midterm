package com.example.midtermpj.imageslider;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.midtermpj.R;

import java.util.ArrayList;

public class ImageSliderActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ImageSliderAdapter sliderAdapter;
    private ArrayList<Uri> imageUris;
    private MediaPlayer mediaPlayer;

    private Handler handler;
    private Runnable autoScrollRunnable;
    private int currentPosition = 0;
    private final int AUTO_SCROLL_DELAY = 3000; // Delay in milliseconds (3 seconds)

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

            // Apply the transition effect with slower animation
            viewPager.setPageTransformer(new SlowPageTransformer());

            // Set up automatic scrolling
            setupAutoScroll();
        } else {
            // Handle the case where no images are available
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show();
        }

        // Set up the back button functionality
        Button backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        // Play background music
        playBackgroundMusic();
    }

    private void setupAutoScroll() {
        handler = new Handler();
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                // Increment the current position and set the new item
                currentPosition = (currentPosition + 1) % imageUris.size();
                viewPager.setCurrentItem(currentPosition, true);

                // Post the Runnable again after the delay
                handler.postDelayed(this, AUTO_SCROLL_DELAY);
            }
        };

        // Start the auto-scroll when the activity is created
        handler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
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

        // Remove any pending posts of the autoScrollRunnable
        if (handler != null) {
            handler.removeCallbacks(autoScrollRunnable);
        }
    }

    // Custom PageTransformer to slow down the transition
    private class SlowPageTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(View page, float position) {
            // Slower transition by adjusting the scale and alpha of the pages during the scroll
            float absPos = Math.abs(position);

            if (absPos <= 1) {
                // Slow down the transition
                page.setAlpha(1 - absPos); // Fading effect
                page.setScaleX(1 - 0.3f * absPos); // Scale effect
                page.setScaleY(1 - 0.3f * absPos); // Scale effect
            } else {
                page.setAlpha(0); // Make page invisible when it is off-screen
            }
        }
    }
}
