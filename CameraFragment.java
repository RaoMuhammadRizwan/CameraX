package com.example.cameraq;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture; // Ensure correct import
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.cameraq.databinding.FragmentCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    private FragmentCameraBinding binding;
    private ExecutorService cameraExecutor;
    private String useCase;
    private VideoCapture videoCapture;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        useCase = getArguments().getString("USE_CASE");
        cameraExecutor = Executors.newSingleThreadExecutor(); // Initialize ExecutorService

        setupUI();
        startCamera();

        binding.backButton.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());

        return view;
    }

    private void setupUI() {
        binding.previewView.setVisibility(View.VISIBLE);
        binding.imageView.setVisibility(View.GONE);
        binding.videoView.setVisibility(View.GONE);
        binding.captureButton.setVisibility(View.GONE);

        switch (useCase) {
            case "face_detector":
                // No additional UI components needed for face detection
                break;

            case "image_capture":
                binding.captureButton.setVisibility(View.VISIBLE);
                binding.captureButton.setOnClickListener(v -> captureImage());
                break;

            case "video_capture":
                binding.captureButton.setVisibility(View.VISIBLE);
                binding.captureButton.setText("Record");
//                binding.captureButton.setOnClickListener(v -> captureVideo());
                break;
        }
    }

//    private void captureVideo() {
//        if (videoCapture != null) {
//            // Check if the videoCapture is already recording
//            if (videoCapture.isRecording()) {
//                // Stop the video recording
//                videoCapture.stopRecording();
//                binding.captureButton.setText("Record");
//            } else {
//                // Start recording
//                File videoFile = new File(requireContext().getExternalFilesDir(null), System.currentTimeMillis() + "_video.mp4");
//                VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture.OutputFileOptions.Builder(videoFile).build();
//
//                videoCapture.getOutput().prepareRecording(requireContext(), outputFileOptions)
//                        .withAudioEnabled() // Optional: enable audio
//                        .start(ContextCompat.getMainExecutor(getContext()), new VideoCapture.OnVideoSavedCallback() {
//                            @Override
//                            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
//                                binding.previewView.setVisibility(View.GONE);
//                                binding.videoView.setVisibility(View.VISIBLE);
//                                binding.videoView.setVideoURI(Uri.fromFile(videoFile));
//                                binding.videoView.start();
//
//                                // Provide feedback to the user
//                                Toast.makeText(getContext(), "Video recorded successfully", Toast.LENGTH_SHORT).show();
//                            }
//
//                            @Override
//                            public void onError(@NonNull VideoCaptureException exception) {
//                                Toast.makeText(getContext(), "Failed to record video: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
//                            }
//                        });
//
//                binding.captureButton.setText("Stop");
//            }
//        }
//    }


    private void captureImage() {
//        if (videoCapture != null) {
//            // Disable video recording if it's active
//            videoCapture.stopRecording();
//        }

        // Ensure the imageCapture instance is properly initialized
        ImageCapture imageCapture = new ImageCapture.Builder().build();

        // Set up the file to save the image
        File photoFile = new File(requireContext().getExternalFilesDir(null), System.currentTimeMillis() + "_photo.jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(getContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // Update UI with the captured image
                binding.previewView.setVisibility(View.GONE);
                binding.imageView.setVisibility(View.VISIBLE);
                binding.imageView.setImageBitmap(BitmapFactory.decodeFile(photoFile.getAbsolutePath()));

                // Optionally, provide feedback to the user
                Toast.makeText(getContext(), "Image captured successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(getContext(), "Failed to capture image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                switch (useCase) {
                    case "face_detector":
                        setupFaceDetection(cameraProvider, preview, cameraSelector);
                        break;

                    case "image_capture":
                        setupImageCapture(cameraProvider, preview, cameraSelector);
                        break;

//                    case "video_capture":
//                        setupVideoCapture(cameraProvider, preview, cameraSelector);
//                        break;
                }

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private void setupFaceDetection(ProcessCameraProvider cameraProvider, Preview preview, CameraSelector cameraSelector) {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build();
            FaceDetector detector = FaceDetection.getClient(options);
            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces.size() > 0) {
                            Toast.makeText(getContext(), "Face detected!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "No face detected.", Toast.LENGTH_SHORT).show();
                        }
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        imageProxy.close();
                    });
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void setupImageCapture(ProcessCameraProvider cameraProvider, Preview preview, CameraSelector cameraSelector) {
        ImageCapture imageCapture = new ImageCapture.Builder().build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

        binding.captureButton.setOnClickListener(v -> {
            File photoFile = new File(requireContext().getExternalFilesDir(null), System.currentTimeMillis() + "_photo.jpg");
            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(getContext()), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    binding.previewView.setVisibility(View.GONE);
                    binding.imageView.setVisibility(View.VISIBLE);
                    binding.imageView.setImageBitmap(BitmapFactory.decodeFile(photoFile.getAbsolutePath()));
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Toast.makeText(getContext(), "Failed to capture image", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

//    private void setupVideoCapture(ProcessCameraProvider cameraProvider, Preview preview, CameraSelector cameraSelector) {
//        // Create a Recorder instance with desired quality settings
//        Recorder recorder = new Recorder.Builder()
//                .setQualitySelector(QualitySelector.from(Quality.HD)) // Set video quality
//                .build();
//
//        // Create a VideoCapture instance with the Recorder
//        videoCapture = VideoCapture.withOutput(recorder);
//
//        cameraProvider.unbindAll();
//        cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);
//
//        binding.captureButton.setOnClickListener(v -> {
//            File videoFile = new File(requireContext().getExternalFilesDir(null), System.currentTimeMillis() + "_video.mp4");
//            VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture.OutputFileOptions.Builder(videoFile).build();
//
//            videoCapture.getOutput().prepareRecording(requireContext(), outputFileOptions)
//                    .withAudioEnabled() // Optional: enable audio
//                    .start(ContextCompat.getMainExecutor(getContext()), new VideoCapture.OnVideoSavedCallback() {
//                        @Override
//                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
//                            binding.previewView.setVisibility(View.GONE);
//                            binding.videoView.setVisibility(View.VISIBLE);
//                            binding.videoView.setVideoURI(Uri.fromFile(videoFile));
//                            binding.videoView.start();
//                        }
//
//                        @Override
//                        public void onError(@NonNull VideoCaptureException exception) {
//                            Toast.makeText(getContext(), "Failed to record video", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//        });
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        binding = null;
    }
}
