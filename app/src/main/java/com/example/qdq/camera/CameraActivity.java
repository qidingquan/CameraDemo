package com.example.qdq.camera;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class CameraActivity extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback, View.OnClickListener {

    private static final String TAG = "CameraActivity";
    private SurfaceView surfaceView;
    private ImageView imageView;
    private SurfaceHolder holder;
    private Camera camera;
    private int orientation;
    private int cameraId= Camera.CameraInfo.CAMERA_FACING_BACK;
    private OrientationEventListener orientationEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        surfaceView = (SurfaceView) findViewById(R.id.camera_view);
        holder = surfaceView.getHolder();
        /*
         * 设置Surface是一个推送类型的Surface,意味着在Surface本身的外部维持绘图缓冲区，
         * 该缓冲区由Camera管理，推送类型的surface是camera预览所需的surface
         */
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //实现SurfaceHolder.CallBack接口，从而在surface发生变化时获得通知
        holder.addCallback(this);

        surfaceView.setFocusable(true);//可聚焦,默认情况下surface不可聚焦
        surfaceView.setFocusableInTouchMode(true);//设置为不禁用，触摸模式下，通常会禁用焦点
        surfaceView.setClickable(true);//可单击
        //为预览视图添加监听
        surfaceView.setOnClickListener(this);
        imageView = (ImageView) findViewById(R.id.photoIv);
        imageView.setOnClickListener(this);
        //初始化监听屏幕方向
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                CameraActivity.this.orientation = orientation;
                setCameraDisplayOrientation(camera);
            }
        };
    }

    /**
     * 实现SurfaceHolder.CallBack接口回调方法
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //活动创建后，获得camera对象
        camera = Camera.open(cameraId);
        initCamera();
        try {
            camera.setPreviewDisplay(holder);
            //最后启动摄像头预览
            camera.startPreview();
        } catch (IOException e) {
            //程序出现异常时我们应该释放Camera对象
            e.printStackTrace();
            camera.release();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orientationEventListener != null) {
            orientationEventListener.enable();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
    }

    /**
     * 该回调方法第一个参数：实际图像的数据的字节数组
     * 第二个参数：捕获该图像的Camera对象的引用
     */
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        //将图片保存到mediaStore中
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        try {
            //获取图片文件的流
            OutputStream os = getContentResolver().openOutputStream(uri);
            //将数据写到文件中
            os.write(data);
            os.flush();
            os.close();
            Toast.makeText(this, "照片已保存", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, PhotoShowActivity.class);
//            intent.putExtra("bitmap",bitmap);
            intent.putExtra("url", uri);
            startActivity(intent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        //调用takePicture方法后，调用startPreview方法可以安全的重新启动它
        camera.startPreview();
    }

    @Override
    public void onClick(View v) {
        if (v == imageView) {
            camera.takePicture(null, null, this);
        } else {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {

                }
            });
        }
    }

    private void initCamera() {
        Camera.Parameters parameters = camera.getParameters();
        //首先判断当前屏幕的方向
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {//横向
            //设置Camera的参数值
            parameters.set("orientation", "landscape");
            //图像应该旋转的角度（有效度数0,90,180,270）
            camera.setDisplayOrientation(0);
            /*
             * 对于2.2以上版本可用，实际上并不执行旋转，它会告知Camera对象在EXIF数据中指定该图像需要旋转的角度，
             * 如没有设置该属性，在其他应用程序中查看图像时，可能会侧面显示
             */
            parameters.setRotation(0);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {//纵向
            parameters.set("orientation", "portrait");
            camera.setDisplayOrientation(90);
            parameters.setRotation(90);
        }
        Camera.Size previewSize = Util.findBestPreviewResolution(this, camera);
        Camera.Size pictureSize = Util.findBestPictureResolution(this, camera);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setPictureSize(pictureSize.width, pictureSize.height);
        try {
            //给相机设置预览器holder
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public  void setCameraDisplayOrientation(android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        Log.e(TAG, "rotation: "+result );
        camera.setDisplayOrientation(result);
    }
    /**
     * 旋转图片
     * @param data
     */
    private void rotatePhoto(byte[] data){

        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        Matrix matrixs = new Matrix();
        if(orientation > 325 || orientation <= 45){
            matrixs.setRotate(90);
        }else if(orientation > 45 && orientation <= 135){
            matrixs.setRotate(180);
        }else if(orientation > 135 && orientation < 225){
            matrixs.setRotate(270);
        }else {
            matrixs.setRotate(0);
        }
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrixs, true);

    }
}
