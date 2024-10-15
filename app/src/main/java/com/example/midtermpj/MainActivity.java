package com.example.midtermpj;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);

        albumNameEditText = findViewById(R.id.albumNameEditText);
        createAlbumBtn = findViewById(R.id.createAlbumBtn);
        albumsRecyclerView = findViewById(R.id.albumsRecyclerView);

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
        });

        albumsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        albumsRecyclerView.setAdapter(albumsAdapter);

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
}
