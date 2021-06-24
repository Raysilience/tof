package com.example.tof;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

/**
 * @Author RUI
 * @Date 2021/4/23
 */
public class DepthView extends View {
    private static final String TAG = DepthView.class.getName();
    private final String mColor = "#42ed45";
    private Paint mBitPaint;
    private Paint mCrossPaint;
    private Paint mTextPaint;

    public Bitmap mBitmap;
    private int mWidth;
    private int mHeight;
    private Rect mSrc;
    private Rect mDst;

    private int mDistance;
    private int[] mDistArray = new int[9];
    private Point mPoint;
    private Point[] mPointArray;
    private int x = 320;
    private int y = 200;
    private int offset = 120;
    private int fpsCount;
    private long oldTime;
    private boolean isD2Copen = false;

    public DepthView(Context context) {
        super(context);
        init(context);
    }

    public DepthView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DepthView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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


        mPoint = new Point(x, y);
        mPointArray = new Point[]{new Point(x, y),
                new Point(x - offset, y),
                new Point(x + offset, y),
                new Point(x, y - offset),
                new Point(x, y + offset),
                new Point(x - offset, y - offset),
                new Point(x + offset, y + offset),
                new Point(x + offset, y - offset),
                new Point(x - offset, y + offset)};
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap != null){
            statisticsFPS(System.currentTimeMillis());

            canvas.drawBitmap(mBitmap, mSrc, mDst, mBitPaint);

            if (!isD2Copen){
                drawCrossArray(canvas, mPointArray, mCrossPaint);
                drawTextArray(canvas, mTextPaint);
            }
        }
    }

    private void drawCrossArray(Canvas canvas, Point[] arr, Paint paint){
        for (Point p : mPointArray){
            drawCross(canvas, scaleX(p.x), scaleY(p.y), paint);
        }
    }

    private void drawCross(Canvas canvas, int x, int y, Paint paint){
        canvas.drawLine(x - 10, y, x + 10, y, paint);
        canvas.drawLine(x, y - 10, x, y + 10, paint);
    }

    private void drawTextArray(Canvas canvas,Paint paint){
        for (int i = 0; i < mPointArray.length; i++){
            canvas.drawText("z: " + (mDistArray[i]/1000.0f) + "m", scaleX(mPointArray[i].x-20), scaleY(mPointArray[i].y+20), paint);
        }
    }


    public void setDepthSize(int width, int height){
        mWidth = width;
        mHeight = height;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);

        mSrc = new Rect(0, 0, mWidth, mHeight);
        mDst = new Rect(-10, 0, 960, 720);
    }

    public void setDistance(ByteBuffer src16){
        int distance = src16.get(2*mWidth*mPoint.y + 2*mPoint.x) & 0xFF;
        distance += (src16.get(2*mWidth*mPoint.y + 2*mPoint.x +1) & 0xFF) << 8;
        this.mDistance = distance & 0xFFFF;
    }

    public void setDistanceArray(ByteBuffer src16){
        for (int i = 0; i < mPointArray.length; i++){
            int distance = src16.get(2*mWidth*mPointArray[i].y + 2*mPointArray[i].x) & 0xFF;
            distance += (src16.get(2*mWidth*mPointArray[i].y + 2*mPointArray[i].x +1) & 0xFF) << 8;
            mDistArray[i] = distance & 0xFFFF;
        }
    }

    private int scaleX(int x){
        return x*(mDst.right - mDst.left)/mWidth+ mDst.left;
    }

    private int scaleY(int y){
        return y*(mDst.bottom - mDst.top)/mHeight + mDst.top;
    }

    public void setD2Copen(boolean d2Copen) {
        isD2Copen = d2Copen;
    }

    /**
     * 统计帧数
     * @param newTime
     */
    private void statisticsFPS(long newTime) {
        fpsCount++;
        long timeValue = newTime - oldTime;
        if (timeValue >= 1000) {
            Log.d("======Fps=======: ", ""+fpsCount);
            fpsCount = 0;
            oldTime = newTime;
        }
    }
}
