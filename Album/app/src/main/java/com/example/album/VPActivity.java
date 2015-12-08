package com.example.album;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.example.album.adapter.BitmapAsyncTask;
import com.example.album.views.MyImageView;

import java.util.ArrayList;
import java.util.List;

public class VPActivity extends Activity implements OnPageChangeListener {

    private static final String TAG = "TAG";
    public static final String URIS = "uris";
    public static final String POSITION = "position";
    private List<View> mViews;
    private ArrayList<String> mUris = new ArrayList<>();
    private ViewPager mViewPager;
    private LruCache<String, Bitmap> mLruCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vp);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mUris.addAll(getIntent().getStringArrayListExtra(URIS));
        mViews = new ArrayList<View>(mUris.size());
        mLruCache = new LruCache<String, Bitmap>((int) Runtime.getRuntime().maxMemory() / 8) {
            @SuppressLint("NewApi")
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        mViewPager.setOffscreenPageLimit(1);// 设置加载页数，为0的时候是3页，默认会加载7页
        mViewPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.vp_pageMargin));//设置页边间距
        mViewPager.setOnPageChangeListener(this);
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
        overridePendingTransition(0, R.transition.exit);
    }

    private class VPActivityAdpter extends PagerAdapter {

        VPActivityAdpter() {
            int size = mUris.size();
            for (int i = 0; i < size; i++) {
                MyImageView picture = new MyImageView(VPActivity.this);
                picture.setLayoutParams(new ViewPager.LayoutParams());
                mViews.add(picture);
            }
        }

        @Override
        public int getCount() {
            return mUris.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

            Bitmap bitmap = mLruCache.get(mUris.get(position));
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                mLruCache.remove(mUris.get(position));
                bitmap = null;
                Log.d(TAG, "destroyItem=" + position);
            }
            ((ViewPager) container).removeView((View) object);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.d(TAG, "instantiateItem=" + position);
            container.addView(mViews.get(position), 0);
            // 当postition=0的时候，不会调用接口OnPageChangeListener中的方法
            if (position == 0) {
                new BitmapAsyncTask(VPActivity.this, (MyImageView) mViews.get(position), mUris.get(position), new int[]{getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels}).execute(mLruCache);
            }
            return mViews.get(position);
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onPageSelected(int arg0) {

        MyImageView picture = (MyImageView) mViews.get(arg0);
        if (mLruCache.get(mUris.get(arg0)) == null) {
            new BitmapAsyncTask(VPActivity.this, (MyImageView) mViews.get(arg0), mUris.get(arg0), new int[]{getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels}).execute(mLruCache);
        } else {
            picture.setImageBitmap(mLruCache.get(mUris.get(arg0)));
        }

    }

}
