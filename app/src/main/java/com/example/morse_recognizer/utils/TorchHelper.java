package com.example.morse_recognizer.utils;


import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

public class TorchHelper {
    private static final String TAG = "TorchHelper";

    private final CameraManager cameraManager;
    private String cameraId;

    public TorchHelper(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (hasFlash != null && hasFlash &&
                        lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Ошибка получения ID камеры", e);
        }
    }

    public void setTorch(boolean on) {
        try {
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, on);

            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Не удалось изменить режим фонарика", e);
        }
    }

}