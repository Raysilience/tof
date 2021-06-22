package com.example.tof;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.tcl.tvcamera.TCameraManager;
import com.tcl.tvcamera.TvCameraApi;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final String TAG = "MainActivity";
    private DepthView myDepthView;
    private Camera2Preview mCamera2;

    private final int PERMISSION_REQUEST_CODE = 1;
    private List<String> mPermissionList = new ArrayList<>();
    private final String[] mPermissions = new String[]{
            Manifest.permission.CAMERA,
    };

    private Spinner mSpinner;
    private ImageProcessor mImageProcessor;
    private EditText editText_range_min;
    private EditText editText_range_max;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermission();

        TvCameraApi api = TvCameraApi.getInstance();
        api.init(0, this);
        api.setRequestFormat(0, TCameraManager.REQUEST_FORMAT_RGB);
        api.setRequestFormat(0, TCameraManager.REQUEST_FORMAT_DEPTH);

        mCamera2 = findViewById(R.id.Camera2Preview);
        myDepthView = findViewById(R.id.Depth);
        myDepthView.setDepthSize(640, 480);
        mCamera2.setDepthView(myDepthView);

        mImageProcessor = ImageProcessor.getInstance(this);

        editText_range_max = findViewById(R.id.range_max);
        editText_range_min = findViewById(R.id.range_min);

        mSpinner = findViewById(R.id.Spinner);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = (String)parent.getItemAtPosition(position);
                ImageProcessor.COLORMAP type = ImageProcessor.COLORMAP.valueOf(item);
                mImageProcessor.setLUT(type);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.e("RUi", "onNothingSelected");

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera2.onResume();
        if (!OpenCVLoader.initDebug()){
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else{
            Log.d(TAG, "OpenCV library found inside package.");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera2.onPause();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkAndRequestPermission(){
        mPermissionList.clear();
        for (String permission: mPermissions){
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED){
                mPermissionList.add(permission);
            }
        }
        if (mPermissionList.size() > 0){
            String[] perm = mPermissionList.toArray(new String[0]);
            requestPermissions(perm, PERMISSION_REQUEST_CODE);
            Log.e(TAG, "申请权限");

        } else{
            Log.e(TAG, "获取全部权限");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e(TAG, "===onRequestPermissionsResult==="+requestCode);
        boolean allAcquired = true;
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allAcquired = false;
                        break;
                    }
                }
                if (allAcquired){
                    Log.e(TAG, "已经获取所需权限");
                    mCamera2.onResume();
                    if (!OpenCVLoader.initDebug()){
                        Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
                    } else{
                        Log.d(TAG, "OpenCV library found inside package.");
                        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                    }
                } else{
                    Log.e(TAG, "未获取所需权限");
                    finish();
                }
        }
    }

    public void onCapture(View view) {
        mCamera2.setCaptureOneFrame(true);
    }

    public void onUpdateRange(View view) {
        String text_range_max = editText_range_max.getText().toString();
        String text_range_min = editText_range_min.getText().toString();
        int range_max, range_min;
        if (!text_range_max.isEmpty()){
            range_max = Integer.parseInt(text_range_max);
            mImageProcessor.setRange_max(range_max);
        }
        if (!text_range_min.isEmpty()){
            range_min = Integer.parseInt(text_range_min);
            mImageProcessor.setRange_min(range_min);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}