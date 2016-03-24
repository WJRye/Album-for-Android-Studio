package com.wj.album.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;

import com.wj.album.R;
import com.wj.album.views.RecycleImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class MainActivityAdapter extends BaseAdapter {

    private static final String TAG = "TAG";
    //是否在滑动
    private boolean mIsScrolling = false;
    //listview滑动状态是否改变
    private boolean mScrollStateChanged = false;
    //默认listview的滑动状态
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    //每一个Item的宽高
    private int[] mWH;
    //是否处于选择图片状态
    private boolean mIsSelect = false;
    private Context mContext;
    //图片路径
    private ArrayList<String> mUris;
    private LayoutInflater mInflater;
    private GridView mGridView;
    //缓存图片
    private LruCache<String, Bitmap> mLruCache;
    //加载图片
    private Set<BitmapAsyncTask> mBitmapAsyncTasks;
    //选中的图片
    private Map<Integer, String> mSelectedImages = new HashMap<>();

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
        Log.d(TAG, "getView-->position=" + position);
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.gridview_item, null, false);
            viewHolder = new ViewHolder();
            viewHolder.picture = (RecycleImageView) convertView.findViewById(R.id.gv_image);
            viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.gv_checkbox);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (mIsSelect) {
            viewHolder.checkBox.setVisibility(View.VISIBLE);
            if (mSelectedImages.containsKey(position)) {
                viewHolder.picture.setAlpha(0.5f);
                viewHolder.checkBox.setChecked(true);
            } else {
                viewHolder.picture.setAlpha(1f);
                viewHolder.checkBox.setChecked(false);
            }
        } else {
            viewHolder.picture.setAlpha(1f);
            viewHolder.checkBox.setVisibility(View.GONE);
        }
        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    viewHolder.picture.setAlpha(0.5f);
                    viewHolder.checkBox.setChecked(true);
                } else {
                    viewHolder.picture.setAlpha(1f);
                    viewHolder.checkBox.setChecked(false);
                }
            }
        });

        // 刚进入页面的时候不会调用接口OnScrollListener中的方法，需要主动去加载图片，以及当删除完图片时，新出现的图片需要加载
        String uri = mUris.get(position);
        Bitmap bitmap = mLruCache.get(uri);
        //防止图片错位显示
        viewHolder.picture.setTag(uri);
        if (!mScrollStateChanged || (mScrollState == OnScrollListener.SCROLL_STATE_IDLE)) {
            if (bitmap != null) {
                viewHolder.picture.setImageBitmap(bitmap);
            } else {
                BitmapAsyncTask bat = new BitmapAsyncTask(mContext, viewHolder.picture, uri, mWH);
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
        private RecycleImageView picture;
        private CheckBox checkBox;
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
            cancelUnfinishTasks();
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
                case OnScrollListener.SCROLL_STATE_IDLE: {
                    mIsScrolling = false;

                    cancelUnfinishTasks();
                    //加载当前页面显示的图片
                    for (int i = 0; i < mVisibleItemCount; i++) {
                        if (!mIsScrolling) {
                            String uri = mUris.get(mFirstVisibleItem + i);
                            ViewHolder viewHolder = (ViewHolder) mGridView.getChildAt(i).getTag();
                            viewHolder.picture.setTag(uri);
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
//                            mLruCache.remove(uri);
//                            bitmap.recycle();
//                            bitmap = null;
//                        }
//                    }
//                    int invisibleAfter = mFirstVisibleItem + mVisibleItemCount;
//                    for (int k = invisibleAfter; k < mTotalItemCount; k++) {
//                        String uri = mUris.get(k);
//                        Bitmap bitmap = mLruCache.get(uri);
//                        if (bitmap != null && !bitmap.isRecycled()) {
//                            mLruCache.remove(uri);
//                            bitmap.recycle();
//                            bitmap = null;
//                        }
//                    }
                }
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

    /**
     * 取消未加载的任务
     */
    private void cancelUnfinishTasks() {
        for (BitmapAsyncTask bat : mBitmapAsyncTasks) {
            if (bat.getStatus() != AsyncTask.Status.FINISHED) {
                bat.cancel(true);
                bat = null;
            }
        }
    }

    /**
     * 添加选中的图片，如果已经添加，则删除
     */
    public void setSelectedImage(int position, boolean isChecked) {
        View view = mGridView.getChildAt(position - mGridView.getFirstVisiblePosition());
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (isChecked) {
            mSelectedImages.put(position, mUris.get(position));
            viewHolder.picture.setAlpha(0.5f);
            viewHolder.checkBox.setChecked(true);
        } else {
            mSelectedImages.remove(position);
            viewHolder.picture.setAlpha(1f);
            viewHolder.checkBox.setChecked(false);
        }

    }

    /**
     * 删除选中的图片
     */
    public void removeSelectedImages() {
        String[] imageUris = mSelectedImages.values().toArray(new String[mSelectedImages.size()]);
        for (String uri : imageUris) {
            mUris.remove(uri);
        }
        mIsSelect = false;
        notifyDataSetChanged();
    }

    /**
     * 清除选中的图片
     */
    public void clearSelectedImages() {
        mSelectedImages.clear();
    }

    public void setIsSelect(boolean isSelect) {
        mIsSelect = isSelect;
        notifyDataSetChanged();
    }
}
