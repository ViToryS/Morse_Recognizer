package com.example.morse_recognizer.utils;

import android.graphics.Rect;
import android.media.Image;
import android.util.Log;
import static com.example.morse_recognizer.values.MorseConstants.*;
import java.nio.ByteBuffer;

public class FlashDetector {
    public interface BrightnessListener {
        void onBrightnessChanged(int brightness);
        void onTextUpdated(String text);
    }

    private int previousBrightness = 0;
    private static final String TAG = "FlashDetector";
    private static final int DOT_DURATION_THRESHOLD = 300;
    private int brightnessThreshold = 130;
    private long lastFlashTime = 0;
    private long lastFlashEndTime = 0;
    private boolean isFlashOn = false;
    private BrightnessListener brightnessListener;
    private boolean flash_end_flag = false;
    private Rect areaToProcess = null;

    private StringBuilder resultText = new StringBuilder();

    public void setBrightnessListener(BrightnessListener listener) {
        this.brightnessListener = listener;
    }

    public void setAreaToProcess(Rect area) {
        this.areaToProcess = area;
    }
    public void processImage(Image image, boolean isRecognising) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        int width = image.getWidth();
        int height = image.getHeight();
        int avgBrightness = calculateAverageBrightness(data, width, height);

        if (isRecognising){
        checkForFlash(avgBrightness);}

        updateBrightnessView(avgBrightness);
    }


    private int calculateAverageBrightness(byte[] data, int width, int height) {
        int sum = 0;
        int count = 0;

        Rect rect = (areaToProcess != null) ? areaToProcess :
                new Rect(0, 0, width, height); // если область не задана — весь кадр

        for (int y = rect.top; y < rect.bottom; y++) {
            for (int x = rect.left; x < rect.right; x++) {
                int index = y * width + x;
                if (index >= 0 && index < data.length) {
                    sum += data[index] & 0xFF;
                    count++;
                }
            }
        }

        return (count > 0) ? (sum / count) : 0;
    }

    private void checkForFlash(int currentBrightness) {
        long currentTime = System.currentTimeMillis();
        Log.d(TAG, "Яркость в кадре: " + currentBrightness);

        int brightnessChange = currentBrightness - previousBrightness;
        previousBrightness = currentBrightness;

        if (currentBrightness < brightnessThreshold ) {
            if (isFlashOn){
            handleFlashEnd(currentTime, currentBrightness);}
            flash_end_flag =false;
        }


        if (currentBrightness > brightnessThreshold) {
            if (!flash_end_flag && !isFlashOn) {
                handleFlashStart(currentTime);
            }

            if (isFlashOn && !flash_end_flag && (brightnessChange < -BRIGHTNESS_CHANGE_THRESHOLD)) {
                handleFlashEnd(currentTime, currentBrightness);
                flash_end_flag = true;
            }
        }
    }


    private void handleFlashStart(long currentTime) {
        isFlashOn = true;

        if (lastFlashTime == 0) {
            appendToResultText("+  ");
            lastFlashTime = currentTime;
            Log.d(TAG, "Вспышка обнаружена: " + lastFlashTime);
            return;
        }

        lastFlashTime = currentTime;
        if (lastFlashEndTime > 0) {
            long pauseDuration = lastFlashTime - lastFlashEndTime;
            Log.d(TAG, "ПАУЗА. Время: " + pauseDuration + "  Яркость: " + previousBrightness);
            if (pauseDuration < DOT_DURATION_THRESHOLD * COEFFICIENT) {
                appendToResultText("");
            } else if (pauseDuration < DOT_DURATION_THRESHOLD * 3 * COEFFICIENT) {
                appendToResultText("  ");
            } else {
                appendToResultText("  +  ");
            }
        }

        Log.d(TAG, "Вспышка обнаружена: " + lastFlashTime);
        Log.d(TAG, "Тест Морзе: " + resultText);
    }


    private void handleFlashEnd(long currentTime, int currentBrightness) {
        isFlashOn = false;
        lastFlashEndTime = currentTime;
        long flashDuration = lastFlashEndTime - lastFlashTime;

        if (flashDuration < DOT_DURATION_THRESHOLD / COEFFICIENT ) {
            appendToResultText(".");
        } else {
            appendToResultText("-");
        }

        Log.d(TAG, "Время вспышки: " + flashDuration + "  Яркость в конце: " + currentBrightness);
        Log.d(TAG, "Тест Морзе: " + resultText);

    }

    private void appendToResultText(String text) {
        resultText.append(text);
        if (brightnessListener != null) {
            brightnessListener.onTextUpdated(resultText.toString());
        }
    }

    public void setBrightnessThreshold(int threshold) {
        this.brightnessThreshold = threshold;
        Log.d(TAG, "Установлена яркость: " + threshold);
    }


    public int getBrightnessThreshold() {
        return brightnessThreshold;
    }

    public void updateBrightnessView(int brightness)
    {
        if (brightnessListener != null) {
            brightnessListener.onBrightnessChanged(brightness);
        }
    }

    public void reset() {

        resultText.setLength(0);
        lastFlashTime = 0;
        lastFlashEndTime = 0;
        isFlashOn = false;
        flash_end_flag = false;

        Log.d(TAG, "Состояние детектора сброшено");
    }
}