package com.example.morse_recognizer.utils;


import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.morse_recognizer.R;

import java.util.Objects;

public class FlashlightController {
    private TransmissionListener transmissionListener;

    private static final long DOT_DURATION = 200;
    private static final long DASH_DURATION = 600;
    private static final long SYMBOL_PAUSE = 200;
    private static final long LETTER_PAUSE = 400;
    private static final long WORD_PAUSE   = 1000;
    private char previousSymbol = '0';
    private final CameraManager cameraManager;
    private String cameraId;
    private boolean isTorchOn = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean transmissionRunning = false;

    public interface TransmissionListener {
        void onTransmissionStopped();
    }
    public void setTransmissionListener(TransmissionListener listener) {
        this.transmissionListener = listener;
    }

    public FlashlightController(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            Log.e("FlashController", Objects.requireNonNull(e.getMessage()));
            Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show();
        }
    }

    public void startMorseTransmission(String morseCode) {
        if (cameraId == null) {
            Log.e("FlashController", "Камера недоступна.");
            return;
        }
        if (transmissionRunning) {
            stopTransmission();
        }

        transmissionRunning = true;
        handler.post(() -> { //
            transmitMorseCode(morseCode, 0);
        });
    }

    public void stopTransmission() {
        transmissionRunning = false;
        handler.removeCallbacksAndMessages(null);
        setTorch(false);

        if (transmissionListener != null) {
            transmissionListener.onTransmissionStopped();
        }
    }


    private void transmitMorseCode(String morseCode, int index) {
        if (!transmissionRunning || index >= morseCode.length()) {
            transmissionRunning = false;
            setTorch(false);
            stopTransmission();
            return;
        }

        char symbol = morseCode.charAt(index);

        switch (symbol) {
            case '.':
                dot();
                handler.postDelayed(() -> {
                    previousSymbol = symbol;
                    transmitMorseCode(morseCode, index + 1);
                }, DOT_DURATION + SYMBOL_PAUSE);
                break;
            case '-':
                dash();
                handler.postDelayed(() -> {
                    previousSymbol = symbol;
                    transmitMorseCode(morseCode, index + 1);
                }, DASH_DURATION + SYMBOL_PAUSE);

                break;
            case '+':
                handler.postDelayed(() -> {
                    previousSymbol = symbol;
                    transmitMorseCode(morseCode, index + 1);
                }, WORD_PAUSE);
                break;
            default:
                if (previousSymbol == '+') {
                    handler.postDelayed(() -> {
                        previousSymbol = symbol;
                        transmitMorseCode(morseCode, index + 1);
                    }, SYMBOL_PAUSE);}
                else{
                    handler.postDelayed(() -> {
                        previousSymbol = symbol;
                        transmitMorseCode(morseCode, index + 1);
                    }, LETTER_PAUSE);}
                break;
        }
    }


    private void dot() {
        setTorch(true);
        handler.postDelayed(() -> setTorch(false), DOT_DURATION);
    }

    private void dash() {
        setTorch(true);
        handler.postDelayed(() -> setTorch(false), DASH_DURATION);
    }

    private void setTorch(boolean on) {
        try {
            cameraManager.setTorchMode(cameraId, on);
            isTorchOn = on;
        } catch (CameraAccessException e) {
            Log.e("FlashController", Objects.requireNonNull(e.getMessage()));
        }
    }
    public boolean isTransmissionRunning() {
        return transmissionRunning;
    }
}