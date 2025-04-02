package com.example.morse_recognizer.ui;

import android.graphics.ImageFormat;
import android.media.Image;
import android.content.pm.PackageManager;
import android.media.ImageReader;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.morse_recognizer.R;
import com.example.morse_recognizer.utils.CameraHelper;
import com.example.morse_recognizer.utils.MorseCodeConverter;
import com.example.morse_recognizer.utils.RecognizingViewModel;
import android.view.animation.AnimationUtils;
import com.example.morse_recognizer.utils.FlashDetector;
import com.example.morse_recognizer.utils.TextToSpeechHelper;

public class RecognizingFragment extends Fragment implements FlashDetector.BrightnessListener{

    private TextureView textureView;
    private TextView resultTextView;
    private TextView translatedresultTextView;
    private ImageView placeholderImage;
    private ImageButton btnRecognize;
    private ImageButton btnSpeak;
    private TextView currentBrightnessTextView;
    private MorseCodeConverter.Language currentLanguage;
    private TextToSpeechHelper ttsHelper;
    private RecognizingViewModel viewModel;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private CameraHelper cameraHelper;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private FlashDetector flashDetector;
    private Animation scaleAnimation;
    private boolean isRecognizing = false;
    TextView btnLanguage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RecognizingViewModel.class);
        cameraHelper = new CameraHelper();
        flashDetector = new FlashDetector();
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(requireContext(), "Для работы с камерой необходимо разрешение", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recognizing, container, false);

        textureView = view.findViewById(R.id.textureView);
        btnRecognize = view.findViewById(R.id.btnRecognize);
        btnLanguage = view.findViewById(R.id.btnLanguage);
        placeholderImage = view.findViewById(R.id.placeholderImage);
        resultTextView = view.findViewById(R.id.resultTextView);
        translatedresultTextView = view.findViewById(R.id.textResultTextView);
        TextView brightnessValueTextView = view.findViewById(R.id.brightnessValueTextView);
        currentBrightnessTextView = view.findViewById(R.id.currentBrightnessTextView);
        SeekBar brightnessSeekBar = view.findViewById(R.id.brightnessSeekBar);
        btnSpeak = view.findViewById(R.id.btnSpeak);
        scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_animation);

        flashDetector.setBrightnessListener(this);
        textureView.setOnClickListener(v -> toggleCamera());
        btnRecognize.setOnClickListener(v -> startRecognition());

        ttsHelper = new TextToSpeechHelper(requireContext());
        ttsHelper.setListener(new TextToSpeechHelper.TTSListener() {
            @Override
            public void onSpeechStart() {
                btnSpeak.setActivated(true);
                btnSpeak.startAnimation(scaleAnimation);
            }
            @Override
            public void onSpeechDone() {
                btnSpeak.setActivated(false);
                btnSpeak.clearAnimation();

            }

            @Override
            public void onSpeechError(String error) {
            }
        });
        currentLanguage = MorseCodeConverter.getCurrentLanguage();
        updateLanguageButton();

        btnSpeak.setOnClickListener(v -> {
            String textToSpeak = translatedresultTextView.getText().toString();
            if (!textToSpeak.isEmpty()) {
                String languageCode = MorseCodeConverter.getCurrentLanguage().getTtsCode();
                ttsHelper.setLanguage(languageCode); // Устанавливаем язык
                ttsHelper.speak(textToSpeak, languageCode);
            }
        });
        btnLanguage.setOnClickListener(v -> {
            currentLanguage = MorseCodeConverter.Language.getNext(currentLanguage);
            MorseCodeConverter.setLanguage(currentLanguage);
            updateLanguageButton();
            showResult();
        });
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                flashDetector.setBrightnessThreshold(progress);
                brightnessValueTextView.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        return view;
    }

    private void startRecognition() {
        if (viewModel.getIsCameraOn().getValue() == null || !viewModel.getIsCameraOn().getValue()) {
            Toast.makeText(requireContext(), "Камера отключена. Включите камеру для распознавания.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (backgroundHandler == null) {
            startBackgroundThread();
            Log.d("Потоки: ", "Фоновый поток запущен после нажатия на кнопку");
        }
        if (!isRecognizing) {
            isRecognizing = true;
            Log.d("Recognizing", "Обработка кадров начата");
            btnRecognize.setActivated(true);
            flashDetector.reset();
            resettingResults();
            btnRecognize.startAnimation(scaleAnimation);
        } else {
            isRecognizing = false;
            Log.d("Recognizing", "Обработка кадров остановлена");
            btnRecognize.setActivated(false);
            btnRecognize.clearAnimation();
            showResult();
        }
    }

    private void stopRecognition() {
        isRecognizing = false;
        Log.d("Recognizing", "Обработка кадров остановлена");
        btnRecognize.setActivated(false);
        btnRecognize.clearAnimation();

    }


    private void toggleCamera() {
        if (viewModel.getIsCameraOn().getValue() == null || !viewModel.getIsCameraOn().getValue()) {
            if (hasCameraPermission()) {
                startCamera();
            } else {
                requestCameraPermission();
            }
        } else {
            closeCamera();
            stopRecognition();
        }
        viewModel.toggleCamera();
        updatePlaceholderVisibility();
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCamera() {
        if (backgroundHandler == null) {
            startBackgroundThread();
        }
        int width = textureView.getWidth();
        int height = textureView.getHeight();
        ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null && isRecognizing) {
                flashDetector.processImage(image, true);

                image.close();
            } else if (image != null) {
                flashDetector.processImage(image, false);
                image.close();
            }
        }, backgroundHandler);

        cameraHelper = new CameraHelper();
        cameraHelper.startCamera(requireContext(), textureView, backgroundHandler, imageReader);

    }

    private void closeCamera() {
        cameraHelper.closeCamera();
        stopRecognition();
        btnRecognize.clearAnimation();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread(); // Запустить фоновый поток
        if (textureView.isAvailable()) {
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopRecognition();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        closeCamera();
        stopBackgroundThread();
        ttsHelper.shutdown();
        super.onDestroyView();
    }

    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("CameraBackground");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
            Log.d("Потоки: ", "Фоновый поток запущен");
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                Log.d("Потоки: ", "Фоновый поток остановлен");
            } catch (InterruptedException e) {
                Log.e("Потоки: ", "Ошибка при остановке фонового потока", e);
            } finally {
                backgroundThread = null;
                backgroundHandler = null;
            }
        }
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Log.d("TextureView", "Поверхность доступна, но камера не запущена");
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        }
    };

    private void updatePlaceholderVisibility() {
        if (viewModel.getIsCameraOn().getValue() != null && viewModel.getIsCameraOn().getValue()) {
            placeholderImage.setVisibility(View.GONE);
            Log.d("камера", "Message: " + "скрыть фото");
        } else {
            placeholderImage.setVisibility(View.VISIBLE);
            Log.d("камера", "Message: " + "показать фото");
        }
    }

    @Override
    public void onBrightnessChanged(int brightness) {
        requireActivity().runOnUiThread(() -> {
            String text = "Текущая яркость: " + brightness;
            currentBrightnessTextView.setText(text);
        });
    }
    @Override
    public void onTextUpdated(String text) {
        requireActivity().runOnUiThread(() -> {
            resultTextView.setText(text);
        });
    }

    public void showResult() {
        requireActivity().runOnUiThread(() -> {
            String translatedText = MorseCodeConverter.
                    convertFromMorse(resultTextView.getText().toString());
            translatedresultTextView.setText(translatedText);
        });
    }


    public void resettingResults() {
        translatedresultTextView.setText("");
        resultTextView.setText("");
    }
    private void updateLanguageButton() {
        btnLanguage.setText(currentLanguage.getButtonText());}
}