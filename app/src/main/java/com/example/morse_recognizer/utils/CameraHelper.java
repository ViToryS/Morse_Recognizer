package com.example.morse_recognizer.utils;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.media.Image;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraHelper {
    private static final String TAG = "CameraHelper";

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private Handler backgroundHandler;
    private ImageReader imageReader;

    private CameraStateListener cameraStateListener;
    // Обработчик изображений
    private ImageProcessingListener imageProcessingListener;

    public interface CameraStateListener {
        void onCameraOpened();
        void onCameraError(String error);
    }

    public interface ImageProcessingListener {
        void onImageProcessed(Image image);
    }

    public CameraHelper(Handler backgroundHandler, CameraStateListener listener, ImageProcessingListener imageListener) {
        this.backgroundHandler = backgroundHandler;
        this.cameraStateListener = listener;
        this.imageProcessingListener = imageListener;
    }

    public void startCamera(Context context, TextureView textureView,
                            ImageReader imageReader, ImageProcessingListener imageProcessingListener ) {
        this.imageReader = imageReader;
        this.imageProcessingListener = imageProcessingListener;

        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            String cameraId = manager.getCameraIdList()[0];

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreview(textureView);

                    imageReader.setOnImageAvailableListener(reader -> {
                        try (Image image = reader.acquireLatestImage()) {
                            if (image != null) {
                                if (imageProcessingListener != null) {
                                    imageProcessingListener.onImageProcessed(image);  // Передаем изображение в слушатель
                                }
                            }
                        }
                    }, backgroundHandler);


                    if (cameraStateListener != null) {
                        cameraStateListener.onCameraOpened();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                    if (cameraStateListener != null) {
                        cameraStateListener.onCameraError("Ошибка при открытии камеры: " + error);
                    }
                    Log.e(TAG, "Ошибка при открытии камеры: " + error);
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            if (cameraStateListener != null) {
                cameraStateListener.onCameraError("Ошибка доступа к камере");}
            Log.e(TAG, "Ошибка доступа к камере", e);
        } catch (SecurityException e) {
            if (cameraStateListener != null) {
                cameraStateListener.onCameraError("Нет разрешения на использование камеры");
            }
            Log.e(TAG, "Нет разрешения на использование камеры", e);
        }
    }

    private void createCameraPreview(TextureView textureView) {
        if (backgroundHandler == null) {
            Log.e(TAG, "Фоновый поток не активен");
            return;
        }
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            Surface surface = new Surface(texture);

            final CaptureRequest.Builder captureRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(imageReader.getSurface());

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    cameraCaptureSession = session;
                    try {
                        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Ошибка при настройке сессии", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Не удалось настроить сессию");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Ошибка при создании превью", e);
        }
    }

    public void closeCamera() {
        if (cameraCaptureSession != null) {
            try {
                cameraCaptureSession.stopRepeating();
                cameraCaptureSession.close();
            } catch (CameraAccessException e) {
                Log.e(TAG, "Ошибка при закрытии сессии", e);
            }
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}