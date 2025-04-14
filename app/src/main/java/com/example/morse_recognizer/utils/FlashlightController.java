package com.example.morse_recognizer.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import static com.example.morse_recognizer.values.MorseConstants.*;

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
            torchHelper.setTorch(false);  // Выключаем фонарик
            stopTransmission();
            return;
        }

        char symbol = morseCode.charAt(index);
        handler.postDelayed(() -> {
        switch (symbol) {
            case '.':
                torchHelper.setTorch(true);  // Включаем фонарик для точки
                handler.postDelayed(() -> {
                    torchHelper.setTorch(false);  // Выключаем после точки
                    transmitMorseCode(morseCode, index + 1);  // Переходим к следующему символу
                }, DOT_DURATION);
                break;
            case '-':
                torchHelper.setTorch(true);  // Включаем фонарик для тире
                handler.postDelayed(() -> {
                    torchHelper.setTorch(false);  // Выключаем после тире
                    transmitMorseCode(morseCode, index + 1);  // Переходим к следующему символу
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
        torchHelper.setTorch(false);  // Выключаем фонарик
        if (transmissionListener != null) {
            transmissionListener.onTransmissionStopped();
        }
    }

    public boolean isTransmissionRunning() {
        return transmissionRunning;
    }
}

