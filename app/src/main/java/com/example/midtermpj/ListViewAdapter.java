package com.example.midtermpj;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.w3c.dom.Text;

import java.util.List;

public class ListViewAdapter extends ArrayAdapter<Product> {
    public ListViewAdapter(@NonNull Context context, int resource, @NonNull List<Product> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;

        if(null == v){
            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.list_item, null);

        }
        Product product = getItem(position);
        ImageView img =(ImageView) v.findViewById(R.id.imageView);
        TextView txtTitle =(TextView) v.findViewById(R.id.txtTitle);
        TextView txtDescription =(TextView) v.findViewById(R.id.txtDescription);

        img.setImageResource(product.getImageId());
        txtTitle.setText(product.getTitle());
        txtDescription.setText(product.getDescription());

        return v;
    }
}
