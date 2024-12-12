package com.example.midtermpj.album;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermpj.R;
import com.example.midtermpj.imagefunctionrelated.AddImage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    EditText albumNameEditText;
    Button createAlbumBtn;
    RecyclerView albumsRecyclerView;
    public static AlbumsAdapter albumsAdapter;
    ArrayList<String> albums = new ArrayList<>();
    DatabaseReference albumRef; // Firebase reference
    boolean isListView = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);

        albumNameEditText = findViewById(R.id.albumNameEditText);
        createAlbumBtn = findViewById(R.id.createAlbumBtn);
        albumsRecyclerView = findViewById(R.id.albumsRecyclerView);
        Button switchButton = findViewById(R.id.switchButton);

        // Firebase Database reference
        albumRef = FirebaseDatabase.getInstance().getReference("albums");

        // Initialize RecyclerView with AlbumsAdapter
        albumsAdapter = new AlbumsAdapter(albums, new AlbumsAdapter.OnAlbumClickListener() {
            @Override
            public void onAlbumClick(String albumName) {
                // Navigate to AddImage activity when album is clicked
                Intent intent = new Intent(MainActivity.this, AddImage.class);
                intent.putExtra("ALBUM_NAME", albumName);
                startActivity(intent);
            }
        }, new AlbumsAdapter.OnDeleteClickListener() {
            @Override
            public void onDeleteClick(String albumName, int position) {
                // Call method to delete album
                deleteAlbumFromFirebase(albumName, position);
            }
        });

        albumsRecyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this, 3));
        albumsRecyclerView.setAdapter(albumsAdapter);
        setRecyclerViewLayoutManager(isListView); // Default is ListView

        // Add click listener to Create Album button
        createAlbumBtn.setOnClickListener(v -> {
            Log.d("MainActivity", "Create album button clicked");
            String albumName = albumNameEditText.getText().toString();
            if (!albumName.isEmpty()) {
                // Add album to Firebase
                addAlbumToFirebase(albumName);

                // Clear the EditText field
                albumNameEditText.setText("");
            } else {
                Toast.makeText(MainActivity.this, "Enter album name", Toast.LENGTH_SHORT).show();
            }
        });

        // Load albums from Firebase
        loadAlbumsFromFirebase();
        switchButton.setOnClickListener(v -> {
            isListView = !isListView; // Toggle between list and grid
            setRecyclerViewLayoutManager(isListView);

            // Update button text
            switchButton.setText(isListView ? "List" : "Grid");
        });
    }

    private void setRecyclerViewLayoutManager(boolean isListView) {
        if (isListView) {
            albumsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else {
            albumsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        }
    }

    private void addAlbumToFirebase(String albumName) {
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(albumName);

        // Initialize the album with a placeholder thumbnail and empty images list
        Map<String, Object> albumData = new HashMap<>();
        albumData.put("thumbnail", ""); // Placeholder thumbnail
        albumData.put("images", new HashMap<>());

        albumRef.setValue(albumData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                albums.add(albumName);
                albumsAdapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Album created", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(MainActivity.this, AddImage.class);
                intent.putExtra("ALBUM_NAME", albumName);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Failed to create album", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAlbumsFromFirebase() {
        albumRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                albums.clear(); // Clear the list before adding new items
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String albumName = snapshot.getKey();
                    if (albumName != null) {
                        albums.add(albumName);

                        // Fetch the thumbnail for the album dynamically
                        String thumbnail = snapshot.child("thumbnail").getValue(String.class);
                        if (thumbnail != null && !thumbnail.isEmpty()) {
                            albumsAdapter.updateThumbnail(albumName, thumbnail);
                        } else {
                            // No thumbnail yet; use a placeholder
                            albumsAdapter.updateThumbnail(albumName, null);
                        }
                    }
                }
                albumsAdapter.notifyDataSetChanged(); // Notify the adapter of dataset changes
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load albums"
                        , Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteAlbumFromFirebase(String albumName, int position) {
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(albumName);

        // Fetch all images from the album
        albumRef.child("images").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot imageSnapshot : snapshot.getChildren()) {
                    String imageUrl = imageSnapshot.getValue(String.class);

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        // Delete each image from Firebase Storage
                        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                        storageRef.delete().addOnFailureListener(e -> {
                            Toast.makeText(MainActivity.this, "Failed to delete image from storage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                // Once all images are deleted, delete the album entry
                albumRef.removeValue().addOnSuccessListener(aVoid -> {
                    albums.remove(position);
                    albumsAdapter.notifyItemRemoved(position);
                    Toast.makeText(MainActivity.this, "Album deleted successfully", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to delete album: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to fetch album images: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
