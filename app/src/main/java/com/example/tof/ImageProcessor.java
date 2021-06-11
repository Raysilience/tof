package com.example.tof;

import android.util.Log;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ImageProcessor {
    enum COLORMAP{
        JET, RAINBOW
    }
    private static final String TAG = ImageProcessor.class.getName();
    private int[] LUT = new int[256];
    private float[] histogram = new float[65536];
    private boolean isLUTSet = false;

    public void setLUT(COLORMAP type) {
        switch (type){
            case JET:
                for (int i = 0; i < 256; i++){
                    int r = (i > 128) ? (i < 192) ? (i - 128) * 4 : 255 : 0;
                    int g = (i < 64) ? i * 4 : (i > 192) ? (256 - (4 * (i - 192))) : 255;
                    int b = (i > 64) ? (i > 128) ? 0 : (256 - 4 * (i - 128)) : 255;
                    this.LUT[i] = getRGB565(r, g, b);
                }
                break;
            case RAINBOW:
                break;
        }
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
     * @param LUT: lookup table
     * @return color in the form of RGB565
     */
    private int scaleNApplyLUT(int val, int range_max, int range_min, int[] LUT){
        // map 65536 to 256 then
        if (!isLUTSet) return 0;
        if (val >= range_min && val <= range_max){
            val = 255 - (int)((float)(val - range_min)*255/(range_max - range_min));
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
                int rgb = scaleNApplyLUT(val, 8191, 128, this.LUT);

                dst16.put((byte) (rgb & 0xFF));
                dst16.put((byte) ((rgb & 0xFFFF) >> 8));

            } catch (BufferUnderflowException ignored){
                break;
            }
        }
        src16.rewind();
        dst16.rewind();
    }
}