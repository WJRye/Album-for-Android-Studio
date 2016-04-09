package com.wj.album.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.GridView;

import com.wj.album.R;
import com.wj.album.asynctask.BitmapAsyncTask;
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

    private boolean mIsIdle = true;
    //每一个Item的宽高
    private int[] mWH;
    //是否处于选择图片状态
    private boolean mIsSelect = false;
    //图片路径
    private ArrayList<String> mUris;
    //缓存图片
    private LruCache<String, Bitmap> mLruCache;
    //加载图片
    private Set<BitmapAsyncTask> mBitmapAsyncTasks;
    //选中的图片
    private Map<Integer, String> mSelectedImages = new HashMap<>();

    public MainActivityAdapter(Context context, GridView gridView, ArrayList<String> uris) {
        mUris = uris;
        mWH = getWidthAndHeight(context);
        setOnScrollListener(gridView);
        mLruCache = new LruCache<String, Bitmap>((int) Runtime.getRuntime().maxMemory() / 8) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        mBitmapAsyncTasks = new HashSet<>(uris.size());
    }

    private void setOnScrollListener(GridView gridView) {
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int count = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    mIsIdle = true;
                    count = view.getChildCount();
                    //加载当前页面显示的图片
                    for (int i = 0; i < count; i++) {
                        if (!mIsIdle) break;
                        ViewHolder viewHolder = (ViewHolder) view.getChildAt(i).getTag();
                        loadImage(viewHolder);
                    }
//                    // 清空除当前页面图片占用的内存
//                    recycleBitmaps(0, view.getFirstVisiblePosition());
//                    recycleBitmaps(view.getFirstVisiblePosition() + count, view.getCount());
                } else {
                    if (mIsIdle) {
                        mIsIdle = false;
                        BitmapAsyncTask[] bats = mBitmapAsyncTasks.toArray(new BitmapAsyncTask[mBitmapAsyncTasks.size()]);
                        for (BitmapAsyncTask bat : bats) {
                            if (bat != null && bat.getStatus() != AsyncTask.Status.FINISHED) {
                                bat.cancel(true);
                                mBitmapAsyncTasks.remove(bat);
                                bat = null;
                            }
                        }
                    }
                }

            }

            /**
             * 回收图片
             *
             * @param index 起始小标
             * @param count 总共数量
             */
            private void recycleBitmaps(int index, int count) {
                for (int i = index; i < count; i++) {
                    String uri = mUris.get(i);
                    Bitmap bitmap = mLruCache.get(uri);
                    if (bitmap != null && !bitmap.isRecycled()) {
                        mLruCache.remove(uri);
                        bitmap.recycle();
                        bitmap = null;
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
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
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.gridview_item, null, false);
            viewHolder = new ViewHolder();
            viewHolder.picture = (RecycleImageView) convertView.findViewById(R.id.gv_image);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) viewHolder.picture.getLayoutParams();
            params.width = mWH[0];
            params.height = mWH[1];
            viewHolder.picture.setLayoutParams(params);
            viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.gv_checkbox);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.position = position;
        loadImage(viewHolder);
        isImageSelected(position, viewHolder);
        setOnCheckedChangeListener(viewHolder);
        return convertView;
    }

    private void isImageSelected(int position, ViewHolder viewHolder) {
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
    }

    private void setOnCheckedChangeListener(final ViewHolder viewHolder) {
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
    }

    /**
     * 加载图片
     *
     * @param viewHolder
     */
    private void loadImage(ViewHolder viewHolder) {
        String uri = mUris.get(viewHolder.position);
        Bitmap bitmap = mLruCache.get(uri);
        if (bitmap != null) {
            viewHolder.picture.setImageBitmap(bitmap);
        } else {
            viewHolder.picture.setImageResource(R.drawable.image_default_bg);
            if (mIsIdle) {
                BitmapAsyncTask bat = new BitmapAsyncTask(viewHolder.picture, uri, mWH);
                mBitmapAsyncTasks.add(bat);
                bat.execute(mLruCache);
            }
        }

    }

    private static final class ViewHolder {
        private int position;
        private RecycleImageView picture;
        private CheckBox checkBox;
    }

    /**
     * 根据屏幕的大小、gridview的horizontalSpacing和padding获取图片的宽高
     *
     * @return 图片的宽高值
     */
    private int[] getWidthAndHeight(Context context) {
        Resources resources = context.getResources();
        int hs = resources.getDimensionPixelSize(R.dimen.gv_horizontalSpacing);
        int padding = resources.getDimensionPixelSize(R.dimen.gv_padding);
        int screenWidth = resources.getDisplayMetrics().widthPixels;
        int col = 3;
        int size = (screenWidth - 2 * hs - 2 * padding) / col;
        return new int[]{size, size};
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


    /**
     * 添加选中的图片，如果已经添加，则删除
     */
    public void setSelectedImage(AbsListView absListView, int position, boolean isChecked) {
        View view = absListView.getChildAt(position - absListView.getFirstVisiblePosition());
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
