package com.example.morse_recognizer.utils;
import android.content.Context;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class TextToSpeechHelper implements TextToSpeech.OnInitListener {
    private final TextToSpeech tts;
    private boolean isReady = false;
    private final TTSListener listener;

    private Runnable onStartRunnable;
    private Runnable onDoneRunnable;

    public interface TTSListener {
        void onSpeechStart();

        void onSpeechDone();

        void onSpeechError(String error);
    }

    public TextToSpeechHelper(Context context, TTSListener listener) {
        this.tts = new TextToSpeech(context, this);
        this.listener = listener;

        this.tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (listener != null) listener.onSpeechStart();
                if (onStartRunnable != null) onStartRunnable.run();
            }

            @Override
            public void onDone(String utteranceId) {
                if (listener != null) listener.onSpeechDone();
                if (onDoneRunnable != null) onDoneRunnable.run();
            }

            @Override
            public void onError(String utteranceId) {
                if (listener != null) listener.onSpeechError("TTS error");
                Log.e("TTS", "Error during speech synthesis.");
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = this.tts.setLanguage(new Locale("ru", "RU"));
            isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED;
        } else {
            Log.e("TTS", "TTS не создан");
            if (listener != null) listener.onSpeechError("TTS не создан");
        }
    }

    public void speakText(String text, Runnable onStart, Runnable onDone) {
        if (isReady && text != null) {
            this.onStartRunnable = onStart;
            this.onDoneRunnable = onDone;

            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");
        } else {
            Log.e("TTSHelper", "TTS не инициирован или текст пустой.");
            if (listener != null) listener.onSpeechError("TTS не готов или текст пустой");
        }
    }


    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public void setLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
            tts.setLanguage(locale);
        }
    }
}