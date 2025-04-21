package com.example.morse_recognizer.morse;

public class MorseConstants {
    public static final long DOT_DURATION = 200;
    public static final long DASH_DURATION = DOT_DURATION * 3;
    public static final long SYMBOL_PAUSE = DOT_DURATION;
    public static final long LETTER_PAUSE = DOT_DURATION;
    public static final long WORD_PAUSE = DOT_DURATION * 5;
    public static final double COEFFICIENT = 1.3;
    public static final int BRIGHTNESS_CHANGE_THRESHOLD = 30;
}