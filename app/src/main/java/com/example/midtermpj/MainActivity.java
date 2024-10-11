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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int Read_Permission = 101;
    private static final int PICK_IMAGE = 1;
    RecyclerView recyclerView;
    TextView textView;
    Button addBtn;
    ArrayList<Uri> uri = new ArrayList<>();
    RecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.totalPhotos);
        recyclerView = findViewById(R.id.myRecyclerView);
        addBtn = findViewById(R.id.addBtn);

        adapter = new RecyclerAdapter(uri,getApplicationContext());

        recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this, 3));
        recyclerView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Read_Permission);

        }

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
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
                    Uri uriImage = data.getClipData().getItemAt(i).getUri();
                    uri.add(uriImage);  // Add image URI to the list
                }

                //notify adapter
                adapter.notifyDataSetChanged();
                textView.setText("Photos(" + uri.size() + ")");
            } else {
                // get single image
                Uri imageUri = data.getData();
                //add code to arraylist
                uri.add(imageUri);
            }
            adapter.notifyDataSetChanged();
            textView.setText("Photos(" + uri.size() + ")");
        } else {
            Toast.makeText(this, "You haven't pick any image", Toast.LENGTH_LONG).show();
        }
    }
}
