package com.example.midtermpj;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class AddImage extends AppCompatActivity implements  RecyclerAdapter.CountImageUpdate {
    private static final int Read_Permission = 101;
    private static final int PICK_IMAGE = 1;
    RecyclerView recyclerView;
    TextView textView;
    Button addBtn;
    Button backBtn;
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

        adapter = new RecyclerAdapter(uri,getApplicationContext(),this);

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && null != data) {
            if (data.getClipData() != null) {

                //get multiple images
                int countImages = data.getClipData().getItemCount();

                for (int i = 0; i < countImages; i++) {
                    uriImage = data.getClipData().getItemAt(i).getUri();
                    uri.add(uriImage);
                    uploadToFirebase();
                }

                //notify adapter
                adapter.notifyDataSetChanged();
                textView.setText("Photos(" + uri.size() + ")");
            } else {
                // get single image
                uriImage = data.getData();
                //add code to arraylist
                uri.add(uriImage);
                uploadToFirebase();
            }
            adapter.notifyDataSetChanged();
            textView.setText("Photos(" + uri.size() + ")");
        } else {
            Toast.makeText(this, "You haven't pick any image", Toast.LENGTH_LONG).show();
        }
    }



    @Override
    public void clicked(int getSize) {
        textView.setText("Photos(" + uri.size() + ")");
    }

    private void uploadToFirebase() {
        final String randomName = UUID.randomUUID().toString();
        storageReference = FirebaseStorage.getInstance().getReference().child("images/" + randomName);

        storageReference.putFile(uriImage)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get the download URL after the upload is complete
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUrl) {
                                saveImageToDatabase(downloadUrl.toString(), albumName);
                                Toast.makeText(AddImage.this, "Image Uploaded", Toast.LENGTH_SHORT).show();
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

    private void loadImagesFromDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("albums").document(albumName).collection("images")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        uri.clear();  // Clear the list before loading new data
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String url = document.getString("url");
                            if (url != null && !url.isEmpty()) {
                                uri.add(Uri.parse(url));  // Add image URIs to the list
                            }
                        }
                        adapter.notifyDataSetChanged();  // Refresh the RecyclerView
                        textView.setText("Photos(" + uri.size() + ")");
                    } else {
                        Toast.makeText(AddImage.this, "Failed to load images", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
