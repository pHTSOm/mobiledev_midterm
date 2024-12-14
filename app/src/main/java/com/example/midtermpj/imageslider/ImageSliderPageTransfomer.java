package com.example.midtermpj.imageslider;
import androidx.viewpager2.widget.ViewPager2;
import android.view.View;
public class ImageSliderPageTransfomer implements ViewPager2.PageTransformer {

    @Override
    public void transformPage(View page, float position) {
        // When the position is between -1 and 1, apply fade and zoom effects
        if (position < -1) { // [-Infinity, -1)
            page.setAlpha(0f); // Fully transparent when off-screen to the left
            page.setScaleX(0.8f); // Optional: reduce the scale for left-offscreen page
            page.setScaleY(0.8f); // Optional: reduce the scale for left-offscreen page
        } else if (position <= 1) { // [-1, 1]
            // Apply fade effect based on position
            float alpha = 1 - Math.abs(position); // The page fades as it moves away from the center
            page.setAlpha(alpha);

            // Apply zoom effect: zoom out as the page moves out of the center
            float scaleFactor = Math.max(0.85f, 1 - Math.abs(position)); // Zoom out as page moves away
            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);
        } else { // (1, +Infinity]
            page.setAlpha(0f); // Fully transparent when off-screen to the right
            page.setScaleX(0.8f); // Optional: reduce the scale for right-offscreen page
            page.setScaleY(0.8f); // Optional: reduce the scale for right-offscreen page
        }
    }
}


