package com.wj.album;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.wj.album.asynctask.BitmapAsyncTask;
import com.wj.album.views.RecycleImageView;

import java.util.ArrayList;

/**
 * User: WangJiang(https://github.com/WJRye)
 * Date: 2016-04-09
 * Time: 21:59
 */
public class VPActivity extends BaseActivity {

    public static final String URIS = "uris";
    public static final String POSITION = "position";
    //图片路径
    private ArrayList<String> mUris;
    //缓存图片
    private LruCache<String, Bitmap> mLruCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
    }

    public void initViews() {
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        mUris = getIntent().getStringArrayListExtra(URIS);
        mLruCache = new LruCache<String, Bitmap>((int) Runtime.getRuntime().maxMemory() / 8) {
            @SuppressLint("NewApi")
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        viewPager.setOffscreenPageLimit(0);// 设置加载页数，为0的时候是3页，默认可能会加载7页
        viewPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.vp_pageMargin));//设置页边间距
        viewPager.setAdapter(new VPActivityAdpter());
        viewPager.setCurrentItem(getIntent().getIntExtra(POSITION, 0));
    }

    @Override
    public int getLayoutResID() {
        return R.layout.activity_vp;
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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private class VPActivityAdpter extends PagerAdapter {

        private int[] mWH = new int[2];

        private VPActivityAdpter() {
            mWH[0] = getResources().getDisplayMetrics().widthPixels;
            mWH[1] = getResources().getDisplayMetrics().heightPixels;
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

        /**
         * 回收除当前，上一张，下一张以外的图片
         *
         * @param position 图片位置s
         */
        private void recycleBitmap(int position) {
            String uri = mUris.get(position);
            Bitmap bitmap = mLruCache.get(uri);
            if (bitmap != null) {
                bitmap.recycle();
                mLruCache.remove(uri);
                bitmap = null;
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            RecycleImageView pictureView = new RecycleImageView(VPActivity.this);
            pictureView.setLayoutParams(new ViewPager.LayoutParams());
            container.addView(pictureView, 0);
            new BitmapAsyncTask(pictureView, mUris.get(position), mWH).execute(mLruCache);
            return pictureView;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

    }

}
