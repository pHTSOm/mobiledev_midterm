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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddImage extends AppCompatActivity implements  RecyclerAdapter.CountImageUpdate
        , RecyclerAdapter.DeleteImageListener, RecyclerAdapter.itemClickListener {
    private static final int Read_Permission = 101;
    private static final int PICK_IMAGE = 1;
    RecyclerView recyclerView;
    TextView textView;
    Button addBtn;
    Button backBtn;
    Button sliderButton;
    ArrayList<Uri> uri = new ArrayList<>();
    RecyclerAdapter adapter;

    private Uri uriImage;
    StorageReference storageReference;
    String albumName;

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

        adapter = new RecyclerAdapter(uri,getApplicationContext(),this, this, this);

        recyclerView.setLayoutManager(new GridLayoutManager(AddImage.this, 3));
        recyclerView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(AddImage.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(AddImage.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Read_Permission);

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
            Toast.makeText(this, "You haven't picked any image", Toast.LENGTH_LONG).show();
        }
    }



    @Override
    public void clicked(int getSize) {
        textView.setText("Photos(" + uri.size() + ")");
    }

    @Override
    public void onDeleteImage(String imageUrl) {
        deleteImageFromGallery(imageUrl);
        deleteImageReferenceFromDatabase(imageUrl);
    }

    private void uploadToFirebase(Uri imageUri) {
        final String randomName = UUID.randomUUID().toString();
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child("images/" + randomName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get the download URL after the upload is complete
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUrl) {
                                // Save each image with its unique download URL to Firestore
                                saveImageToDatabase(downloadUrl.toString(), albumName);
                                Toast.makeText(AddImage.this, "Image Uploaded: " + downloadUrl.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddImage.this, "Upload Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void saveImageToDatabase(String imageUrl, String albumName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> image = new HashMap<>();
        image.put("url", imageUrl);
        image.put("album", albumName);

        db.collection("albums").document(albumName).collection("images").add(image)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Save the document ID for easier reference later
                        String documentId = documentReference.getId();
                        Toast.makeText(AddImage.this, "Image saved to database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddImage.this, "Failed to save image to database", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void deleteImageFromGallery(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(AddImage.this, "Invalid image URL", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
            storageReference.delete()
                    .addOnSuccessListener(aVoid -> {
                        // Handle successful deletion
                        Toast.makeText(AddImage.this, "Image deleted from storage", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure
                        Toast.makeText(AddImage.this, "Failed to delete image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (IllegalArgumentException e) {
            Toast.makeText(AddImage.this, "Failed to parse the storage URI", Toast.LENGTH_SHORT).show();
        }
    }



    private void deleteImageReferenceFromDatabase(String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query to find the document that matches the image URL
        db.collection("albums").document(albumName).collection("images")
                .whereEqualTo("url", imageUrl)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Delete the document by its ID
                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(AddImage.this, "Image reference deleted from database", Toast.LENGTH_SHORT).show();
                                        // Now remove the image from your local list
                                        uri.remove(Uri.parse(imageUrl));
                                        adapter.notifyDataSetChanged();
                                        textView.setText("Photos(" + uri.size() + ")");
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(AddImage.this, "Failed to delete image reference: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(AddImage.this, "Failed to find image reference in database", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void loadImagesFromDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("albums").document(albumName).collection("images")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        uri.clear();  // Clear the list before loading new data
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String url = document.getString("url");
                            if (url != null && !url.isEmpty() && !uriContains(url)) {
                                uri.add(Uri.parse(url));  // Add image URIs to the list if not already present
                            }
                        }
                        adapter.notifyDataSetChanged();  // Refresh the RecyclerView
                        textView.setText("Photos(" + uri.size() + ")");
                    } else {
                        Toast.makeText(AddImage.this, "Failed to load images", Toast.LENGTH_SHORT).show();
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

        TextView textView = dialog.findViewById(R.id.zoomPhoto_Text);
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