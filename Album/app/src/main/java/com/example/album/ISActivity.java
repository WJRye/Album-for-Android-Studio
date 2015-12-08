package com.example.album;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ViewSwitcher.ViewFactory;

import com.example.album.utils.BitmapUtil;
import com.example.album.views.MyImageView;

public class ISActivity extends Activity implements ViewFactory,
		OnTouchListener {

	private static final int ID = 0x123;
	private float startX = 0.0f;
	private float endX = 0.0f;
	private int index = 0;
	private boolean isMove = false;
	private Animation inAnimation;
	private Animation outAnimation;
	private Bitmap cacheBitmap;
	private ArrayList<String> uris;
	private ImageSwitcher switcher;
	private ImageSwitcher.LayoutParams params;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_imageswithcer);

		index = getIntent().getIntExtra(VPActivity.POSITION, 0);
		uris = getIntent().getStringArrayListExtra(VPActivity.URIS);
		switcher = (ImageSwitcher) findViewById(R.id.imageswither);
		params = new ImageSwitcher.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
		switcher.setFactory(this);
		switcher.setOnTouchListener(this);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public View makeView() {
		MyImageView picture = new MyImageView(this);
		picture.setId(ID);
		cacheBitmap = BitmapUtil.compress(this, uris.get(index), getResources()
				.getDisplayMetrics().widthPixels, getResources()
				.getDisplayMetrics().heightPixels);
		picture.setScaleType(ScaleType.CENTER);
		picture.setLayoutParams(params);
		picture.setImageBitmap(cacheBitmap);
		return picture;
	}

	@SuppressLint({ "NewApi", "ClickableViewAccessibility" })
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			startX = event.getX();
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (isMove) {
				endX = event.getX();
				// 判断向左滑动
				if (startX - endX > 50) {
					index = (index + 1 < uris.size()) ? ++index : 0;
					inAnimation = new TranslateAnimation(-100f, 0f, 0f, 0f);
					outAnimation = new TranslateAnimation(0f, -100f, 0f, 0f);
				// 判断向右滑动
				} else if (endX - startX > 50) {
					index = index - 1 > 0 ? --index : uris.size() - 1;
					inAnimation = new TranslateAnimation(100f, 0f, 0f, 0f);
					outAnimation = new TranslateAnimation(0f, 100f, 0f, 0f);
				}
				recycleCacheBitmap();
				Bitmap bitmap = BitmapUtil.compress(this, uris.get(index),
						getResources().getDisplayMetrics().widthPixels,
						getResources().getDisplayMetrics().heightPixels);
				ImageView picture = (ImageView) switcher.findViewById(ID);
				picture.setImageBitmap(bitmap);
				switcher.setInAnimation(inAnimation);
				switcher.setOutAnimation(outAnimation);
				inAnimation.startNow();
				outAnimation.startNow();
				cacheBitmap = bitmap;
			}
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			isMove = true;
		}
		return true;
	}

	// 回收掉缓存的Bitmap
	private void recycleCacheBitmap() {
		if (cacheBitmap != null && !cacheBitmap.isRecycled()) {
			cacheBitmap.recycle();
			cacheBitmap = null;
		}

	}

}
