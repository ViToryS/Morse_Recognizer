package com.example.morse_recognizer.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.morse_recognizer.morse.MorseConstants;


public class FlashlightController {
    private static FlashlightController instance;
    private final TorchHelper torchHelper;

    private TransmissionListener transmissionListener;
    private boolean transmissionRunning = false;

    private FlashlightController(Context context) {
        torchHelper = TorchHelper.getInstance(context);
    }

    public static synchronized FlashlightController getInstance(Context context) {
        if (instance == null) {
            instance = new FlashlightController(context.getApplicationContext());
        }
        return instance;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface TransmissionListener {
        void onTransmissionStopped();
    }
    public void setTransmissionListener(TransmissionListener listener) {
        this.transmissionListener = listener;
    }



    public void startMorseTransmission(String morseCode) {
        if (transmissionRunning) {
            stopTransmission();
        }

        transmissionRunning = true;
        transmitMorseCode(morseCode, 0);
    }


    private void transmitMorseCode(String morseCode, int index) {
        if (!transmissionRunning || index >= morseCode.length()) {
            transmissionRunning = false;
            torchHelper.setTorch(false);
            stopTransmission();
            return;
        }
        char symbol = morseCode.charAt(index);
        long currentTime = System.currentTimeMillis();
        Log.d("трансляция", "Символ: " + symbol + "Время"+ currentTime );
        handler.postDelayed(() -> {
        switch (symbol) {
            case '.':
                torchHelper.setTorch(true);
                handler.postDelayed(() -> {
                    torchHelper.setTorch(false);
                    transmitMorseCode(morseCode, index + 1);
                }, MorseConstants.getDotDuration());
                break;
            case '-':
                torchHelper.setTorch(true);
                handler.postDelayed(() -> {
                    torchHelper.setTorch(false);
                    transmitMorseCode(morseCode, index + 1);
                }, MorseConstants.getDashDuration());
                break;
            case '+':
                handler.postDelayed(() -> transmitMorseCode(morseCode, index + 2),
                        MorseConstants.getWordPause());
                break;
            default:
                handler.postDelayed(() -> transmitMorseCode(morseCode, index + 1), MorseConstants.getLetterPause());
                break;
        }
        }, MorseConstants.getSymbolPause());
    }

    public void stopTransmission() {
        transmissionRunning = false;
        torchHelper.setTorch(false);
        if (transmissionListener != null) {
            transmissionListener.onTransmissionStopped();
        }
    }
}

