package com.wj.album.asynctask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.wj.album.utils.BitmapUtil;

/**
 * 该类主要用作于加载图片
 */
public class BitmapAsyncTask extends AsyncTask<LruCache<String, Bitmap>, Void, Bitmap> {
    private final long KEY = System.currentTimeMillis();
    private int[] mWH;
    private String mUri;
    private ImageView mPiture;
    private Context mContext;

    public BitmapAsyncTask(ImageView picture, String uri, int[] wh) {
        if (picture == null) throw new NullPointerException("The ImageView is null!");
        mPiture = picture;
        mPiture.setTag((int) KEY, uri);
        mContext = picture.getContext();
        mUri = uri;
        mWH = wh;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        //mUri.equals(mPiture.getTag())为防止错位
        if (result != null && mUri.equals(mPiture.getTag((int) KEY))) {
            mPiture.setImageBitmap(result);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected Bitmap doInBackground(@SuppressWarnings("unchecked") LruCache<String, Bitmap>... params) {
        LruCache<String, Bitmap> lruCache = params[0];
        Bitmap bitmap = lruCache.get(mUri);
        if (bitmap == null) {
            bitmap = BitmapUtil.compress(mContext, mUri, mWH[0], mWH[1]);
            if (bitmap != null) {
                lruCache.put(mUri, bitmap);
            }
        }
        return bitmap;
    }

}
