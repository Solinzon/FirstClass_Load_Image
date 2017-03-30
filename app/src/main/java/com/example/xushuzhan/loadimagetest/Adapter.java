package com.example.xushuzhan.loadimagetest;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xushuzhan on 2017/3/30.
 */

public class Adapter extends RecyclerView.Adapter<Adapter.MyViewHolder> {
    Context mContext;
    List<String> mUrl;
    private static final String TAG = "Adapter";

    public Adapter(Context context, List<String> url) {
        mContext = context;
        mUrl = url;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: "+holder.imageView.getWidth()+">"+holder.itemView.getHeight());
        ImageLoader.build(mContext).bindBitmap(mUrl.get(position), holder.imageView, holder.imageView.getWidth(), holder.itemView.getHeight());
    }

    @Override
    public int getItemCount() {
        return mUrl.size();

    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public MyViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.iv_main_pic);
        }
    }
}
