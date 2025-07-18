package com.example.emo;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.emo.databinding.FragmentRelaxationBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelaxationFragment extends Fragment {

    private FragmentRelaxationBinding binding;
    private LinearLayout relaxationContainer;
    private Map<String, List<Technique>> techniqueCategories;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentRelaxationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        relaxationContainer = binding.relaxationContainer;
        
        // Инициализация данных
        initializeTechniques();
        
        // Отображение методик
        displayTechniqueCategories();
    }
    
    private void initializeTechniques() {
        // Создаем категории методик
        techniqueCategories = new HashMap<>();
        techniqueCategories.put("Релаксационные техники", new ArrayList<>());
        
        // Упражнение "Быстрая релаксация"
        techniqueCategories.get("Релаксационные техники").add(
            new Technique(
                "Упражнение «Быстрая релаксация»",
                "Техника для быстрого снятия физического и психологического напряжения, помогает восстановить энергию.",
                "1. Лягте поудобнее, руки вдоль тела, закройте глаза и ничего не предпринимайте. Просто лежите.\n\n2. Подумайте о чем-нибудь приятном. Можете вспомнить что-либо или вообразить. Например, можете представить себе, что лежите на лугу возле ручья; день прекрасный, журчит ручей, пахнет травами.\n\n3. Если вам вспомнится или представится что-нибудь неприятное, о чем вам не хотелось бы думать, просто не реагируйте на это.\n\n4. Вызовите в себе ощущения, которые предшествуют вашему погружению в сон: тяжесть в руках, в ногах, чувство общей расслабленности. Мысленно и расслабленно (без напряжения) сосредоточьте на этом внимание. Через некоторое время вы почувствуете, что испытываете нечто подобное.\n\n5. Представьте, как ощущение расслабленности и покоя распространяется на все ваше тело.\n\n6. Почувствуйте, как с каждым последующим выдохом расслабленность и покой становятся все более ощутимыми.\n\n7. Если вам действительно будет хорошо, вы скажете себе, что это именно то, чего вы желали. Так и лежите, раз вам приятно.\n\n8. Можете просто отдыхать или потешить себя какими-нибудь приятными фантазиями. Представьте себе, что ваша душа как бы отправилась на прогулку по тем местам, где вы чувствуете себя хорошо.\n\n9. Продолжайте лежать до тех пор, пока вам это нравится, доставляет удовольствие.\n\n10. Не торопитесь с окончанием. Тело само подскажет оптимальный темп. Можно потереть глаза или потянуться, как после пробуждения. Сядьте лишь после того, как вам этого действительно захочется.\n\n11. Отследите свое состояние.\n\nЭта техника особенно эффективна для быстрого восстановления после стрессовых ситуаций или умственного переутомления. Даже 10-15 минут такой практики позволяют значительно улучшить самочувствие и повысить продуктивность."
            )
        );
        
        // Прогрессивная мышечная релаксация по Джекобсону
        techniqueCategories.get("Релаксационные техники").add(
            new Technique(
                "Прогрессивная мышечная релаксация по Джекобсону",
                "Метод осознанного напряжения и расслабления различных групп мышц для снятия физических зажимов.",
                "Прогрессивная мышечная релаксация по Джекобсону\n\nЭта техника была разработана американским физиологом Эдмундом Джекобсоном в 1920-х годах. Основной принцип заключается в том, что после сильного напряжения мышцы естественным образом расслабляются глубже. Техника помогает научиться распознавать и контролировать мышечное напряжение.\n\nКак выполнять:\n\n1. Займите удобное положение лежа или сидя.\n2. Начните с глубокого дыхания — сделайте 5-6 медленных вдохов и выдохов.\n3. Последовательно напрягайте и расслабляйте различные группы мышц. Каждую группу напрягайте на 5-7 секунд, затем расслабляйте на 20-30 секунд, концентрируясь на ощущении расслабления.\n\nПоследовательность мышечных групп:\n• Кисти рук — сожмите кулаки\n• Предплечья и плечи — согните руки в локтях и напрягите бицепсы\n• Плечи — поднимите к ушам\n• Верхняя часть спины — сведите лопатки вместе\n• Шея — осторожно откиньте голову назад\n• Лицо — напрягите лоб, сожмите челюсти, зажмурьте глаза\n• Грудь и живот — глубоко вдохните и напрягите мышцы\n• Ягодицы — напрягите ягодичные мышцы\n• Бедра — напрягите мышцы бедер\n• Голени — вытяните носки на себя\n• Стопы — подогните пальцы ног\n\nПосле завершения упражнения оставайтесь в состоянии расслабления еще 1-2 минуты, наслаждаясь ощущением покоя во всем теле.\n\nРегулярная практика (15-20 минут в день) помогает:\n• Уменьшить хроническое мышечное напряжение\n• Снизить уровень стресса и тревоги\n• Улучшить качество сна\n• Снизить интенсивность головных болей напряжения\n• Увеличить осознанность ощущений в теле\n• Улучшить контроль над физическими проявлениями эмоций\n\nЭту технику можно практиковать как полную последовательность перед сном или использовать частично в течение дня для быстрого снятия напряжения в конкретных группах мышц (например, расслабление плеч и шеи во время работы за компьютером)."
            )
        );
        
        // Техника визуализации "Безопасное место"
        techniqueCategories.get("Релаксационные техники").add(
            new Technique(
                "Визуализация «Безопасное место»",
                "Мощная техника для снятия тревоги и стресса через создание мысленного образа места, вызывающего чувство безопасности и комфорта.",
                "Визуализация «Безопасное место»\n\nВизуализация — это техника создания мысленных образов, которая активно используется в психотерапии, спортивной психологии и медитативных практиках. Техника «Безопасное место» помогает создать ментальное убежище, где вы можете почувствовать себя полностью защищенным и расслабленным.\n\nКак выполнять:\n\n1. Займите удобное положение, закройте глаза и сделайте несколько глубоких вдохов.\n\n2. Представьте место, где вы чувствуете себя абсолютно безопасно и комфортно. Это может быть реальное место из вашего опыта или воображаемое. Например, пляж, лес, горы, уютная комната, сад и т.д.\n\n3. Постепенно добавляйте детали к вашему образу:\n   • Что вы видите вокруг? Обратите внимание на цвета, формы, освещение.\n   • Какие звуки присутствуют? Шум волн, пение птиц, тишина...\n   • Какие запахи вы ощущаете? Морской бриз, аромат цветов, запах леса...\n   • Что вы чувствуете физически? Тепло солнца, прохладный ветерок, мягкость песка под ногами...\n   • Какие вкусы могут присутствовать? Возможно, свежие фрукты или прохладная вода...\n\n4. Полностью погрузитесь в этот образ, позвольте себе ощутить безопасность, покой и расслабление.\n\n5. Когда вы будете готовы вернуться, медленно сосчитайте от 5 до 1, на каждом счете ощущая, как возвращаетесь в настоящий момент.\n\n6. Откройте глаза, сохраняя чувство спокойствия и безопасности.\n\nПреимущества практики:\n• Быстрое снижение тревоги и стресса\n• Создание психологического ресурса для сложных ситуаций\n• Улучшение эмоциональной саморегуляции\n• Помощь при панических атаках\n• Улучшение сна при практике перед отходом ко сну\n\nВы можете записать описание вашего безопасного места или создать аудиозапись инструкций для себя, чтобы в моменты стресса быстрее погружаться в это состояние. С практикой простое воспоминание о вашем безопасном месте может вызывать ощущение спокойствия и защищенности даже в стрессовых ситуациях."
            )
        );
    }
    
    private void displayTechniqueCategories() {
        // Очищаем контейнер
        relaxationContainer.removeAllViews();
        
        // Создаем карточки для каждой категории методик
        for (Map.Entry<String, List<Technique>> category : techniqueCategories.entrySet()) {
            // Добавляем карточки для каждой методики в категории
            for (Technique technique : category.getValue()) {
                addTechniqueCard(technique);
            }
        }
    }
    
    private void addTechniqueCard(Technique technique) {
        // Создаем карточку
        CardView cardView = new CardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 8, 0, 8);
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(8);
        cardView.setRadius(16);
        cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.card_background));
        
        // Контейнер для содержимого карточки
        LinearLayout cardContent = new LinearLayout(getContext());
        cardContent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(24, 24, 24, 24);
        
        // Заголовок методики
        TextView titleView = new TextView(getContext());
        titleView.setText(technique.getTitle());
        titleView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(null, Typeface.BOLD);
        cardContent.addView(titleView);
        
        // Описание методики
        TextView descriptionView = new TextView(getContext());
        descriptionView.setText(technique.getShortDescription());
        descriptionView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color));
        descriptionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        descriptionView.setPadding(0, 8, 0, 0);
        cardContent.addView(descriptionView);
        
        // Детальное описание методики (изначально скрыто)
        TextView detailsView = new TextView(getContext());
        detailsView.setText(technique.getDetailedDescription());
        detailsView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color));
        detailsView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        detailsView.setPadding(0, 16, 0, 0);
        detailsView.setVisibility(View.GONE);
        cardContent.addView(detailsView);
        
        // Добавляем содержимое в карточку
        cardView.addView(cardContent);
        
        // Добавляем обработчик нажатия для разворачивания/сворачивания
        cardView.setOnClickListener(v -> {
            if (detailsView.getVisibility() == View.VISIBLE) {
                Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
                detailsView.startAnimation(slideUp);
                slideUp.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        detailsView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
            } else {
                detailsView.setVisibility(View.VISIBLE);
                Animation slideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
                detailsView.startAnimation(slideDown);
            }
        });
        
        // Добавляем карточку в контейнер
        relaxationContainer.addView(cardView);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // Класс для хранения информации о методике или технике релаксации
    private static class Technique {
        private final String title;
        private final String shortDescription;
        private final String detailedDescription;
        
        public Technique(String title, String shortDescription, String detailedDescription) {
            this.title = title;
            this.shortDescription = shortDescription;
            this.detailedDescription = detailedDescription;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getShortDescription() {
            return shortDescription;
        }
        
        public String getDetailedDescription() {
            return detailedDescription;
        }
    }
} 