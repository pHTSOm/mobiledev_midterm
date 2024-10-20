package com.example.midtermpj;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{
    EditText albumNameEditText;
    Button createAlbumBtn;
    RecyclerView albumsRecyclerView;
    AlbumsAdapter albumsAdapter;
    ArrayList<String> albums = new ArrayList<>();
    DatabaseReference albumRef; // Firebase reference
    boolean isListView= true;
    ScaleGestureDetector scaleGestureDetector;
    float FACTOR = 1.0f;
    ImageView imageView;

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
        setRecyclerViewLayoutManager(isListView);//defaul is listView

        // Add click listener to Create Album button
        createAlbumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        // Load albums from Firebase
        loadAlbumsFromFirebase();
        switchButton.setOnClickListener(v -> {
            isListView = !isListView; // toggle between list and grid
            setRecyclerViewLayoutManager(isListView);

            // Update button text
            if (isListView) {
                switchButton.setText("List");
            } else {
                switchButton.setText("Grid");
            }
        });
    }

    private void setRecyclerViewLayoutManager(boolean isListView) {
        if (isListView){
            albumsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }else {
            albumsRecyclerView.setLayoutManager(new GridLayoutManager(this,3));
        }
    }

    private void addAlbumToFirebase(String albumName) {
        albumRef.child(albumName).setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("MainActivity", "Album successfully created in Firebase");
                albums.add(albumName);
                albumsAdapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Album created", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(MainActivity.this, AddImage.class);
                intent.putExtra("ALBUM_NAME", albumName); // Pass the album name if needed
                startActivity(intent); // Start AddImage activity
            } else {
                Log.d("MainActivity", "Failed to create album in Firebase");
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
                    }
                }
                albumsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load albums", Toast.LENGTH_SHORT).show();
            }
        });
    }

        private void deleteAlbumFromFirebase(String albumName, int position) {
            DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums");
            albumRef.child(albumName).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Remove the album from the local list and notify the adapter
                    albums.remove(position);
                    albumsAdapter.notifyItemRemoved(position);
                    Toast.makeText(MainActivity.this, "Album deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to delete album", Toast.LENGTH_SHORT).show();
                }
            });
        }
}
