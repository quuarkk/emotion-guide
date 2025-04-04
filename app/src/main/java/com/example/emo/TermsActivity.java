package com.example.emo;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class TermsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        // Настройка Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Включаем стрелку возврата
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        TextView termsText = findViewById(R.id.terms_content);
        termsText.setText("Настоящее приложение является трекером ментального здоровья и носит исключительно рекомендательный характер. " +
                "Оно не является медицинским инструментом и не заменяет профессиональную медицинскую помощь. " +
                "Используйте рекомендации на свой страх и риск.\n\n" +
                "Дополнительные условия:\n" +
                "- Мы не несем ответственности за любые последствия, связанные с использованием приложения.\n" +
                "- Все данные, введенные в приложение, могут быть использованы для улучшения работы приложения в соответствии с нашей Политикой конфиденциальности.");
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Закрываем активность и возвращаемся назад
        return true;
    }
}