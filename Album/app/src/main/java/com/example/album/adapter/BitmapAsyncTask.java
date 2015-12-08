package com.example.album.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.example.album.utils.BitmapUtil;

public class BitmapAsyncTask extends
		AsyncTask<LruCache<String, Bitmap>, Void, Bitmap> {
	private int[] mWH;
	private String mUri;
	private ImageView mPiture;
	private Context mContext;

	@SuppressLint("NewApi")
	public BitmapAsyncTask(Context context, ImageView picture, String uri,
			int[] widthHeight) {
		this.mContext = context;
		this.mPiture = picture;
		this.mUri = uri;
		this.mWH = widthHeight;
	}

    @Override
	protected void onPostExecute(Bitmap result) {
		if (result != null) {
			mPiture.setImageBitmap(result);
		}
	}

	@SuppressLint("NewApi")
	@Override
	protected Bitmap doInBackground(@SuppressWarnings("unchecked") LruCache<String, Bitmap>... params) {
		LruCache<String, Bitmap> lruCache = params[0];
		Bitmap bitmap = lruCache.get(mUri);
		if (bitmap == null) {
			bitmap = BitmapUtil.compress(mContext, mUri, mWH[0],
					mWH[1]);
			if (bitmap != null) {
				lruCache.put(mUri, bitmap);
			}
		}
		return bitmap;
	}

}
