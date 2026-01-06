package com.example.quizsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.ViewHolder> {
    private ArrayList<String> quizList;
    private boolean isTeacherMode;
    private OnItemClickListener listener;
    private OnDeleteClickListener deleteListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    // Updated constructor
    public QuizAdapter(ArrayList<String> quizList, boolean isTeacherMode) {
        this.quizList = quizList;
        this.isTeacherMode = isTeacherMode;
    }

    // Keep old constructor for backward compatibility
    public QuizAdapter(ArrayList<String> quizList) {
        this(quizList, false);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (isTeacherMode) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quiz_teacher, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quiz, parent, false);
        }
        return new ViewHolder(view, isTeacherMode);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String quizName = quizList.get(position);
        holder.quizNameTextView.setText(quizName);
        holder.teacherNameTextView.setText("Teacher: Admin");

        // Set delete button visibility and listener for teacher mode
        if (isTeacherMode && holder.deleteButton != null) {
            if (quizName.equals("No quizzes yet. Click 'Add New Quiz' to create one.")) {
                holder.deleteButton.setVisibility(View.GONE);
            } else {
                holder.deleteButton.setVisibility(View.VISIBLE);
                holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (deleteListener != null && position != RecyclerView.NO_POSITION) {
                            deleteListener.onDeleteClick(position);
                        }
                    }
                });
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return quizList.size();
    }

    public void updateQuizList(ArrayList<String> newQuizList) {
        this.quizList = newQuizList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView quizNameTextView;
        TextView teacherNameTextView;
        Button deleteButton; // For teacher mode

        public ViewHolder(View itemView, boolean isTeacherMode) {
            super(itemView);
            quizNameTextView = itemView.findViewById(R.id.quizNameTextView);
            teacherNameTextView = itemView.findViewById(R.id.teacherNameTextView);

            if (isTeacherMode) {
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }

        // Keep old constructor for backward compatibility
        public ViewHolder(View itemView) {
            this(itemView, false);
        }
    }
}