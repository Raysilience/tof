package com.example.tof;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.Toast;

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
    private DepthView mD2CView;
    private Camera2Preview mCamera2;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;

    private final int PERMISSION_REQUEST_CODE = 1;
    private List<String> mPermissionList = new ArrayList<>();
    private final String[] mPermissions = new String[]{
            Manifest.permission.CAMERA,
    };

    private ImageProcessor mImageProcessor;
    private EditText editText_range_min;
    private EditText editText_range_max;
    private PopupWindow mPopupWindow;

    private boolean isD2Copen = false;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermission();
        initStream();
        mImageProcessor = ImageProcessor.getInstance(this);

        mCamera2 = findViewById(R.id.Camera2Preview);
        myDepthView = findViewById(R.id.Depth);
        myDepthView.setDepthSize(640, 480);
        mCamera2.setDepthView(myDepthView);

        mD2CView = findViewById(R.id.D2C);
        mD2CView.setDepthSize(640, 480);
        mCamera2.setD2CView(mD2CView);
        mD2CView.setVisibility(View.INVISIBLE);

        editText_range_max = findViewById(R.id.range_max);
        editText_range_min = findViewById(R.id.range_min);

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dropdown_listview, null, false);
        ListView lv = view.findViewById(R.id.colormap_lv);
        ImageProcessor.COLORMAP[] data = ImageProcessor.COLORMAP.values();
        ArrayAdapter<ImageProcessor.COLORMAP> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, data);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ImageProcessor.COLORMAP type = (ImageProcessor.COLORMAP) parent.getItemAtPosition(position);
                mImageProcessor.setLUT(type);
            }
        });
        this.mPopupWindow = new PopupWindow(view, 300, 300);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);
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

    public void onSelectColormap(View view) {
        mPopupWindow.showAsDropDown(view);
    }

    public void onD2C(View view) {
        Button btn = findViewById(R.id.Btn_D2C);
        if (!isD2Copen){
            mD2CView.setVisibility(View.VISIBLE);
            btn.setText("关闭D2C");
            isD2Copen = true;
        } else {
            mD2CView.setVisibility(View.INVISIBLE);
            btn.setText("开启D2C");
            isD2Copen = false;
        }
        mD2CView.setD2Copen(isD2Copen);
    }

    private void initStream(){
        TvCameraApi api = TvCameraApi.getInstance();
        api.init(0, this);
        api.setRequestFormat(0, TCameraManager.REQUEST_FORMAT_RGB);
        api.setRequestFormat(0, TCameraManager.REQUEST_FORMAT_DEPTH);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}