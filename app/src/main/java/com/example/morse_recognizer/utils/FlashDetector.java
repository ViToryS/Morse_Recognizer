package com.example.morse_recognizer.utils;

import android.graphics.Rect;
import android.media.Image;
import android.util.Log;
import static com.example.morse_recognizer.morse.MorseConstants.*;

import com.example.morse_recognizer.morse.MorseConstants;

import java.nio.ByteBuffer;

public class FlashDetector {
    public interface BrightnessListener {
        void onBrightnessChanged(int brightness);
        void onTextUpdated(String text);
        void onFpsTooLow(float fps);
        void onTooShortFlashDetected(int brightness);
    }
    private int previousBrightness = 0;
    private static final String TAG = "FlashDetector";
    private int brightnessThreshold = 130;
    private long lastFlashTime = 0;
    private long lastFlashEndTime = 0;
    private boolean isFlashOn = false;
    private BrightnessListener brightnessListener;
    private boolean flash_end_flag = false;
    private Rect areaToProcess = null;
    long currentTime;
    private long lastFrameTimestamp = 0;
    private static final float FPS_THRESHOLD = 5f;
    private static final long LOW_FPS_DURATION_MS = 5000;

    private long lowFpsStartTime = 0;
    private boolean hasWarnedAboutFps = false;
    private final StringBuilder resultText = new StringBuilder();

    public void setBrightnessListener(BrightnessListener listener) {
        this.brightnessListener = listener;
    }

    public void setAreaToProcess(Rect area) {
        this.areaToProcess = area;
    }
    public void processImage(Image image, boolean isRecognising) {

        long currentTimestamp = System.currentTimeMillis();

        if (lastFrameTimestamp > 0) {
            long delta = currentTimestamp - lastFrameTimestamp;
            if (delta > 0) {
                float measuredFps = 1000f / delta;
                Log.d(TAG, "Измеренный FPS: " + measuredFps);
                onFpsMeasured(measuredFps);
            }
        }
        lastFrameTimestamp = currentTimestamp;


        int width = image.getWidth();
        int height = image.getHeight();
        int avgBrightness = calculateAverageBrightness(image, width, height);

        if (isRecognising){
        checkForFlash(avgBrightness);}

        updateBrightnessView(avgBrightness);
    }

    private int calculateAverageBrightness(Image image, int width, int height) {
        int sum = 0;
        int count = 0;

        Rect rect = (areaToProcess != null) ? areaToProcess : new Rect(0, 0, width, height);

        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();

        byte[] yData = new byte[buffer.remaining()];
        buffer.get(yData);

        for (int y = rect.top; y < rect.bottom; y++) {
            for (int x = rect.left; x < rect.right; x++) {

                int rotatedY = width - 1 - x;

                int index = rotatedY * rowStride + y * pixelStride;
                if (index >= 0 && index < yData.length) {
                    int luminance = yData[index] & 0xFF;
                    sum += luminance;
                    count++;
                }
            }
        }

        return (count > 0) ? (sum / count) : 0;
    }



    private void checkForFlash(int currentBrightness) {
        long previousCurrentTime = currentTime;
        currentTime = System.currentTimeMillis();
        Log.d(TAG, "Яркость в кадре: " + currentBrightness + "Время: " + currentTime );
        previousBrightness = currentBrightness;

        if (currentBrightness < brightnessThreshold ) {
            if (isFlashOn){
            handleFlashEnd(previousCurrentTime, currentBrightness);}
            flash_end_flag =false;
        }

        if (currentBrightness > brightnessThreshold) {
            if (!flash_end_flag && !isFlashOn) {
                handleFlashStart(currentTime);
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
            if (pauseDuration < MorseConstants.getDotDuration() * COEFFICIENT) {
                appendToResultText("");
            } else if (pauseDuration < MorseConstants.getDotDuration() * 3 * COEFFICIENT) {
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

        long flashDuration = currentTime - lastFlashTime;

        if (flashDuration < MorseConstants.getDotDuration()*0.25){
            brightnessListener.onTooShortFlashDetected(brightnessThreshold);
            return;}
        lastFlashEndTime = currentTime;
        if (flashDuration < MorseConstants.getDotDuration()*COEFFICIENT) {
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
    public void onFpsMeasured(float fps) {
        long now = System.currentTimeMillis();

        if (fps < (FPS_THRESHOLD*Math.pow((9 - MorseConstants.getDotDuration()/ 100f), 0.6))) {
            if (lowFpsStartTime == 0) {
                lowFpsStartTime = now;
            } else if (now - lowFpsStartTime >= LOW_FPS_DURATION_MS && !hasWarnedAboutFps) {
                hasWarnedAboutFps = true;
                brightnessListener.onFpsTooLow(fps);
            }
        } else {
            lowFpsStartTime = 0;
            hasWarnedAboutFps = false;
        }
    }
}