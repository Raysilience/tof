package com.example.tof;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;

import org.junit.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        ByteBuffer src = ByteBuffer.allocate(10);
        src.put((byte) 0);
        src.put((byte) 1);
        src.put((byte) 2);
        src.put((byte) 3);
        src.rewind();
        System.out.println("val: " + src.get(2));
        System.out.println("src position: " + src.position());
        System.out.println("src limit: " + src.limit());
    }

    private void converter(ByteBuffer src16, ByteBuffer dst8, int range_max, int range_min){
        src16.rewind();
        dst8.rewind();
        System.out.println("src16 position: " + src16.position());
        System.out.println("src16 limit: " + src16.limit());
        System.out.println("dst8 position: " + dst8.position());
        System.out.println("dst8 limit: " + dst8.limit());

        while(true){
            try{
//                int val = (src16.get() << 8) & 0xFFFF;
//                val += src16.get() & 0xFFFF;
//                System.out.println("val: " + val);
//                byte bt = 0;
                byte bt = src16.get();
                src16.get();
//                if (val >= range_min && val <= range_max){
//                    bt = (byte)((val - range_min)*((float)255/(range_max - range_min)));
//                }
                dst8.put(bt);
            } catch (BufferUnderflowException ignored){
                System.out.println("we break");

                break;
            }
        }
        dst8.rewind();
        src16.rewind();
        System.out.println("src16 position: " + src16.position());
        System.out.println("src16 limit: " + src16.limit());
        System.out.println("dst8 position: " + dst8.position());
        System.out.println("dst8 limit: " + dst8.limit());
    }
}