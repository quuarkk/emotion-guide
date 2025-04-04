package com.example.emo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.emo.databinding.FragmentTestsBinding;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestsFragment extends Fragment {

    private FragmentTestsBinding binding;
    private Map<String, List<Test>> testCategories;
    private LinearLayout testsContainer;
    private Test currentTest;
    private int currentQuestionIndex = 0;
    private List<Integer> userAnswers = new ArrayList<>();

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

        testsContainer = binding.testsContainer;
        
        // Инициализация тестов
        initializeTests();
        
        // Отображение категорий тестов
        displayTestCategories();
    }
    
    private void initializeTests() {
        // Создаем категории тестов
        testCategories = new HashMap<>();
        testCategories.put("Эмоциональный интеллект", new ArrayList<>());
        testCategories.put("Стресс", new ArrayList<>());
        testCategories.put("Эмоциональное выгорание", new ArrayList<>());
        testCategories.put("Самооценка", new ArrayList<>());
        testCategories.put("Тревожность", new ArrayList<>());
        testCategories.put("Настроение", new ArrayList<>());
        
        // Тест на эмоциональный интеллект
        Test emotionalIntelligenceTest = new Test(
                "Тест на эмоциональный интеллект",
                "Оценка способности понимать эмоции и управлять ими",
                getEmotionalIntelligenceTestQuestions(),
                answers -> {
                    TestResult result = new TestResult();
                    
                    // Подсчет общего балла
                    int totalScore = 0;
                    for (int answer : answers) {
                        totalScore += answer;
                    }
                    
                    // Преобразуем в шкалу от 0 до 10
                    float eiScore = totalScore / (float) (answers.size() * 4) * 10;
                    
                    // Сохраняем для совместимости со старым кодом
                    result.setWellbeingScore(eiScore);
                    result.setActivityScore(eiScore);
                    result.setMoodScore(eiScore);
                    
                    // Формируем интерпретацию результатов
                    StringBuilder interpretation = new StringBuilder();
                    interpretation.append("РЕЗУЛЬТАТЫ ТЕСТА НА ЭМОЦИОНАЛЬНЫЙ ИНТЕЛЛЕКТ\n\n");
                    
                    interpretation.append("Уровень эмоционального интеллекта: ").append(String.format("%.1f", eiScore)).append(" из 10\n\n");
                    
                    if (eiScore < 4) {
                        interpretation.append("У вас низкий уровень эмоционального интеллекта. Вам может быть сложно распознавать и управлять своими эмоциями, а также понимать эмоции других людей.\n\n");
                        interpretation.append("Низкий эмоциональный интеллект может проявляться в следующем:\n");
                        interpretation.append("• Трудности в распознавании собственных эмоций\n");
                        interpretation.append("• Сложности в управлении эмоциональными реакциями\n");
                        interpretation.append("• Непонимание эмоций и мотивов других людей\n");
                        interpretation.append("• Проблемы в межличностных отношениях\n");
                        interpretation.append("• Трудности в адаптации к изменениям\n\n");
                        
                        interpretation.append("Рекомендации для развития эмоционального интеллекта:\n");
                        interpretation.append("• Ведите дневник эмоций, записывая свои чувства и ситуации, которые их вызвали\n");
                        interpretation.append("• Развивайте навыки осознанности и самонаблюдения через медитацию\n");
                        interpretation.append("• Изучайте литературу по эмоциональному интеллекту\n");
                        interpretation.append("• Практикуйте активное слушание в общении с другими\n");
                        interpretation.append("• Обратите внимание на невербальные сигналы в общении\n");
                        interpretation.append("• Расширяйте свой эмоциональный словарь\n");
                        interpretation.append("• Рассмотрите возможность участия в тренингах по развитию эмоционального интеллекта\n");
                        interpretation.append("• Обратитесь к психологу для индивидуальной работы\n");
                    } else if (eiScore < 7) {
                        interpretation.append("У вас средний уровень эмоционального интеллекта. Вы обладаете определенными навыками распознавания и управления эмоциями, но есть потенциал для развития.\n\n");
                        interpretation.append("Средний эмоциональный интеллект характеризуется:\n");
                        interpretation.append("• Базовым пониманием собственных эмоций\n");
                        interpretation.append("• Способностью управлять эмоциями в большинстве ситуаций\n");
                        interpretation.append("• Умением распознавать основные эмоции других людей\n");
                        interpretation.append("• Относительно стабильными межличностными отношениями\n");
                        interpretation.append("• Способностью к эмпатии в очевидных ситуациях\n\n");
                        
                        interpretation.append("Рекомендации для дальнейшего развития:\n");
                        interpretation.append("• Продолжайте развивать навыки эмпатии и активного слушания\n");
                        interpretation.append("• Практикуйте техники управления эмоциями в стрессовых ситуациях\n");
                        interpretation.append("• Обращайте больше внимания на невербальные сигналы в общении\n");
                        interpretation.append("• Развивайте навыки конструктивного выражения эмоций\n");
                        interpretation.append("• Учитесь распознавать более тонкие эмоциональные состояния\n");
                        interpretation.append("• Практикуйте рефлексию после эмоционально насыщенных ситуаций\n");
                        interpretation.append("• Развивайте навыки разрешения конфликтов\n");
                    } else {
                        interpretation.append("У вас высокий уровень эмоционального интеллекта. Вы хорошо понимаете свои и чужие эмоции, эффективно управляете ими и используете эту информацию для построения отношений и принятия решений.\n\n");
                        interpretation.append("Высокий эмоциональный интеллект проявляется в следующем:\n");
                        interpretation.append("• Глубокое понимание собственных эмоций и их причин\n");
                        interpretation.append("• Эффективное управление эмоциональными состояниями\n");
                        interpretation.append("• Способность точно распознавать эмоции других людей\n");
                        interpretation.append("• Развитая эмпатия и понимание мотивов поведения\n");
                        interpretation.append("• Успешные межличностные отношения\n");
                        interpretation.append("• Эффективное разрешение конфликтов\n");
                        interpretation.append("• Адаптивность к изменениям\n\n");
                        
                        interpretation.append("Рекомендации для поддержания и дальнейшего развития:\n");
                        interpretation.append("• Делитесь своими знаниями и навыками с другими\n");
                        interpretation.append("• Продолжайте развивать эмоциональный интеллект в сложных ситуациях\n");
                        interpretation.append("• Используйте свои навыки для улучшения отношений и повышения эффективности в работе\n");
                        interpretation.append("• Рассмотрите возможность менторства или коучинга для других\n");
                        interpretation.append("• Изучайте более глубокие аспекты эмоционального интеллекта\n");
                        interpretation.append("• Практикуйте осознанность для поддержания эмоционального баланса\n");
                    }
                    
                    result.setInterpretation(interpretation.toString());
                    
                    return new TestResult[] { result };
                }
        );
        testCategories.get("Эмоциональный интеллект").add(emotionalIntelligenceTest);
        
        // Тест PSM-25 (Шкала психологического стресса)
        Test psm25Test = new Test(
                "Тест PSM-25",
                "Шкала психологического стресса",
                getPSM25Questions(),
                answers -> {
                    TestResult result = new TestResult();
                    
                    // Подсчет общего балла
                    int totalScore = 0;
                    for (int answer : answers) {
                        totalScore += answer;
                    }
                    
                    // Формируем интерпретацию результатов
                    StringBuilder interpretation = new StringBuilder();
                    interpretation.append("РЕЗУЛЬТАТЫ ТЕСТА PSM-25 (ШКАЛА ПСИХОЛОГИЧЕСКОГО СТРЕССА)\n\n");
                    
                    interpretation.append("Общий балл: ").append(totalScore).append("\n\n");
                    
                    // Интерпретация уровня стресса согласно методике PSM-25
                    if (totalScore <= 99) {
                        interpretation.append("Низкий уровень стресса. Состояние психологической адаптированности к рабочим нагрузкам.\n\n");
                        interpretation.append("Рекомендации:\n");
                        interpretation.append("- Поддерживайте текущий баланс между работой и отдыхом\n");
                        interpretation.append("- Продолжайте практиковать эффективные стратегии управления стрессом\n");
                        interpretation.append("- Регулярно занимайтесь физическими упражнениями для поддержания хорошего самочувствия\n");
                    } else if (totalScore <= 154) {
                        interpretation.append("Средний уровень стресса. Умеренный уровень психологического напряжения.\n\n");
                        interpretation.append("Рекомендации:\n");
                        interpretation.append("- Обратите внимание на факторы, вызывающие стресс, и постарайтесь их минимизировать\n");
                        interpretation.append("- Практикуйте техники релаксации (глубокое дыхание, медитация, прогрессивная мышечная релаксация)\n");
                        interpretation.append("- Уделяйте достаточно времени для отдыха и восстановления\n");
                        interpretation.append("- Поддерживайте здоровый образ жизни (сон, питание, физическая активность)\n");
                    } else {
                        interpretation.append("Высокий уровень стресса. Состояние дезадаптации и психологического дискомфорта, требующее применения широкого спектра средств и методов для снижения нервно-психической напряженности.\n\n");
                        interpretation.append("Рекомендации:\n");
                        interpretation.append("- Обратитесь к психологу или психотерапевту для профессиональной помощи\n");
                        interpretation.append("- Пересмотрите свой режим дня и рабочую нагрузку\n");
                        interpretation.append("- Освойте и регулярно практикуйте техники управления стрессом\n");
                        interpretation.append("- Уделяйте особое внимание качеству сна и отдыха\n");
                        interpretation.append("- Обратитесь за поддержкой к близким людям\n");
                        interpretation.append("- Рассмотрите возможность временного снижения рабочей нагрузки\n");
                    }
                    
                    result.setTimestamp(System.currentTimeMillis());
                    result.setInterpretation(interpretation.toString());
                    
                    return new TestResult[] { result };
                }
        );
        testCategories.get("Стресс").add(psm25Test);
        
        // Тест Спилберга-Ханина на тревожность
        Test spielbergerTest = new Test(
                "Тест Спилберга-Ханина",
                "Шкала тревожности Спилберга-Ханина",
                getSpielbergerTestQuestions(),
                answers -> {
                    TestResult result = new TestResult();
                    
                    // Подсчет баллов по шкалам
                    int situationalAnxietyScore = 0;
                    int personalAnxietyScore = 0;
                    
                    // Первые 20 вопросов - ситуативная тревожность
                    for (int i = 0; i < 20; i++) {
                        // Прямые вопросы: 3, 4, 6, 7, 9, 12, 13, 14, 17, 18
                        if (i == 2 || i == 3 || i == 5 || i == 6 || i == 8 || i == 11 || i == 12 || i == 13 || i == 16 || i == 17) {
                            situationalAnxietyScore += answers.get(i);
                        } else {
                            // Обратные вопросы: 1, 2, 5, 8, 10, 11, 15, 16, 19, 20
                            situationalAnxietyScore += (5 - answers.get(i));
                        }
                    }
                    
                    // Следующие 20 вопросов - личностная тревожность
                    for (int i = 20; i < 40; i++) {
                        // Прямые вопросы: 22, 23, 24, 25, 28, 29, 31, 32, 34, 35, 37, 38, 40
                        if (i == 21 || i == 22 || i == 23 || i == 24 || i == 27 || i == 28 || i == 30 || i == 31 || i == 33 || i == 34 || i == 36 || i == 37 || i == 39) {
                            personalAnxietyScore += answers.get(i);
                        } else {
                            // Обратные вопросы: 21, 26, 27, 30, 33, 36, 39
                            personalAnxietyScore += (5 - answers.get(i));
                        }
                    }
                    
                    // Формируем интерпретацию результатов
                    StringBuilder interpretation = new StringBuilder();
                    interpretation.append("РЕЗУЛЬТАТЫ ТЕСТА СПИЛБЕРГА-ХАНИНА НА ТРЕВОЖНОСТЬ\n\n");
                    
                    interpretation.append("Ситуативная тревожность: ").append(situationalAnxietyScore).append(" баллов\n");
                    interpretation.append("Личностная тревожность: ").append(personalAnxietyScore).append(" баллов\n\n");
                    
                    // Интерпретация ситуативной тревожности
                    interpretation.append("Ситуативная тревожность:\n");
                    if (situationalAnxietyScore <= 30) {
                        interpretation.append("Низкий уровень тревожности. Вы спокойны и уравновешены в текущей ситуации.\n\n");
                    } else if (situationalAnxietyScore <= 45) {
                        interpretation.append("Умеренный уровень тревожности. Вы испытываете некоторое беспокойство в текущей ситуации, но оно находится в пределах нормы.\n\n");
                    } else {
                        interpretation.append("Высокий уровень тревожности. Вы испытываете значительное напряжение и беспокойство в текущей ситуации.\n\n");
                    }
                    
                    // Интерпретация личностной тревожности
                    interpretation.append("Личностная тревожность:\n");
                    if (personalAnxietyScore <= 30) {
                        interpretation.append("Низкий уровень тревожности. Вы обычно спокойны и не склонны воспринимать большинство ситуаций как угрожающие.\n\n");
                    } else if (personalAnxietyScore <= 45) {
                        interpretation.append("Умеренный уровень тревожности. У вас средняя склонность к беспокойству в различных ситуациях.\n\n");
                    } else {
                        interpretation.append("Высокий уровень тревожности. Вы склонны воспринимать многие ситуации как угрожающие и реагировать на них состоянием тревоги.\n\n");
                    }
                    
                    // Рекомендации
                    interpretation.append("Рекомендации:\n");
                    
                    if (situationalAnxietyScore > 45) {
                        interpretation.append("Для снижения ситуативной тревожности:\n");
                        interpretation.append("- Практикуйте техники глубокого дыхания и релаксации\n");
                        interpretation.append("- Используйте методы осознанности и медитации\n");
                        interpretation.append("- Анализируйте причины текущего беспокойства и ищите конструктивные решения\n");
                        interpretation.append("- Обратитесь за поддержкой к близким людям\n\n");
                    }
                    
                    if (personalAnxietyScore > 45) {
                        interpretation.append("Для работы с личностной тревожностью:\n");
                        interpretation.append("- Рассмотрите возможность консультации с психологом или психотерапевтом\n");
                        interpretation.append("- Изучите и практикуйте когнитивно-поведенческие техники\n");
                        interpretation.append("- Регулярно занимайтесь физическими упражнениями\n");
                        interpretation.append("- Развивайте навыки управления стрессом\n");
                        interpretation.append("- Обеспечьте достаточный отдых и качественный сон\n");
                    }
                    
                    result.setTimestamp(System.currentTimeMillis());
                    result.setInterpretation(interpretation.toString());
                    
                    return new TestResult[] { result };
                }
        );
        testCategories.get("Тревожность").add(spielbergerTest);
        
        // Тест на эмоциональное выгорание (Бойко)
        Test boykoTest = new Test(
                "Тест на эмоциональное выгорание (Бойко)",
                "Диагностика уровня эмоционального выгорания",
                getBoykoTestQuestions(),
                answers -> {
                    TestResult result = new TestResult();
                    result.setTimestamp(System.currentTimeMillis());
                    
                    // Подсчет баллов по фазам выгорания
                    int tensionPhaseScore = 0;
                    int resistancePhaseScore = 0;
                    int exhaustionPhaseScore = 0;
                    
                    // Фаза "Напряжение"
                    // Симптом "Переживание психотравмирующих обстоятельств" (вопросы 1-7)
                    int traumaticCircumstancesScore = 0;
                    for (int i = 0; i < 7 && i < answers.size(); i++) {
                        traumaticCircumstancesScore += answers.get(i);
                    }
                    tensionPhaseScore += traumaticCircumstancesScore;
                    
                    // Симптом "Неудовлетворенность собой" (вопросы 8-14)
                    int selfDissatisfactionScore = 0;
                    for (int i = 7; i < 14 && i < answers.size(); i++) {
                        selfDissatisfactionScore += answers.get(i);
                    }
                    tensionPhaseScore += selfDissatisfactionScore;
                    
                    // Симптом "Загнанность в клетку" (вопросы 15-21)
                    int cagednessScore = 0;
                    for (int i = 14; i < 21 && i < answers.size(); i++) {
                        cagednessScore += answers.get(i);
                    }
                    tensionPhaseScore += cagednessScore;
                    
                    // Симптом "Тревога и депрессия" (вопросы 22-28)
                    int anxietyDepressionScore = 0;
                    for (int i = 21; i < 28 && i < answers.size(); i++) {
                        anxietyDepressionScore += answers.get(i);
                    }
                    tensionPhaseScore += anxietyDepressionScore;
                    
                    // Фаза "Резистенция"
                    // Симптом "Неадекватное избирательное эмоциональное реагирование" (вопросы 29-35)
                    int inadequateEmotionalResponseScore = 0;
                    for (int i = 28; i < 35 && i < answers.size(); i++) {
                        inadequateEmotionalResponseScore += answers.get(i);
                    }
                    resistancePhaseScore += inadequateEmotionalResponseScore;
                    
                    // Симптом "Эмоционально-нравственная дезориентация" (вопросы 36-42)
                    int emotionalMoralDisorientationScore = 0;
                    for (int i = 35; i < 42 && i < answers.size(); i++) {
                        emotionalMoralDisorientationScore += answers.get(i);
                    }
                    resistancePhaseScore += emotionalMoralDisorientationScore;
                    
                    // Симптом "Расширение сферы экономии эмоций" (вопросы 43-49)
                    int emotionalEconomyScore = 0;
                    for (int i = 42; i < 49 && i < answers.size(); i++) {
                        emotionalEconomyScore += answers.get(i);
                    }
                    resistancePhaseScore += emotionalEconomyScore;
                    
                    // Симптом "Редукция профессиональных обязанностей" (вопросы 50-56)
                    int professionalReductionScore = 0;
                    for (int i = 49; i < 56 && i < answers.size(); i++) {
                        professionalReductionScore += answers.get(i);
                    }
                    resistancePhaseScore += professionalReductionScore;
                    
                    // Фаза "Истощение"
                    // Симптом "Эмоциональный дефицит" (вопросы 57-63)
                    int emotionalDeficitScore = 0;
                    for (int i = 56; i < 63 && i < answers.size(); i++) {
                        emotionalDeficitScore += answers.get(i);
                    }
                    exhaustionPhaseScore += emotionalDeficitScore;
                    
                    // Симптом "Эмоциональная отстраненность" (вопросы 64-70)
                    int emotionalDetachmentScore = 0;
                    for (int i = 63; i < 70 && i < answers.size(); i++) {
                        emotionalDetachmentScore += answers.get(i);
                    }
                    exhaustionPhaseScore += emotionalDetachmentScore;
                    
                    // Симптом "Личностная отстраненность (деперсонализация)" (вопросы 71-77)
                    int personalDetachmentScore = 0;
                    for (int i = 70; i < 77 && i < answers.size(); i++) {
                        personalDetachmentScore += answers.get(i);
                    }
                    exhaustionPhaseScore += personalDetachmentScore;
                    
                    // Симптом "Психосоматические и психовегетативные нарушения" (вопросы 78-84)
                    int psychosomaticDisordersScore = 0;
                    for (int i = 77; i < 84 && i < answers.size(); i++) {
                        psychosomaticDisordersScore += answers.get(i);
                    }
                    exhaustionPhaseScore += psychosomaticDisordersScore;
                    
                    // Общий балл эмоционального выгорания
                    int totalBurnoutScore = tensionPhaseScore + resistancePhaseScore + exhaustionPhaseScore;
                    
                    // Сохраняем для совместимости со старым кодом
                    // Преобразуем в шкалу от 0 до 10 (инвертированную, где 0 - сильное выгорание, 10 - отсутствие выгорания)
                    float normalizedScore = 10 - (totalBurnoutScore / 360f * 10);
                    result.setWellbeingScore(normalizedScore);
                    result.setActivityScore(normalizedScore);
                    result.setMoodScore(normalizedScore);
                    
                    // Формируем интерпретацию результатов
                    StringBuilder interpretation = new StringBuilder();
                    interpretation.append("РЕЗУЛЬТАТЫ ТЕСТА НА ЭМОЦИОНАЛЬНОЕ ВЫГОРАНИЕ (МЕТОДИКА В.В. БОЙКО)\n\n");
                    
                    interpretation.append("Общий уровень эмоционального выгорания: ").append(totalBurnoutScore).append(" баллов\n\n");
                    
                    // Интерпретация общего уровня выгорания
                    if (totalBurnoutScore < 50) {
                        interpretation.append("У вас отсутствует эмоциональное выгорание. Вы эффективно справляетесь с профессиональными стрессами.\n\n");
                    } else if (totalBurnoutScore < 100) {
                        interpretation.append("У вас начальная стадия эмоционального выгорания. Обратите внимание на свое эмоциональное состояние и примите меры профилактики.\n\n");
                    } else if (totalBurnoutScore < 150) {
                        interpretation.append("У вас формирующееся эмоциональное выгорание. Необходимо принять меры по снижению стресса и восстановлению эмоционального равновесия.\n\n");
                    } else {
                        interpretation.append("У вас сформировавшееся эмоциональное выгорание. Рекомендуется обратиться к специалисту для получения профессиональной помощи.\n\n");
                    }
                    
                    // Анализ по фазам
                    interpretation.append("АНАЛИЗ ПО ФАЗАМ ЭМОЦИОНАЛЬНОГО ВЫГОРАНИЯ:\n\n");
                    
                    // Фаза "Напряжение"
                    interpretation.append("1. Фаза \"Напряжение\": ").append(tensionPhaseScore).append(" баллов\n");
                    if (tensionPhaseScore < 37) {
                        interpretation.append("Фаза не сформировалась\n");
                    } else if (tensionPhaseScore < 60) {
                        interpretation.append("Фаза в стадии формирования\n");
                    } else {
                        interpretation.append("Фаза сформировалась\n");
                    }
                    
                    // Симптомы фазы "Напряжение"
                    interpretation.append("   - Переживание психотравмирующих обстоятельств: ").append(traumaticCircumstancesScore).append(" баллов");
                    if (traumaticCircumstancesScore < 10) interpretation.append(" (не сложился)\n");
                    else if (traumaticCircumstancesScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    interpretation.append("   - Неудовлетворенность собой: ").append(selfDissatisfactionScore).append(" баллов");
                    if (selfDissatisfactionScore < 10) interpretation.append(" (не сложился)\n");
                    else if (selfDissatisfactionScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    interpretation.append("   - Загнанность в клетку: ").append(cagednessScore).append(" баллов");
                    if (cagednessScore < 10) interpretation.append(" (не сложился)\n");
                    else if (cagednessScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    interpretation.append("   - Тревога и депрессия: ").append(anxietyDepressionScore).append(" баллов");
                    if (anxietyDepressionScore < 10) interpretation.append(" (не сложился)\n");
                    else if (anxietyDepressionScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    // Фаза "Резистенция"
                    interpretation.append("\n2. Фаза \"Резистенция\": ").append(resistancePhaseScore).append(" баллов\n");
                    if (resistancePhaseScore < 37) {
                        interpretation.append("Фаза не сформировалась\n");
                    } else if (resistancePhaseScore < 60) {
                        interpretation.append("Фаза в стадии формирования\n");
                    } else {
                        interpretation.append("Фаза сформировалась\n");
                    }
                    
                    // Симптомы фазы "Резистенция"
                    interpretation.append("   - Неадекватное избирательное эмоциональное реагирование: ").append(inadequateEmotionalResponseScore).append(" баллов");
                    if (inadequateEmotionalResponseScore < 10) interpretation.append(" (не сложился)\n");
                    else if (inadequateEmotionalResponseScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    interpretation.append("   - Эмоционально-нравственная дезориентация: ").append(emotionalMoralDisorientationScore).append(" баллов");
                    if (emotionalMoralDisorientationScore < 10) interpretation.append(" (не сложился)\n");
                    else if (emotionalMoralDisorientationScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    interpretation.append("   - Расширение сферы экономии эмоций: ").append(emotionalEconomyScore).append(" баллов");
                    if (emotionalEconomyScore < 10) interpretation.append(" (не сложился)\n");
                    else if (emotionalEconomyScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    interpretation.append("   - Редукция профессиональных обязанностей: ").append(professionalReductionScore).append(" баллов");
                    if (professionalReductionScore < 10) interpretation.append(" (не сложился)\n");
                    else if (professionalReductionScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    // Фаза "Истощение"
                    interpretation.append("\n3. Фаза \"Истощение\": ").append(exhaustionPhaseScore).append(" баллов\n");
                    if (exhaustionPhaseScore < 37) {
                        interpretation.append("Фаза не сформировалась\n");
                    } else if (exhaustionPhaseScore < 60) {
                        interpretation.append("Фаза в стадии формирования\n");
                    } else {
                        interpretation.append("Фаза сформировалась\n");
                    }
                    
                    // Симптомы фазы "Истощение"
                    interpretation.append("   - Эмоциональный дефицит: ").append(emotionalDeficitScore).append(" баллов");
                    if (emotionalDeficitScore < 10) interpretation.append(" (не сложился)\n");
                    else if (emotionalDeficitScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    interpretation.append("   - Эмоциональная отстраненность: ").append(emotionalDetachmentScore).append(" баллов");
                    if (emotionalDetachmentScore < 10) interpretation.append(" (не сложился)\n");
                    else if (emotionalDetachmentScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    interpretation.append("   - Личностная отстраненность (деперсонализация): ").append(personalDetachmentScore).append(" баллов");
                    if (personalDetachmentScore < 10) interpretation.append(" (не сложился)\n");
                    else if (personalDetachmentScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    interpretation.append("   - Психосоматические и психовегетативные нарушения: ").append(psychosomaticDisordersScore).append(" баллов");
                    if (psychosomaticDisordersScore < 10) interpretation.append(" (не сложился)\n");
                    else if (psychosomaticDisordersScore < 16) interpretation.append(" (складывающийся симптом)\n");
                    else interpretation.append(" (сложившийся симптом)\n");
                    
                    // Рекомендации
                    interpretation.append("\nРЕКОМЕНДАЦИИ ПО ПРОФИЛАКТИКЕ И ПРЕОДОЛЕНИЮ ЭМОЦИОНАЛЬНОГО ВЫГОРАНИЯ:\n\n");
                    
                    if (totalBurnoutScore < 50) {
                        interpretation.append("Для поддержания эмоционального благополучия:\n");
                        interpretation.append("• Продолжайте поддерживать баланс между работой и отдыхом\n");
                        interpretation.append("• Регулярно практикуйте техники релаксации и управления стрессом\n");
                        interpretation.append("• Уделяйте время физической активности и хобби\n");
                        interpretation.append("• Поддерживайте социальные связи и общение с близкими\n");
                    } else if (totalBurnoutScore < 100) {
                        interpretation.append("Для профилактики развития выгорания:\n");
                        interpretation.append("• Пересмотрите свой режим труда и отдыха\n");
                        interpretation.append("• Выделите время для восстановления эмоциональных ресурсов\n");
                        interpretation.append("• Практикуйте техники релаксации (медитация, глубокое дыхание)\n");
                        interpretation.append("• Обратите внимание на качество сна\n");
                        interpretation.append("• Занимайтесь физическими упражнениями\n");
                        interpretation.append("• Развивайте навыки тайм-менеджмента\n");
                    } else if (totalBurnoutScore < 150) {
                        interpretation.append("Для преодоления формирующегося выгорания:\n");
                        interpretation.append("• Обратитесь к психологу для консультации\n");
                        interpretation.append("• Пересмотрите свои профессиональные цели и приоритеты\n");
                        interpretation.append("• Освойте техники управления стрессом\n");
                        interpretation.append("• Временно снизьте рабочую нагрузку, если это возможно\n");
                        interpretation.append("• Уделите особое внимание физическому здоровью\n");
                        interpretation.append("• Практикуйте осознанность и принятие своих эмоций\n");
                        interpretation.append("• Найдите источники эмоциональной поддержки\n");
                    } else {
                        interpretation.append("Для преодоления сформировавшегося выгорания:\n");
                        interpretation.append("• Обратитесь к психотерапевту или психологу для профессиональной помощи\n");
                        interpretation.append("• Рассмотрите возможность временного отпуска или снижения нагрузки\n");
                        interpretation.append("• Пересмотрите свои жизненные и профессиональные ценности\n");
                        interpretation.append("• Практикуйте регулярные техники восстановления (медитация, релаксация)\n");
                        interpretation.append("• Обеспечьте полноценный отдых и сон\n");
                        interpretation.append("• Обратите внимание на питание и физическую активность\n");
                        interpretation.append("• Найдите новые источники вдохновения и мотивации\n");
                    }
                    
                    result.setInterpretation(interpretation.toString());
                    
                    return new TestResult[] { result };
                }
        );
        testCategories.get("Эмоциональное выгорание").add(boykoTest);
        
        // Тест Маслач
        Test maslachTest = new Test(
                "Тест на профессиональное выгорание (Маслач)",
                "Диагностика профессионального выгорания по методике К. Маслач",
                getMaslachTestQuestions(),
                answers -> {
                    TestResult result = new TestResult();
                    
                    // Подсчет баллов по шкалам
                    int emotionalExhaustionScore = 0;
                    int depersonalizationScore = 0;
                    int personalAccomplishmentScore = 0;
                    
                    // Шкала "Эмоциональное истощение"
                    for (int i = 0; i < 9; i++) {
                        emotionalExhaustionScore += answers.get(i);
                    }
                    
                    // Шкала "Деперсонализация"
                    for (int i = 9; i < 14; i++) {
                        depersonalizationScore += answers.get(i);
                    }
                    
                    // Шкала "Редукция профессиональных достижений"
                    for (int i = 14; i < 22; i++) {
                        personalAccomplishmentScore += answers.get(i);
                    }
                    
                    // Инвертируем шкалу редукции профессиональных достижений
                    personalAccomplishmentScore = 48 - personalAccomplishmentScore;
                    
                    // Формируем интерпретацию результатов
                    StringBuilder interpretation = new StringBuilder();
                    interpretation.append("РЕЗУЛЬТАТЫ ТЕСТА НА ПРОФЕССИОНАЛЬНОЕ ВЫГОРАНИЕ (МЕТОДИКА К. МАСЛАЧ)\n\n");
                    
                    interpretation.append("Эмоциональное истощение: ").append(emotionalExhaustionScore).append(" баллов\n");
                    if (emotionalExhaustionScore <= 15) {
                        interpretation.append("Низкий уровень эмоционального истощения\n");
                    } else if (emotionalExhaustionScore <= 24) {
                        interpretation.append("Средний уровень эмоционального истощения\n");
                    } else {
                        interpretation.append("Высокий уровень эмоционального истощения\n");
                    }
                    
                    interpretation.append("\nДеперсонализация: ").append(depersonalizationScore).append(" баллов\n");
                    if (depersonalizationScore <= 5) {
                        interpretation.append("Низкий уровень деперсонализации\n");
                    } else if (depersonalizationScore <= 10) {
                        interpretation.append("Средний уровень деперсонализации\n");
                    } else {
                        interpretation.append("Высокий уровень деперсонализации\n");
                    }
                    
                    interpretation.append("\nРедукция профессиональных достижений: ").append(personalAccomplishmentScore).append(" баллов\n");
                    if (personalAccomplishmentScore <= 16) {
                        interpretation.append("Низкий уровень редукции профессиональных достижений\n");
                    } else if (personalAccomplishmentScore <= 31) {
                        interpretation.append("Средний уровень редукции профессиональных достижений\n");
                    } else {
                        interpretation.append("Высокий уровень редукции профессиональных достижений\n");
                    }
                    
                    interpretation.append("\nОБЩАЯ ИНТЕРПРЕТАЦИЯ:\n");
                    if (emotionalExhaustionScore > 24 && depersonalizationScore > 10 && personalAccomplishmentScore > 31) {
                        interpretation.append("У вас высокий уровень профессионального выгорания по всем трем компонентам. Это серьезное состояние, требующее профессиональной помощи и значительных изменений в профессиональной деятельности.\n");
                    } else if (emotionalExhaustionScore > 24 || depersonalizationScore > 10 || personalAccomplishmentScore > 31) {
                        interpretation.append("У вас наблюдаются признаки профессионального выгорания по одному или нескольким компонентам. Рекомендуется обратить внимание на свое психологическое состояние и принять меры по его улучшению.\n");
                    } else {
                        interpretation.append("У вас низкий или средний уровень профессионального выгорания. Рекомендуется поддерживать баланс между работой и отдыхом для профилактики выгорания.\n");
                    }
                    
                    interpretation.append("\nРЕКОМЕНДАЦИИ:\n");
                    if (emotionalExhaustionScore > 15) {
                        interpretation.append("Для снижения эмоционального истощения:\n");
                        interpretation.append("- Выделяйте время для полноценного отдыха и восстановления\n");
                        interpretation.append("- Практикуйте техники релаксации и медитации\n");
                        interpretation.append("- Обратитесь за поддержкой к коллегам или близким\n");
                    }
                    
                    if (depersonalizationScore > 5) {
                        interpretation.append("\nДля снижения деперсонализации:\n");
                        interpretation.append("- Развивайте эмпатию и навыки коммуникации\n");
                        interpretation.append("- Участвуйте в групповых мероприятиях и тренингах\n");
                        interpretation.append("- Ищите смысл и ценность в своей работе\n");
                    }
                    
                    if (personalAccomplishmentScore > 16) {
                        interpretation.append("\nДля повышения профессиональной эффективности:\n");
                        interpretation.append("- Ставьте реалистичные цели и отмечайте достижения\n");
                        interpretation.append("- Развивайте профессиональные навыки\n");
                        interpretation.append("- Ищите новые подходы и методы в работе\n");
                    }
                    
                    result.setTimestamp(System.currentTimeMillis());
                    result.setInterpretation(interpretation.toString());
                    
                    return new TestResult[] { result };
                }
        );
        testCategories.get("Эмоциональное выгорание").add(maslachTest);
        
        // Тест на самооценку
        Test selfEsteemTest = new Test(
                "Тест на самооценку",
                "Оценка уровня самооценки",
                getSelfEsteemTestQuestions(),
                answers -> {
                    TestResult result = new TestResult();
                    
                    // Подсчет средней самооценки по всем шкалам
                    int totalScore = 0;
                    for (int answer : answers) {
                        totalScore += answer;
                    }
                    
                    // Преобразуем в шкалу от 0 до 100
                    float selfEsteemScore = totalScore / (float) answers.size() * 25; // Если ответы от 0 до 4, умножаем на 25
                    
                    // Формируем интерпретацию результатов
                    StringBuilder interpretation = new StringBuilder();
                    interpretation.append("РЕЗУЛЬТАТЫ ТЕСТА НА САМООЦЕНКУ (МЕТОДИКА ДЕМБО-РУБИНШТЕЙН)\n\n");
                    
                    interpretation.append("Уровень самооценки: ").append(String.format("%.1f", selfEsteemScore)).append(" из 100\n\n");
                    
                    // Интерпретация уровня самооценки согласно методике Дембо-Рубинштейн
                    if (selfEsteemScore <= 40) {
                        interpretation.append("Заниженная самооценка. Вы склонны недооценивать свои способности, достижения и личностные качества.\n\n");
                        interpretation.append("Заниженная самооценка может быть связана с неуверенностью в себе, повышенной самокритичностью, негативным опытом в прошлом или сравнением себя с другими. Люди с заниженной самооценкой часто испытывают трудности в принятии решений, боятся неудач и избегают новых вызовов.\n\n");
                        interpretation.append("Рекомендации:\n");
                        interpretation.append("- Ведите дневник успехов, записывая даже небольшие достижения\n");
                        interpretation.append("- Практикуйте позитивные утверждения (аффирмации)\n");
                        interpretation.append("- Перестаньте сравнивать себя с другими\n");
                        interpretation.append("- Окружите себя поддерживающими людьми\n");
                        interpretation.append("- Ставьте реалистичные цели и отмечайте прогресс\n");
                        interpretation.append("- Развивайте навыки и таланты в интересующих вас областях\n");
                        interpretation.append("- При необходимости обратитесь к психологу для работы над самооценкой\n");
                    } else if (selfEsteemScore <= 70) {
                        interpretation.append("Адекватная (средняя) самооценка. Вы реалистично оцениваете свои способности, достижения и личностные качества.\n\n");
                        interpretation.append("Адекватная самооценка характеризуется здоровым балансом между уверенностью в себе и самокритичностью. Люди с адекватной самооценкой принимают себя такими, какие они есть, осознают свои сильные и слабые стороны, способны принимать конструктивную критику и учиться на ошибках.\n\n");
                        interpretation.append("Рекомендации:\n");
                        interpretation.append("- Продолжайте развиваться в интересующих вас областях\n");
                        interpretation.append("- Поддерживайте баланс между самопринятием и стремлением к росту\n");
                        interpretation.append("- Регулярно анализируйте свои достижения и ставьте новые цели\n");
                        interpretation.append("- Практикуйте благодарность и осознанность\n");
                        interpretation.append("- Поддерживайте здоровые отношения с окружающими\n");
                    } else {
                        interpretation.append("Завышенная самооценка. Вы склонны переоценивать свои способности, достижения и личностные качества.\n\n");
                        interpretation.append("Завышенная самооценка может проявляться в чрезмерной уверенности в себе, нереалистичных ожиданиях, трудностях в принятии критики и признании своих ошибок. Люди с завышенной самооценкой могут испытывать проблемы в общении, так как часто не учитывают мнения и чувства других людей.\n\n");
                        interpretation.append("Рекомендации:\n");
                        interpretation.append("- Развивайте навыки рефлексии и самоанализа\n");
                        interpretation.append("- Учитесь принимать конструктивную критику\n");
                        interpretation.append("- Практикуйте эмпатию и активное слушание\n");
                        interpretation.append("- Ставьте реалистичные цели и планы\n");
                        interpretation.append("- Признавайте свои ошибки и учитесь на них\n");
                        interpretation.append("- Развивайте навыки сотрудничества и командной работы\n");
                    }
                    
                    result.setTimestamp(System.currentTimeMillis());
                    result.setInterpretation(interpretation.toString());
                    
                    return new TestResult[] { result };
                }
        );
        testCategories.get("Самооценка").add(selfEsteemTest);
        
        // Шкала оценки настроения
        Test moodScaleTest = new Test(
                "Шкала оценки настроения",
                "Оценка текущего эмоционального состояния",
                getMoodScaleTestQuestions(),
                answers -> {
                    TestResult result = new TestResult();
                    result.setTimestamp(System.currentTimeMillis());
                    
                    // Подсчет баллов по категориям настроения
                    int positiveAffectScore = 0;
                    int negativeAffectScore = 0;
                    
                    // Позитивный аффект (вопросы 1, 3, 5, 7, 9)
                    positiveAffectScore += answers.get(0);
                    positiveAffectScore += answers.get(2);
                    positiveAffectScore += answers.get(4);
                    positiveAffectScore += answers.get(6);
                    positiveAffectScore += answers.get(8);
                    
                    // Негативный аффект (вопросы 2, 4, 6, 8, 10)
                    negativeAffectScore += answers.get(1);
                    negativeAffectScore += answers.get(3);
                    negativeAffectScore += answers.get(5);
                    negativeAffectScore += answers.get(7);
                    negativeAffectScore += answers.get(9);
                    
                    // Общий балл настроения (позитивный аффект минус негативный аффект)
                    int moodBalance = positiveAffectScore - negativeAffectScore;
                    
                    // Нормализуем для совместимости со старым кодом
                    float normalizedMoodScore = (moodBalance + 20) / 40f * 10; // Преобразуем в шкалу от 0 до 10
                    result.setWellbeingScore(normalizedMoodScore);
                    result.setActivityScore(normalizedMoodScore);
                    result.setMoodScore(normalizedMoodScore);
                    
                    // Формируем интерпретацию результатов
                    StringBuilder interpretation = new StringBuilder();
                    interpretation.append("РЕЗУЛЬТАТЫ ШКАЛЫ ОЦЕНКИ НАСТРОЕНИЯ\n\n");
                    
                    interpretation.append("Позитивный аффект: ").append(positiveAffectScore).append(" из 20\n");
                    interpretation.append("Негативный аффект: ").append(negativeAffectScore).append(" из 20\n");
                    interpretation.append("Баланс настроения: ").append(moodBalance).append("\n\n");
                    
                    // Интерпретация баланса настроения
                    if (moodBalance > 10) {
                        interpretation.append("У вас очень позитивное настроение. Вы испытываете много положительных эмоций и мало отрицательных.\n\n");
                        interpretation.append("Характеристики вашего текущего эмоционального состояния:\n");
                        interpretation.append("• Высокий уровень энергии и энтузиазма\n");
                        interpretation.append("• Оптимистичный взгляд на жизнь\n");
                        interpretation.append("• Ощущение радости и удовлетворения\n");
                        interpretation.append("• Низкий уровень тревоги и беспокойства\n");
                        interpretation.append("• Высокая мотивация и готовность к действиям\n\n");
                        
                        interpretation.append("Рекомендации:\n");
                        interpretation.append("• Используйте это позитивное состояние для решения сложных задач\n");
                        interpretation.append("• Делитесь своим позитивным настроением с окружающими\n");
                        interpretation.append("• Отмечайте, какие факторы способствуют вашему хорошему настроению\n");
                        interpretation.append("• Практикуйте благодарность для поддержания позитивного настроя\n");
                    } else if (moodBalance > 0) {
                        interpretation.append("У вас умеренно позитивное настроение. Положительные эмоции преобладают над отрицательными.\n\n");
                        interpretation.append("Характеристики вашего текущего эмоционального состояния:\n");
                        interpretation.append("• Достаточный уровень энергии\n");
                        interpretation.append("• В целом позитивный взгляд на ситуации\n");
                        interpretation.append("• Преобладание приятных эмоций\n");
                        interpretation.append("• Умеренный уровень мотивации\n");
                        interpretation.append("• Присутствие некоторых негативных эмоций\n\n");
                        
                        interpretation.append("Рекомендации:\n");
                        interpretation.append("• Обратите внимание на факторы, которые вызывают негативные эмоции\n");
                        interpretation.append("• Практикуйте техники релаксации для снижения стресса\n");
                        interpretation.append("• Уделяйте время приятным занятиям для поддержания позитивного настроя\n");
                        interpretation.append("• Развивайте навыки управления эмоциями\n");
                    } else if (moodBalance > -10) {
                        interpretation.append("У вас умеренно негативное настроение. Отрицательные эмоции преобладают над положительными.\n\n");
                        interpretation.append("Характеристики вашего текущего эмоционального состояния:\n");
                        interpretation.append("• Сниженный уровень энергии\n");
                        interpretation.append("• Тенденция к негативной оценке ситуаций\n");
                        interpretation.append("• Преобладание неприятных эмоций\n");
                        interpretation.append("• Возможное наличие тревоги или раздражительности\n");
                        interpretation.append("• Снижение мотивации\n\n");
                        
                        interpretation.append("Рекомендации:\n");
                        interpretation.append("• Практикуйте техники управления стрессом (глубокое дыхание, медитация)\n");
                        interpretation.append("• Обратите внимание на негативные мысли и попробуйте их переформулировать\n");
                        interpretation.append("• Уделите время физической активности\n");
                        interpretation.append("• Общайтесь с поддерживающими людьми\n");
                        interpretation.append("• Обеспечьте себе достаточный отдых\n");
                        interpretation.append("• Если негативное настроение сохраняется длительное время, рассмотрите возможность консультации специалиста\n");
                    } else {
                        interpretation.append("У вас выраженное негативное настроение. Отрицательные эмоции значительно преобладают над положительными.\n\n");
                        interpretation.append("Характеристики вашего текущего эмоционального состояния:\n");
                        interpretation.append("• Низкий уровень энергии\n");
                        interpretation.append("• Пессимистичный взгляд на ситуации\n");
                        interpretation.append("• Выраженные негативные эмоции (грусть, тревога, раздражение)\n");
                        interpretation.append("• Трудности с концентрацией и принятием решений\n");
                        interpretation.append("• Значительное снижение мотивации\n\n");
                        
                        interpretation.append("Рекомендации:\n");
                        interpretation.append("• Обратитесь к психологу или психотерапевту для профессиональной поддержки\n");
                        interpretation.append("• Практикуйте техники самопомощи при стрессе и тревоге\n");
                        interpretation.append("• Обеспечьте себе достаточный отдых и сон\n");
                        interpretation.append("• Поддерживайте регулярную физическую активность\n");
                        interpretation.append("• Ограничьте потребление новостей и социальных сетей\n");
                        interpretation.append("• Обратитесь за поддержкой к близким людям\n");
                        interpretation.append("• Уделите внимание базовым потребностям (питание, сон, отдых)\n");
                    }
                    
                    // Анализ позитивного аффекта
                    interpretation.append("\nАНАЛИЗ ПОЗИТИВНОГО АФФЕКТА:\n");
                    if (positiveAffectScore < 8) {
                        interpretation.append("Низкий уровень позитивного аффекта может указывать на сниженное настроение, апатию или депрессивные тенденции. Рекомендуется обратить внимание на источники радости и удовольствия в вашей жизни.\n");
                    } else if (positiveAffectScore < 14) {
                        interpretation.append("Средний уровень позитивного аффекта говорит о наличии положительных эмоций, но есть потенциал для их усиления. Обратите внимание на занятия, которые приносят вам радость и удовлетворение.\n");
                    } else {
                        interpretation.append("Высокий уровень позитивного аффекта свидетельствует о хорошем эмоциональном состоянии, энтузиазме и энергичности. Продолжайте заниматься тем, что приносит вам положительные эмоции.\n");
                    }
                    
                    // Анализ негативного аффекта
                    interpretation.append("\nАНАЛИЗ НЕГАТИВНОГО АФФЕКТА:\n");
                    if (negativeAffectScore < 8) {
                        interpretation.append("Низкий уровень негативного аффекта указывает на отсутствие выраженных отрицательных эмоций, что является хорошим показателем эмоционального благополучия.\n");
                    } else if (negativeAffectScore < 14) {
                        interpretation.append("Средний уровень негативного аффекта говорит о наличии некоторых отрицательных эмоций. Обратите внимание на их источники и практикуйте техники управления стрессом.\n");
                    } else {
                        interpretation.append("Высокий уровень негативного аффекта свидетельствует о значительном эмоциональном дискомфорте. Рекомендуется обратить внимание на источники стресса и тревоги, а также рассмотреть возможность консультации специалиста.\n");
                    }
                    
                    result.setInterpretation(interpretation.toString());
                    
                    return new TestResult[] { result };
                }
        );
        testCategories.get("Настроение").add(moodScaleTest);
    }
    
    private List<Question> getStressTestQuestions() {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("Я часто чувствую беспокойство без видимой причины", getStandardAnswers()));
        questions.add(new Question("Мне трудно сосредоточиться на задачах", getStandardAnswers()));
        // ... другие вопросы
        return questions;
    }
    
    private List<Question> createStressTestQuestions() {
        List<Question> questions = new ArrayList<>();
        
        // Вопросы из PDF-файла
        questions.add(new Question("1. Я напряжен и взволнован", createStressAnswerOptions()));
        questions.add(new Question("2. У меня ком в горле, и/или я ощущаю сухость во рту", createStressAnswerOptions()));
        questions.add(new Question("3. Я перегружен работой. Мне совсем не хватает времени", createStressAnswerOptions()));
        questions.add(new Question("4. Я проглатываю пищу или забываю поесть", createStressAnswerOptions()));
        questions.add(new Question("5. Я обдумываю свои идеи снова и снова; я меняю свои планы; мои мысли постоянно повторяются", createStressAnswerOptions()));
        questions.add(new Question("6. Я чувствую себя одиноким, изолированным и непонятым", createStressAnswerOptions()));
        questions.add(new Question("7. Я страдаю от физического недомогания; у меня болит голова, напряжены мышцы шеи, боли в спине, спазмы в желудке", createStressAnswerOptions()));
        questions.add(new Question("8. Я поглощен мыслями, измучен или обеспокоен", createStressAnswerOptions()));
        questions.add(new Question("9. Меня внезапно бросает то в жар, то в холод", createStressAnswerOptions()));
        questions.add(new Question("10. Я забываю о встречах или делах, которые должен сделать или решить", createStressAnswerOptions()));
        questions.add(new Question("11. Я легко могу заплакать", createStressAnswerOptions()));
        questions.add(new Question("12. Я чувствую себя уставшим", createStressAnswerOptions()));
        questions.add(new Question("13. Я крепко стискиваю зубы", createStressAnswerOptions()));
        questions.add(new Question("14. Я неспокоен", createStressAnswerOptions()));
        questions.add(new Question("15. Мне тяжело дышать, и/или у меня внезапно перехватывает дыхание", createStressAnswerOptions()));
        questions.add(new Question("16. Я имею проблемы с пищеварением и с кишечником (боли, колики, расстройства или запоры)", createStressAnswerOptions()));
        questions.add(new Question("17. Я встревожен, обеспокоен или смущен", createStressAnswerOptions()));
        questions.add(new Question("18. Я легко пугаюсь; шум или шорох вызывает испуг", createStressAnswerOptions()));
        questions.add(new Question("19. Мне необходимо более 30 минут для того, чтобы уснуть", createStressAnswerOptions()));
        questions.add(new Question("20. Я сбит с толку; мои мысли спутаны; мне не хватает сосредоточенности, и я не могу сконцентрировать внимание", createStressAnswerOptions()));
        questions.add(new Question("21. У меня усталый вид; мешки или круги под глазами", createStressAnswerOptions()));
        questions.add(new Question("22. Я чувствую тяжесть на своих плечах", createStressAnswerOptions()));
        questions.add(new Question("23. Я встревожен. Мне необходимо постоянно двигаться; я не могу устоять на одном месте", createStressAnswerOptions()));
        questions.add(new Question("24. Мне трудно контролировать свои поступки, эмоции, настроения или жесты", createStressAnswerOptions()));
        questions.add(new Question("25. Я напряжен", createStressAnswerOptions()));
        
        return questions;
    }
    
    private List<String> createStressAnswerOptions() {
        List<String> options = new ArrayList<>();
        options.add("Никогда (1)");
        options.add("Редко (2)");
        options.add("Иногда (3)");
        options.add("Часто (4)");
        options.add("Постоянно (5)");
        options.add("Очень часто (6)");
        options.add("Всё время (7)");
        return options;
    }
    
    private List<Question> createAnxietyTestQuestions() {
        List<Question> questions = new ArrayList<>();
        
        // Вопросы для теста тревожности
        questions.add(new Question("1. Я испытываю беспокойство", createAnxietyAnswerOptions()));
        questions.add(new Question("2. Я легко раздражаюсь", createAnxietyAnswerOptions()));
        questions.add(new Question("3. Я боюсь неудачи", createAnxietyAnswerOptions()));
        questions.add(new Question("4. Мне трудно сосредоточиться", createAnxietyAnswerOptions()));
        questions.add(new Question("5. Я чувствую напряжение", createAnxietyAnswerOptions()));
        // Добавьте больше вопросов при необходимости
        
        return questions;
    }
    
    private List<String> createAnxietyAnswerOptions() {
        List<String> options = new ArrayList<>();
        options.add("Совсем нет (1)");
        options.add("Немного (2)");
        options.add("Умеренно (3)");
        options.add("Сильно (4)");
        return options;
    }
    
    private List<Question> createMoodTestQuestions() {
        List<Question> questions = new ArrayList<>();
        
        // Вопросы для оценки настроения
        questions.add(new Question("1. Как вы оцениваете своё настроение сейчас?", createMoodAnswerOptions()));
        questions.add(new Question("2. Насколько вы довольны сегодняшним днем?", createMoodAnswerOptions()));
        questions.add(new Question("3. Насколько позитивно вы смотрите на ближайшее будущее?", createMoodAnswerOptions()));
        questions.add(new Question("4. Насколько вы чувствуете себя энергичным?", createMoodAnswerOptions()));
        questions.add(new Question("5. Насколько вы чувствуете себя спокойным?", createMoodAnswerOptions()));
        
        return questions;
    }
    
    private List<String> createMoodAnswerOptions() {
        List<String> options = new ArrayList<>();
        options.add("Очень плохо (1)");
        options.add("Плохо (2)");
        options.add("Нейтрально (3)");
        options.add("Хорошо (4)");
        options.add("Очень хорошо (5)");
        return options;
    }
    
    private List<Question> createSANTestQuestions() {
        List<Question> questions = new ArrayList<>();
        
        // Вопросы для теста САН
        // Самочувствие
        questions.add(new Question("1. Самочувствие хорошее - Самочувствие плохое", createSANAnswerOptions()));
        questions.add(new Question("2. Чувствую себя сильным - Чувствую себя слабым", createSANAnswerOptions()));
        
        // Активность
        questions.add(new Question("3. Пассивный - Активный", createSANAnswerOptions()));
        questions.add(new Question("4. Малоподвижный - Подвижный", createSANAnswerOptions()));
        
        // Настроение
        questions.add(new Question("5. Веселый - Грустный", createSANAnswerOptions()));
        questions.add(new Question("6. Хорошее настроение - Плохое настроение", createSANAnswerOptions()));
        
        // Добавьте больше вопросов при необходимости
        
        return questions;
    }
    
    private List<String> createSANAnswerOptions() {
        List<String> options = new ArrayList<>();
        options.add("1");
        options.add("2");
        options.add("3");
        options.add("4");
        options.add("5");
        options.add("6");
        options.add("7");
        return options;
    }
    
    private void displayTestCategories() {
        testsContainer.removeAllViews();
        
        for (Map.Entry<String, List<Test>> entry : testCategories.entrySet()) {
            String category = entry.getKey();
            List<Test> tests = entry.getValue();
            
            // Создаем заголовок категории
            TextView categoryTitle = new TextView(requireContext());
            categoryTitle.setText(category);
            categoryTitle.setTextSize(18);
            categoryTitle.setPadding(0, 16, 0, 8);
            testsContainer.addView(categoryTitle);
            
            // Добавляем тесты этой категории
            for (Test test : tests) {
                CardView cardView = new CardView(requireContext());
                cardView.setCardElevation(4);
                cardView.setRadius(8);
                cardView.setUseCompatPadding(true);
                
                LinearLayout cardContent = new LinearLayout(requireContext());
                cardContent.setOrientation(LinearLayout.VERTICAL);
                cardContent.setPadding(16, 16, 16, 16);
                
                TextView testTitle = new TextView(requireContext());
                testTitle.setText(test.getTitle());
                testTitle.setTextSize(16);
                
                TextView testDescription = new TextView(requireContext());
                testDescription.setText(test.getDescription());
                testDescription.setTextSize(14);
                testDescription.setPadding(0, 8, 0, 16);
                
                Button startButton = new Button(requireContext());
                startButton.setText("Пройти тест");
                startButton.setOnClickListener(v -> startTest(test));
                
                cardContent.addView(testTitle);
                cardContent.addView(testDescription);
                cardContent.addView(startButton);
                
                cardView.addView(cardContent);
                testsContainer.addView(cardView);
            }
        }
    }
    
    private void startTest(Test test) {
        currentTest = test;
        currentQuestionIndex = 0;
        userAnswers.clear();
        displayTest();
    }
    
    private void displayTest() {
        // Очищаем контейнер
        testsContainer.removeAllViews();
        
        // Создаем верхнюю панель с кнопкой возврата и заголовком
        LinearLayout topBar = new LinearLayout(requireContext());
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        topBar.setPadding(0, 16, 0, 16);
        topBar.setGravity(android.view.Gravity.CENTER_VERTICAL); // Выравниваем элементы по вертикали
        
        // Кнопка возврата (стрелка назад) в минималистичном стиле
        TextView backButton = new TextView(requireContext());
        backButton.setText("←");
        backButton.setTextSize(24); // Увеличиваем размер стрелки
        backButton.setPadding(16, 0, 16, 0);
        backButton.setOnClickListener(v -> showExitConfirmationDialog());
        
        // Заголовок теста
        TextView testTitle = new TextView(requireContext());
        testTitle.setText(currentTest.getTitle());
        testTitle.setTextSize(18);
        testTitle.setPadding(8, 0, 0, 0);
        
        // Устанавливаем параметры для корректного отображения заголовка
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, // Ширина будет определяться весом
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.weight = 1; // Заголовок займет все доступное пространство
        testTitle.setLayoutParams(titleParams);
        
        topBar.addView(backButton);
        topBar.addView(testTitle);
        testsContainer.addView(topBar);
        
        // Отображаем текущий вопрос
        Question currentQuestion = currentTest.getQuestions().get(currentQuestionIndex);
        
        TextView questionText = new TextView(requireContext());
        questionText.setText(currentQuestion.getText());
        questionText.setTextSize(16);
        questionText.setPadding(16, 16, 16, 16);
        testsContainer.addView(questionText);
        
        // Создаем группу радиокнопок для вариантов ответа
        RadioGroup radioGroup = new RadioGroup(requireContext());
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        radioGroup.setPadding(16, 0, 16, 0);
        
        for (int i = 0; i < currentQuestion.getOptions().size(); i++) {
            RadioButton radioButton = new RadioButton(requireContext());
            radioButton.setText(currentQuestion.getOptions().get(i));
            radioButton.setId(i);
            radioGroup.addView(radioButton);
        }
        
        testsContainer.addView(radioGroup);
        
        // Если пользователь уже отвечал на этот вопрос, выбираем соответствующую радиокнопку
        if (currentQuestionIndex < userAnswers.size()) {
            radioGroup.check(userAnswers.get(currentQuestionIndex));
        }
        
        // Кнопки "Назад" и "Далее"
        LinearLayout buttonContainer = new LinearLayout(requireContext());
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setPadding(16, 24, 16, 16);
        
        Button prevButton = new Button(requireContext());
        prevButton.setText("Назад");
        prevButton.setEnabled(currentQuestionIndex > 0);
        prevButton.setOnClickListener(v -> {
            // Сохраняем ответ пользователя
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                if (currentQuestionIndex < userAnswers.size()) {
                    userAnswers.set(currentQuestionIndex, selectedId);
                } else {
                    userAnswers.add(selectedId);
                }
            }
            
            // Переходим к предыдущему вопросу
            currentQuestionIndex--;
            displayTest();
        });
        
        Button nextButton = new Button(requireContext());
        boolean isLastQuestion = currentQuestionIndex == currentTest.getQuestions().size() - 1;
        nextButton.setText(isLastQuestion ? "Завершить" : "Далее");
        nextButton.setOnClickListener(v -> {
            // Проверяем, выбран ли вариант ответа
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(requireContext(), "Пожалуйста, выберите вариант ответа", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Сохраняем ответ пользователя
            if (currentQuestionIndex < userAnswers.size()) {
                userAnswers.set(currentQuestionIndex, selectedId);
            } else {
                userAnswers.add(selectedId);
            }
            
            if (isLastQuestion) {
                // Завершаем тест и сохраняем результаты
                finishTest();
            } else {
                // Переходим к следующему вопросу
                currentQuestionIndex++;
                displayTest();
            }
        });
        
        // Устанавливаем параметры для кнопок, чтобы они занимали равное пространство
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                0, // Ширина будет определяться весом
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.weight = 1; // Каждая кнопка займет половину доступного пространства
        buttonParams.setMargins(8, 0, 8, 0); // Добавляем отступы между кнопками
        
        prevButton.setLayoutParams(buttonParams);
        nextButton.setLayoutParams(buttonParams);
        
        buttonContainer.addView(prevButton);
        buttonContainer.addView(nextButton);
        testsContainer.addView(buttonContainer);
        
        // Добавляем индикатор прогресса
        TextView progressIndicator = new TextView(requireContext());
        progressIndicator.setText("Вопрос " + (currentQuestionIndex + 1) + " из " + currentTest.getQuestions().size());
        progressIndicator.setGravity(android.view.Gravity.CENTER);
        progressIndicator.setPadding(0, 16, 0, 0);
        testsContainer.addView(progressIndicator);
    }
    
    private void finishTest() {
        // Вычисляем результаты теста
        TestResult[] results = currentTest.calculateResults(userAnswers);
        
        // Сохраняем результаты в базу данных
        saveTestResults(results);
        
        // Отображаем результаты
        displayTestResults(results);
    }

    private void displayTestResults(TestResult[] results) {
        // Очищаем контейнер
        testsContainer.removeAllViews();
        
        // Создаем верхнюю панель с кнопкой возврата и заголовком
        LinearLayout topBar = new LinearLayout(requireContext());
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        topBar.setPadding(0, 16, 0, 16);
        topBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // Кнопка возврата (стрелка назад) в минималистичном стиле
        TextView backButton = new TextView(requireContext());
        backButton.setText("←");
        backButton.setTextSize(24);
        backButton.setPadding(16, 0, 16, 0);
        backButton.setOnClickListener(v -> {
            resetTest();
            displayTestCategories();
        });
        
        // Заголовок
        TextView titleView = new TextView(requireContext());
        titleView.setText("Результаты теста");
        titleView.setTextSize(18);
        
        // Устанавливаем параметры для корректного отображения заголовка
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.weight = 1;
        titleView.setLayoutParams(titleParams);
        
        topBar.addView(backButton);
        topBar.addView(titleView);
        testsContainer.addView(topBar);
        
        // Отображаем результаты теста
        for (TestResult result : results) {
            CardView resultCard = new CardView(requireContext());
            resultCard.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            resultCard.setCardElevation(4);
            resultCard.setRadius(8);
            resultCard.setUseCompatPadding(true);
            
            LinearLayout cardContent = new LinearLayout(requireContext());
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(16, 16, 16, 16);
            
            // Отображаем интерпретацию результата
            TextView interpretationView = new TextView(requireContext());
            interpretationView.setText(result.getInterpretation());
            interpretationView.setTextSize(16);
            
            cardContent.addView(interpretationView);
            resultCard.addView(cardContent);
            testsContainer.addView(resultCard);
        }
        
        // Кнопка возврата к списку тестов
        Button backToListButton = new Button(requireContext());
        backToListButton.setText("Вернуться к списку тестов");
        backToListButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        backToListButton.setOnClickListener(v -> {
            resetTest();
            displayTestCategories();
        });
        
        // Добавляем отступ перед кнопкой
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(16, 24, 16, 16);
        backToListButton.setLayoutParams(buttonParams);
        
        testsContainer.addView(backToListButton);
    }

    private void saveTestResults(TestResult[] results) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = user.getUid();
        DatabaseReference userTestsRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("test_results");
        
        // Определяем тип теста на основе его названия
        String testType = getTestTypeFromTitle(currentTest.getTitle());
        
        // Сохраняем каждый результат
        for (TestResult result : results) {
            String resultId = userTestsRef.child(testType).push().getKey();
            if (resultId != null) {
                userTestsRef.child(testType).child(resultId).setValue(result)
                        .addOnSuccessListener(aVoid -> {
                            // Результат успешно сохранен
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Ошибка при сохранении результата: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    private String getTestTypeFromTitle(String title) {
        if (title.contains("стресс")) {
            return "stress_test";
        } else if (title.contains("эмоциональный интеллект")) {
            return "emotional_intelligence_test";
        } else if (title.contains("Бойко")) {
            return "boyko_test";
        } else if (title.contains("Маслач")) {
            return "maslach_test";
        } else if (title.contains("самооценк")) {
            return "self_esteem_test";
        } else {
            // Если не удалось определить тип теста, используем общий тип
            return "general_test";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // Вспомогательные классы
    
    private static class Test {
        private final String title;
        private final String description;
        private final List<Question> questions;
        private final TestResultCalculator resultCalculator;
        
        public Test(String title, String description, List<Question> questions, TestResultCalculator resultCalculator) {
            this.title = title;
            this.description = description;
            this.questions = questions;
            this.resultCalculator = resultCalculator;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public List<Question> getQuestions() {
            return questions;
        }
        
        public TestResult[] calculateResults(List<Integer> answers) {
            return resultCalculator.calculate(answers);
        }
    }
    
    private static class Question {
        private final String text;
        private final List<String> options;
        
        public Question(String text, List<String> options) {
            this.text = text;
            this.options = options;
        }
        
        public String getText() {
            return text;
        }
        
        public List<String> getOptions() {
            return options;
        }
    }
    
    private interface TestResultCalculator {
        TestResult[] calculate(List<Integer> answers);
    }

    // Метод для сохранения результатов теста в Firebase
    private void saveTestResultToFirebase(TestResult result, String testType) {
        TestResultsManager testResultsManager = new TestResultsManager();
        testResultsManager.saveTestResult(result, testType, new TestResultsManager.OnTestResultSavedListener() {
            @Override
            public void onTestResultSaved() {
                Toast.makeText(requireContext(), "Результат сохранен", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTestResultError(String errorMessage) {
                Toast.makeText(requireContext(), "Ошибка при сохранении результата: " + errorMessage, 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Метод для получения вопросов теста на эмоциональный интеллект
    private List<Question> getEmotionalIntelligenceTestQuestions() {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("Я хорошо понимаю свои эмоции", getStandardAnswers()));
        questions.add(new Question("Я могу точно определить, что чувствуют другие люди", getStandardAnswers()));
        // ... другие вопросы
        return questions;
    }

    // Метод для получения вопросов теста Бойко
    private List<Question> getBoykoTestQuestions() {
        List<Question> questions = new ArrayList<>();
        
        // Фаза "Напряжение"
        // Симптом "Переживание психотравмирующих обстоятельств"
        questions.add(new Question("Я чувствую себя эмоционально опустошенным к концу рабочего дня", getStandardAnswers()));
        questions.add(new Question("Я замечаю, что стал более черствым по отношению к людям", getStandardAnswers()));
        questions.add(new Question("Меня тревожат мысли о работе", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что моя работа эмоционально истощает меня", getStandardAnswers()));
        questions.add(new Question("Я замечаю, что стал более раздражительным", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя на пределе возможностей", getStandardAnswers()));
        questions.add(new Question("Я чувствую разочарование в своей работе", getStandardAnswers()));
        
        // Симптом "Неудовлетворенность собой"
        questions.add(new Question("Я недоволен собой на работе", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что недостаточно компетентен в своей работе", getStandardAnswers()));
        questions.add(new Question("Я не удовлетворен своими профессиональными достижениями", getStandardAnswers()));
        questions.add(new Question("Я сомневаюсь в значимости своей работы", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что мог бы сделать больше на своей работе", getStandardAnswers()));
        questions.add(new Question("Я думаю, что выбрал неправильную профессию", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что моя работа не приносит пользы", getStandardAnswers()));
        
        // Симптом "Загнанность в клетку"
        questions.add(new Question("Я чувствую себя загнанным в тупик", getStandardAnswers()));
        questions.add(new Question("Я не вижу выхода из сложившейся ситуации", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что не могу изменить ситуацию на работе", getStandardAnswers()));
        questions.add(new Question("Я ощущаю безвыходность ситуации", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя пойманным в ловушку обстоятельств", getStandardAnswers()));
        questions.add(new Question("Я ощущаю бессилие что-либо изменить", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что нахожусь в тупике", getStandardAnswers()));
        
        // Симптом "Тревога и депрессия"
        questions.add(new Question("Я испытываю тревогу без видимой причины", getStandardAnswers()));
        questions.add(new Question("У меня бывает подавленное настроение", getStandardAnswers()));
        questions.add(new Question("Я чувствую беспокойство по поводу своей работы", getStandardAnswers()));
        questions.add(new Question("Я испытываю чувство безнадежности", getStandardAnswers()));
        questions.add(new Question("Я замечаю у себя признаки депрессии", getStandardAnswers()));
        questions.add(new Question("Меня беспокоят мысли о будущем", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя подавленным", getStandardAnswers()));
        
        // Фаза "Резистенция"
        // Симптом "Неадекватное избирательное эмоциональное реагирование"
        questions.add(new Question("Я стал более холодным в общении с коллегами", getStandardAnswers()));
        questions.add(new Question("Я замечаю, что стал избирательно реагировать на ситуации", getStandardAnswers()));
        questions.add(new Question("Я стараюсь избегать эмоционально напряженных ситуаций", getStandardAnswers()));
        questions.add(new Question("Я замечаю, что стал более равнодушным к проблемам других", getStandardAnswers()));
        questions.add(new Question("Я стал более формально выполнять свои обязанности", getStandardAnswers()));
        questions.add(new Question("Я стараюсь сократить время общения с коллегами", getStandardAnswers()));
        questions.add(new Question("Я замечаю, что стал более черствым в общении", getStandardAnswers()));
        
        // Добавьте остальные вопросы для теста Бойко...
        
        return questions;
    }

    // Метод для получения вопросов теста Маслач
    private List<Question> getMaslachTestQuestions() {
        List<Question> questions = new ArrayList<>();
        List<String> options = Arrays.asList("Никогда", "Очень редко", "Редко", "Иногда", "Часто", "Очень часто", "Ежедневно");
        
        // Эмоциональное истощение
        questions.add(new Question("1. Я чувствую себя эмоционально опустошенным", options));
        questions.add(new Question("2. К концу рабочего дня я чувствую себя как выжатый лимон", options));
        questions.add(new Question("3. Я чувствую себя усталым, когда встаю утром и должен идти на работу", options));
        questions.add(new Question("4. Я хорошо понимаю, что чувствуют мои коллеги и клиенты, и использую это в интересах дела", options));
        questions.add(new Question("5. Я общаюсь с моими клиентами только формально, без лишних эмоций", options));
        questions.add(new Question("6. Я чувствую себя энергичным и эмоционально воодушевленным", options));
        questions.add(new Question("7. Я умею находить правильное решение в конфликтных ситуациях", options));
        questions.add(new Question("8. Я чувствую угнетенность и апатию", options));
        questions.add(new Question("9. Я могу позитивно влиять на продуктивность работы моих коллег", options));
        
        // Деперсонализация
        questions.add(new Question("10. В последнее время я стал более черствым по отношению к людям", options));
        questions.add(new Question("11. Я переживаю, что моя работа делает меня более эмоционально черствым", options));
        questions.add(new Question("12. Я чувствую, что работаю слишком напряженно", options));
        questions.add(new Question("13. Мне безразлично, что происходит с моими клиентами", options));
        questions.add(new Question("14. Я чувствую, что нахожусь на пределе своих возможностей", options));
        questions.add(new Question("15. Я не чувствую, что моя работа приносит пользу", options));
        
        // Редукция профессиональных достижений
        questions.add(new Question("16. В моей работе я успешно решаю эмоционально напряженные вопросы", options));
        questions.add(new Question("17. Я чувствую, что эффективно решаю проблемы моих клиентов", options));
        questions.add(new Question("18. Я чувствую себя воодушевленным после работы с клиентами", options));
        questions.add(new Question("19. Я многого достиг в своей профессии", options));
        questions.add(new Question("20. Я чувствую, что я на своем месте и делаю нужную работу", options));
        questions.add(new Question("21. В своей работе я спокойно справляюсь с эмоциональными проблемами", options));
        questions.add(new Question("22. Я чувствую, что клиенты обвиняют меня в своих проблемах", options));
        
        return questions;
    }

    // Метод для получения вопросов теста самооценки
    private List<Question> getSelfEsteemTestQuestions() {
        List<Question> questions = new ArrayList<>();
        List<String> options = Arrays.asList("Очень часто", "Часто", "Иногда", "Редко", "Никогда");
        
        questions.add(new Question("1. Я часто волнуюсь понапрасну", options));
        questions.add(new Question("2. Мне хочется, чтобы мои друзья подбадривали меня", options));
        questions.add(new Question("3. Я боюсь выглядеть глупцом", options));
        questions.add(new Question("4. Я беспокоюсь за свое будущее", options));
        questions.add(new Question("5. Внешний вид других куда лучше, чем мой", options));
        questions.add(new Question("6. Как жаль, что многие не понимают меня", options));
        questions.add(new Question("7. Чувствую, что не умею как следует разговаривать с людьми", options));
        questions.add(new Question("8. Люди ждут от меня очень многого", options));
        questions.add(new Question("9. Чувствую себя скованным", options));
        questions.add(new Question("10. Мне кажется, что со мной должна случиться какая-нибудь неприятность", options));
        questions.add(new Question("11. Меня волнует мысль о том, как люди относятся ко мне", options));
        questions.add(new Question("12. Я чувствую, что люди говорят обо мне за моей спиной", options));
        questions.add(new Question("13. Я не чувствую себя в безопасности", options));
        questions.add(new Question("14. Мне не с кем поделиться своими мыслями", options));
        questions.add(new Question("15. Люди не особенно интересуются моими достижениями", options));
        
        return questions;
    }

    // Метод для отображения диалога подтверждения выхода из теста
    private void showExitConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Выход из теста");
        builder.setMessage("Вы уверены, что хотите выйти из теста? Ваши ответы не будут сохранены.");
        builder.setPositiveButton("Да", (dialog, which) -> {
            // Сбрасываем текущий тест и возвращаемся к списку тестов
            resetTest();
            displayTestCategories();
        });
        builder.setNegativeButton("Нет", (dialog, which) -> {
            // Ничего не делаем, просто закрываем диалог
            dialog.dismiss();
        });
        builder.show();
    }

    // Метод для сброса текущего теста
    private void resetTest() {
        currentTest = null;
        currentQuestionIndex = 0;
        userAnswers.clear();
    }

    // Добавим методы для получения стандартных вариантов ответов
    private List<String> getStandardAnswers() {
        List<String> answers = new ArrayList<>();
        answers.add("Никогда");
        answers.add("Редко");
        answers.add("Иногда");
        answers.add("Часто");
        answers.add("Всегда");
        return answers;
    }

    private List<String> getAnxietyAnswers() {
        List<String> answers = new ArrayList<>();
        answers.add("Совсем нет");
        answers.add("Пожалуй, так");
        answers.add("Верно");
        answers.add("Совершенно верно");
        return answers;
    }

    // Добавим методы для получения вопросов тестов
    private List<Question> getPSM25Questions() {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("Я чувствую себя измотанным", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что мне не с кем поговорить", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя подавленным", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя напряженным", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что многие люди меня не любят", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя в тупике", getStandardAnswers()));
        questions.add(new Question("Я испытываю головные боли", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя беспомощным", getStandardAnswers()));
        questions.add(new Question("Я не могу избавиться от навязчивых мыслей", getStandardAnswers()));
        questions.add(new Question("Я легко утомляюсь", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что никому не могу доверять", getStandardAnswers()));
        questions.add(new Question("Я легко раздражаюсь", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя разбитым", getStandardAnswers()));
        questions.add(new Question("Я чувствую печаль", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что я никому не нужен", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что запутался в своей жизни", getStandardAnswers()));
        questions.add(new Question("Я испытываю мышечное напряжение", getStandardAnswers()));
        questions.add(new Question("У меня проблемы со сном", getStandardAnswers()));
        questions.add(new Question("Я чувствую безнадежность", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя неудачником", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя в ловушке", getStandardAnswers()));
        questions.add(new Question("Я чувствую себя никчемным", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что люди недружелюбны ко мне", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что жизнь несправедлива", getStandardAnswers()));
        questions.add(new Question("Я чувствую, что не могу продолжать дальше", getStandardAnswers()));
        return questions;
    }

    private List<Question> getSpielbergerTestQuestions() {
        List<Question> questions = new ArrayList<>();
        
        // Вопросы на ситуативную тревожность
        questions.add(new Question("Я спокоен", getAnxietyAnswers()));
        questions.add(new Question("Мне ничто не угрожает", getAnxietyAnswers()));
        questions.add(new Question("Я нахожусь в напряжении", getAnxietyAnswers()));
        questions.add(new Question("Я испытываю сожаление", getAnxietyAnswers()));
        questions.add(new Question("Я чувствую себя свободно", getAnxietyAnswers()));
        questions.add(new Question("Я расстроен", getAnxietyAnswers()));
        questions.add(new Question("Меня волнуют возможные неудачи", getAnxietyAnswers()));
        questions.add(new Question("Я чувствую себя отдохнувшим", getAnxietyAnswers()));
        questions.add(new Question("Я встревожен", getAnxietyAnswers()));
        questions.add(new Question("Я испытываю чувство внутреннего удовлетворения", getAnxietyAnswers()));
        questions.add(new Question("Я уверен в себе", getAnxietyAnswers()));
        questions.add(new Question("Я нервничаю", getAnxietyAnswers()));
        questions.add(new Question("Я не нахожу себе места", getAnxietyAnswers()));
        questions.add(new Question("Я взвинчен", getAnxietyAnswers()));
        questions.add(new Question("Я не чувствую скованности, напряженности", getAnxietyAnswers()));
        questions.add(new Question("Я доволен", getAnxietyAnswers()));
        questions.add(new Question("Я озабочен", getAnxietyAnswers()));
        questions.add(new Question("Я слишком возбужден и мне не по себе", getAnxietyAnswers()));
        questions.add(new Question("Мне радостно", getAnxietyAnswers()));
        questions.add(new Question("Мне приятно", getAnxietyAnswers()));
        
        // Вопросы на личностную тревожность
        questions.add(new Question("Я испытываю удовольствие", getAnxietyAnswers()));
        questions.add(new Question("Я очень быстро устаю", getAnxietyAnswers()));
        questions.add(new Question("Я легко могу заплакать", getAnxietyAnswers()));
        questions.add(new Question("Я хотел бы быть таким же счастливым, как и другие", getAnxietyAnswers()));
        questions.add(new Question("Нередко я проигрываю из-за того, что недостаточно быстро принимаю решения", getAnxietyAnswers()));
        questions.add(new Question("Обычно я чувствую себя бодрым", getAnxietyAnswers()));
        questions.add(new Question("Я спокоен, хладнокровен и собран", getAnxietyAnswers()));
        questions.add(new Question("Ожидаемые трудности обычно очень тревожат меня", getAnxietyAnswers()));
        questions.add(new Question("Я слишком переживаю из-за пустяков", getAnxietyAnswers()));
        questions.add(new Question("Я вполне счастлив", getAnxietyAnswers()));
        questions.add(new Question("Я принимаю все слишком близко к сердцу", getAnxietyAnswers()));
        questions.add(new Question("Мне не хватает уверенности в себе", getAnxietyAnswers()));
        questions.add(new Question("Обычно я чувствую себя в безопасности", getAnxietyAnswers()));
        questions.add(new Question("Я стараюсь избегать критических ситуаций", getAnxietyAnswers()));
        questions.add(new Question("У меня бывает хандра", getAnxietyAnswers()));
        questions.add(new Question("Я доволен", getAnxietyAnswers()));
        questions.add(new Question("Всякие пустяки отвлекают и волнуют меня", getAnxietyAnswers()));
        questions.add(new Question("Я так сильно переживаю свои разочарования, что потом долго не могу о них забыть", getAnxietyAnswers()));
        questions.add(new Question("Я уравновешенный человек", getAnxietyAnswers()));
        questions.add(new Question("Меня охватывает сильное беспокойство, когда я думаю о своих делах и заботах", getAnxietyAnswers()));
        
        return questions;
    }

    // Метод для получения вопросов шкалы оценки настроения
    private List<Question> getMoodScaleTestQuestions() {
        List<Question> questions = new ArrayList<>();
        
        // Позитивный аффект
        questions.add(new Question("Я чувствую себя радостным", getStandardAnswers()));
        questions.add(new Question("Я испытываю грусть", getStandardAnswers())); // Негативный
        questions.add(new Question("Я полон энергии", getStandardAnswers()));
        questions.add(new Question("Я чувствую тревогу", getStandardAnswers())); // Негативный
        questions.add(new Question("Я чувствую себя воодушевленным", getStandardAnswers()));
        questions.add(new Question("Я чувствую раздражение", getStandardAnswers())); // Негативный
        questions.add(new Question("Я испытываю интерес к происходящему", getStandardAnswers()));
        questions.add(new Question("Я чувствую усталость", getStandardAnswers())); // Негативный
        questions.add(new Question("Я чувствую удовлетворение", getStandardAnswers()));
        questions.add(new Question("Я чувствую беспокойство", getStandardAnswers())); // Негативный
        
        return questions;
    }
} 