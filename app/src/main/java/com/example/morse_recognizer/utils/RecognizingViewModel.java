package com.example.morse_recognizer.utils;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RecognizingViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isCameraOn = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isRecognizing = new MutableLiveData<>(false);
    private final MutableLiveData<String> morseText = new MutableLiveData<>("");
    private final MutableLiveData<String> translatedText = new MutableLiveData<>("");
    private final MutableLiveData<Integer> brightness = new MutableLiveData<>(0);
    private final MutableLiveData<MorseCodeConverter.Language> currentLanguage =
            new MutableLiveData<>(MorseCodeConverter.getCurrentLanguage());
    private final MutableLiveData<String> languageButtonText = new MutableLiveData<>(
            MorseCodeConverter.getCurrentLanguage().getButtonText());
    public LiveData<Boolean> getIsCameraOn() {
        return isCameraOn;
    }

    public LiveData<Boolean> getIsRecognizing() {
        return isRecognizing;
    }

    public LiveData<String> getMorseText() {
        return morseText;
    }

    public LiveData<String> getTranslatedText() {
        return translatedText;
    }

    public LiveData<Integer> getBrightness() {
        return brightness;
    }

    public void toggleCamera() {
        Boolean current = isCameraOn.getValue();
        isCameraOn.setValue(current == null || !current);
    }

    public void setRecognizing(boolean value) {
        isRecognizing.setValue(value);
    }

    public void updateMorseText(String text) {
        morseText.postValue(text);
        // Автоматически обновляем перевод
        translatedText.postValue(MorseCodeConverter.convertFromMorse(text));
    }

    public void clearTexts() {
        morseText.setValue("");
        translatedText.setValue("");
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

        String currentMorse = morseText.getValue();
        if (currentMorse != null && !currentMorse.isEmpty()) {
            translatedText.setValue(MorseCodeConverter.convertFromMorse(currentMorse));
        }
    }
}