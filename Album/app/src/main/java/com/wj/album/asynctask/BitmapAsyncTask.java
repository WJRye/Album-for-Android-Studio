package com.wj.album.asynctask;

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
        mUri = uri;
        //防止图片加载错位
        mPiture.setTag(KEY, mUri);
        mWH = wh;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result != null && mUri.equals(mPiture.getTag(KEY))) {
            mPiture.setImageBitmap(result);
        }
    }

    @Override
    protected Bitmap doInBackground(LruCache<String, Bitmap>... params) {
        Bitmap bitmap = BitmapUtil.compress(mPiture.getContext(), mUri, mWH[0], mWH[1]);
        if (bitmap != null) {
            params[0].put(mUri, bitmap);
        }
        return bitmap;
    }

}
