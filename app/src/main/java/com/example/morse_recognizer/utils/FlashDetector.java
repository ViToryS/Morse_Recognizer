package com.example.morse_recognizer.utils;

import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class FlashDetector {
    private static final String TAG = "FlashDetector";
    private static final int SAMPLE_SIZE = 30; // Количество кадров для вычисления среднего

    private final int[] brightnessHistory = new int[SAMPLE_SIZE];
    private int historyIndex = 0;
    private int historySum = 0;
    private int previousBrightness = 0;
    private long lastFlashTime = 0;
    private boolean isFlashOn = false;
    private boolean isCalibrating = true;
    private long calibrationStartTime = 0;

    public void processImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        int avgBrightness = calculateAverageBrightness(data);

        if (isCalibrating) {
            updateBrightnessHistory(avgBrightness);
            checkCalibration();
        } else {
            checkForFlash(avgBrightness);
        }
    }

    private int calculateAverageBrightness(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += b & 0xFF;
        }
        return sum / data.length;
    }

    private void updateBrightnessHistory(int currentBrightness) {
        historySum -= brightnessHistory[historyIndex];
        brightnessHistory[historyIndex] = currentBrightness;
        historySum += currentBrightness;
        historyIndex = (historyIndex + 1) % SAMPLE_SIZE;
    }

    private int getAverageBrightness() {
        return historySum / SAMPLE_SIZE;
    }

    private void checkCalibration() {
        if (isCalibrating && System.currentTimeMillis() - calibrationStartTime > 3000) {
            isCalibrating = false;
            Log.d(TAG, "Calibration complete. Average brightness: " + getAverageBrightness());
        }
    }

    private void checkForFlash(int currentBrightness) {
        int averageBrightness = getAverageBrightness();
        int brightnessThreshold = averageBrightness + 50;
        int brightnessChangeThreshold = 30;

        if (currentBrightness > brightnessThreshold &&
                Math.abs(currentBrightness - previousBrightness) > brightnessChangeThreshold) {
            if (!isFlashOn) {
                isFlashOn = true;
                lastFlashTime = System.currentTimeMillis();
                Log.d(TAG, "Flash detected at: " + lastFlashTime);
            }
        } else {
            if (isFlashOn) {
                isFlashOn = false;
                long flashDuration = System.currentTimeMillis() - lastFlashTime;
                Log.d(TAG, "Flash ended. Duration: " + flashDuration + "ms");
            }
        }
        previousBrightness = currentBrightness;
    }

    public void startCalibration() {
        isCalibrating = true;
        calibrationStartTime = System.currentTimeMillis();
        Arrays.fill(brightnessHistory, 0);
        historySum = 0;
        historyIndex = 0;
    }
}