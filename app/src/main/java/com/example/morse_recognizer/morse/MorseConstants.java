package com.example.morse_recognizer.morse;

public class MorseConstants {
    private static long dotDuration = 200;

    public static long getDotDuration() {
        return dotDuration;
    }

    public static void setDotDuration(long duration) {
        dotDuration = duration;
    }

    public static long getDashDuration() {
        return dotDuration * 3;
    }

    public static long getSymbolPause() {
        return dotDuration;
    }
    public static long getLetterPause() {
        return dotDuration;
    }
    public static long getWordPause() {
        return dotDuration*5;
    }

    public static final double COEFFICIENT = 2;

}