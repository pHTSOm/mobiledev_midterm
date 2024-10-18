package com.example.midtermpj;


import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.Map;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.ViewHolder> {

    private ArrayList<String> albums;
    private OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(String albumName);
    }

    public AlbumsAdapter(ArrayList<String> albums, OnAlbumClickListener listener) {
        this.albums = albums;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.album_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String albumName = albums.get(position);
        holder.albumNameTextView.setText(albumName);

        holder.renameIcon.setOnClickListener(v -> {
            // Show dialog to rename the album
            showRenameDialog(holder.itemView.getContext(), albumName, position);
        });

        holder.galleryImageView.setImageResource(R.drawable.photos_gallery);

        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(albumName));
    }

    private void showRenameDialog(Context context, String oldAlbumName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Rename Album");

        // Set up the input
        final EditText input = new EditText(context);
        input.setText(oldAlbumName);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newAlbumName = input.getText().toString();
            if (!newAlbumName.isEmpty()) {
                // Update the album name in Firebase
                updateAlbumNameInFirebase(context,oldAlbumName, newAlbumName, position);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateAlbumNameInFirebase(Context context, String oldAlbumName, String newAlbumName, int position) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("albums");

        // Check if the new album name already exists to prevent overwriting
        databaseReference.child(newAlbumName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Toast.makeText(context, "Album with this name already exists.", Toast.LENGTH_SHORT).show();
                } else {
                    // Set the new album name
                    databaseReference.child(newAlbumName).setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                // Delete the old album entry
                                databaseReference.child(oldAlbumName).removeValue()
                                        .addOnSuccessListener(aVoid1 -> {
                                            // Update the local list and notify the adapter
                                            albums.set(position, newAlbumName);
                                            notifyItemChanged(position);
                                            Toast.makeText(context, "Rename successful", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            // Handle the error during old album deletion
                                            Toast.makeText(context, "Error deleting old album: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                // Handle the error during new album creation
                                Toast.makeText(context, "Error creating new album: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Error checking existing album: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }





    @Override
    public int getItemCount() {
        return albums.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView galleryImageView;
        TextView albumNameTextView;
        ImageView renameIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            galleryImageView = itemView.findViewById(R.id.galleryImageView);
            albumNameTextView = itemView.findViewById(R.id.albumNameTextView);
            renameIcon = itemView.findViewById(R.id.renameIcon);
        }
    }
}
