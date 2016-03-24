package com.wj.album.adapter;

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
    private int[] mWH;
    private String mUri;
    private ImageView mPiture;
    private Context mContext;

    public BitmapAsyncTask(Context context, ImageView picture, String uri, int[] widthHeight) {
        this.mContext = context;
        this.mPiture = picture;
        this.mUri = uri;
        this.mWH = widthHeight;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        //mUri.equals(mPiture.getTag())为防止错位
        if (result != null && mUri.equals(mPiture.getTag())) {
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
