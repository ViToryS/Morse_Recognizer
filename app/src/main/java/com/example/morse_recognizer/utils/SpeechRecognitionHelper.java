package com.example.morse_recognizer.utils;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

public class SpeechRecognitionHelper {
    public interface SpeechResultListener {
        void onResult(String text);
        void onError(String message);
        void onDone();
    }

    private final SpeechRecognizer speechRecognizer;
    private SpeechResultListener listener;

    public SpeechRecognitionHelper(Context context) {
        this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        setupRecognizer();
    }

    private void setupRecognizer() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float roodB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {
                if (listener != null) {
                    listener.onDone();
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}

            @Override
            public void onError(int error) {
                if (listener != null) {
                    listener.onDone();
                    listener.onError("Ошибка распознавания речи");
                }
            }

            @Override
            public void onResults(Bundle results) {
                if (listener != null) {
                    listener.onDone();
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        listener.onResult(matches.get(0));
                    } else {
                        listener.onError("Ничего не распознано");
                    }
                }
            }
        });
    }

    public void setListener(SpeechResultListener listener) {
        this.listener = listener;
    }

    public void startListening(String languageCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode);
        speechRecognizer.startListening(intent);
    }

    public void destroy() {
        speechRecognizer.destroy();
    }
}
