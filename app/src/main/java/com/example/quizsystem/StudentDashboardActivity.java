package com.example.quizsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;

public class StudentDashboardActivity extends AppCompatActivity {
    private TextView welcomeTextView;
    private Button aiQuizButton, viewResultsButton;
    private RecyclerView quizRecyclerView;
    private QuizAdapter quizAdapter;
    private ArrayList<String> quizList;
    private FirebaseAuth mAuth;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);

        welcomeTextView = findViewById(R.id.welcomeTextView);
        aiQuizButton = findViewById(R.id.aiQuizButton);
        viewResultsButton = findViewById(R.id.viewResultsButton);
        quizRecyclerView = findViewById(R.id.quizRecyclerView);

        // Initialize the quizList first
        quizList = new ArrayList<>();

        // Initialize the adapter with the quizList
// In StudentDashboardActivity.java, update this line:
        quizAdapter = new QuizAdapter(quizList, false); // false for student mode
        // Set up RecyclerView
        quizRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        quizRecyclerView.setAdapter(quizAdapter);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Check if user is actually a student
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String savedRole = sharedPreferences.getString(currentUser.getEmail(), "");

            if (!savedRole.equals("student")) {
                // User is not registered as student
                mAuth.signOut();
                Toast.makeText(this, "Please login as Student", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            welcomeTextView.setText("Welcome, Student " + currentUser.getEmail());
            loadAllQuizzes();
        } else {
            // Not logged in
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        aiQuizButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudentDashboardActivity.this, QuizActivity.class);
                intent.putExtra("mode", "ai");
                startActivity(intent);
            }
        });

        viewResultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudentDashboardActivity.this, ResultActivity.class);
                intent.putExtra("role", "student");
                startActivity(intent);
            }
        });

        quizAdapter.setOnItemClickListener(new QuizAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String quizName = quizList.get(position);
                if (!quizName.equals("No quizzes available")) {
                    Intent intent = new Intent(StudentDashboardActivity.this, QuizActivity.class);
                    intent.putExtra("mode", "attempt");
                    intent.putExtra("quizName", quizName);
                    startActivity(intent);
                }
            }
        });
    }

    private void loadAllQuizzes() {
        ArrayList<String> newQuizList = dbHelper.getAllQuizzes();
        if (newQuizList.isEmpty()) {
            newQuizList = new ArrayList<>();
            newQuizList.add("No quizzes available");
        }

        // Update the existing list
        quizList.clear();
        quizList.addAll(newQuizList);
        quizAdapter.updateQuizList(quizList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            mAuth.signOut();
            startActivity(new Intent(StudentDashboardActivity.this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh quiz list when returning to this activity
        loadAllQuizzes();
    }
}