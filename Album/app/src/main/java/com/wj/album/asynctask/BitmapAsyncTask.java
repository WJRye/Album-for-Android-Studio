package com.wj.album.asynctask;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.wj.album.utils.BitmapUtil;

/**
 * 该类主要用作于加载图片
 */
public class BitmapAsyncTask extends AsyncTask<LruCache<String, Bitmap>, Void, Bitmap> {
    private final int KEY = (int) System.currentTimeMillis();
    private int[] mWH;
    private String mUri;
    private ImageView mPiture;

    public BitmapAsyncTask(ImageView picture, String uri, int[] wh) {
        if (picture == null) throw new NullPointerException("The ImageView is null!");
        mPiture = picture;
        //防止图片加载错位
        mPiture.setTag(KEY, uri);
        mUri = uri;
        mWH = wh;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result != null && mUri.equals(mPiture.getTag(KEY))) {
            mPiture.setImageBitmap(result);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected Bitmap doInBackground(@SuppressWarnings("unchecked") LruCache<String, Bitmap>... params) {
        LruCache<String, Bitmap> lruCache = params[0];
        Bitmap bitmap = lruCache.get(mUri);
        if (bitmap == null) {
            bitmap = BitmapUtil.compress(mPiture.getContext(), mUri, mWH[0], mWH[1]);
            if (bitmap != null) {
                lruCache.put(mUri, bitmap);
            }
        }
        return bitmap;
    }

}
