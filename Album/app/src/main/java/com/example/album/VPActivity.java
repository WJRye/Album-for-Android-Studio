package com.example.album;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.example.album.adapter.BitmapAsyncTask;
import com.example.album.views.RecycleImageView;

import java.util.ArrayList;

public class VPActivity extends Activity {

    public static final String URIS = "uris";
    public static final String POSITION = "position";
    private ArrayList<String> mUris;
    private ViewPager mViewPager;
    private LruCache<String, Bitmap> mLruCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vp);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mUris = getIntent().getStringArrayListExtra(URIS);
        mLruCache = new LruCache<String, Bitmap>((int) Runtime.getRuntime().maxMemory() / 8) {
            @SuppressLint("NewApi")
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        mViewPager.setOffscreenPageLimit(0);// 设置加载页数，为0的时候是3页，默认会加载7页
        // viewPager.setPageMargin(marginPixels);设置页边间距
        mViewPager.setAdapter(new VPActivityAdpter());
        mViewPager.setCurrentItem(getIntent().getIntExtra(POSITION, 0));
    }

    @Override
    protected void onDestroy() {
        if (mLruCache != null) {
            mLruCache.evictAll();
            mLruCache = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private class VPActivityAdpter extends PagerAdapter {

        private int width = 0;
        private int height = 0;

        VPActivityAdpter() {
            width = getResources().getDisplayMetrics().widthPixels;
            height = getResources().getDisplayMetrics().heightPixels;
        }

        @Override
        public int getCount() {
            return mUris.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

            if (position >= 0 && position <= mUris.size() - 1) {
                recycleBitmap(position);
            }

            ((ViewPager) container).removeView((View) object);
        }

        /*
        * 回收除当前，上一张，下一张以外的图片
        * */
        private void recycleBitmap(int position) {
            String uri = mUris.get(position);
            Bitmap bitmap = mLruCache.get(uri);
            if (bitmap != null) {
                bitmap.recycle();
                mLruCache.remove(uri);
                bitmap = null;
                Log.d("TAG", "destroyItem=" + position);
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.d("TAG", "instantiateItem=" + position);
            RecycleImageView pictureView = new RecycleImageView(VPActivity.this);
            pictureView.setLayoutParams(new ViewPager.LayoutParams());
            container.addView(pictureView, 0);
            new BitmapAsyncTask(VPActivity.this, pictureView, mUris.get(position), new int[]{width, height}).execute(mLruCache);
            return pictureView;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

    }

}
