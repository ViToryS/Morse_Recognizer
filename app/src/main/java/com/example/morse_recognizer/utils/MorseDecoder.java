package com.example.morse_recognizer.utils;

import android.util.Log;

public class MorseDecoder {
    private static final String TAG = "MorseDecoder";

    public void processFlash(long startTime, long endTime) {
        long duration = endTime - startTime;
        if (duration < 200) {
            Log.d(TAG, "Короткая вспышка (точка)");
        } else {
            Log.d(TAG, "Длинная вспышка (тире)");
        }
    }
}