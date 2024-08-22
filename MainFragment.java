package com.example.cameraq;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cameraq.databinding.FragmentMainBinding;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Set up CardView click listeners
        binding.cardFaceDetector.setOnClickListener(v -> navigateToCameraFragment("face_detector"));
        binding.cardImageCapture.setOnClickListener(v -> navigateToCameraFragment("image_capture"));
        binding.cardVideoCapture.setOnClickListener(v -> navigateToCameraFragment("video_capture"));

        return view;
    }

    private void navigateToCameraFragment(String useCase) {
        Bundle bundle = new Bundle();
        bundle.putString("USE_CASE", useCase);
        Navigation.findNavController(requireView()).navigate(R.id.action_mainFragment_to_cameraFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
