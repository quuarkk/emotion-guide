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
        testCategories = new HashMap<>();
        
        // Категория: Стресс
        List<Test> stressTests = new ArrayList<>();
        
        // Тест на уровень стресса (из PDF)
        Test stressTest = new Test(
                "Шкала психологического стресса PSM-25",
                "Измерение стрессовых ощущений по соматическим, поведенческим и эмоциональным признакам",
                createStressTestQuestions(),
                answers -> {
                    // Подсчет результата теста на стресс
                    float sum = 0;
                    for (int answer : answers) {
                        sum += answer;
                    }
                    
                    float stressLevel = sum;
                    String interpretation;
                    
                    if (stressLevel < 100) {
                        interpretation = "Низкий уровень стресса";
                    } else if (stressLevel <= 154) {
                        interpretation = "Средний уровень стресса";
                    } else {
                        interpretation = "Высокий уровень стресса";
                    }
                    
                    // Создаем результат теста
                    TestResult result = new TestResult(
                            stressLevel / 175 * 10, // нормализуем до шкалы 0-10
                            0, // активность не измеряется в этом тесте
                            (175 - stressLevel) / 175 * 10, // инвертируем для настроения
                            new Date().getTime()
                    );
                    
                    // Сохраняем результат в Firebase
                    saveTestResultToFirebase(result, "stress_test");
                    
                    return new TestResult[] { result, null };
                }
        );
        stressTests.add(stressTest);
        
        // Тест на тревожность
        Test anxietyTest = new Test(
                "Шкала тревожности Спилбергера-Ханина",
                "Оценка уровня ситуативной и личностной тревожности",
                createAnxietyTestQuestions(),
                answers -> {
                    // Подсчет результата теста на тревожность
                    float sum = 0;
                    for (int answer : answers) {
                        sum += answer;
                    }
                    
                    float anxietyLevel = sum / answers.size() * 10; // нормализуем до шкалы 0-10
                    
                    TestResult result = new TestResult(
                            anxietyLevel,
                            0,
                            (10 - anxietyLevel), // инвертируем для настроения
                            new Date().getTime()
                    );
                    
                    // Сохраняем результат в Firebase
                    saveTestResultToFirebase(result, "anxiety_test");
                    
                    return new TestResult[] { result, null };
                }
        );
        stressTests.add(anxietyTest);
        
        testCategories.put("Стресс и тревожность", stressTests);
        
        // Категория: Эмоциональный интеллект
        List<Test> eiTests = new ArrayList<>();
        
        // Тест на эмоциональный интеллект (из PDF)
        Test eiTest = new Test(
                "Тест на эмоциональный интеллект",
                "Оценка способности понимать свои и чужие эмоции и управлять ими",
                createEmotionalIntelligenceQuestions(),
                answers -> {
                    // Подсчет результата теста на эмоциональный интеллект
                    int sum = 0;
                    for (int answer : answers) {
                        sum += answer;
                    }
                    
                    float eiScore = sum / (float)answers.size() * 2; // нормализуем до шкалы 0-10
                    
                    String interpretation;
                    if (eiScore < 4) {
                        interpretation = "Низкий уровень эмоционального интеллекта";
                    } else if (eiScore < 7) {
                        interpretation = "Средний уровень эмоционального интеллекта";
                    } else {
                        interpretation = "Высокий уровень эмоционального интеллекта";
                    }
                    
                    TestResult result = new TestResult(
                            eiScore, // эмоциональный интеллект влияет на самочувствие
                            eiScore * 0.7f, // и на активность
                            eiScore * 0.9f, // и на настроение
                            new Date().getTime()
                    );
                    
                    // Сохраняем результат в Firebase
                    saveTestResultToFirebase(result, "emotional_intelligence_test");
                    
                    return new TestResult[] { result, null };
                }
        );
        eiTests.add(eiTest);
        
        testCategories.put("Эмоциональный интеллект", eiTests);
        
        // Категория: Настроение
        List<Test> moodTests = new ArrayList<>();
        
        // Тест на настроение
        Test moodTest = new Test(
                "Шкала оценки настроения",
                "Быстрая оценка текущего эмоционального состояния",
                createMoodTestQuestions(),
                answers -> {
                    float moodScore = 0;
                    for (int answer : answers) {
                        moodScore += answer;
                    }
                    
                    moodScore = moodScore / answers.size() * 2; // нормализуем до шкалы 0-10
                    
                    TestResult result = new TestResult(
                            0,
                            0,
                            moodScore,
                            new Date().getTime()
                    );
                    
                    // Сохраняем результат в Firebase
                    saveTestResultToFirebase(result, "mood_test");
                    
                    return new TestResult[] { result, null };
                }
        );
        moodTests.add(moodTest);
        
        testCategories.put("Настроение", moodTests);
        
        // Категория: Самочувствие и активность
        List<Test> wellbeingTests = new ArrayList<>();
        
        // Тест САН (Самочувствие, Активность, Настроение)
        Test sanTest = new Test(
                "Тест САН",
                "Оценка самочувствия, активности и настроения",
                createSANTestQuestions(),
                answers -> {
                    // Подсчет результатов по трем шкалам
                    float wellbeingSum = 0;
                    float activitySum = 0;
                    float moodSum = 0;
                    
                    // Вопросы для самочувствия: 1, 2, 7, 8, 13, 14, 19, 20, 25, 26
                    // Вопросы для активности: 3, 4, 9, 10, 15, 16, 21, 22, 27, 28
                    // Вопросы для настроения: 5, 6, 11, 12, 17, 18, 23, 24, 29, 30
                    
                    for (int i = 0; i < answers.size(); i++) {
                        int category = i % 6;
                        if (category < 2) {
                            wellbeingSum += answers.get(i);
                        } else if (category < 4) {
                            activitySum += answers.get(i);
                        } else {
                            moodSum += answers.get(i);
                        }
                    }
                    
                    int wellbeingCount = answers.size() / 3;
                    int activityCount = answers.size() / 3;
                    int moodCount = answers.size() / 3;
                    
                    float wellbeingScore = wellbeingSum / wellbeingCount;
                    float activityScore = activitySum / activityCount;
                    float moodScore = moodSum / moodCount;
                    
                    TestResult result = new TestResult(
                            wellbeingScore,
                            activityScore,
                            moodScore,
                            new Date().getTime()
                    );
                    
                    // Сохраняем результат в Firebase
                    saveTestResultToFirebase(result, "san_test");
                    
                    return new TestResult[] { result, null };
                }
        );
        wellbeingTests.add(sanTest);
        
        testCategories.put("Самочувствие и активность", wellbeingTests);
        
        // Категория: Профессиональное выгорание
        List<Test> burnoutTests = new ArrayList<>();
        
        // Тест Бойко
        Test boykoTest = new Test(
                "Тест на эмоциональное выгорание (Бойко)",
                "Диагностика эмоционального выгорания по методике В.В. Бойко",
                getBoykoTestQuestions(),
                answers -> {
                    // Подсчет результатов теста Бойко
                    int napryazhenie = 0;
                    int rezistencia = 0;
                    int istoshenie = 0;
                    
                    // Подсчет баллов по фазам
                    for (int i = 0; i < 8; i++) {
                        if (answers.get(i) == 0) napryazhenie++; // "Да" = 1 балл
                    }
                    
                    for (int i = 8; i < 16; i++) {
                        if (answers.get(i) == 0) rezistencia++; // "Да" = 1 балл
                    }
                    
                    for (int i = 16; i < 24; i++) {
                        if (answers.get(i) == 0) istoshenie++; // "Да" = 1 балл
                    }
                    
                    int totalScore = napryazhenie + rezistencia + istoshenie;
                    float wellbeingScore = 10 - totalScore / 2.4f; // Преобразуем в шкалу от 0 до 10
                    
                    TestResult result = new TestResult();
                    result.setWellbeingScore(wellbeingScore);
                    result.setActivityScore((float) napryazhenie / 8 * 10); // Активность как показатель напряжения
                    result.setMoodScore(10 - (float) istoshenie / 8 * 10); // Настроение обратно пропорционально истощению
                    result.setTimestamp(System.currentTimeMillis());
                    
                    return new TestResult[] { result };
                }
        );
        burnoutTests.add(boykoTest);
        
        // Тест Маслач
        Test maslachTest = new Test(
                "Тест на профессиональное выгорание (Маслач)",
                "Диагностика профессионального выгорания по методике К. Маслач и С. Джексон",
                getMaslachTestQuestions(),
                answers -> {
                    // Подсчет результатов теста Маслач
                    int emotionalExhaustion = 0;
                    int depersonalization = 0;
                    int personalAccomplishment = 0;
                    
                    // Подсчет баллов по шкалам
                    for (int i = 0; i < 9; i++) {
                        emotionalExhaustion += answers.get(i);
                    }
                    
                    for (int i = 9; i < 15; i++) {
                        depersonalization += answers.get(i);
                    }
                    
                    for (int i = 15; i < 22; i++) {
                        personalAccomplishment += answers.get(i);
                    }
                    
                    // Инвертируем шкалу личных достижений (высокие баллы = низкое выгорание)
                    personalAccomplishment = 42 - personalAccomplishment;
                    
                    // Общий балл выгорания
                    int totalBurnout = emotionalExhaustion + depersonalization + personalAccomplishment;
                    
                    // Преобразуем в шкалу от 0 до 10 (где 10 - отсутствие выгорания)
                    float wellbeingScore = 10 - (float) totalBurnout / 132 * 10;
                    
                    TestResult result = new TestResult();
                    result.setWellbeingScore(wellbeingScore);
                    result.setActivityScore(10 - (float) emotionalExhaustion / 54 * 10);
                    result.setMoodScore(10 - (float) depersonalization / 30 * 10);
                    result.setTimestamp(System.currentTimeMillis());
                    
                    return new TestResult[] { result };
                }
        );
        burnoutTests.add(maslachTest);
        
        // Добавляем категорию выгорания
        testCategories.put("Профессиональное выгорание", burnoutTests);
        
        // Категория: Самооценка
        List<Test> selfEsteemTests = new ArrayList<>();
        
        // Тест самооценки
        Test selfEsteemTest = new Test(
                "Тест на самооценку",
                "Диагностика уровня самооценки личности",
                getSelfEsteemTestQuestions(),
                answers -> {
                    // Подсчет результатов теста самооценки
                    int totalScore = 0;
                    
                    // Подсчет баллов (инвертированная шкала: 0 - очень часто, 4 - никогда)
                    for (Integer answer : answers) {
                        totalScore += answer;
                    }
                    
                    // Преобразуем в шкалу от 0 до 10 (где 10 - высокая самооценка)
                    float wellbeingScore = (float) totalScore / 60 * 10;
                    
                    TestResult result = new TestResult();
                    result.setWellbeingScore(wellbeingScore);
                    result.setActivityScore(wellbeingScore * 0.8f); // Активность связана с самооценкой
                    result.setMoodScore(wellbeingScore * 0.9f); // Настроение тесно связано с самооценкой
                    result.setTimestamp(System.currentTimeMillis());
                    
                    return new TestResult[] { result };
                }
        );
        selfEsteemTests.add(selfEsteemTest);
        
        // Добавляем категорию самооценки
        testCategories.put("Самооценка", selfEsteemTests);
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
        
        // Отображаем результаты
        for (TestResult result : results) {
            if (result == null) continue; // Пропускаем null-результаты
            
            CardView resultCard = new CardView(requireContext());
            resultCard.setCardElevation(4);
            resultCard.setRadius(8);
            resultCard.setUseCompatPadding(true);
            
            LinearLayout cardContent = new LinearLayout(requireContext());
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(16, 16, 16, 16);
            
            // Показываем основные метрики
            TextView wellbeingView = new TextView(requireContext());
            wellbeingView.setText("Общее самочувствие: " + String.format("%.1f", result.getWellbeingScore()) + "/10");
            wellbeingView.setTextSize(16);
            wellbeingView.setPadding(0, 0, 0, 8);
            
            TextView activityView = new TextView(requireContext());
            activityView.setText("Активность: " + String.format("%.1f", result.getActivityScore()) + "/10");
            activityView.setTextSize(16);
            activityView.setPadding(0, 0, 0, 8);
            
            TextView moodView = new TextView(requireContext());
            moodView.setText("Настроение: " + String.format("%.1f", result.getMoodScore()) + "/10");
            moodView.setTextSize(16);
            
            cardContent.addView(wellbeingView);
            cardContent.addView(activityView);
            cardContent.addView(moodView);
            
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

    // Создание вопросов для теста эмоционального интеллекта
    private List<Question> createEmotionalIntelligenceQuestions() {
        List<Question> questions = new ArrayList<>();
        
        // Варианты ответов для теста эмоционального интеллекта
        List<String> eiOptions = new ArrayList<>();
        eiOptions.add("Совершенно не согласен");
        eiOptions.add("Скорее не согласен");
        eiOptions.add("Скорее согласен");
        eiOptions.add("Полностью согласен");
        
        // Вопросы из PDF-файла
        questions.add(new Question("1. Я хорошо осознаю свои эмоции", eiOptions));
        questions.add(new Question("2. Я могу точно определить, какие эмоции испытываю в данный момент", eiOptions));
        questions.add(new Question("3. Я понимаю, как мои эмоции влияют на мое поведение", eiOptions));
        questions.add(new Question("4. Я легко распознаю эмоции других людей", eiOptions));
        questions.add(new Question("5. Я хорошо понимаю причины эмоций других людей", eiOptions));
        questions.add(new Question("6. Я могу помочь другому человеку справиться с негативными эмоциями", eiOptions));
        questions.add(new Question("7. Я могу контролировать свои эмоции в стрессовых ситуациях", eiOptions));
        questions.add(new Question("8. Я могу быстро успокоиться после неожиданных неприятностей", eiOptions));
        questions.add(new Question("9. Я могу переключаться с негативных эмоций на позитивные", eiOptions));
        questions.add(new Question("10. Я умею использовать эмоции для повышения эффективности деятельности", eiOptions));
        questions.add(new Question("11. Я могу поставить себя на место другого человека", eiOptions));
        questions.add(new Question("12. Я хорошо понимаю невербальные сигналы других людей", eiOptions));
        questions.add(new Question("13. Я могу определить, когда человек говорит одно, а чувствует другое", eiOptions));
        questions.add(new Question("14. Я умею вдохновлять и мотивировать других людей", eiOptions));
        questions.add(new Question("15. Я могу создавать позитивную атмосферу в группе", eiOptions));
        
        return questions;
    }

    // Метод для получения вопросов теста Бойко
    private List<Question> getBoykoTestQuestions() {
        List<Question> questions = new ArrayList<>();
        List<String> options = Arrays.asList("Да", "Нет");
        
        // Фаза "Напряжение"
        questions.add(new Question("1. Я постоянно переживаю психотравмирующие обстоятельства", options));
        questions.add(new Question("2. Я чувствую себя загнанным в клетку", options));
        questions.add(new Question("3. Я чувствую себя усталым и подавленным", options));
        questions.add(new Question("4. Я испытываю тревогу и напряжение", options));
        questions.add(new Question("5. Я переживаю неудачи на работе", options));
        questions.add(new Question("6. Я чувствую безысходность", options));
        questions.add(new Question("7. Я переживаю бессонницу", options));
        questions.add(new Question("8. Меня беспокоят боли в сердце", options));
        
        // Фаза "Резистенция"
        questions.add(new Question("9. Я стал более равнодушным к людям", options));
        questions.add(new Question("10. Я замечаю, что моя работа ожесточает меня", options));
        questions.add(new Question("11. Меня тревожит то, что работа эмоционально опустошает меня", options));
        questions.add(new Question("12. Я чувствую, что работаю на пределе своих возможностей", options));
        questions.add(new Question("13. Я не могу справиться с эмоциональными перегрузками на работе", options));
        questions.add(new Question("14. Мне кажется, что я перестал сочувствовать своим коллегам", options));
        questions.add(new Question("15. Я стал более черствым по отношению к людям", options));
        questions.add(new Question("16. Я чувствую эмоциональную опустошенность", options));
        
        // Фаза "Истощение"
        questions.add(new Question("17. Я не могу вникать в проблемы своих коллег и клиентов", options));
        questions.add(new Question("18. Мне безразлично то, что происходит с моими коллегами", options));
        questions.add(new Question("19. Я чувствую, что нахожусь на грани нервного срыва", options));
        questions.add(new Question("20. Я чувствую, что мои нервы натянуты до предела", options));
        questions.add(new Question("21. Я чувствую опустошенность и усталость после рабочего дня", options));
        questions.add(new Question("22. Я сомневаюсь в значимости моей работы", options));
        questions.add(new Question("23. Я испытываю разочарование в своей профессии", options));
        questions.add(new Question("24. Я не верю, что могу что-то изменить к лучшему в своей работе", options));
        
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
} 