package com.example.studentattendance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE)

        // If data is already saved, skip this screen and go directly to the Camera
        if (sharedPreferences.contains("studentName") && sharedPreferences.contains("enrollmentId")) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_setup)

        val etStudentName = findViewById<EditText>(R.id.etStudentName)
        val etEnrollmentId = findViewById<EditText>(R.id.etEnrollmentId)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val name = etStudentName.text.toString().trim()
            val enrollmentId = etEnrollmentId.text.toString().trim()

            if (name.isEmpty() || enrollmentId.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save the data permanently
            sharedPreferences.edit().apply {
                putString("studentName", name)
                putString("enrollmentId", enrollmentId)
                apply()
            }

            Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}