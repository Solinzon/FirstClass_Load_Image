package com.example.xushuzhan.loadimagetest;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;
import java.io.InputStream;


/**
 * Created by xushuzhan on 2017/3/30.
 * 图片的压缩类
 */

public class ImageResizer {
    private static final String TAG = "ImageResizer";

    /**
     * 计算采样率
     *
     * @param options
     * @param reqWidth  ImageView的宽度
     * @param reqHeight ImageView的高度
     * @return
     */
    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        if (reqHeight == 0 || reqWidth == 0) {
            return 1;
        }
        //图片的宽高
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= width) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * 从资源文件加载bitmap并且压缩
     *
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        //首先测量图片的基本信息，以便计算采样率
        BitmapFactory.Options options = new BitmapFactory.Options();
        //仅仅测量图片，而不加载
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        //计算采样率
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        //加载图片了
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * 从FileDescriptor加载并压缩
     *
     * @param fd
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap decodeSampleBitmapFromFileDescriptor(FileDescriptor fd, int reqWidth, int reqHeight) {
        //首先测量图片的基本信息，以便计算采样率
        BitmapFactory.Options options = new BitmapFactory.Options();
        //仅仅测量图片，而不加载
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);
        //计算采样率
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        //加载图片了
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }

    /**
     * 从InputStreamr加载并压缩
     * @param in
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap decodeSampleBitmapFromInputStream(InputStream in, int reqWidth, int reqHeight) {
        //首先测量图片的基本信息，以便计算采样率
        BitmapFactory.Options options = new BitmapFactory.Options();
        //仅仅测量图片，而不加载
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, options);
        //计算采样率
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        //加载图片了
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(in, null, options);
    }

}
