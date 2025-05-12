package com.example.morse_recognizer.ui;

import android.graphics.ImageFormat;
import android.media.Image;
import android.content.pm.PackageManager;
import android.media.ImageReader;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import android.widget.FrameLayout;
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
import com.example.morse_recognizer.morse.MorseCodeConverter;
import com.example.morse_recognizer.ui.components.OverlayView;
import com.example.morse_recognizer.viewmodel.MorseViewModel;
import android.view.animation.AnimationUtils;
import com.example.morse_recognizer.utils.FlashDetector;
import com.example.morse_recognizer.utils.TextToSpeechHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;


public class RecognizingFragment extends Fragment implements FlashDetector.BrightnessListener,
        CameraHelper.ImageProcessingListener, CameraHelper.CameraStateListener{
    private long lastFpsWarningTime = 0;
    private static final long FPS_WARNING_INTERVAL_MS = 10_000;

    private CameraHelper cameraHelper;
    private TextToSpeechHelper ttsHelper;
    private MorseViewModel viewModel;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private FlashDetector flashDetector;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private TextView brightnessValueTextView;
    private TextureView textureView;
    private TextView resultTextView;
    private TextView translatedResultTextView;
    private ImageView placeholderImage;
    private TextView currentBrightnessTextView;
    private ImageButton btnRecognize;
    private ImageButton btnSpeak;
    private TextView btnLanguage;
    private Animation scaleAnimation;
    private SeekBar brightnessSeekBar;

    OverlayView overlayView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MorseViewModel.class);
        flashDetector = new FlashDetector();
        ttsHelper = new TextToSpeechHelper(requireContext(), null);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(requireContext(), "Для работы с камерой необходимо" +
                                " разрешение", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recognizing, container, false);
        overlayView = view.findViewById(R.id.overlayView);
        flashDetector.setAreaToProcess(overlayView.getSelectionRect());
        textureView = view.findViewById(R.id.textureView);
        btnLanguage = view.findViewById(R.id.btnLanguage);
        placeholderImage = view.findViewById(R.id.placeholderImage);
        resultTextView = view.findViewById(R.id.resultTextView);
        translatedResultTextView = view.findViewById(R.id.textResultTextView);
        brightnessValueTextView = view.findViewById(R.id.brightnessValueTextView);
        currentBrightnessTextView = view.findViewById(R.id.currentBrightnessTextView);

        btnRecognize = view.findViewById(R.id.btnRecognize);
        brightnessSeekBar = view.findViewById(R.id.brightnessSeekBar);
        btnSpeak = view.findViewById(R.id.btnSpeak);
        scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_animation);
        setupListeners();
        return view;
    }

    public void setupListeners(){
        placeholderImage.setOnClickListener(v -> toggleCamera());
        textureView.setOnClickListener(v -> toggleCamera());
        btnRecognize.setOnClickListener(v -> startRecognition());
        flashDetector.setBrightnessListener(this);
        btnSpeak.setOnClickListener(v -> {
            String textToSpeak = translatedResultTextView.getText().toString();
            String languageCode = MorseCodeConverter.getCurrentLanguage().getTtsCode();
            ttsHelper.setLanguage(languageCode);
            if (!textToSpeak.isEmpty()) {
                ttsHelper.speakText(textToSpeak,
                        () -> {
                            btnSpeak.setActivated(true);
                            btnSpeak.startAnimation(scaleAnimation);
                        },
                        () -> {
                            btnSpeak.setActivated(false);
                            btnSpeak.clearAnimation();
                        });
            }
        });
        btnLanguage.setOnClickListener(v -> viewModel.switchToNextLanguage());
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
        viewModel.getRecognizingMorseText().observe(getViewLifecycleOwner(), text ->
                resultTextView.setText(text));
        viewModel.getRecognizingText().observe(getViewLifecycleOwner(), translated ->
                translatedResultTextView.setText(translated));
        viewModel.getBrightness().observe(getViewLifecycleOwner(), brightness -> {
            String text = "Текущая яркость: " + brightness;
            currentBrightnessTextView.setText(text);
        });
        viewModel.getLanguageButtonText().observe(getViewLifecycleOwner(), btnLanguage::setText);
        viewModel.getIsRecognizing().observe(getViewLifecycleOwner(), recognizing -> {
            if (recognizing != null && recognizing) {
                btnRecognize.setActivated(true);
                btnRecognize.startAnimation(scaleAnimation);
            } else {
                btnRecognize.setActivated(false);
                btnRecognize.clearAnimation();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (!textureView.isAvailable()) {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        viewModel.setRecognizing(false);
        stopBackgroundThread();
        textureView.setSurfaceTextureListener(null);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        closeCamera();
        stopBackgroundThread();
        ttsHelper.shutdown();
        super.onDestroyView();
    }

    private void startRecognition() {
        if (cameraHelper != null) {
            Boolean recognizing = viewModel.getIsRecognizing().getValue();
            if (recognizing == null || !recognizing) {
                viewModel.setRecognizing(true);
                flashDetector.reset();
                lastFpsWarningTime = System.currentTimeMillis();
            } else {
                viewModel.setRecognizing(false);
            }
        }
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
            viewModel.setRecognizing(false);
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
        if (cameraHelper == null) {
            cameraHelper = new CameraHelper(backgroundHandler, this, this);
        }
        if (textureView.isAvailable()) {
            cameraHelper.startCamera(requireContext(), textureView, createImageReader());
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    private ImageReader createImageReader() {
        int width = textureView.getWidth();
        int height = textureView.getHeight();
        return ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2);
    }

    private void closeCamera() {
        if (cameraHelper != null) {
            cameraHelper.closeCamera();}
        viewModel.setRecognizing(false);
        viewModel.setIsCameraOn(false);
    }

    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("CameraBackground");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                Log.e("Потоки: ", "Ошибка при остановке фонового потока", e);
            } finally {
                backgroundThread = null;
                backgroundHandler = null;
            }
        }
    }

    private void updatePlaceholderVisibility() {
        if (viewModel.getIsCameraOn().getValue() != null && viewModel.getIsCameraOn().getValue()) {
            placeholderImage.setVisibility(View.GONE);
        } else {
            placeholderImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBrightnessChanged(int brightness) {
        viewModel.updateBrightness(brightness);
    }

    @Override
    public void onTextUpdated(String text) {
        viewModel.updateRecognizingText(text);
    }

    @Override
    public void onCameraOpened() {
        Log.d("CameraHelper", "Камера открыта");
    }

    @Override
    public void onCameraError(String error) {
        Log.e("CameraHelper", "Ошибка: " + error);
    }

    @Override
    public void onImageProcessed(Image image) {
        Boolean recognizing = viewModel.getIsRecognizing().getValue();
        flashDetector.setAreaToProcess(overlayView.getSelectionRect());
        flashDetector.processImage(image, recognizing != null && recognizing);
    }

    private final TextureView.SurfaceTextureListener textureListener =
            new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width,
                                              int height)
        {startCamera();}

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width,
                                                int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
    };

    private void showCustomBottomSheet(String message) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                R.style.TransparentBottomSheet);

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.custom_bottom_sheet, new FrameLayout(requireContext()),
                        false);

        TextView messageText = view.findViewById(R.id.messageText);
        messageText.setText(message);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        dialog.show();
        new Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 2000);
    }

    @Override
    public void onFpsTooLow(float fps) {
        long now = System.currentTimeMillis();
        if (now - lastFpsWarningTime > FPS_WARNING_INTERVAL_MS) {
            lastFpsWarningTime = now;
            requireActivity().runOnUiThread(() -> showCustomBottomSheet
                    ("Число кадров в секунду: "
                    + (int)fps +
                    "\nУвеличьте интервал Морзе для корректной работы."));
        }
    }

    @Override
    public void onTooShortFlashDetected(int brightness){
        int newBrightness =brightness+3;
        viewModel.updateBrightness(newBrightness);
        brightnessSeekBar.setProgress(newBrightness);
        requireActivity().runOnUiThread(() -> showCustomBottomSheet(
                "Порог яркости изменен с: "+brightness+" до " + newBrightness));
    }
}