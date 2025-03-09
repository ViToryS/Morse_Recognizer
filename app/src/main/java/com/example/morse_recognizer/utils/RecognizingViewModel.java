package com.example.morse_recognizer.utils;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RecognizingViewModel extends ViewModel {
    private MutableLiveData<Boolean> isCameraOn = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsCameraOn() {
        return isCameraOn;
    }

    public void toggleCamera() {
        isCameraOn.setValue(!isCameraOn.getValue());
    }
}