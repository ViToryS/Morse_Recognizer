package com.example.morse_recognizer.ui;

import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.example.morse_recognizer.utils.RecognizingViewModel;

import com.example.morse_recognizer.utils.FlashDetector;
import com.example.morse_recognizer.utils.MorseDecoder;

public class RecognizingFragment extends Fragment {

    private TextureView textureView;
    private ImageView placeholderImage;
    private ImageButton btnRecognize;
    private CameraHelper cameraHelper;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private RecognizingViewModel viewModel;


    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RecognizingViewModel.class);
        cameraHelper = new CameraHelper();


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
        placeholderImage = view.findViewById(R.id.placeholderImage);

        textureView.setOnClickListener(v -> toggleCamera());

        return view;
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
            startBackgroundThread(); // Запустить поток, если он не активен
        }
        cameraHelper.startCamera(requireContext(), textureView, backgroundHandler);
    }

    private void closeCamera() {
        cameraHelper.closeCamera();
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
        stopBackgroundThread(); // Остановить фоновый поток
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        closeCamera();
        stopBackgroundThread(); // Остановить фоновый поток
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
}