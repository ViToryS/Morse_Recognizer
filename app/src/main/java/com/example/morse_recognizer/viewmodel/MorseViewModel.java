package com.example.morse_recognizer.viewmodel;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.morse_recognizer.morse.MorseCodeConverter;
import com.example.morse_recognizer.morse.MorseConstants;

public class MorseViewModel extends ViewModel {
    private final MutableLiveData<Integer> dotDuration = new MutableLiveData<>((int)
            MorseConstants.getDotDuration());
    private final MutableLiveData<Boolean> isSendingMorse = new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> isCameraOn = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isRecognizing = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> brightness = new MutableLiveData<>(0);

    private final MutableLiveData<MorseCodeConverter.Language> currentLanguage =
            new MutableLiveData<>(MorseCodeConverter.getCurrentLanguage());

    private final MutableLiveData<String> languageButtonText = new MutableLiveData<>(
            MorseCodeConverter.getCurrentLanguage().getButtonText());

    private final MutableLiveData<String> sendingText = new MutableLiveData<>("");
    private final MutableLiveData<String> sendingMorseText = new MutableLiveData<>("");
    private final MutableLiveData<String> recognizingText = new MutableLiveData<>("");
    private final MutableLiveData<String> recognizingMorseText = new MutableLiveData<>("");

    public void updateSendingText(String text) {
        sendingText.postValue(text);
        sendingMorseText.postValue(MorseCodeConverter.convertToMorse(text));
    }

    public void updateRecognizingText(String morseText) {
        recognizingMorseText.postValue(morseText);
        recognizingText.postValue(MorseCodeConverter.convertFromMorse(morseText));
    }
    public LiveData<String> getSendingText() {
        return sendingText;
    }

    public LiveData<String> getSendingMorseText() {
        return sendingMorseText;
    }

    public LiveData<String> getRecognizingText() {
        return recognizingText;
    }

    public LiveData<String> getRecognizingMorseText() {
        return recognizingMorseText;
    }
    public LiveData<Integer> getDotDuration() {
        return dotDuration;
    }

    public void setDotDuration(int duration) {
        int normalizedValue = Math.max(100, Math.min(800, duration));
        normalizedValue = (normalizedValue / 100) * 100;

        dotDuration.setValue(normalizedValue);
        MorseConstants.setDotDuration(normalizedValue);
    }
    public LiveData<Boolean> getIsSendingMorse() {
        return isSendingMorse;
    }

    public void setIsSendingMorse(boolean isSending) {
        isSendingMorse.postValue(isSending);
    }

    public LiveData<Boolean> getIsCameraOn() {
        return isCameraOn;
    }

    public void toggleCamera() {
        Boolean current = isCameraOn.getValue();
        isCameraOn.setValue(current == null || !current);
    }

    public LiveData<Boolean> getIsRecognizing() {
        return isRecognizing;
    }
    public void setRecognizing(boolean value) {
        isRecognizing.setValue(value);
    }

    public void setIsCameraOn(boolean isOn) {
        isCameraOn.setValue(isOn);
    }
    public LiveData<Integer> getBrightness() {
        return brightness;
    }
    public void updateBrightness(int value) {
        brightness.postValue(value);
    }

    public LiveData<String> getLanguageButtonText() {
        return languageButtonText;
    }

    public void switchToNextLanguage() {
        MorseCodeConverter.Language current = currentLanguage.getValue();
        assert current != null;
        MorseCodeConverter.Language next = MorseCodeConverter.Language.getNext(current);
        MorseCodeConverter.setLanguage(next);

        currentLanguage.setValue(next);
        languageButtonText.setValue(next.getButtonText());

        String currentRecognizingMorse = recognizingMorseText.getValue();
        if (currentRecognizingMorse != null && !currentRecognizingMorse.isEmpty()) {
            recognizingText.setValue(MorseCodeConverter.convertFromMorse(currentRecognizingMorse));
        }

        String currentSendingText = sendingText.getValue();
        if (currentSendingText != null && !currentSendingText.isEmpty()) {
            sendingMorseText.setValue(MorseCodeConverter.convertToMorse(currentSendingText));
        }
    }



}