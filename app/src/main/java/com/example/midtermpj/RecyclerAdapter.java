package com.example.midtermpj;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder>{

    private ArrayList<Uri> uriArrayList;
    private Context context;
    CountImageUpdate countImageUpdate;


    public RecyclerAdapter(ArrayList<Uri> uri, Context context, CountImageUpdate countImageUpdate) {
        this.uriArrayList = uri;
        this.context = context;
        this.countImageUpdate = countImageUpdate;
    }

    @NonNull
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.image_single,parent,false);

        return new ViewHolder(view,countImageUpdate);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
//        holder.imageView.setImageURI(uriArrayList.get(position));

        Glide.with(context)
                .load(uriArrayList.get(position))
                .placeholder(R.drawable.placeholder_image)  // Placeholder while loading
                .error(R.drawable.error_image)
                .into(holder.imageView);

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int imagePosition = holder.getAdapterPosition();
                if(imagePosition != RecyclerView.NO_POSITION){
                    uriArrayList.remove(imagePosition);
                    notifyItemRemoved(imagePosition);
                    notifyItemRangeChanged(imagePosition, uriArrayList.size());
                    countImageUpdate.clicked(uriArrayList.size());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return uriArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView,delete;
        CountImageUpdate countImageUpdate;
        public ViewHolder(@NonNull View itemView, CountImageUpdate countImageUpdate) {
            super(itemView);
            this.countImageUpdate = countImageUpdate;
            imageView = itemView.findViewById(R.id.image);
            delete = itemView.findViewById(R.id.delete);
        }
    }

    public interface CountImageUpdate{
        void clicked(int getSize);
    }
}
