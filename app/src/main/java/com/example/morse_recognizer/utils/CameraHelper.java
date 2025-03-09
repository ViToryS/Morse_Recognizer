package com.example.morse_recognizer.utils;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;


import androidx.annotation.NonNull;

import java.util.Arrays;

public class CameraHelper {
    private static final String TAG = "CameraHelper";

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private Handler backgroundHandler;


    public void startCamera(Context context, TextureView textureView, Handler backgroundHandler) {
        this.backgroundHandler = backgroundHandler;
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            String cameraId = manager.getCameraIdList()[0];

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreview(textureView);
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
                    Log.e(TAG, "Ошибка при открытии камеры: " + error);
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Ошибка доступа к камере", e);
        } catch (SecurityException e) {
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

            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);


            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
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