package com.example.quizsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuizActivity extends AppCompatActivity {
    private TextView quizTitleTextView, questionTextView;
    private LinearLayout addQuizLayout, attemptQuizLayout;
    private EditText quizNameEditText, questionEditText, optionAEditText, optionBEditText, optionCEditText, optionDEditText;
    private RadioGroup correctAnswerRadioGroup, answerRadioGroup;
    private Button saveQuizButton, nextButton, submitButton, addAnotherButton;
    private RadioButton answerA, answerB, answerC, answerD;
    private String mode;
    private ArrayList<String> questions;
    private ArrayList<String[]> options;
    private ArrayList<Integer> correctAnswers;
    private int currentQuestion = 0;
    private int score = 0;
    private FirebaseAuth mAuth;
    private DBHelper dbHelper;
    private String currentQuizName;
    private boolean aiQuestionsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);

        Intent intent = getIntent();
        mode = intent.getStringExtra("mode");

        quizTitleTextView = findViewById(R.id.quizTitleTextView);
        addQuizLayout = findViewById(R.id.addQuizLayout);
        attemptQuizLayout = findViewById(R.id.attemptQuizLayout);

        quizNameEditText = findViewById(R.id.quizNameEditText);
        questionEditText = findViewById(R.id.questionEditText);
        optionAEditText = findViewById(R.id.optionAEditText);
        optionBEditText = findViewById(R.id.optionBEditText);
        optionCEditText = findViewById(R.id.optionCEditText);
        optionDEditText = findViewById(R.id.optionDEditText);
        correctAnswerRadioGroup = findViewById(R.id.correctAnswerRadioGroup);
        saveQuizButton = findViewById(R.id.saveQuizButton);
        addAnotherButton = findViewById(R.id.addAnotherButton);

        questionTextView = findViewById(R.id.questionTextView);
        answerRadioGroup = findViewById(R.id.answerRadioGroup);
        answerA = findViewById(R.id.answerA);
        answerB = findViewById(R.id.answerB);
        answerC = findViewById(R.id.answerC);
        answerD = findViewById(R.id.answerD);
        nextButton = findViewById(R.id.nextButton);
        submitButton = findViewById(R.id.submitButton);

        if (mode.equals("add") || mode.equals("edit")) {
            quizTitleTextView.setText("Create New Quiz");
            addQuizLayout.setVisibility(View.VISIBLE);
            attemptQuizLayout.setVisibility(View.GONE);
            addAnotherButton.setVisibility(View.VISIBLE);

            if (mode.equals("edit")) {
                currentQuizName = intent.getStringExtra("quizName");
                quizNameEditText.setText(currentQuizName);
                quizTitleTextView.setText("Edit Quiz: " + currentQuizName);
                quizNameEditText.setEnabled(false);
            }

            saveQuizButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveQuiz();
                }
            });

            addAnotherButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addAnotherQuestion();
                }
            });

        } else if (mode.equals("attempt") || mode.equals("ai")) {
            quizTitleTextView.setText("Attempt Quiz");
            addQuizLayout.setVisibility(View.GONE);
            attemptQuizLayout.setVisibility(View.VISIBLE);
            addAnotherButton.setVisibility(View.GONE);

            questions = new ArrayList<>();
            options = new ArrayList<>();
            correctAnswers = new ArrayList<>();

            if (mode.equals("ai")) {
                quizTitleTextView.setText("AI Quiz - Loading...");
                loadAIQuestions();
            } else {
                currentQuizName = intent.getStringExtra("quizName");
                loadQuestionsFromDB();
                quizTitleTextView.setText("Quiz: " + currentQuizName);
                displayQuestion(); // Manual quiz can display immediately
            }

            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (answerRadioGroup.getCheckedRadioButtonId() == -1) {
                        Toast.makeText(QuizActivity.this, "Please select an answer", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    checkAnswer();
                    currentQuestion++;
                    if (currentQuestion < questions.size()) {
                        displayQuestion();
                    } else {
                        submitQuiz();
                    }
                }
            });

            submitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (answerRadioGroup.getCheckedRadioButtonId() == -1) {
                        Toast.makeText(QuizActivity.this, "Please select an answer", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    checkAnswer();
                    submitQuiz();
                }
            });
        }
    }

    private void saveQuiz() {
        String quizName = quizNameEditText.getText().toString().trim();
        String question = questionEditText.getText().toString().trim();
        String optionA = optionAEditText.getText().toString().trim();
        String optionB = optionBEditText.getText().toString().trim();
        String optionC = optionCEditText.getText().toString().trim();
        String optionD = optionDEditText.getText().toString().trim();

        if (quizName.isEmpty() || question.isEmpty() || optionA.isEmpty() ||
                optionB.isEmpty() || optionC.isEmpty() || optionD.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = correctAnswerRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Select correct answer", Toast.LENGTH_SHORT).show();
            return;
        }

        String correctOption = "A";
        if (selectedId == R.id.radioB) correctOption = "B";
        else if (selectedId == R.id.radioC) correctOption = "C";
        else if (selectedId == R.id.radioD) correctOption = "D";

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String teacherId = currentUser != null ? currentUser.getUid() : "teacher1";

        long quizId;
        if (mode.equals("add")) {
            quizId = dbHelper.addQuiz(quizName, teacherId);
            currentQuizName = quizName;
        } else {
            quizId = dbHelper.getQuizIdByName(currentQuizName);
        }

        if (quizId != -1) {
            dbHelper.addQuestion((int)quizId, question, optionA, optionB, optionC, optionD, correctOption);
            Toast.makeText(this, "Question saved successfully", Toast.LENGTH_SHORT).show();
            clearQuestionFields();
        } else {
            Toast.makeText(this, "Error saving question", Toast.LENGTH_SHORT).show();
        }
    }

    private void addAnotherQuestion() {
        String question = questionEditText.getText().toString().trim();
        String optionA = optionAEditText.getText().toString().trim();
        String optionB = optionBEditText.getText().toString().trim();
        String optionC = optionCEditText.getText().toString().trim();
        String optionD = optionDEditText.getText().toString().trim();

        if (question.isEmpty() || optionA.isEmpty() || optionB.isEmpty() || optionC.isEmpty() || optionD.isEmpty()) {
            Toast.makeText(this, "Fill all question fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = correctAnswerRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Select correct answer", Toast.LENGTH_SHORT).show();
            return;
        }

        String correctOption = "A";
        if (selectedId == R.id.radioB) correctOption = "B";
        else if (selectedId == R.id.radioC) correctOption = "C";
        else if (selectedId == R.id.radioD) correctOption = "D";

        int quizId = dbHelper.getQuizIdByName(currentQuizName);
        if (quizId != -1) {
            dbHelper.addQuestion(quizId, question, optionA, optionB, optionC, optionD, correctOption);
            Toast.makeText(this, "Question added successfully", Toast.LENGTH_SHORT).show();
            clearQuestionFields();
        }
    }

    private void clearQuestionFields() {
        questionEditText.setText("");
        optionAEditText.setText("");
        optionBEditText.setText("");
        optionCEditText.setText("");
        optionDEditText.setText("");
        correctAnswerRadioGroup.clearCheck();
        questionEditText.requestFocus();
    }

    private void loadAIQuestions() {
        // Show loading message
        questionTextView.setText("Loading AI questions... Please wait.");
        nextButton.setEnabled(false);
        nextButton.setVisibility(View.GONE);
        submitButton.setEnabled(false);
        submitButton.setVisibility(View.GONE);

        // Disable radio buttons while loading
        answerA.setEnabled(false);
        answerB.setEnabled(false);
        answerC.setEnabled(false);
        answerD.setEnabled(false);

        // Use thread pool for API call
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Using OpenTDB API for quiz questions (5 questions, multiple choice)
                URL url = new URL("https://opentdb.com/api.php?amount=5&type=multiple");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray results = jsonResponse.getJSONArray("results");

                    // Clear any existing questions
                    questions.clear();
                    options.clear();
                    correctAnswers.clear();

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject questionObj = results.getJSONObject(i);

                        // Decode HTML entities
                        String question = android.text.Html.fromHtml(questionObj.getString("question")).toString();
                        String correctAnswer = android.text.Html.fromHtml(questionObj.getString("correct_answer")).toString();

                        JSONArray incorrectAnswers = questionObj.getJSONArray("incorrect_answers");
                        ArrayList<String> allAnswers = new ArrayList<>();
                        allAnswers.add(correctAnswer);

                        for (int j = 0; j < incorrectAnswers.length(); j++) {
                            String incorrectAnswer = android.text.Html.fromHtml(incorrectAnswers.getString(j)).toString();
                            allAnswers.add(incorrectAnswer);
                        }

                        // Shuffle answers
                        Collections.shuffle(allAnswers);

                        // Find correct answer index after shuffling
                        int correctIndex = allAnswers.indexOf(correctAnswer);

                        // Add to lists
                        questions.add(question);
                        options.add(allAnswers.toArray(new String[0]));
                        correctAnswers.add(correctIndex);
                    }

                    runOnUiThread(() -> {
                        if (questions.size() >= 5) {
                            aiQuestionsLoaded = true;
                            quizTitleTextView.setText("AI Quiz");
                            displayQuestion();
                            enableQuizUI();
                            Toast.makeText(QuizActivity.this, questions.size() + " AI Questions Loaded!", Toast.LENGTH_SHORT).show();
                        } else {
                            // If we don't have 5 questions, use fallback
                            loadFallbackAIQuestions();
                        }
                    });

                } else {
                    runOnUiThread(() -> {
                        loadFallbackAIQuestions();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    // Load fallback questions if API fails
                    loadFallbackAIQuestions();
                });
            }
        });
        executor.shutdown();
    }

    private void enableQuizUI() {
        // Enable all UI elements for attempting quiz
        nextButton.setEnabled(true);
        nextButton.setVisibility(View.VISIBLE);
        submitButton.setEnabled(true);
        answerA.setEnabled(true);
        answerB.setEnabled(true);
        answerC.setEnabled(true);
        answerD.setEnabled(true);
    }

    private void loadFallbackAIQuestions() {
        // Clear any existing questions
        questions.clear();
        options.clear();
        correctAnswers.clear();

        // Fallback questions - ensure we have 5 questions
        questions.add("What is the capital of France?");
        options.add(new String[]{"London", "Berlin", "Paris", "Madrid"});
        correctAnswers.add(2);

        questions.add("Which planet is known as the Red Planet?");
        options.add(new String[]{"Venus", "Mars", "Jupiter", "Saturn"});
        correctAnswers.add(1);

        questions.add("What is 2 + 2?");
        options.add(new String[]{"3", "4", "5", "6"});
        correctAnswers.add(1);

        questions.add("Who painted the Mona Lisa?");
        options.add(new String[]{"Vincent van Gogh", "Leonardo da Vinci", "Pablo Picasso", "Michelangelo"});
        correctAnswers.add(1);

        questions.add("What is the largest ocean on Earth?");
        options.add(new String[]{"Atlantic Ocean", "Indian Ocean", "Arctic Ocean", "Pacific Ocean"});
        correctAnswers.add(3);

        runOnUiThread(() -> {
            aiQuestionsLoaded = true;
            quizTitleTextView.setText("AI Quiz");
            displayQuestion();
            enableQuizUI();
            Toast.makeText(this, "5 AI Questions Loaded!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadQuestionsFromDB() {
        int quizId = dbHelper.getQuizIdByName(currentQuizName);
        if (quizId != -1) {
            ArrayList<String[]> dbQuestions = dbHelper.getQuestionsByQuiz(quizId);
            for (String[] questionData : dbQuestions) {
                questions.add(questionData[0]);
                options.add(new String[]{questionData[1], questionData[2], questionData[3], questionData[4]});
                String correct = questionData[5];
                int correctIndex = 0;
                if (correct.equals("B")) correctIndex = 1;
                else if (correct.equals("C")) correctIndex = 2;
                else if (correct.equals("D")) correctIndex = 3;
                correctAnswers.add(correctIndex);
            }
        }

        if (questions.isEmpty()) {
            Toast.makeText(this, "No questions available for this quiz", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayQuestion() {
        if (currentQuestion < questions.size()) {
            questionTextView.setText("Q" + (currentQuestion + 1) + ": " + questions.get(currentQuestion));
            String[] currentOptions = options.get(currentQuestion);
            answerA.setText("A: " + currentOptions[0]);
            answerB.setText("B: " + currentOptions[1]);
            answerC.setText("C: " + currentOptions[2]);
            answerD.setText("D: " + currentOptions[3]);
            answerRadioGroup.clearCheck();

            // Update button visibility
            if (currentQuestion == questions.size() - 1) {
                nextButton.setVisibility(View.GONE);
                submitButton.setVisibility(View.VISIBLE);
            } else {
                nextButton.setVisibility(View.VISIBLE);
                submitButton.setVisibility(View.GONE);
            }
        }
    }

    private void checkAnswer() {
        int selectedId = answerRadioGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            int selectedAnswer = -1;
            if (selectedId == R.id.answerA) selectedAnswer = 0;
            else if (selectedId == R.id.answerB) selectedAnswer = 1;
            else if (selectedId == R.id.answerC) selectedAnswer = 2;
            else if (selectedId == R.id.answerD) selectedAnswer = 3;

            if (selectedAnswer == correctAnswers.get(currentQuestion)) {
                score++;
            }
        }
    }

    private void submitQuiz() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String studentName = currentUser != null ? currentUser.getEmail() : "Student";

        if (mode.equals("attempt")) {
            String studentId = currentUser != null ? currentUser.getUid() : "student1";
            int quizId = dbHelper.getQuizIdByName(currentQuizName);
            if (quizId != -1) {
                dbHelper.addResult(quizId, studentId, studentName, score);
            }
        }
        // AI quiz results are not saved to database

        Intent resultIntent = new Intent(QuizActivity.this, ResultActivity.class);
        resultIntent.putExtra("score", score);
        resultIntent.putExtra("total", questions.size());
        resultIntent.putExtra("studentName", studentName);
        resultIntent.putExtra("quizName", mode.equals("ai") ? "AI Quiz" : currentQuizName);
        resultIntent.putExtra("role", "student");
        resultIntent.putExtra("quizType", mode); // "ai" or "attempt"
        startActivity(resultIntent);
        finish();
    }
}