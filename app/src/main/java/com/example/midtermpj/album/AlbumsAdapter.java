package com.example.midtermpj.album;

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
import com.example.midtermpj.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.ViewHolder> {

    private final ArrayList<String> albums;
    private final OnAlbumClickListener listener;
    private final OnDeleteClickListener onDeleteClickListener;
    private final Map<String, String> albumThumbnails = new HashMap<>();

    public AlbumsAdapter(ArrayList<String> albums, OnAlbumClickListener listener
            , OnDeleteClickListener onDeleteClickListener) {
        this.albums = albums;
        this.listener = listener;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.album_item
                , parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String albumName = albums.get(position);

        holder.albumNameTextView.setText(albumName);

        if ("Like Image".equals(albumName)) {
            // Use the hardcoded thumbnail URL for "Like Image"
            String favoriteThumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/mobilemidtermpj.appspot.com/o/likeimage.png?alt=media&token=2fc0a73e-615b-4155-99a0-273204a772a1";

            Glide.with(holder.itemView.getContext())
                    .load(favoriteThumbnailUrl)
                    .placeholder(R.drawable.placeholder_image) // Optional: placeholder image while loading
                    .into(holder.galleryImageView);
        }else {
            // Get reference to the album's thumbnail in Firebase
            DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(albumName).child("thumbnail");

            albumRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String thumbnailUrl = snapshot.getValue(String.class); // Fetch the thumbnail URL from Firebase

                    if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                        // Load the thumbnail using Glide
                        Glide.with(holder.itemView.getContext())
                                .load(thumbnailUrl)
                                .placeholder(R.drawable.placeholder_image)
                                .into(holder.galleryImageView);
                    } else {
                        // If no thumbnail is found, use placeholder
                        holder.galleryImageView.setImageResource(R.drawable.placeholder_image);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error (optional)
                    Toast.makeText(holder.itemView.getContext(), "Error loading thumbnail: "
                            + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        // Handle delete icon visibility
        if ("Like Image".equals(albumName)) {
            holder.deleteAlbumIcon.setVisibility(View.GONE);
        } else {
            holder.deleteAlbumIcon.setVisibility(View.VISIBLE);
            holder.deleteAlbumIcon.setOnClickListener(v -> onDeleteClickListener
                    .onDeleteClick(albumName, position));
        }

        if(albums.contains("Like Image")){
            albums.remove("Like Image");
            albums.add(0,"Like Image");
        }

        // Handle album click
        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(albumName));

        // Handle rename album functionality
        holder.renameIcon.setOnClickListener(v -> showRenameDialog(holder.itemView.getContext(), albumName, position));
    }


    @Override
    public int getItemCount() {
        return albums.size();
    }

    private void showRenameDialog(Context context, String oldAlbumName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Rename Album");

        final EditText input = new EditText(context);
        input.setText(oldAlbumName);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newAlbumName = input.getText().toString().trim();
            if (!newAlbumName.isEmpty() && !albums.contains(newAlbumName)) {
                updateAlbumNameInFirebase(context, oldAlbumName, newAlbumName, position);
            } else {
                Toast.makeText(context, "Invalid or duplicate album name."
                        , Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void updateAlbumNameInFirebase(Context context, String oldAlbumName, String newAlbumName, int position) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("albums");

        databaseReference.child(newAlbumName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(context, "Album with this name already exists."
                            , Toast.LENGTH_SHORT).show();
                } else {
                    databaseReference.child(newAlbumName).setValue(true).addOnSuccessListener(aVoid -> {
                        databaseReference.child(oldAlbumName).removeValue().addOnSuccessListener(aVoid1 -> {
                            albums.set(position, newAlbumName);
                            notifyItemChanged(position);
                            Toast.makeText(context, "Rename successful"
                                    , Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(context, "Error deleting old album: "
                                    + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }).addOnFailureListener(e -> {
                        Toast.makeText(context, "Error creating new album: "
                                + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error: " + error.getMessage()
                        , Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateThumbnail(String albumName, String thumbnailUrl) {
        albumThumbnails.put(albumName, thumbnailUrl);
        notifyDataSetChanged();
    }

    public void addImageToLikeAlbum(String imageUrl, Context context) {
        String likeAlbumName = "Like Image";
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums")
                .child(likeAlbumName);

        albumRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> albumData = new HashMap<>();
                    albumData.put("thumbnail", imageUrl);
                    albumData.put("images", new HashMap<>());

                    albumRef.setValue(albumData).addOnSuccessListener(aVoid -> {
                        albums.add(0, likeAlbumName);
                        albumThumbnails.put(likeAlbumName, imageUrl);
                        notifyDataSetChanged();
                        Toast.makeText(context, "Like Image album created"
                                , Toast.LENGTH_SHORT).show();
                        addImageToAlbum(albumRef, imageUrl, context);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to create Like Image album: "
                                + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    addImageToAlbum(albumRef, imageUrl, context);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error accessing Like Image album: "
                        + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addImageToAlbum(DatabaseReference albumRef, String imageUrl, Context context) {
        String imageId = albumRef.child("images").push().getKey();
        if (imageId != null) {
            albumRef.child("images").child(imageId).setValue(imageUrl).addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Image added to Like Image album"
                        , Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to add image: " + e.getMessage()
                        , Toast.LENGTH_SHORT).show();
            });
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView galleryImageView, renameIcon, deleteAlbumIcon;
        TextView albumNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            galleryImageView = itemView.findViewById(R.id.galleryImageView);
            albumNameTextView = itemView.findViewById(R.id.albumNameTextView);
            renameIcon = itemView.findViewById(R.id.renameIcon);
            deleteAlbumIcon = itemView.findViewById(R.id.deleteAlbum_ic);
        }
    }

    public interface OnAlbumClickListener {
        void onAlbumClick(String albumName);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(String albumName, int position);
    }

}