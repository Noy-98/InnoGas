package com.itech.innogas

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        val Email : TextInputEditText = findViewById(R.id.email)
        val Password : TextInputEditText = findViewById(R.id.password)
        val rememberMe : CheckBox = findViewById(R.id.remember_me_checkbox)

        val ProgressBar : ProgressBar = findViewById(R.id.loginProgressBar)

        val forgotPasswordBttn = findViewById<TextView>(R.id.forgot_password_bttn)
        val signupBttn = findViewById<TextView>(R.id.sign_up_bttn)
        val login : AppCompatButton = findViewById(R.id.login_bttn)

        // Load saved email and password
        val savedEmail = sharedPreferences.getString("email", "")
        val savedPassword = sharedPreferences.getString("password", "")
        val isRemembered = sharedPreferences.getBoolean("rememberMe", false)

        Email.setText(savedEmail)
        Password.setText(savedPassword)
        rememberMe.isChecked = isRemembered

        forgotPasswordBttn.setOnClickListener {
            val intent = Intent (this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        signupBttn.setOnClickListener {
            val intent = Intent (this, SignupActivity::class.java)
            startActivity(intent)
        }

        login.setOnClickListener {
            ProgressBar.visibility = View.VISIBLE

            val email = Email.text.toString()
            val pass = Password.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                if (email.isEmpty()) {
                    Email.error = "Email is Required!"
                }
                if (pass.isEmpty()) {
                    Password.error = "Password is Required!"
                }
                Toast.makeText(this,"All fields are Required!", Toast.LENGTH_SHORT).show()
                ProgressBar.visibility = View.GONE
            } else if (!email.matches(emailPattern.toRegex())) {
                Email.error = "Please Enter a Valid Email Address!"
                Toast.makeText(this,"Enter valid email address", Toast.LENGTH_SHORT).show()
                ProgressBar.visibility = View.GONE
            } else {
                auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {loginTask ->
                    if (loginTask.isSuccessful){
                        val user = auth.currentUser
                        if (user != null) {
                            if(user.isEmailVerified){
                                // Save email and password if "Remember Me" is checked
                                if (rememberMe.isChecked) {
                                    editor.putString("email", email)
                                    editor.putString("password", pass)
                                    editor.putBoolean("rememberMe", true)
                                    editor.apply()
                                } else {
                                    editor.clear()
                                    editor.apply()
                                }
                                // Retrieve user type from the database
                                database.child("UsersTbl").child(user.uid).child("user_type").addListenerForSingleValueEvent(object :
                                    ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        val userType = dataSnapshot.getValue(String::class.java)
                                        if (userType != null) {
                                            ProgressBar.visibility = View.GONE
                                            Toast.makeText(this@LoginActivity, "Login Successfully", Toast.LENGTH_SHORT).show()
                                            if (userType == "Vendor") {
                                                startActivity(Intent(this@LoginActivity, VendorDashboard::class.java))
                                            } else if (userType == "Customer") {
                                                startActivity(Intent(this@LoginActivity, CustomerDashboard::class.java))
                                            }
                                            finish()
                                        } else {
                                            ProgressBar.visibility = View.GONE
                                            Toast.makeText(this@LoginActivity, "User type not found.", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        ProgressBar.visibility = View.GONE
                                        Toast.makeText(this@LoginActivity, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                                    }

                                })
                            }else {
                                ProgressBar.visibility = View.GONE
                                Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }else{
                        ProgressBar.visibility = View.GONE
                        Toast.makeText(this, "Something went wrong, try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}