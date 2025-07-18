package com.example.emo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.emo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AboutFragment extends Fragment {

    private TextInputEditText nameInput;
    private TextInputEditText messageInput;
    private TextInputEditText contactInput;
    private MaterialButton submitButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        nameInput = view.findViewById(R.id.nameInput);
        messageInput = view.findViewById(R.id.messageInput);
        contactInput = view.findViewById(R.id.contactInput);
        submitButton = view.findViewById(R.id.submitButton);

        submitButton.setOnClickListener(v -> submitForm());

        return view;
    }

    private void submitForm() {
        String name = nameInput.getText() != null ? nameInput.getText().toString() : "";
        String message = messageInput.getText() != null ? messageInput.getText().toString() : "";
        String contact = contactInput.getText() != null ? contactInput.getText().toString() : "";

        if (message.trim().isEmpty()) {
            messageInput.setError("Пожалуйста, введите ваше сообщение");
            return;
        }

        // TODO: Здесь будет логика отправки формы на сервер
        // Пока просто показываем сообщение об успехе
        Toast.makeText(getContext(), "Сообщение отправлено", Toast.LENGTH_SHORT).show();
        
        // Очищаем поля
        nameInput.setText("");
        messageInput.setText("");
        contactInput.setText("");
    }
} 