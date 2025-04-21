package com.example.morse_recognizer.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import static com.example.morse_recognizer.morse.MorseConstants.*;

public class FlashlightController {
    private final TorchHelper torchHelper;
    private TransmissionListener transmissionListener;
    private boolean transmissionRunning = false;


    private final Handler handler = new Handler(Looper.getMainLooper());


    public interface TransmissionListener {
        void onTransmissionStopped();
    }
    public void setTransmissionListener(TransmissionListener listener) {
        this.transmissionListener = listener;
    }

    public FlashlightController(Context context) {
        torchHelper = new TorchHelper(context);
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
                }, DOT_DURATION);
                break;
            case '-':
                torchHelper.setTorch(true);
                handler.postDelayed(() -> {
                    torchHelper.setTorch(false);
                    transmitMorseCode(morseCode, index + 1);
                }, DASH_DURATION);
                break;
            case '+':
                handler.postDelayed(() -> {
                    transmitMorseCode(morseCode, index + 2);
                }, WORD_PAUSE);
                break;
            default:
                handler.postDelayed(() -> {
                    transmitMorseCode(morseCode, index + 1);
                }, LETTER_PAUSE);
                break;
        }
        }, SYMBOL_PAUSE);
    }

    public void stopTransmission() {
        transmissionRunning = false;
        torchHelper.setTorch(false);
        if (transmissionListener != null) {
            transmissionListener.onTransmissionStopped();
        }
    }

    public boolean isTransmissionRunning() {
        return transmissionRunning;
    }
}

