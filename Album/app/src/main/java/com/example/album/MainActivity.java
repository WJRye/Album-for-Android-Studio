package com.example.album;


import android.animation.LayoutTransition;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.example.album.adapter.MainActivityAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Administrator
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TAG";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    //手机是否支持相机
    private boolean mCameraSupported = false;
    //自定义的存储图片的路径，因为系统存储图片的路径在DCIM中，所以这里也存储在DCIM中
    private final String ROOT_PATH = "/mnt/sdcard/DCIM/Album";
    //缓存图片的文件
    private File mCacheFile;
    //当前拍照存储的图片的路径
    private String mCurrentUri;

    //所有图片的路径
    private ArrayList<String> mUris = new ArrayList<>();
    private GridView mGridView;
    private MainActivityAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraSupported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        mGridView = (GridView) findViewById(R.id.album_gv);
        mGridView.setSelector(new ColorDrawable());// 去掉gridview默认的选择颜色
        // Images.Media.DATE_ADDED(date_added):The time the file was added to the media provider
        mUris.addAll(getUris(Images.Media.EXTERNAL_CONTENT_URI, "date_added desc"));
        mAdapter = new MainActivityAdapter(this, mGridView, mUris);
        mGridView.setAdapter(mAdapter);
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(LayoutTransition.CHANGE_DISAPPEARING, 500);
        mGridView.setLayoutTransition(transition);

        registerListener();

    }

    private void registerListener() {
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, VPActivity.class);
                intent.putExtra(VPActivity.POSITION, position);
                intent.putStringArrayListExtra(VPActivity.URIS, mUris);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(view, (int) view.getX(), (int) view.getY(), 0, 0);
                ActivityCompat.startActivity(MainActivity.this, intent, options.toBundle());
            }
        });

        mGridView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            private MenuItem selectedItem;
            //选中图片的路径
            private ArrayList<String> checkedUris = new ArrayList<>();
            //选中图片的位置
            private List<Integer> checkedPositions = new ArrayList<>(mUris.size());

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                Log.d(TAG, "position=" + position + ";id=" + id + ";checked=" + checked);

                if (checked) {
                    checkedUris.add(mUris.get(position));
                    checkedPositions.add(Integer.valueOf(position));
                    mGridView.getChildAt(position - mGridView.getFirstVisiblePosition()).setAlpha(0.5f);
                } else {
                    checkedUris.remove(mUris.get(position));
                    checkedPositions.remove(Integer.valueOf(position));
                    mGridView.getChildAt(position - mGridView.getFirstVisiblePosition()).setAlpha(1f);
                }

                setMenuItemSelectedTitle();
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                getMenuInflater().inflate(R.menu.menu_action, menu);
                selectedItem = menu.findItem(R.id.action_selected);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:
                        if (!checkedPositions.isEmpty()) {
                            SparseBooleanArray checkedArray = mGridView.getCheckedItemPositions();

                            for (int checkedPosition : checkedPositions) {
                                int row = getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", new String[]{mUris.get(checkedPosition)});
                                if (row == 1) {
                                    checkedArray.put(checkedPosition, false);
                                }
                            }
                            mUris.removeAll(checkedUris);
                            mAdapter.notifyDataSetChanged();
                        }
                        mode.finish();
                        break;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                //清楚选择的条目
                checkedPositions.clear();
                checkedUris.clear();
            }

            private void setMenuItemSelectedTitle() {
                selectedItem.setTitle(getString(R.string.action_selected, mGridView.getCheckedItemCount()));
            }
        });

    }

    /**
     * 获得手机里所有的图片的路径
     */
    private ArrayList<String> getUris(Uri uri, String sortOrder) {
        ArrayList<String> uris = new ArrayList<>();
        Cursor cursor = getContentResolver().query(uri, null, null, null, sortOrder);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    uris.add(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)));
                }
            }
            cursor.close();
        }
        return uris;
    }

    @Override
    protected void onStop() {
        scanFile();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mAdapter.clear();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_camera:
                if (mCameraSupported) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        mCurrentUri = getPath();
                        if (mCurrentUri != null) {
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mCurrentUri)));
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        } else {
                            Toast.makeText(this, getString(R.string.sdcard_error), Toast.LENGTH_SHORT);
                        }
                    }
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            if (mCurrentUri != null) {
                mGridView.smoothScrollToPositionFromTop(0, 0, 1);
                mUris.add(0, mCurrentUri);
                mAdapter.notifyDataSetChanged();
            }

        }

    }


    /*
    * Request the media scanner to scan a file and add it to the media database.
     */
    private void scanFile() {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(new File(ROOT_PATH)));
        sendBroadcast(intent);
    }

    /*
    * 获得存储图片的路径
    * */
    private String getPath() {
        String path = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (mCacheFile == null) {
                mCacheFile = new File(ROOT_PATH);
                if (!mCacheFile.exists()) {
                    mCacheFile.mkdirs();
                }
            }
            path = mCacheFile.getPath() + File.separator + System.currentTimeMillis() + ".jpg";
        }
        return path;
    }
}
