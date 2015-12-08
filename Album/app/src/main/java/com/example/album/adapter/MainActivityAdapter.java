package com.example.album.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.GridView;

import com.example.album.R;
import com.example.album.views.MyImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivityAdapter extends BaseAdapter {

    private static final String TAG = "TAG";

    private boolean mIsScrolling = false;
    private boolean mScrollStateChanged = false;
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    //每一个Item的宽高
    private int[] mWH;
    private Context mContext;
    private ArrayList<String> mUris;
    private LayoutInflater mInflater;
    private GridView mGridView;
    //缓存图片
    private LruCache<String, Bitmap> mLruCache;
    //加载图片
    private Set<BitmapAsyncTask> mBitmapAsyncTasks;

    public MainActivityAdapter(Context context, GridView gridView, ArrayList<String> uris) {
        mUris = uris;
        mContext = context;
        mWH = getWidthAndHeight();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mGridView = gridView;
        mGridView.setOnScrollListener(new ScrollListener());
        mLruCache = new LruCache<String, Bitmap>((int) Runtime.getRuntime().maxMemory() / 8) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        mBitmapAsyncTasks = new HashSet<>(uris.size());
    }

    @Override
    public int getCount() {
        return mUris.size();
    }

    @Override
    public Object getItem(int position) {
        return mUris.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView");
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.gridview_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.picture = (MyImageView) convertView.findViewById(R.id.gv_image);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        SparseBooleanArray array = mGridView.getCheckedItemPositions();
        if (array != null) {
            if (array.get(position, false)) {
                convertView.setAlpha(0.5f);
            } else {
                convertView.setAlpha(1f);
            }
        }


        // 刚进入页面的时候不会调用接口OnScrollListener中的方法，需要主动去加载图片，以及当删除完图片时，新出现的图片需要加载
        Bitmap bitmap = mLruCache.get(mUris.get(position));
        if (!mScrollStateChanged || (mScrollState == OnScrollListener.SCROLL_STATE_IDLE)) {
            if (bitmap != null) {
                viewHolder.picture.setImageBitmap(bitmap);
            } else {
                BitmapAsyncTask bat = new BitmapAsyncTask(mContext, viewHolder.picture, mUris.get(position), mWH);
                mBitmapAsyncTasks.add(bat);
                bat.execute(mLruCache);
            }
        } else {
            if (bitmap != null) {
                viewHolder.picture.setImageBitmap(bitmap);
            } else {
                viewHolder.picture.setImageResource(R.drawable.image_default_bg);
            }
        }

        return convertView;
    }

    private static final class ViewHolder {
        private MyImageView picture;
    }

    /**
     * 根据屏幕的大小、gridview的horizontalSpacing和padding获取图片的宽高
     *
     * @return 图片的宽高值
     */
    private int[] getWidthAndHeight() {
        Resources resources = mContext.getResources();
        int hs = resources.getDimensionPixelSize(R.dimen.gv_horizontalSpacing);
        int padding = resources.getDimensionPixelSize(R.dimen.gv_padding);
        int screenWidth = resources.getDisplayMetrics().widthPixels;

        int wh = (screenWidth - 2 * hs - 2 * padding) / 3;

        return new int[]{wh, wh};
    }


    /**
     * 退出Activity时调用，清除引用，清除占用的内存
     */
    public void clear() {
        if (mLruCache != null) {
            mLruCache.evictAll();
            mLruCache = null;
        }
        if (mBitmapAsyncTasks != null) {
            mBitmapAsyncTasks.clear();
            mBitmapAsyncTasks = null;
        }
    }

    private class ScrollListener implements OnScrollListener {
        private int mFirstVisibleItem;
        private int mVisibleItemCount;
        private int mTotalItemCount;

        @SuppressWarnings("unchecked")
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
//            Log.d(TAG, "onScrollStateChanged");
            mScrollState = scrollState;
            if (!mScrollStateChanged) mScrollStateChanged = true;
            switch (scrollState) {
                case OnScrollListener.SCROLL_STATE_IDLE:
                    mIsScrolling = false;
                    // 取消未加载完的任务
                    for (BitmapAsyncTask bat : mBitmapAsyncTasks) {
                        if (bat.getStatus() != AsyncTask.Status.FINISHED) {
                            bat.cancel(true);
                            bat = null;
                        }
                    }
                    //加载当前页面显示的图片
                    for (int i = 0; i < mVisibleItemCount; i++) {
                        if (!mIsScrolling) {
                            String uri = mUris.get(mFirstVisibleItem + i);
                            ViewHolder viewHolder = (ViewHolder) mGridView.getChildAt(i).getTag();
                            if (mLruCache.get(uri) == null) {
                                BitmapAsyncTask bitmapAsyncTask = new BitmapAsyncTask(mContext, viewHolder.picture, uri, mWH);
                                mBitmapAsyncTasks.add(bitmapAsyncTask);
                                bitmapAsyncTask.execute(mLruCache);
                            } else {
                                viewHolder.picture.setImageBitmap(mLruCache.get(uri));
                            }
                        } else {
                            //已经开始滑动，取消加载图片
                            break;
                        }
                    }
//                    // 清空除当前页面图片占用的内存
//                    for (int j = 0; j < mFirstVisibleItem; j++) {
//                        String uri = mUris.get(j);
//                        Bitmap bitmap = mLruCache.get(uri);
//                        if (bitmap != null && !bitmap.isRecycled()) {
//                            bitmap.recycle();
//                            bitmap = null;
//                            mLruCache.remove(uri);
//                        }
//                    }
//                    int invisibleAfter = mFirstVisibleItem + mVisibleItemCount;
//                    for (int k = invisibleAfter; k < mTotalItemCount; k++) {
//                        String uri = mUris.get(k);
//                        Bitmap bitmap = mLruCache.get(uri);
//                        if (bitmap != null && !bitmap.isRecycled()) {
//                            bitmap.recycle();
//                            bitmap = null;
//                            mLruCache.remove(uri);
//                        }
//                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//            Log.d(TAG, "onScroll");
            mFirstVisibleItem = firstVisibleItem;
            mVisibleItemCount = visibleItemCount;
            mTotalItemCount = totalItemCount;
            if (mScrollStateChanged && !mIsScrolling) {
                mIsScrolling = true;
            }
        }

    }

}
