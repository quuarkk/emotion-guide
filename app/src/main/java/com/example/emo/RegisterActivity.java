package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.ImageButton;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.emo.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ImageButton backButton = findViewById(R.id.back_btn);

        // Устанавливаем обработчик клика
        backButton.setOnClickListener(v -> {
            finish(); // Закрывает текущую активность и возвращает на предыдущий экран
        });

        binding.signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.emailEt.getText().toString().isEmpty() || binding.passwordEt.getText().toString().isEmpty()
                        || binding.usernameEt.getText().toString().isEmpty()){
                    Toast.makeText(getApplicationContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                }else{
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(binding.emailEt.getText().toString(), binding.passwordEt.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()){
                                        HashMap<String, String> userInfo = new HashMap<>();
                                        userInfo.put("email", binding.emailEt.getText().toString());
                                        userInfo.put("username", binding.usernameEt.getText().toString());
                                        userInfo.put("profileImage", "");
                                        userInfo.put("chats", "");

                                        FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                .setValue(userInfo);

                                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                    }
                                }
                            });

                }
            }
        });
    }
}