package com.example.emo.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.emo.MainActivity;
import com.example.emo.R;
import com.example.emo.TermsActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterFragment extends Fragment {
    private static final String TAG = "RegisterFragment";
    private boolean isRegistering = false;
    
    private EditText usernameEt, emailEt, passwordEt, confirmPasswordEt;
    private CheckBox termsCheckbox;
    private TextView termsText;
    private Button signUpBtn;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        
        usernameEt = view.findViewById(R.id.username_et);
        emailEt = view.findViewById(R.id.email_et);
        passwordEt = view.findViewById(R.id.password_et);
        confirmPasswordEt = view.findViewById(R.id.confirm_password_et);
        termsCheckbox = view.findViewById(R.id.terms_checkbox);
        termsText = view.findViewById(R.id.terms_text);
        signUpBtn = view.findViewById(R.id.sign_up_btn);
        progressBar = requireActivity().findViewById(R.id.progressBar);
        
        setupTermsText();
        setupInputFilters();
        setupRegisterButton();
        
        return view;
    }
    
    private void setupTermsText() {
        // Настройка кликабельного текста "Пользовательское соглашение"
        if (termsText == null) {
            Log.e(TAG, "terms_text не найден в макете!");
            Toast.makeText(requireContext(), "Ошибка: terms_text не найден", Toast.LENGTH_LONG).show();
            return;
        }

        // Используем SpannableString для кликабельной ссылки
        String fullText = getString(R.string.terms_accept).replaceAll("<[^>]+>", ""); // Удаляем HTML-теги
        SpannableString spannableString = new SpannableString(fullText);

        String linkText = "Пользовательского соглашения";
        int start = fullText.indexOf(linkText);
        int end = start + linkText.length();

        if (start == -1) {
            Log.e(TAG, "Текст 'Пользовательского соглашения' не найден в строке: " + fullText);
            Toast.makeText(requireContext(), "Ошибка отображения условий", Toast.LENGTH_SHORT).show();
            return;
        }

        // Делаем текст кликабельным
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                try {
                    Log.d(TAG, "Попытка открыть TermsActivity");
                    startActivity(new Intent(requireActivity(), TermsActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при открытии TermsActivity: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Не удалось открыть соглашение: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Добавляем подчеркивание
        spannableString.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Изменяем цвет ссылки
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.blue)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        termsText.setText(spannableString);
        termsText.setMovementMethod(LinkMovementMethod.getInstance());
        termsText.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void setupInputFilters() {
        // Устанавливаем фильтр для всех полей EditText
        InputFilter noNewLineFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (source.charAt(i) == '\n') {
                        return "";
                    }
                }
                return null;
            }
        };

        emailEt.setFilters(new InputFilter[]{noNewLineFilter});
        passwordEt.setFilters(new InputFilter[]{noNewLineFilter});
        confirmPasswordEt.setFilters(new InputFilter[]{noNewLineFilter});
        usernameEt.setFilters(new InputFilter[]{noNewLineFilter});
    }

    private void setupRegisterButton() {
        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRegistering) {
                    Log.d(TAG, "Регистрация уже выполняется, игнорируем повторный клик");
                    return;
                }

                String email = emailEt.getText().toString().trim();
                String password = passwordEt.getText().toString().trim();
                String confirmPassword = confirmPasswordEt.getText().toString().trim();
                String username = usernameEt.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || username.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(requireContext(), "Поля не могут быть пустыми", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(requireContext(), "Введите корректный email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(requireContext(), " Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (username.length() < 3) {
                    Toast.makeText(requireContext(), "Имя пользователя должно содержать минимум 3 символа", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isNetworkAvailable()) {
                    Toast.makeText(requireContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!termsCheckbox.isChecked()) {
                    Toast.makeText(requireContext(), "Вы должны принять условия Пользовательского соглашения", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "Начало процесса регистрации");
                isRegistering = true;
                progressBar.setVisibility(View.VISIBLE);
                signUpBtn.setEnabled(false);

                Handler handler = new Handler(Looper.getMainLooper());
                Runnable timeoutRunnable = () -> {
                    Log.w(TAG, "Тайм-аут: запрос к Firebase не завершился вовремя");
                    isRegistering = false;
                    progressBar.setVisibility(View.GONE);
                    signUpBtn.setEnabled(true);
                    Toast.makeText(requireContext(), "Время ожидания истекло. Проверьте подключение и попробуйте снова.", Toast.LENGTH_LONG).show();
                };
                handler.postDelayed(timeoutRunnable, 10000);

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                handler.removeCallbacks(timeoutRunnable);
                                Log.d(TAG, "createUserWithEmailAndPassword completed: " + task.isSuccessful());
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Регистрация успешна, сохранение данных в FirebaseDatabase");
                                    HashMap<String, Object> userInfo = new HashMap<>(); // Изменяем тип на Object для гибкости
                                    userInfo.put("email", email);
                                    userInfo.put("username", username);
                                    userInfo.put("termsAccepted", termsCheckbox.isChecked()); // Добавляем состояние чекбокса

                                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    Log.d(TAG, "User ID: " + userId);

                                    DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
                                    usersRef.child(userId).setValue(userInfo).addOnCompleteListener(databaseTask -> {
                                        isRegistering = false;
                                        progressBar.setVisibility(View.GONE);
                                        signUpBtn.setEnabled(true);

                                        if (databaseTask.isSuccessful()) {
                                            Log.d(TAG, "Данные пользователя успешно сохранены");
                                            Toast.makeText(requireContext(), "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(requireActivity(), MainActivity.class));
                                            requireActivity().finish();
                                        } else {
                                            Log.e(TAG, "Ошибка при сохранении данных пользователя: " + databaseTask.getException().getMessage());
                                            Toast.makeText(requireContext(), "Ошибка при сохранении данных: " + databaseTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                } else {
                                    isRegistering = false;
                                    progressBar.setVisibility(View.GONE);
                                    signUpBtn.setEnabled(true);
                                    Log.e(TAG, "Ошибка регистрации: " + task.getException().getMessage());
                                    Toast.makeText(requireContext(), "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
} 