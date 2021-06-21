package com.example.tof;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
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
    private Point mPoint;

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

        mPoint = new Point(320, 240);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap != null){
            canvas.drawBitmap(mBitmap, mSrc, mDst, mBitPaint);

            drawCross(canvas, mPoint.x*(mDst.right - mDst.left)/mWidth + mDst.left, mPoint.y*(mDst.bottom - mDst.top)/mHeight + mDst.top, mCrossPaint);

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

        mSrc = new Rect(0, 0, mWidth, mHeight);
        mDst = new Rect(0, 50, 960, 770);
    }

    public void setDistance(ByteBuffer src16){
        int distance = src16.get(2*mWidth*mPoint.y + 2*mPoint.x) & 0xFF;
        distance += (src16.get(2*mWidth*mPoint.y + 2*mPoint.x +1) & 0xFF) << 8;
        this.mDistance = distance & 0xFFFF;
    }

    public Point getmPoint() {
        return mPoint;
    }

    public void setmPoint(Point mPoint) {
        this.mPoint = mPoint;
    }
}
