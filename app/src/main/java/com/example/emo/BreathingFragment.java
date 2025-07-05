package com.example.emo;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.emo.databinding.FragmentBreathingBinding;

import java.util.ArrayList;
import java.util.List;

public class BreathingFragment extends Fragment {

    private FragmentBreathingBinding binding;
    private LinearLayout breathingContainer;
    private List<Technique> breathingTechniques;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentBreathingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        breathingContainer = binding.breathingContainer;
        
        // Инициализация данных
        initializeBreathingTechniques();
        
        // Отображение методик
        displayBreathingTechniques();
    }
    
    private void initializeBreathingTechniques() {
        breathingTechniques = new ArrayList<>();
        
        // Добавляем дыхательные техники
        breathingTechniques.add(
            new Technique(
                "Упражнение «Отдых»",
                "Помогает расслабить шею и плечи через наклоны, снимает напряжение.",
                "Исходное положение — стоя, выпрямиться, поставить ноги на ширину плеч. Вдох. На выдохе наклониться, расслабив шею и плечи так, чтобы голова и руки свободно свисали к полу. Дышать глубоко, следить за своим дыханием. Находиться в таком положении в течение 1–2 минут. Затем медленно выпрямиться."
            )
        );
        
        breathingTechniques.add(
            new Technique(
                "Упражнение «Передышка»",
                "Техника для ситуаций, когда мы расстроены и сдерживаем дыхание.",
                "Обычно, когда мы бываем чем-то расстроены, мы начинаем сдерживать дыхание. Высвобождение дыхания — один из способов расслабления. В течение трех минут дышите медленно, спокойно и глубоко. Можете даже закрыть глаза. Наслаждайтесь этим глубоким неторопливым дыханием, представьте, что все ваши неприятности улетучиваются."
            )
        );
        
        breathingTechniques.add(
            new Technique(
                "Упражнение «Успокоение»",
                "Техника дыхания с счетом и задержкой для достижения состояния спокойствия.",
                "Сидя или стоя постарайтесь по возможности расслабить мышцы тела и сосредоточьте внимание на дыхании. На счет 1-2-3-4 делайте медленный глубокий вдох (при этом живот выпячивается вперед, а грудная клетка неподвижна);\n— на следующие четыре счета проводится задержка дыхания;\n— затем плавный вдох на счет 1-2-3-4-5-6;\n— снова задержка перед следующим вдохом на счет 1-2-3-4.\nУже через 3–5 минут такого дыхания вы заметите, что ваше состояние стало заметно спокойней и уравновешенней."
            )
        );
        
        breathingTechniques.add(
            new Technique(
                "Упражнение «Пушинка»",
                "Техника для развития плавного и контролируемого дыхания.",
                "Представьте, что перед вашим носом на расстоянии 10–12 см висит пушинка. Дышите только носом и так плавно, чтобы пушинка не колыхалась."
            )
        );
        
        breathingTechniques.add(
            new Technique(
                "Упражнение «Вдох с задержкой дыхания»",
                "Техника для ситуаций раздражения или гнева, когда мы забываем делать нормальный вдох.",
                "Поскольку в ситуации раздражения, гнева мы забываем делать нормальный вдох:\n— глубоко вдохните;\n— задержите дыхание так долго, как сможете;\n— сделайте несколько глубоких вдохов;\n— снова задержите дыхание;\n— выдох."
            )
        );
        
        breathingTechniques.add(
            new Technique(
                "Упражнение «Дыхание при стрессе»",
                "Ритмическое дыхание, сочетаемое с ходьбой для снятия стресса.",
                "Для этого рекомендуется использовать ритмическое полное дыхание. Лучше всего его сочетать с ритмичной неторопливой ходьбой, которая и задает дыхательный ритм. Вначале придерживайтесь ритма два шага вдох, два шага выдох. Затем увеличивайте продолжительность выдоха: два шага - вдох, три шага - выдох."
            )
        );
        
        breathingTechniques.add(
            new Technique(
                "Упражнение «Естественное дыхание»",
                "Методика восстановления естественного типа дыхания с использованием диафрагмы.",
                "Для восстановления естественного типа дыхания необходимо помнить, что диафрагма — самая сильная мышца для вдоха, а брюшной пресс (живот) — для выдоха. Если эти мышцы функционируют правильно, то при вдохе живот немного выпячивается, диафрагма уплотняется и нажимает на внутренности, таким образом массируя их. А при выдохе живот уменьшается (сокращаются мышцы брюшного пресса, и диафрагма принимает куполообразное положение). Диафрагмальное дыхание лучше всего проводить лежа на спине, согнув колени, однако этому виду дыхания необходимо научиться и в положении сидя."
            )
        );
        
        breathingTechniques.add(
            new Technique(
                "Упражнение «Осознанное дыхание»",
                "Техника постепенного углубления дыхания с медленным выдохом.",
                "Сядьте удобно, расслабьтесь, спину держите прямо. Делайте неглубокий вдох, выдох, затем второй — глубже, выдох, и третий — полной грудью, после чего выдыхайте медленно, чтобы выдох по времени был равен трем вдохам."
            )
        );
        
        // Добавляем технику дыхания 4-7-8
        breathingTechniques.add(
            new Technique(
                "Техника дыхания 4-7-8",
                "Мощная техника для быстрого снятия стресса, тревоги и напряжения. Помогает уснуть и улучшает контроль эмоций.",
                "Техника дыхания 4-7-8 (по методу доктора Эндрю Вейла)\n\nЭта техника является мощным инструментом для быстрого снятия стресса и тревоги. Она также очень эффективна для тех, кто страдает бессонницей.\n\nКак выполнять:\n\n1. Сядьте удобно с прямой спиной.\n2. Положите кончик языка к нёбу за передними верхними зубами (и держите его там на протяжении всего упражнения).\n3. Полностью выдохните через рот, создавая тихий свистящий звук.\n4. Закройте рот и спокойно вдохните через нос, считая до 4.\n5. Задержите дыхание, считая до 7.\n6. Сделайте полный выдох через рот со свистящим звуком, считая до 8.\n7. Это один цикл. Повторите цикл еще 3 раза (всего 4 цикла).\n\nЭффекты от регулярной практики:\n• Снижение уровня стресса и тревоги\n• Улучшение качества сна\n• Повышение устойчивости к стрессу\n• Снижение кровяного давления\n• Улучшение контроля эмоций\n\nВажно: Сначала упражнение выполняйте не более 4 циклов подряд. С опытом вы можете увеличить количество циклов, но начинайте именно с четырех. Выполняйте технику дважды в день, со временем вы заметите значительные улучшения в своем эмоциональном состоянии."
            )
        );
        
        // Добавляем коробочное дыхание
        breathingTechniques.add(
            new Technique(
                "Коробочное дыхание",
                "Техника, используемая военными и спецназом для сохранения спокойствия в стрессовых ситуациях.",
                "Коробочное дыхание (квадратное дыхание)\n\nЭта техника также известна как «дыхание по квадрату» и широко используется военными, спецназом и профессиональными спортсменами для быстрого восстановления спокойствия и концентрации в стрессовых ситуациях.\n\nКак выполнять:\n\n1. Сядьте в удобное положение с прямой спиной.\n2. Выдохните полностью весь воздух из легких.\n3. Вдохните через нос, медленно считая до 4, наполняя легкие воздухом.\n4. Задержите дыхание, считая до 4.\n5. Медленно выдыхайте через рот, считая до 4.\n6. Задержите дыхание с пустыми легкими, считая до 4.\n7. Повторите цикл 4-5 раз или больше, пока не почувствуете себя спокойнее.\n\nПреимущества коробочного дыхания:\n• Быстрое снижение стресса и тревоги\n• Повышение концентрации и ясности ума\n• Регуляция нервной системы\n• Улучшение кислородного обмена\n• Снижение кровяного давления\n• Улучшение контроля эмоций\n\nКоробочное дыхание особенно полезно применять перед важными событиями, публичными выступлениями, во время напряженных ситуаций или при появлении симптомов тревоги. Регулярная практика этой техники помогает развить большую устойчивость к стрессу."
            )
        );
    }
    
    private void displayBreathingTechniques() {
        // Очищаем контейнер
        breathingContainer.removeAllViews();
        
        // Добавляем карточки для каждой дыхательной техники
        for (Technique technique : breathingTechniques) {
            addTechniqueCard(technique);
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
                detailsView.setVisibility(View.GONE);
            } else {
                detailsView.setVisibility(View.VISIBLE);
            }
        });
        
        // Добавляем карточку в контейнер
        breathingContainer.addView(cardView);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // Внутренний класс для хранения данных о технике
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