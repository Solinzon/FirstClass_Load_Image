package com.example.xushuzhan.loadimagetest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static android.R.attr.bitmap;
import static android.R.attr.key;


/**
 * Created by xushuzhan on 2017/3/29.
 */

public class ImageLoader {
    private static final String TAG = "ImageLoader";
    //obtainMessage的TAG
    private static final int MESSAGE_POST_RESULT = 1;
    //cpu的核心数量
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    //线程池的核心线程数量
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    //线程池的最大线程数量
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    //线程的等待时长
    private static final long KEEP_ALIVE = 10L;
    //ImageView的KEY
    private static final int TAG_KEY_URL = R.id.iv_main_pic;
    //硬盘缓存的最大值50M
    private static final long DISK_CACHE_SISE = 1024 * 1024 * 50;
    //创建一个ThreadFactory，为线程池做准备
    private static final ThreadFactory mThreadFactory = new ThreadFactory() {
        //线程安全的加减操作
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
        }
    };

    //创建一个线程池
    public static final Executor THREAD_POOL_EXECUTOR =
            new ThreadPoolExecutor(CORE_POOL_SIZE,
                    MAXIMUM_POOL_SIZE,
                    KEEP_ALIVE,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    mThreadFactory
            );

    //创建一个在主线程运行的Handler
    private Handler mMainHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            LoaderResult rusult = (LoaderResult) msg.obj;
            ImageView imageView = rusult.imageView;
            String url = (String) imageView.getTag(TAG_KEY_URL);
            if (url.equals(rusult.url)) {
                imageView.setImageBitmap(rusult.bitmap);
            } else {
                Log.d(TAG, "url匹配失败，bitmap已经更新");
            }
        }
    };


    private Context mContext;
    private ImageResizer mImageResizer;
    private LruCache<String, Bitmap> mMemoryCache;
    //硬盘缓存的地址
    private String mFilePath = null;

    private ImageLoader(Context context) {
        mContext = context.getApplicationContext();//获取application的上下文
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);//单位为kb
        int cacheSize = maxMemory / 8;//取最大内存的1／8；
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024; //单位为kb
            }
        };
        mFilePath = mContext.getExternalCacheDir().getPath();
        mImageResizer = new ImageResizer();
    }

    /**
     * 用一个静态方法来初始化类
     *
     * @param context
     * @return
     */
    public static ImageLoader build(Context context) {
        return new ImageLoader(context);
    }

    /**
     * 将Bitmap加入LruCache
     *
     * @param key
     * @param bitmap
     */
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null && key != null && bitmap != null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * 从LruCache中取出bitmap
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    /**
     * 从LruCache中取出bitmap
     * @param url 图片的url
     * @return
     */
    private Bitmap loadBitmapFromMemoryCache(String url) {
        Log.d(TAG, "fromLruCache: ");
        return getBitmapFromMemoryCache(getMD5(url));
    }

    /**
     * 从网络加载图片
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap loadBitmaoFromHttp(String url, int reqWidth, int reqHeight) {
        Log.d(TAG, "fromHttp: ");
        if (Looper.myLooper() == Looper.getMainLooper()) { //如果当前在主线程上
            throw new RuntimeException("不能在主线程进行网络操作");
        }
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        Bitmap bitmap = null;
        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setDoInput(true);
            urlConnection.connect();
            inputStream = urlConnection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
            saveBitmap(inputStream,mFilePath,url,reqWidth,reqHeight);//将bitmap保存到本地
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            urlConnection.disconnect();
        }
        return bitmap;
    }

    /**
     * 从硬盘加载图片
     *
     * @param url
     * @return
     */
    private Bitmap loadBitmapFromDisk(String url) {
        Bitmap bitmap = null;
        //文件名字
        String key = getMD5(url) + ".jpg";
        //获取缓存文件夹
        File file = new File(mFilePath);
        //获取所有文件
        File[] files = file.listFiles();
        //遍历磁盘缓存中找到这个图片
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(key)) {
                bitmap = BitmapFactory.decodeFile(files[i].getPath());
                Log.d(TAG, "fromDisk");
                break;
            }
        }
        addBitmapToMemoryCache(getMD5(url), bitmap);
        return bitmap;
    }

    /**
     * 加载bitmap(同步加载)
     *
     * @param url
     * @param reqWidth  ImageView所期望的宽度（就是ImageView的宽度）
     * @param reqHeight ImageView所期望的高度（就是ImageView的高度）
     * @return
     */
    public Bitmap loadBitmap(String url, int reqWidth, int reqHeight) {
        Bitmap bitmap = loadBitmapFromMemoryCache(url);
        if (bitmap != null) {
            Log.d(TAG, "loadBitmap: 从缓存中找出了Bitmap");
            return bitmap;
        }
        bitmap = loadBitmapFromDisk(url);
        if (bitmap!=null){
            return bitmap;
        }
        bitmap = loadBitmaoFromHttp(url, reqWidth, reqHeight);
        addBitmapToMemoryCache(getMD5(url), bitmap);
        return bitmap;
    }

    /**
     * 给ImageView设置Bitmap（异步加载） 不知道目标ImageView的时候就用这个方法
     *
     * @param url
     * @param imageview
     */
    public void bindBitmap(final String url, final ImageView imageview) {
        bindBitmap(url, imageview, 0, 0);
    }

    /**
     * 给ImageView设置（异步加载）
     *
     * @param url
     * @param imageview
     * @param reqWidth
     * @param reqHeight
     */
    public void bindBitmap(final String url, final ImageView imageview, final int reqWidth, final int reqHeight) {
        imageview.setImageResource(R.drawable.loading);
        imageview.setTag(TAG_KEY_URL, url);
        Bitmap bitmap = loadBitmapFromMemoryCache(url);//先尝试从缓存中找bitmap
        if (bitmap != null) {
            Log.d(TAG, "bindBitmap: 从缓存中找出了Bitmap,现在设置给ImageView");
            imageview.setImageBitmap(bitmap);
            return;
        }

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(url, reqWidth, reqHeight);
                if (bitmap != null) {
                    LoaderResult result = new LoaderResult(imageview, url, bitmap);
                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).sendToTarget();
                }
            }
        };

        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

    /**
     * 保存Bitmap到本地
     * @param in
     * @param url
     */
    public void saveBitmap(InputStream in, String url) {
        saveBitmap(in, null, url, 0, 0);
    }

    /**
     * 保存Bitmap到本地
     * @param in
     * @param path
     * @param url
     * @param reqWidth
     * @param reqHeight
     */
    public void saveBitmap(InputStream in, String path, String url, int reqWidth, int reqHeight) {
        String filePath;
        String fileName = getMD5(url) + ".jpg";
        if (path == null) {
            filePath = mFilePath;
        } else {
            filePath = path;
        }
        File bitmapDir = new File(filePath);
        if (!bitmapDir.exists()) {
            bitmapDir.mkdir();
        }
        try {
            if (getDiskSize(filePath) > DISK_CACHE_SISE) {
                clearDisk(path);
            }
            File file = new File(filePath, fileName);
            if (file.exists()) {
                return;
            }
            FileOutputStream fos = new FileOutputStream(file);
            Bitmap bitmap = mImageResizer.decodeSampleBitmapFromInputStream(in, reqWidth, reqHeight);
            if (bitmap!=null){
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                Log.d(TAG, "已保存: " + fileName);

            }
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取文件的总大小
     *
     * @return size
     */
    public long getDiskSize(String path) {
        long size = 0;
        try {
            File file = new File(path);//获取缓存文件夹
            File[] files = file.listFiles();//获取所有文件
            for (int i = 0; i < files.length; i++) {
                size = size + files[i].length();
            }
        } catch (Exception e) {

        }
        return size;
    }

    /**
     * 清除目录所有文件
     */
    public void clearDisk(String path) {
        File file = new File(path);//获取缓存文件夹
        File[] files = file.listFiles();//获取所有文件
        for (File f : files) {
            f.delete();
        }
    }


    /**
     * MD5加密
     *
     * @param val 待加密字符串
     * @return 加密后的string
     * @throws NoSuchAlgorithmException
     */
    public static String getMD5(String val) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md5.update(val.getBytes());
        byte[] m = md5.digest();//加密
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < m.length; i++) {
            sb.append(m[i]);
        }
        return sb.toString();
    }

    private static class LoaderResult {
        public ImageView imageView;
        public String url;
        public Bitmap bitmap;

        public LoaderResult(ImageView imageView, String url, Bitmap bitmap) {
            this.imageView = imageView;
            this.url = url;
            this.bitmap = bitmap;
        }
    }
}
