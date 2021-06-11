package com.example.tof;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * @Author RUI
 * @Date 2021/4/23
 */
public class MyView extends View {
    private static final String TAG = MyView.class.getName();
    private final String mColor = "#42ed45";
    private Paint mBitPaint;
    private Paint mCrossPaint;
    private Paint mTextPaint;
    public Bitmap mBitmap;
    private int mWidth;
    private int mHeight;
    private int mDistance;

    private ImageProcessor mImageProc;

    private Rect mSrc;
    private Rect mDst;

    private int[] mPixel;

    public MyView(Context context) {
        super(context);
        init(context);
    }

    public MyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mCrossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCrossPaint.setColor(Color.WHITE);
        mCrossPaint.setStyle(Paint.Style.STROKE);
        mCrossPaint.setStrokeWidth(3f);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setStrokeWidth(2f);
        mTextPaint.setTextSize(25);

        mBitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBitPaint.setFilterBitmap(true);
        mBitPaint.setDither(true);

        mPixel = new int[1];
        mDst = new Rect(0, 50, 960, 770);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d("RUi", "onDraw");
        if (mBitmap != null){


            canvas.drawBitmap(mBitmap, mSrc, mDst, mBitPaint);

            drawCross(canvas, (mDst.right - mDst.left)/2, (mDst.bottom - mDst.top)/2, mCrossPaint);

            canvas.drawText("z: " + (this.mDistance/1000.0f) + "m", mDst.right - 150, mDst.bottom-80, mTextPaint);
        }
    }

    private void drawCross(Canvas canvas, int x, int y, Paint paint){
        canvas.drawLine(x - 10, y, x + 10, y, paint);
        canvas.drawLine(x, y - 10, x, y + 10, paint);
    }

    public void setDepthSize(int width, int height){
        mWidth = width;
        mHeight = height;

        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
//        Log.e("+++++++Rui++++++", mBitmap.getByteCount() + "");
        mSrc = new Rect(0, 0, mWidth, mHeight);
    }

    public void setDistance(int val){
        this.mDistance = val;
    }

}
