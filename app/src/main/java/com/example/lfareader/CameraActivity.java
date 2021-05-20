package com.example.lfareader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private String imageName;
    private static final String TAG = "DisplayImageActivity";
    private String finalImageName;
    private TextureView mTextureView;
    private ImageView mViewFinder;
    private Image capturedImage;
    private byte[] finalBytes;
    private byte[] bytesToSend;
    private Bitmap imageBitmap;
    private CameraDevice mCameraDevice;
    private String cameraId;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest mCaptureRequest;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private Handler mHandler;
    private CameraCharacteristics characteristics;
    private static final int RequestCameraPermission = 100;
    private static final String Tag = "AndroidCameraAPI";
    private Size imageDimension;
    private static final SparseIntArray Orientations = new SparseIntArray();
    static {
        Orientations.append(Surface.ROTATION_0, 90);
        Orientations.append(Surface.ROTATION_90, 0);
        Orientations.append(Surface.ROTATION_180, 270);
        Orientations.append(Surface.ROTATION_270, 180);
    }
    FloatingActionButton saveImageButton;
    FloatingActionButton discardImageButton;
    FloatingActionButton captureButton;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        imageName = getIntent().getStringExtra("fileName");
        saveImageButton = findViewById(R.id.saveImageBtn);
        saveImageButton.setVisibility(View.GONE);
        saveImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    saveImage(finalBytes);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        });
        discardImageButton = findViewById(R.id.discardImageBtn);
        discardImageButton.setVisibility(View.GONE);
        discardImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discardImage();
            }
        });
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setClickable(true);
        openCamera();
        captureButton = findViewById(R.id.captureImageBtn);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

    }

    private void saveImage (byte[] bytes) throws IOException{
        final File file = createFile();
        OutputStream output = null;
        output = new FileOutputStream(file);
        output.write(bytes);
    }

    @SuppressLint("RestrictedApi")
    private void discardImage(){
        discardImageButton.setVisibility(View.GONE);
        saveImageButton.setVisibility(View.GONE);
        captureButton.setVisibility(View.VISIBLE);
        createCameraPreview();
    }

    public File createFile(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateTime = sdf.format(Calendar.getInstance().getTime());
        String ImageName = imageName+"_"+dateTime+".jpeg";
        File imageDir = new File(getFilesDir(),"/");
        if(!imageDir.exists()){
            imageDir.mkdirs();
        }
        File imageFile = new File(imageDir, ImageName);
        finalImageName = getFilesDir().toString()+"/"+ImageName;
        return imageFile;
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.e(Tag, "onOpened");
            mCameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };

    final CameraCaptureSession.CaptureCallback captureCallBackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };

    private void takePicture(){
        if (mCameraDevice == null) {
            Log.e(Tag, "Camera Device is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0<jpegSizes.length) {
                Log.d(TAG, "takePicture: "+ Arrays.toString(jpegSizes));
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_TORCH);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
            captureBuilder.addTarget(reader.getSurface());
            Byte quality = 100;
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, quality);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, Orientations.get(rotation));
//            final File file = createFile();

            ImageReader.OnImageAvailableListener readerlistener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        capturedImage = image;
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        finalBytes = new byte[buffer.capacity()];
                        buffer.get(finalBytes);

//                        imageBitmap = BitmapFactory.decodeByteArray(finalBytes, 0, finalBytes.length);
//                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//                        bytesToSend = stream.toByteArray();

                    }
                    finally {
                        if(image!=null){
                            image.close();
                        }
                    }
                }

//                private void save(byte[] bytes) throws IOException{
//                    OutputStream output = null;
//                    try{
//                        output = new FileOutputStream(file);
//                        output.write(bytes);
//                    }
//                    finally {
//                        if(null!=output){
//                            output.close();
//                        }
//                    }
//                }
            };
            reader.setOnImageAvailableListener(readerlistener, mHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @SuppressLint("RestrictedApi")
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText(getApplicationContext(),"Saved: "+file,Toast.LENGTH_SHORT).show();
//                    createCameraPreview();
                    captureButton.setVisibility(View.GONE);
                    saveImageButton.setVisibility(View.VISIBLE);
                    discardImageButton.setVisibility(View.VISIBLE);
                }
            };

            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, mHandler);

        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(Tag, "is Camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
//            manager.setTorchMode(cameraId, true);
            characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCameraPermission);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);

//            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);


//            Camera cam = Camera.open();
//            Camera.Parameters p = cam.getParameters();
//            p.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
//            cam.setParameters(p);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(Tag, "openCamera X");
    }

    private void createCameraPreview(){
        try{
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);
            Log.d(TAG, "createCameraPreview: camera settings");
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);

//            mCaptureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);

            mCaptureRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice){
                        return;
                    }
                    mCameraCaptureSession = cameraCaptureSession;
                    updatePreview();
                    mTextureView.setOnTouchListener(new CameraFocusOnTouchHandler(characteristics, mCaptureRequestBuilder, mCameraCaptureSession, mHandler));

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getApplicationContext(),"Configuration Changed",Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview(){
        if (mCameraDevice == null) {
            Log.e(Tag, "updatePreview Error, Return");
        }
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mHandler);
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
    }
}