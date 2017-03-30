package com.example.xushuzhan.loadimagetest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.data;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivityTAG";
    List<String> imageUrl = new ArrayList<>();
    RecyclerView recyclerView;
    Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getData();
    }

    private void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        adapter = new Adapter(MainActivity.this, imageUrl);
        recyclerView.setAdapter(adapter);
    }

    private void getData() {
        HttpUtils.sendHttpRequest("http://gank.io/api/data/%E7%A6%8F%E5%88%A9/0/0", new HttpUtils.CallBack() {
            @Override
            public void onFinish(final String response) {
                Gson gson = new Gson();
                Data data = gson.fromJson(response, Data.class);

                for (int i = 0; i < data.getResults().length; i++) {
                    imageUrl.add(data.getResults()[i].getUrl());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initView();
                    }
                });
            }
        });
    }
}
