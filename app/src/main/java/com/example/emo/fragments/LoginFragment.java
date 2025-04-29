package com.example.emo.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.emo.AuthActivity;
import com.example.emo.MainActivity;
import com.example.emo.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";
    
    private EditText emailEt, passwordEt;
    private TextView forgotPasswordTv;
    private Button loginBtn;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        
        emailEt = view.findViewById(R.id.email_et);
        passwordEt = view.findViewById(R.id.password_et);
        forgotPasswordTv = view.findViewById(R.id.forgot_password_tv);
        loginBtn = view.findViewById(R.id.login_btn);
        progressBar = requireActivity().findViewById(R.id.progressBar);
        
        setupInputFilters();
        setupLoginButton();
        setupForgotPassword();
        
        return view;
    }
    
    private void setupInputFilters() {
        // Устанавливаем фильтр для всех полей EditText, чтобы запретить перенос строки
        InputFilter noNewLineFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                // Проверяем каждый вводимый символ
                for (int i = start; i < end; i++) {
                    if (source.charAt(i) == '\n') {
                        // Если найден символ переноса строки, возвращаем пустую строку (игнорируем его)
                        return "";
                    }
                }
                return null; // Разрешаем ввод
            }
        };

        // Применяем фильтр ко всем полям
        emailEt.setFilters(new InputFilter[]{noNewLineFilter});
        passwordEt.setFilters(new InputFilter[]{noNewLineFilter});
    }

    private void setupLoginButton() {
        // Добавляем обработчик для кнопки "Готово" на клавиатуре (для поля пароля)
        passwordEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginBtn.performClick(); // Симулируем клик по кнопке входа
                return true;
            }
            return false;
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEt.getText().toString().trim();
                String password = passwordEt.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), "Поля не могут быть пустыми", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(requireContext(), "Введите корректный email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(requireContext(), "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                loginBtn.setEnabled(false);

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                loginBtn.setEnabled(true);
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Вход успешен, переход в MainActivity");
                                    startActivity(new Intent(requireActivity(), MainActivity.class));
                                    requireActivity().finish();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            loginBtn.setEnabled(true);
                            Log.e(TAG, "Ошибка входа: " + e.getMessage());
                            Toast.makeText(requireContext(), "Ошибка входа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });
    }

    private void setupForgotPassword() {
        forgotPasswordTv.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Введите ваш email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Введите корректный email", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Письмо для сброса пароля отправлено");
                            Toast.makeText(requireContext(), "Письмо для сброса пароля отправлено", Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "Ошибка отправки письма: " + task.getException().getMessage());
                            Toast.makeText(requireContext(), "Ошибка отправки письма: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
} 