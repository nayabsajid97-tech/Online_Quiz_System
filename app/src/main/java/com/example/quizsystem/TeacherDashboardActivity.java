package com.example.quizsystem;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;

public class TeacherDashboardActivity extends AppCompatActivity {
    private TextView welcomeTextView;
    private Button addQuizButton, viewResultsButton;
    private RecyclerView quizRecyclerView;
    private QuizAdapter quizAdapter;
    private ArrayList<String> quizList;
    private FirebaseAuth mAuth;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);

        welcomeTextView = findViewById(R.id.welcomeTextView);
        addQuizButton = findViewById(R.id.addQuizButton);
        viewResultsButton = findViewById(R.id.viewResultsButton);
        quizRecyclerView = findViewById(R.id.quizRecyclerView);

        // Initialize the quizList first
        quizList = new ArrayList<>();

        // Initialize the adapter with the quizList
        quizAdapter = new QuizAdapter(quizList, true); // true for teacher mode

        // Set up RecyclerView
        quizRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        quizRecyclerView.setAdapter(quizAdapter);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Check if user is actually a teacher
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String savedRole = sharedPreferences.getString(currentUser.getEmail(), "");

            if (!savedRole.equals("teacher")) {
                // User is not registered as teacher
                mAuth.signOut();
                Toast.makeText(this, "Please login as Teacher", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            welcomeTextView.setText("Welcome, Teacher " + currentUser.getEmail());
            loadTeacherQuizzes(currentUser.getUid());
        } else {
            // Not logged in
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        addQuizButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TeacherDashboardActivity.this, QuizActivity.class);
                intent.putExtra("mode", "add");
                startActivity(intent);
            }
        });

        viewResultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TeacherDashboardActivity.this, ResultActivity.class);
                intent.putExtra("role", "teacher");
                startActivity(intent);
            }
        });

        // Set click listener for quiz items (for editing)
        quizAdapter.setOnItemClickListener(new QuizAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String quizName = quizList.get(position);
                if (!quizName.equals("No quizzes yet. Click 'Add New Quiz' to create one.")) {
                    Intent intent = new Intent(TeacherDashboardActivity.this, QuizActivity.class);
                    intent.putExtra("mode", "edit");
                    intent.putExtra("quizName", quizName);
                    startActivity(intent);
                }
            }
        });

        // Set delete listener for quiz items
        quizAdapter.setOnDeleteClickListener(new QuizAdapter.OnDeleteClickListener() {
            @Override
            public void onDeleteClick(int position) {
                String quizName = quizList.get(position);
                if (!quizName.equals("No quizzes yet. Click 'Add New Quiz' to create one.")) {
                    showDeleteConfirmationDialog(position, quizName);
                }
            }
        });
    }

    private void showDeleteConfirmationDialog(final int position, final String quizName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Quiz");
        builder.setMessage("Are you sure you want to delete '" + quizName + "'?\nThis will also delete all questions and results for this quiz.");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteQuiz(position, quizName);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteQuiz(int position, String quizName) {
        int quizId = dbHelper.getQuizIdByName(quizName);
        if (quizId != -1) {
            dbHelper.deleteQuiz(quizId);
            Toast.makeText(this, "Quiz deleted successfully", Toast.LENGTH_SHORT).show();

            // Remove from list and update UI
            quizList.remove(position);
            quizAdapter.notifyItemRemoved(position);

            // If list is empty, show message
            if (quizList.isEmpty()) {
                quizList.add("No quizzes yet. Click 'Add New Quiz' to create one.");
                quizAdapter.notifyDataSetChanged();
            }
        } else {
            Toast.makeText(this, "Failed to delete quiz", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTeacherQuizzes(String teacherId) {
        ArrayList<String> newQuizList = dbHelper.getQuizzesByTeacher(teacherId);
        if (newQuizList.isEmpty()) {
            newQuizList = new ArrayList<>();
            newQuizList.add("No quizzes yet. Click 'Add New Quiz' to create one.");
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
            startActivity(new Intent(TeacherDashboardActivity.this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh quiz list when returning to this activity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadTeacherQuizzes(currentUser.getUid());
        }
    }
}