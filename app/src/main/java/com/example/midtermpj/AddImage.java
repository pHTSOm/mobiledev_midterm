package com.example.midtermpj;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddImage
        extends AppCompatActivity
        implements RecyclerAdapter.CountImageUpdate,
        RecyclerAdapter.DeleteImageListener,
        RecyclerAdapter.itemClickListener {
    private static final int READ_PERMISSION = 101;
    private static final int PICK_IMAGE = 1;

    RecyclerView recyclerView;
    TextView textView;
    Button addBtn;
    Button backBtn;
    Button sliderButton;
    ArrayList<Uri> uri = new ArrayList<>();
    RecyclerAdapter adapter;
    StorageReference storageReference;
    String albumName;
    private Uri uriImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        albumName = getIntent().getStringExtra("ALBUM_NAME");

        textView = findViewById(R.id.totalPhotos);
        recyclerView = findViewById(R.id.myRecyclerView);
        addBtn = findViewById(R.id.addBtn);
        backBtn = findViewById(R.id.backBtn);
        sliderButton = findViewById(R.id.sliderBtn);

        adapter = new RecyclerAdapter(uri, getApplicationContext(),
                this, this, this);
        recyclerView.setLayoutManager(new GridLayoutManager(AddImage.this, 3));
        recyclerView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(
                AddImage.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(AddImage.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION);

        }

        loadImagesFromDatabase();

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button sliderButton = findViewById(R.id.sliderBtn);
        sliderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AddImage.this, ImageSliderActivity.class);
                // Assuming `uri` contains the list of image URIs
                intent.putParcelableArrayListExtra("imageUris", uri);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && null != data) {
            if (data.getClipData() != null) {
                // Get multiple images
                int countImages = data.getClipData().getItemCount();

                for (int i = 0; i < countImages; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();  // Get each URI
                    uri.add(imageUri);  // Add to the URI list
                    uploadToFirebase(imageUri);  // Pass each URI to the upload function
                }

                // Notify adapter
                adapter.notifyDataSetChanged();
                textView.setText("Photos(" + uri.size() + ")");

            } else if (data.getData() != null) {
                // Get single image
                Uri imageUri = data.getData();
                uri.add(imageUri);  // Add to the URI list
                uploadToFirebase(imageUri);  // Pass the URI to the upload function
                adapter.notifyDataSetChanged();
                textView.setText("Photos(" + uri.size() + ")");
            }
        } else {
            Toast.makeText(this, "You haven't picked any image"
                    , Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void clicked(int getSize) {
        textView.setText("Photos(" + uri.size() + ")");
    }

    @Override
    public void onDeleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(AddImage.this, "Invalid image URL"
                    , Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove from Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        storageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from Realtime Database
                    DatabaseReference albumRef = FirebaseDatabase.getInstance()
                            .getReference("albums").child(albumName).child("images");
                    albumRef.orderByValue().equalTo(imageUrl)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                                        childSnapshot.getRef().removeValue();
                                    }
                                    // Update local list and RecyclerView
                                    uri.remove(Uri.parse(imageUrl));
                                    adapter.notifyDataSetChanged();
                                    textView.setText("Photos(" + uri.size() + ")");
                                    Toast.makeText(AddImage.this, "Image deleted successfully", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(AddImage.this, "Failed to delete image reference: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(AddImage.this, "Failed to delete image from storage: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void uploadToFirebase(Uri imageUri) {
        final String randomName = UUID.randomUUID().toString();
        StorageReference imageRef = FirebaseStorage.getInstance().getReference()
                .child("images/" + randomName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUrl -> {
                            String imageUrl = downloadUrl.toString();

                            // Save to Realtime Database
                            DatabaseReference albumRef = FirebaseDatabase.getInstance()
                                    .getReference("albums").child(albumName);

                            // Generate a unique ID for the image
                            String imageId = albumRef.child("images").push().getKey();

                            if (imageId != null) {
                                // Add the image URL to the album
                                albumRef.child("images").child(imageId).setValue(imageUrl)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(AddImage.this
                                                    , "Image uploaded successfully"
                                                    , Toast.LENGTH_SHORT).show();

                                            // Update the thumbnail if this is the first image
                                            albumRef.child("thumbnail")
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            if (!snapshot.exists() || snapshot
                                                                    .getValue(String.class)
                                                                    .isEmpty()) {
                                                                albumRef.child("thumbnail")
                                                                        .setValue(imageUrl);

                                                                // Notify AlbumsAdapter of the new thumbnail
                                                                MainActivity.albumsAdapter
                                                                        .updateThumbnail(albumName, imageUrl);

                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                                                            Toast.makeText(AddImage.this
                                                                    , "Failed to set thumbnail"
                                                                    , Toast.LENGTH_SHORT).show();
                                                        }
                                                    });

                                        })
                                        .addOnFailureListener(e -> Toast.makeText(AddImage.this
                                                , "Failed to update database: " + e.getMessage()
                                                , Toast.LENGTH_SHORT).show());
                            }
                        }))
                .addOnFailureListener(e -> Toast.makeText(AddImage.this
                        , "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void saveImageToDatabase(String imageUrl, String albumName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> image = new HashMap<>();
        image.put("url", imageUrl);
        image.put("album", albumName);

        db.collection("albums").document(albumName).collection("images")
                .add(image)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Save the document ID for easier reference later
                        String documentId = documentReference.getId();
                        Toast.makeText(AddImage.this, "Image saved to database"
                                , Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddImage.this, "Failed to save image to database"
                                , Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadImagesFromDatabase() {
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums")
                .child(albumName).child("images");

        albumRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                uri.clear(); // Clear the list before loading new data
                for (DataSnapshot imageSnapshot : snapshot.getChildren()) {
                    String imageUrl = imageSnapshot.getValue(String.class);
                    if (imageUrl != null && !uriContains(imageUrl)) {
                        uri.add(Uri.parse(imageUrl));
                    }
                }
                adapter.notifyDataSetChanged();
                textView.setText("Photos(" + uri.size() + ")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddImage.this, "Failed to load images"
                        , Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean uriContains(String url) {
        for (Uri uriItem : uri) {
            if (uriItem.toString().equals(url)) {
                return true;
            }
        }
        return false;
    }

    private void showImageSlider() {
        // Launch the new ImageSliderActivity with the image URIs
        Intent intent = new Intent(AddImage.this, ImageSliderActivity.class);
        intent.putParcelableArrayListExtra("imageUris", uri);  // Pass the image URIs
        startActivity(intent);
    }

    @Override
    public void itemClick(int position) {

        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.zoom_photo);

//        TextView textView = dialog.findViewById(R.id.zoomPhoto_Text);
        ImageView imageView = dialog.findViewById(R.id.image_view_zoom);
        Button buttonClose = dialog.findViewById(R.id.btn_close_zoom);

        textView.setText("Image" + position);

        Glide.with(this)
                .load(uri.get(position)) // Load the URI using Glide
                .placeholder(R.drawable.placeholder_image)  // Placeholder while loading
                .error(R.drawable.error_image) // Show an error image if it fails to load
                .into(imageView);

        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}