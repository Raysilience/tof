package com.example.tof;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.strictmode.SqliteObjectLeakedViolation;
import android.util.Log;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ImageProcessor {
    enum COLORMAP{
        JET, PLASMA, VIRIDIS, SMOOTH_COOL_WARM, RAINBOW
    }
    private static final String TAG = ImageProcessor.class.getName();
    private int[] LUT = new int[256];
    private float[] histogram = new float[65536];
    private boolean isLUTSet = false;
    private DatabaseHelper dbHelper;
    private static ImageProcessor instance;
    private int range_min = 100;
    private int range_max = 4096;

    public static synchronized ImageProcessor getInstance(Context context){
        if (instance == null){
            instance = new ImageProcessor(context);
        }
        return instance;
    }

    private ImageProcessor(Context context){
        dbHelper = DatabaseHelper.getInstance(context);
    }

    public void setLUT(COLORMAP type) {
        dbHelper.update(type);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + type.toString(), null);
        int r, g, b, i = 0;
        while (cursor.moveToNext() && i < 256){
            r = cursor.getInt(1);
            g = cursor.getInt(2);
            b = cursor.getInt(3);
            this.LUT[i++] = getRGB565(r, g, b);
        }
        cursor.close();
        db.close();

        this.LUT[0] = 0;
        this.isLUTSet = true;
    }

    private int getRGB565(int r, int g, int b){
        int val = (r >> 3) << 11;
        val |= (g >> 2) << 5;
        val |= (b >> 3);
        return val & 0xFFFF;
    }

    /**
     * max-min normalize val and map it to 0~255 then apply colormap
     * @param val: 16bit
     * @param range_max: maximum depth value
     * @param range_min: minimum depth value
     * @return color in the form of RGB565
     */
    private int scaleNApplyLUT(int val, int range_max, int range_min){
        if (!isLUTSet) return 0;
        if (val >= range_min && val <= range_max){
            val = (int)((float)(val - range_min)*255/(range_max - range_min));
        } else {
            val = 0;
        }
        return LUT[val & 0xFF];
    }


    /**
     * apply histogram mapping then colormap
     * @param val: 16-bit value of the current depth
     * @param LUT: lookup table
     * @return color in the form of RGB565
     */
    private int histNApplyLUT(int val, int[] LUT){
        val = 255 - (int)(this.histogram[val & 0xFFFF] * 255);
        return LUT[val & 0xFF];
    }

    private void calcEqualizedHist(ByteBuffer src) {
        src.rewind();
        int totalPoints = 0;

        while (true) {
            try {
                int val = (src.get() & 0xFF);
                val += (src.get() & 0xFF) << 8;
                if (val > 0) {
                    totalPoints++;
                    this.histogram[val]++;
                }
            } catch (BufferUnderflowException ignored) {
                break;
            }
        }
        this.histogram[0] = 0f;
        if (totalPoints != 0){
            for (int i = 1; i < 65536; i++) {
                this.histogram[i] = this.histogram[i - 1] + this.histogram[i] / (float) totalPoints;
            }
        }
        src.rewind();
    }

    /**
     * apply colormap and save result into bytebuffer
     * @param src16: bytebuffer containing 16bit depth data
     * @param dst16: bytebuffer containing 16bit rgb565 data
     */
    public void applyColorMap(ByteBuffer src16, ByteBuffer dst16){
        src16.rewind();
        dst16.rewind();

//        calcEqualizedHist(src16);
        while(true){
            try{
                int val = (src16.get() & 0xFF);
                val += (src16.get() & 0xFF) << 8;

//                int rgb = histNApplyLUT(val, this.LUT);
                int rgb = scaleNApplyLUT(val, range_max, range_min);
                dst16.put((byte) (rgb & 0xFF));
                dst16.put((byte) ((rgb & 0xFFFF) >> 8));

            } catch (BufferUnderflowException | IllegalStateException ignored){
                break;
            }
        }
        src16.rewind();
        dst16.rewind();
    }

    public int getRange_min() {
        return range_min;
    }

    public void setRange_min(int range_min) {
        this.range_min = range_min;
    }

    public int getRange_max() {
        return range_max;
    }

    public void setRange_max(int range_max) {
        this.range_max = range_max;
    }
}