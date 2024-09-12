package com.itech.innogas

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val forgotPasswordBttn = findViewById<TextView>(R.id.forgot_password_bttn)
        val signupBttn = findViewById<TextView>(R.id.sign_up_bttn)

        forgotPasswordBttn.setOnClickListener {
            val intent = Intent (this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        signupBttn.setOnClickListener {
            val intent = Intent (this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
}