package com.example.morse_recognizer.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.example.morse_recognizer.R;
import com.example.morse_recognizer.utils.FlashlightController;
import com.example.morse_recognizer.utils.MorseCodeConverter;


public class SendingFragment extends Fragment implements FlashlightController.TransmissionListener {
    private EditText inputField;
    private TextView resultField;
    private FlashlightController flashlightController;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Animation scaleAnimation;
    private MorseCodeConverter.Language currentLanguage;
    ImageButton sendButton;
    TextView btnLanguage;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startMorseTransmission();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.need_permission),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sending, container, false);
        inputField = view.findViewById(R.id.inputField);
        sendButton = view.findViewById(R.id.sendButton);
        btnLanguage = view.findViewById(R.id.btnLanguage);
        flashlightController = new FlashlightController(requireContext());
        flashlightController.setTransmissionListener(this);
        scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_animation);
        resultField = view.findViewById(R.id.translatedResultField);

        currentLanguage = MorseCodeConverter.getCurrentLanguage();
        updateLanguageButton();

        btnLanguage.setOnClickListener(v -> {
            currentLanguage = MorseCodeConverter.Language.getNext(currentLanguage);
            MorseCodeConverter.setLanguage(currentLanguage);
            updateLanguageButton();
            updateMorseTranslation();
        });

        inputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateMorseTranslation();}
        });

        sendButton.setOnClickListener(v -> {
            if (flashlightController.isTransmissionRunning()) {
                stopMorseTransmission();}
            else{
                if (hasCameraPermission()){
                startMorseTransmission();
                } else {
                requestCameraPermission();
                }
            }});
        return view;
    }
    private void updateLanguageButton() {
        btnLanguage.setText(currentLanguage.getButtonText());}

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);

    }
    private void startMorseTransmission() {
        String text = inputField.getText().toString().toUpperCase();
        if (!text.isEmpty()){
            String morseCode = MorseCodeConverter.convertToMorse(text);
            flashlightController.startMorseTransmission(morseCode);
            updateButtonState(true);
        }
    }
    private void stopMorseTransmission() {
        flashlightController.stopTransmission();
        updateButtonState(false);
    }

    private void updateButtonState(boolean isTransmitting) {
        sendButton.setSelected(isTransmitting);
        if (isTransmitting) {
            sendButton.startAnimation(scaleAnimation);
        } else {
            sendButton.clearAnimation();
        }
}

    @Override
    public void onTransmissionStopped() {
        updateButtonState(false);
    }

    private void updateMorseTranslation() {
        String text = inputField.getText().toString().toUpperCase();
        String morseCode = MorseCodeConverter.convertToMorse(text);
        resultField.setText(morseCode);
    }
}
