package com.example.midtermpj;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ViewStub stubGrid;
    private ViewStub stubList;
    private ListView listView;
    private GridView gridView;
    private ListViewAdapter listViewAdapter;
    private GridViewAdapter gridViewAdapter;
    private List<Product> productList;
    private int currentViewMode=0;

    static final int VIEW_MODE_LISTVIEW = 0;
    static final int VIEW_MODE_GRIDVIEW = 1;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        stubList = findViewById(R.id.stub_list);
        stubGrid = findViewById(R.id.stub_grid);

        stubList.inflate();
        stubGrid.inflate();

        listView =(ListView) findViewById(R.id.mylistview);
        gridView =(GridView) findViewById(R.id.mygridview);

        getProductList();

        Button switchViewButton = findViewById(R.id.button_switch);
        switchViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentViewMode ==VIEW_MODE_LISTVIEW){
                    currentViewMode = VIEW_MODE_GRIDVIEW;
                }else {
                    currentViewMode = VIEW_MODE_LISTVIEW;
                }
                switchView();

                SharedPreferences sharedPreferences =getSharedPreferences("ViewMode", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("currentViewMode", currentViewMode);
                editor.apply();

            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("ViewMode", MODE_PRIVATE);
        currentViewMode = sharedPreferences.getInt("currentViewMode", VIEW_MODE_LISTVIEW);

        // Initialize the view mode based on saved preferences
        switchView();
    }

    private void switchView() {
        if (VIEW_MODE_LISTVIEW == currentViewMode){
            stubList.setVisibility(View.VISIBLE);
            stubGrid.setVisibility(View.GONE);
        }
        else{
            stubList.setVisibility(View.GONE);
            stubGrid.setVisibility(View.VISIBLE);
        }
        setAdapters();
        
    }

    private void setAdapters() {
        if (VIEW_MODE_LISTVIEW == currentViewMode){
            listViewAdapter =new ListViewAdapter(this, R.layout.list_item, productList);
            listView.setAdapter(listViewAdapter);
        }else {
            gridViewAdapter = new GridViewAdapter(this, R.layout.grid_item, productList);
            gridView.setAdapter((gridViewAdapter));
        }
    }

    public List<Product> getProductList() {
        productList = new ArrayList<>();
        productList.add(new Product(R.drawable.ic_launcher_foreground, "Title 1", "This is description 1"));
        productList.add(new Product(R.drawable.ic_launcher_foreground, "Title 2", "This is description 2"));
        productList.add(new Product(R.drawable.ic_launcher_foreground, "Title 3", "This is description 3"));
        productList.add(new Product(R.drawable.ic_launcher_foreground, "Title 4", "This is description 4"));
        productList.add(new Product(R.drawable.ic_launcher_foreground, "Title 5", "This is description 5"));
        productList.add(new Product(R.drawable.ic_launcher_foreground, "Title 6", "This is description 6"));
        productList.add(new Product(R.drawable.ic_launcher_foreground, "Title 7", "This is description 7"));
        productList.add(new Product(R.drawable.ic_launcher_foreground, "Title 8", "This is description 8"));
        productList.add(new Product(R.drawable.ic_launcher_foreground, "Title 9", "This is description 9"));
        productList.add(new Product(R.drawable.ic_launcher_foreground, "Title 10", "This is description 10"));

        return productList;
    }

}