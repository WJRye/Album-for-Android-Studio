package com.wj.album;/**
 * Created by wangjiang on 2016/4/9.
 */

import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * User: WangJiang(https://github.com/WJRye)
 * Date: 2016-04-09
 * Time: 21:59
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(getLayoutResID());
        initViews();
    }

    /**
     * 初始化View
     */
    protected abstract void initViews();

    /**
     * @return 资源Layout Resource ID
     */
    public abstract int getLayoutResID();
}
