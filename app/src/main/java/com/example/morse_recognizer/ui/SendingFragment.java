package com.example.morse_recognizer.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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
import com.example.morse_recognizer.morse.MorseCodeConverter;
import com.example.morse_recognizer.viewmodel.MorseViewModel;
import com.example.morse_recognizer.utils.SpeechRecognitionHelper;


public class SendingFragment extends Fragment implements FlashlightController.TransmissionListener {

    private FlashlightController flashlightController;
    private SpeechRecognitionHelper speechHelper;
    private MorseCodeConverter.Language currentLanguage;
    private MorseViewModel viewModel;
    private EditText inputField;
    private TextView resultField;
    ImageButton sendButton;
    TextView btnLanguage;

    private Animation scaleAnimation;

    private ActivityResultLauncher<String> audioPermissionLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        flashlightController = new FlashlightController(requireContext());
        speechHelper = new SpeechRecognitionHelper(requireContext());
        viewModel = new ViewModelProvider(this).get(MorseViewModel.class);
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
        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startSpeechRecognition();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.need_audio_permission),
                                Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sending, container, false);

        inputField = view.findViewById(R.id.inputField);
        resultField = view.findViewById(R.id.translatedResultField);

        sendButton = view.findViewById(R.id.sendButton);
        btnLanguage = view.findViewById(R.id.btnLanguage);
        ImageButton voiceInputButton = view.findViewById(R.id.voiceInputButton);

        scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_animation);

        currentLanguage = MorseCodeConverter.getCurrentLanguage();
        updateLanguageButton();

        flashlightController.setTransmissionListener(this);
        btnLanguage.setOnClickListener(v -> {
            currentLanguage = MorseCodeConverter.Language.getNext(currentLanguage);
            MorseCodeConverter.setLanguage(currentLanguage);
            updateLanguageButton();
            updateMorseTranslation();
        });

        voiceInputButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                voiceInputButton.setActivated(true);
                voiceInputButton.startAnimation(scaleAnimation);
                startSpeechRecognition();
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
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

        speechHelper.setListener(new SpeechRecognitionHelper.SpeechResultListener() {

            @Override
            public void onResult(String text) {
                inputField.setText(text);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onDone() {
                voiceInputButton.setActivated(false);
                voiceInputButton.clearAnimation();
            }
        });
        btnLanguage.setOnClickListener(v -> {
            viewModel.switchToNextLanguage();
            viewModel.updateInputText(inputField.getText().toString());
        });
        viewModel.getLanguageButtonText().observe(getViewLifecycleOwner(), text -> btnLanguage.setText(text));

        viewModel.getLanguageButtonText().observe(getViewLifecycleOwner(), btnLanguage::setText);
        viewModel.getTranslatedText().observe(getViewLifecycleOwner(), translated -> resultField.setText(translated));
        viewModel.getMorseText().observe(getViewLifecycleOwner(), text -> inputField.setText(text));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (speechHelper != null) {
            speechHelper.destroy();
        }
    }

    private void updateLanguageButton() {
        btnLanguage.setText(currentLanguage.getButtonText());}

    private void startSpeechRecognition() {
        String langCode = MorseCodeConverter.getCurrentLanguage().getTtsCode();
        speechHelper.startListening(langCode);
    }
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
