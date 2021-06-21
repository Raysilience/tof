package com.example.tof;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.opencv.core.Point;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Camera2Preview extends TextureView {
    private static final String TAG = "Camera2Preview";
    private static final String PATH = "/sdcard/";
    private Context mContext;
    private String mCameraId;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;

    private DepthView myDepthView;
    private ImageProcessor mImageProc;
    private boolean isCaptureOneFrame = false;
    private ByteBuffer dst16 = ByteBuffer.allocate(614400);
    private Point mPoint;

    private int photoIdx = 0;

    public Camera2Preview(Context context) {
        this(context, null);
    }

    public Camera2Preview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Camera2Preview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        Log.e(TAG, String.valueOf(context != null));
        mImageProc = ImageProcessor.getInstance(context);
        mImageProc.setLUT(ImageProcessor.COLORMAP.PLASMA);
    }

    private final SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) { }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) { }
    };

    public void onResume(){
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        if(isAvailable()){
            openCamera();
        }else{
            setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    public void onPause(){
        closeCamera();
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void openCamera(){
        CameraManager manager = (CameraManager)mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] list = manager.getCameraIdList();
            if (list != null) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "openCamera: no Camera Permission");
                    return;
                }
                manager.openCamera(list[0], mStateCallback, mBackgroundHandler);
            }
        }catch (Exception e){
            Log.d(TAG, "getCameraIdList FAIL");
            e.printStackTrace();
        }
    }

    public void closeCamera(){
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }

        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "onOpened: ");
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "onDisconnected: ");
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.d(TAG, "onError: ");
            cameraDevice.close();
            mCameraDevice = null;
            Log.e(TAG, "CameraDevice.StateCallback onError errorCode= " + error);
        }
    };

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = getSurfaceTexture();
            if(texture == null){
                Log.d(TAG, "createCameraPreviewSession: texture is null");
            }

            Surface surface = new Surface(texture);

            mImageReader = ImageReader.newInstance(640, 480, ImageFormat.DEPTH16, 1);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

            mPreviewRequestBuilder= mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),//
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "onConfigured: ");
                            // 相机已经关闭
                            if (null == mCameraDevice) {
                                return;
                            }

                            // 当session准备好后，我们开始显示预览
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // 相机预览时应连续自动对焦
                                //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // 设置闪光灯在必要时自动打开
                                //setAutoFlash(mPreviewRequestBuilder);
                                // 最终,显示相机预览
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "CameraCaptureSession.StateCallback onConfigureFailed");
                        }

                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image img = reader.acquireNextImage();
            ByteBuffer src16 = img.getPlanes()[0].getBuffer();
            Log.d(TAG, "onImageAvailable SIZE = " + src16.remaining());

            if (isCaptureOneFrame){
                saveOneFrame(PATH + "depth_" +  photoIdx++ + ".png", src16);
                isCaptureOneFrame = false;
            }

            myDepthView.setDistance(src16);

            mImageProc.applyColorMap(src16, dst16);
            myDepthView.mBitmap.copyPixelsFromBuffer(dst16);
            myDepthView.postInvalidate();

            img.close();

        }
    };

    public void setDepthView(DepthView view){
        myDepthView = view;
    }

    public void setCaptureOneFrame(boolean flag){
        this.isCaptureOneFrame = flag;
    }


    private void saveOneFrame(String filename, ByteBuffer src){
        FileOutputStream out = null;
        try {
            File file = new File(filename);
            if(!file.exists()){
                file.createNewFile();
            }
            out = new FileOutputStream(file, false);
        } catch (IOException e){
            e.printStackTrace();
        }
        byte[] data = new byte[src.remaining()];
        src.get(data);
        writeData(out, data);
        src.rewind();
    }

    private void writeData(FileOutputStream out, byte[] data){
        try{
            if(out != null) {
                Log.e(TAG, "======write data======");
                out.write(data);
            }
        } catch (IOException ioe){
            Log.e(TAG, ioe.toString());
            ioe.printStackTrace();
            try {
                out.close();
            } catch (Exception ignored){
            }
        }
    }
}