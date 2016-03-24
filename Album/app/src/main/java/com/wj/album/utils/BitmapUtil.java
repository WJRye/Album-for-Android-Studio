package com.wj.album.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * 处理Bitmap的工具类
 */
public final class BitmapUtil {

    private BitmapUtil() {
    }

    /**
     * 压缩图片
     */
    @SuppressLint("NewApi")
    public static final Bitmap compress(Context context, String uri, int dstWidth, int dstHeight) {
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new BufferedInputStream(new FileInputStream(uri)), null, opts);
            int height = opts.outHeight;
            int width = opts.outWidth;
            int inSampleSize = 1;

            int degree = readPictureDegree(uri);
            if (degree == 0 || degree == 180) {
                if (width > height && width > dstWidth) {
                    inSampleSize = Math.round((float) width / (float) dstWidth);
                } else if (height > width && height > dstHeight) {
                    inSampleSize = Math.round((float) height / (float) dstHeight);
                }
            } else if (degree == 90 || degree == 270) {
                // 图片有旋转时，宽和高调换了
                if (width > height && width > dstHeight) {
                    inSampleSize = Math.round((float) width / (float) dstHeight);
                } else if (height > width && height > dstWidth) {
                    inSampleSize = Math.round((float) height / (float) dstWidth);
                }
            }

            if (inSampleSize <= 1) inSampleSize = 1;
            opts.inSampleSize = inSampleSize;
            opts.inPreferredConfig = Config.RGB_565;
            opts.inPurgeable = true;
            opts.inInputShareable = true;
            opts.inTargetDensity = context.getResources().getDisplayMetrics().densityDpi;
            opts.inScaled = true;
            opts.inTempStorage = new byte[16 * 1024];
            opts.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeStream(new BufferedInputStream(new FileInputStream(uri)), null, opts);
            if (bitmap != null) {
                // 处理旋转了一定角度的图片，比如有些机型拍出的照片默认旋转了90度的
                Matrix matrix = new Matrix();
                matrix.postRotate(degree);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            // 防止内存溢出导致程序崩溃而强制退出
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * @param path 图片路径
     * @return 图片旋转的度数
     */
    private static final int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }
}
