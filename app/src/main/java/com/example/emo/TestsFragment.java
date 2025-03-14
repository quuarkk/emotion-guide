package com.example.emo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.emo.databinding.FragmentTestsBinding;

public class TestsFragment extends Fragment {

    private FragmentTestsBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentTestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Здесь будет логика для психологических тестов
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 