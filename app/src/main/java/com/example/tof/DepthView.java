package com.example.tof;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

/**
 * @Author RUI
 * @Date 2021/4/23
 */
public class DepthView extends View {
    private static final String TAG = DepthView.class.getName();
    private Paint mBitPaint;
    private Paint mCrossPaint;
    private Paint mTextPaint;

    public Bitmap mBitmap;
    private Bitmap mLegendGray;
    private Bitmap mLegendColor;
    private ByteBuffer src8;
    private ByteBuffer dst16;

    private int mWidth;
    private int mHeight;
    private Rect mDst;
    private Rect mLegendDst;

    private int mDistance;
    private int[] mDistArray = new int[9];
    private Point mPoint;
    private Point[] mPointArray;
    private int x = 320;
    private int y = 200;
    private int interval = 120;

    private int fpsCount;
    private long oldTime;

    private boolean isD2Copen = false;
    private ImageProcessor mImageProcessor;


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
                new Point(x - interval, y),
                new Point(x + interval, y),
                new Point(x, y - interval),
                new Point(x, y + interval),
                new Point(x - interval, y - interval),
                new Point(x + interval, y + interval),
                new Point(x + interval, y - interval),
                new Point(x - interval, y + interval)};

        mImageProcessor = ImageProcessor.getInstance(context);
        mLegendGray = getBitmap(context, R.mipmap.gray);
        mLegendColor = Bitmap.createBitmap(mLegendGray.getWidth(), mLegendGray.getHeight(), Bitmap.Config.RGB_565);
        dst16 = ByteBuffer.allocate(mLegendColor.getByteCount());
        src8 = ByteBuffer.allocate(mLegendGray.getByteCount());

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap != null){
            statisticsFPS(System.currentTimeMillis());

            canvas.drawBitmap(mBitmap, null, mDst, mBitPaint);

            if (!isD2Copen){
                drawCrossArray(canvas, mPointArray, mCrossPaint);
                drawTextArray(canvas, mTextPaint);
                drawLegend(canvas, mBitPaint, mTextPaint);
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

    private void drawLegend(Canvas canvas, Paint bitmapPaint, Paint textPaint){
        canvas.drawBitmap(mLegendColor, null, mLegendDst, bitmapPaint);

        canvas.drawText("MIN: " + mImageProcessor.getRange_min()/1000.0f + "m", mLegendDst.left, mLegendDst.bottom-50, textPaint);
        canvas.drawText("MAX: " + mImageProcessor.getRange_max()/1000.0f + "m", mLegendDst.right, mLegendDst.bottom-50, textPaint);

    }

    public void setDepthSize(int width, int height){
        mWidth = width;
        mHeight = height;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);

        mDst = new Rect(-10, 0, 960, 730);
        mLegendDst = new Rect(140, 700, 140+650, 30+700);
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

    private Bitmap getBitmap(Context context, int resId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        TypedValue value = new TypedValue();
        context.getResources().openRawResource(resId, value);
        options.inTargetDensity = value.density;
        options.inScaled = false;//不缩放
        options.inPreferredConfig = Bitmap.Config.ALPHA_8;
        return BitmapFactory.decodeResource(context.getResources(), resId, options);
    }

    public void setLegend(){
        mLegendGray.copyPixelsToBuffer(src8);
        mImageProcessor.convertLegend(src8, dst16);
        mLegendColor.copyPixelsFromBuffer(dst16);
    }
}
