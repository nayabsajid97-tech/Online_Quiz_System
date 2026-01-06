package com.example.quizsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {
    private TextView resultTitleTextView, quizNameTextView, studentNameTextView, scoreTextView, percentageTextView, statusTextView;
    private LinearLayout singleResultLayout;
    private RecyclerView resultsRecyclerView;
    private Button backButton;
    private String role;
    private FirebaseAuth mAuth;
    private DBHelper dbHelper;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);
        currentUser = mAuth.getCurrentUser();

        resultTitleTextView = findViewById(R.id.resultTitleTextView);
        singleResultLayout = findViewById(R.id.singleResultLayout);
        quizNameTextView = findViewById(R.id.quizNameTextView);
        studentNameTextView = findViewById(R.id.studentNameTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        percentageTextView = findViewById(R.id.percentageTextView);
        statusTextView = findViewById(R.id.statusTextView);
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);
        backButton = findViewById(R.id.backButton);

        Intent intent = getIntent();
        role = intent.getStringExtra("role");

        if (role == null) {
            role = "student";
        }

        if (role.equals("teacher")) {
            resultTitleTextView.setText("All Student Results");
            singleResultLayout.setVisibility(View.GONE);
            resultsRecyclerView.setVisibility(View.VISIBLE);
            loadTeacherResults();
        } else {
            resultTitleTextView.setText("Your Result");
            singleResultLayout.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.GONE);

            // Check if coming from quiz submission or from dashboard
            if (intent.hasExtra("score") && intent.hasExtra("total")) {
                loadStudentResult(intent);
            } else {
                loadStudentAllResults();
            }
        }

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateBack();
            }
        });
    }

    private void loadStudentResult(Intent intent) {
        int score = intent.getIntExtra("score", 0);
        int total = intent.getIntExtra("total", 1);
        String studentName = intent.getStringExtra("studentName");
        String quizName = intent.getStringExtra("quizName");

        if (studentName == null && currentUser != null) {
            studentName = currentUser.getEmail();
        }

        if (quizName == null) {
            quizName = "Quiz";
        }

        double percentage = (score * 100.0) / total;
        String status = percentage >= 60 ? "Pass" : "Fail";

        quizNameTextView.setText("Quiz: " + quizName);
        studentNameTextView.setText("Student: " + studentName);
        scoreTextView.setText("Score: " + score + " / " + total);
        percentageTextView.setText("Percentage: " + String.format("%.1f", percentage) + "%");
        statusTextView.setText("Status: " + status);

        if (status.equals("Pass")) {
            statusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            statusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void loadStudentAllResults() {
        if (currentUser != null) {
            // Only show database results (not AI quiz results)
            ArrayList<String[]> results = dbHelper.getStudentResults(currentUser.getUid());

            if (results.isEmpty()) {
                singleResultLayout.setVisibility(View.VISIBLE);
                resultsRecyclerView.setVisibility(View.GONE);
                quizNameTextView.setText("No Results Yet");
                studentNameTextView.setText("Student: " + currentUser.getEmail());
                scoreTextView.setText("Score: 0 / 0");
                percentageTextView.setText("Percentage: 0%");
                statusTextView.setText("Status: N/A");
                statusTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            } else {
                // Show all results in RecyclerView
                singleResultLayout.setVisibility(View.GONE);
                resultsRecyclerView.setVisibility(View.VISIBLE);

                resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                ResultsAdapter resultsAdapter = new ResultsAdapter(results);
                resultsRecyclerView.setAdapter(resultsAdapter);
            }
        }
    }

    private void loadTeacherResults() {
        ArrayList<String[]> results = dbHelper.getAllResults();

        if (results.isEmpty()) {
            results.add(new String[]{"No results", "No quizzes attempted", "0/0", "0%", "N/A"});
        }

        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ResultsAdapter resultsAdapter = new ResultsAdapter(results);
        resultsRecyclerView.setAdapter(resultsAdapter);
    }

    private void navigateBack() {
        Intent intent;
        if (role.equals("teacher")) {
            intent = new Intent(ResultActivity.this, TeacherDashboardActivity.class);
        } else {
            intent = new Intent(ResultActivity.this, StudentDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }
}

class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {
    private ArrayList<String[]> results;

    public ResultsAdapter(ArrayList<String[]> results) {
        this.results = results;
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String[] result = results.get(position);
        holder.studentNameTextView.setText(result[0]);
        holder.quizNameTextView.setText(result[1]);
        holder.scoreTextView.setText(result[2]);
        holder.percentageTextView.setText(result[3]);
        holder.statusTextView.setText(result[4]);

        if (result[4].equals("Pass")) {
            holder.statusTextView.setTextColor(android.graphics.Color.GREEN);
        } else if (result[4].equals("Fail")) {
            holder.statusTextView.setTextColor(android.graphics.Color.RED);
        } else {
            holder.statusTextView.setTextColor(android.graphics.Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameTextView, quizNameTextView, scoreTextView, percentageTextView, statusTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.itemStudentName);
            quizNameTextView = itemView.findViewById(R.id.itemQuizName);
            scoreTextView = itemView.findViewById(R.id.itemScore);
            percentageTextView = itemView.findViewById(R.id.itemPercentage);
            statusTextView = itemView.findViewById(R.id.itemStatus);
        }
    }
}