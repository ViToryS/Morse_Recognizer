package com.example.morse_recognizer.utils;
import android.content.Context;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class TextToSpeechHelper implements TextToSpeech.OnInitListener {
    private final TextToSpeech tts;
    private boolean isReady = false;
    private TTSListener listener;

    public interface TTSListener {
        void onSpeechStart();
        void onSpeechDone();
        void onSpeechError(String error);
    }

    public TextToSpeechHelper(Context context) {
        tts = new TextToSpeech(context, this);

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (listener != null) listener.onSpeechStart();
            }
            @Override
            public void onDone(String utteranceId) {
                if (listener != null) listener.onSpeechDone();
            }
            @Override
            public void onError(String utteranceId) {
                if (listener != null) listener.onSpeechError("TTS error");
            }
        });
    }

    public void setListener(TTSListener listener) {
        this.listener = listener;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("ru", "RU"));
            isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED;
        }
        else {
            Log.e("TTS", "TTS не создан");
            if (listener != null) listener.onSpeechError("TTS не создан");
        }
    }

    public void speak(String text, String languageCode) {
        if (isReady && text != null) {
            Locale locale = new Locale(languageCode);
            if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(locale);
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");
        }
    }

    public void speakText(String text, Runnable onStart, Runnable onDone) {
        if (isReady && text != null) {
            // Действие перед началом речи
            if (onStart != null) {
                onStart.run();
            }

            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");

            // Обработка завершения речи
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    if (onStart != null) {
                        onStart.run();
                    }
                }

                @Override
                public void onDone(String utteranceId) {
                    if (onDone != null) {
                        onDone.run();
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e("TTS", "Error during speech synthesis.");
                }
            });
        } else {
            Log.e("TTSHelper", "TTS не инициализирован.");
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