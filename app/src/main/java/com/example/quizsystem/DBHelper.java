package com.example.quizsystem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "QuizSystem.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_QUIZ = "quiz";
    private static final String COLUMN_QUIZ_ID = "quiz_id";
    private static final String COLUMN_QUIZ_NAME = "quiz_name";
    private static final String COLUMN_TEACHER_ID = "teacher_id";

    private static final String TABLE_QUESTION = "question";
    private static final String COLUMN_QUESTION_ID = "question_id";
    private static final String COLUMN_QUESTION_TEXT = "question";
    private static final String COLUMN_OPTION_A = "option_a";
    private static final String COLUMN_OPTION_B = "option_b";
    private static final String COLUMN_OPTION_C = "option_c";
    private static final String COLUMN_OPTION_D = "option_d";
    private static final String COLUMN_CORRECT_OPTION = "correct_option";

    private static final String TABLE_RESULT = "result";
    private static final String COLUMN_RESULT_ID = "result_id";
    private static final String COLUMN_STUDENT_ID = "student_id";
    private static final String COLUMN_STUDENT_NAME = "student_name";
    private static final String COLUMN_MARKS = "marks";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createQuizTable = "CREATE TABLE " + TABLE_QUIZ + " (" +
                COLUMN_QUIZ_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_QUIZ_NAME + " TEXT, " +
                COLUMN_TEACHER_ID + " TEXT)";

        String createQuestionTable = "CREATE TABLE " + TABLE_QUESTION + " (" +
                COLUMN_QUESTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_QUIZ_ID + " INTEGER, " +
                COLUMN_QUESTION_TEXT + " TEXT, " +
                COLUMN_OPTION_A + " TEXT, " +
                COLUMN_OPTION_B + " TEXT, " +
                COLUMN_OPTION_C + " TEXT, " +
                COLUMN_OPTION_D + " TEXT, " +
                COLUMN_CORRECT_OPTION + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_QUIZ_ID + ") REFERENCES " + TABLE_QUIZ + "(" + COLUMN_QUIZ_ID + "))";

        String createResultTable = "CREATE TABLE " + TABLE_RESULT + " (" +
                COLUMN_RESULT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_QUIZ_ID + " INTEGER, " +
                COLUMN_STUDENT_ID + " TEXT, " +
                COLUMN_STUDENT_NAME + " TEXT, " +
                COLUMN_MARKS + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_QUIZ_ID + ") REFERENCES " + TABLE_QUIZ + "(" + COLUMN_QUIZ_ID + "))";

        db.execSQL(createQuizTable);
        db.execSQL(createQuestionTable);
        db.execSQL(createResultTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUIZ);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESULT);
        onCreate(db);
    }

    public long addQuiz(String quizName, String teacherId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUIZ_NAME, quizName);
        values.put(COLUMN_TEACHER_ID, teacherId);
        return db.insert(TABLE_QUIZ, null, values);
    }

    public long addQuestion(int quizId, String question, String optionA, String optionB, String optionC, String optionD, String correctOption) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUIZ_ID, quizId);
        values.put(COLUMN_QUESTION_TEXT, question);
        values.put(COLUMN_OPTION_A, optionA);
        values.put(COLUMN_OPTION_B, optionB);
        values.put(COLUMN_OPTION_C, optionC);
        values.put(COLUMN_OPTION_D, optionD);
        values.put(COLUMN_CORRECT_OPTION, correctOption);
        return db.insert(TABLE_QUESTION, null, values);
    }

    public long addResult(int quizId, String studentId, String studentName, int marks) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUIZ_ID, quizId);
        values.put(COLUMN_STUDENT_ID, studentId);
        values.put(COLUMN_STUDENT_NAME, studentName);
        values.put(COLUMN_MARKS, marks);
        return db.insert(TABLE_RESULT, null, values);
    }

    public ArrayList<String> getQuizzesByTeacher(String teacherId) {
        ArrayList<String> quizzes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_QUIZ + " WHERE " + COLUMN_TEACHER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{teacherId});

        if (cursor.moveToFirst()) {
            do {
                quizzes.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_NAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return quizzes;
    }

    public ArrayList<String> getAllQuizzes() {
        ArrayList<String> quizzes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_QUIZ;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                quizzes.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_NAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return quizzes;
    }

    public ArrayList<String[]> getQuestionsByQuiz(int quizId) {
        ArrayList<String[]> questions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_QUESTION + " WHERE " + COLUMN_QUIZ_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(quizId)});

        if (cursor.moveToFirst()) {
            do {
                String[] questionData = new String[6];
                questionData[0] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUESTION_TEXT));
                questionData[1] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPTION_A));
                questionData[2] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPTION_B));
                questionData[3] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPTION_C));
                questionData[4] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPTION_D));
                questionData[5] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CORRECT_OPTION));
                questions.add(questionData);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return questions;
    }

    public ArrayList<String[]> getResultsByQuiz(int quizId) {
        ArrayList<String[]> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_RESULT + " WHERE " + COLUMN_QUIZ_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(quizId)});

        if (cursor.moveToFirst()) {
            do {
                String[] resultData = new String[4];
                resultData[0] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_NAME));
                resultData[1] = String.valueOf(getQuizNameById(quizId));
                resultData[2] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKS));
                resultData[3] = calculateStatus(Integer.parseInt(resultData[2]));
                results.add(resultData);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return results;
    }

    public ArrayList<String[]> getAllResults() {
        ArrayList<String[]> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Get all results with quiz names and total questions count
        String query = "SELECT r.*, q." + COLUMN_QUIZ_NAME + ", " +
                "(SELECT COUNT(*) FROM " + TABLE_QUESTION + " qs WHERE qs." + COLUMN_QUIZ_ID + " = r." + COLUMN_QUIZ_ID + ") as total_questions " +
                "FROM " + TABLE_RESULT + " r " +
                "JOIN " + TABLE_QUIZ + " q ON r." + COLUMN_QUIZ_ID + " = q." + COLUMN_QUIZ_ID;

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String[] resultData = new String[5];
                resultData[0] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_NAME));
                resultData[1] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_NAME));

                int marks = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MARKS));
                int totalQuestions = cursor.getInt(cursor.getColumnIndexOrThrow("total_questions"));

                // Use actual total questions from the quiz
                if (totalQuestions > 0) {
                    resultData[2] = marks + "/" + totalQuestions;
                    int percentage = (marks * 100) / totalQuestions;
                    resultData[3] = percentage + "%";
                    resultData[4] = calculateStatus(percentage);
                } else {
                    resultData[2] = marks + "/0";
                    resultData[3] = "0%";
                    resultData[4] = "N/A";
                }

                results.add(resultData);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return results;
    }

    public ArrayList<String[]> getStudentResults(String studentId) {
        ArrayList<String[]> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT r.*, q." + COLUMN_QUIZ_NAME + ", " +
                "(SELECT COUNT(*) FROM " + TABLE_QUESTION + " qs WHERE qs." + COLUMN_QUIZ_ID + " = r." + COLUMN_QUIZ_ID + ") as total_questions " +
                "FROM " + TABLE_RESULT + " r " +
                "JOIN " + TABLE_QUIZ + " q ON r." + COLUMN_QUIZ_ID + " = q." + COLUMN_QUIZ_ID + " " +
                "WHERE r." + COLUMN_STUDENT_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{studentId});

        if (cursor.moveToFirst()) {
            do {
                String[] resultData = new String[5];
                resultData[0] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_NAME));
                resultData[1] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_NAME));

                int marks = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MARKS));
                int totalQuestions = cursor.getInt(cursor.getColumnIndexOrThrow("total_questions"));

                if (totalQuestions > 0) {
                    resultData[2] = marks + "/" + totalQuestions;
                    int percentage = (marks * 100) / totalQuestions;
                    resultData[3] = percentage + "%";
                    resultData[4] = calculateStatus(percentage);
                } else {
                    resultData[2] = marks + "/0";
                    resultData[3] = "0%";
                    resultData[4] = "N/A";
                }

                results.add(resultData);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return results;
    }

    public int getQuizIdByName(String quizName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_QUIZ_ID + " FROM " + TABLE_QUIZ + " WHERE " + COLUMN_QUIZ_NAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{quizName});

        int quizId = -1;
        if (cursor.moveToFirst()) {
            quizId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_ID));
        }
        cursor.close();
        return quizId;
    }

    public String getQuizNameById(int quizId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_QUIZ_NAME + " FROM " + TABLE_QUIZ + " WHERE " + COLUMN_QUIZ_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(quizId)});

        String quizName = "";
        if (cursor.moveToFirst()) {
            quizName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_NAME));
        }
        cursor.close();
        return quizName;
    }

    public void deleteQuiz(int quizId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QUESTION, COLUMN_QUIZ_ID + " = ?", new String[]{String.valueOf(quizId)});
        db.delete(TABLE_RESULT, COLUMN_QUIZ_ID + " = ?", new String[]{String.valueOf(quizId)});
        db.delete(TABLE_QUIZ, COLUMN_QUIZ_ID + " = ?", new String[]{String.valueOf(quizId)});
    }

    public int getStudentAttemptCount(int quizId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_RESULT + " WHERE " + COLUMN_QUIZ_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(quizId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    private String calculateStatus(int percentage) {
        return percentage >= 60 ? "Pass" : "Fail";
    }
}