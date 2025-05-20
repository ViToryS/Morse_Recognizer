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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
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


        viewModel = new ViewModelProvider(requireActivity()).get(MorseViewModel.class);
        flashlightController = FlashlightController.getInstance(requireContext());
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(requireContext(), getString(R.string.need_permission),
                                Toast.LENGTH_SHORT).show();
                    }
                });
        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
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

        SeekBar intervalSeekBar = view.findViewById(R.id.interval_SeekBar);
        TextView chosenIntervalTextView = view.findViewById(R.id.intervalValueTextView);

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

        viewModel.getDotDuration().observe(getViewLifecycleOwner(), duration -> {
            if (duration != null) {
                int progress = (duration / 100) - 1;
                intervalSeekBar.setProgress(progress);
                chosenIntervalTextView.setText(String.valueOf(duration));
            }
        });
        viewModel.getIsSendingMorse().observe(getViewLifecycleOwner(), isSending -> {
            updateButtonState(isSending);
            intervalSeekBar.setEnabled(!isSending);
            if (isSending) {
                startMorseTransmission();
            } else {
                stopMorseTransmission();
            }
        });
        int initialValue = 100;
        chosenIntervalTextView.setText(String.valueOf(initialValue));

        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int actualValue = (progress + 1) * 100;
                    actualValue = Math.min(actualValue, 800); // Ограничение максимума

                    viewModel.setDotDuration(actualValue);
                    chosenIntervalTextView.setText(String.valueOf(actualValue));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Не требуется
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Не требуется
            }
        });


        inputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            private String previousText = "";

            @Override
            public void afterTextChanged(Editable s) {
                String currentText = s.toString();
                if (!currentText.equals(previousText)) {
                    previousText = currentText;
                    viewModel.updateSendingText(currentText);
                    updateMorseTranslation();
                }
            }
        });

        sendButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                return;
            }
            String text = inputField.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Введите текст", Toast.LENGTH_SHORT).show();
                return;
            }

            Boolean currentState = viewModel.getIsSendingMorse().getValue();
            boolean newState = currentState == null || !currentState;
            viewModel.setIsSendingMorse(newState);
        });


        speechHelper = new SpeechRecognitionHelper(requireContext(), new SpeechRecognitionHelper.SpeechResultListener() {

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
            viewModel.updateSendingText(inputField.getText().toString());
        });
        viewModel.getLanguageButtonText().observe(getViewLifecycleOwner(), text -> btnLanguage.setText(text));

        if (viewModel.getSendingText().getValue() != null) {
            inputField.setText(viewModel.getSendingText().getValue());
        }
        if (viewModel.getSendingMorseText().getValue() != null) {
            resultField.setText(viewModel.getSendingMorseText().getValue());
        }
        viewModel.getSendingText().observe(getViewLifecycleOwner(), text -> {
            if (!inputField.getText().toString().equals(text)) {
                inputField.setText(text);
            }
        });

        viewModel.getSendingMorseText().observe(getViewLifecycleOwner(), morse -> {
            if (!resultField.getText().toString().equals(morse)) {
                resultField.setText(morse);
            }
        });
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

    private void startMorseTransmission() {

        String text = inputField.getText().toString().toUpperCase();
        if (!text.isEmpty()){
            String morseCode = MorseCodeConverter.convertToMorse(text);
            Log.e("sfr", "после запуска starMorseTransmission значение " + viewModel.getIsSendingMorse().getValue());
            flashlightController.startMorseTransmission(morseCode);
        }
    }
    private void stopMorseTransmission() {
        flashlightController.stopTransmission();
    }

    private void updateButtonState(boolean isTransmitting) {
        requireActivity().runOnUiThread(() -> {
            sendButton.setSelected(isTransmitting);
            if (isTransmitting) {
                sendButton.startAnimation(scaleAnimation);
            } else {
                sendButton.clearAnimation();
            }
        });
    }

    @Override
    public void onTransmissionStopped() {
        if (Boolean.TRUE.equals(viewModel.getIsSendingMorse().getValue())){
        viewModel.setIsSendingMorse(false);}
    }

    private void updateMorseTranslation() {
        String text = inputField.getText().toString().toUpperCase();
        String morseCode = MorseCodeConverter.convertToMorse(text);
        resultField.setText(morseCode);
    }
}
