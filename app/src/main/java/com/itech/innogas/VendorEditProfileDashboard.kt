package com.itech.innogas

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask

class VendorEditProfileDashboard : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storageReference: StorageReference
    private lateinit var databaseReference: FirebaseDatabase
    private lateinit var progressBar: ProgressBar  // Add this line

    private lateinit var imageUri: Uri

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    imageUri = uri
                    val profileImageView = findViewById<ShapeableImageView>(R.id.profilepic)
                    Glide.with(this)
                        .load(imageUri)
                        .into(profileImageView)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vendor_edit_profile_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance()
        storageReference = FirebaseStorage.getInstance().reference

        // Initialize ProgressBar
        progressBar = findViewById(R.id.profileProgressBar)

        val backBttn = findViewById<FloatingActionButton>(R.id.go_back_bttn)
        val editBttn = findViewById<AppCompatButton>(R.id.edit_bttn)
        val edit_profile_picBttn = findViewById<ShapeableImageView>(R.id.profilepic)

        backBttn.setOnClickListener {
            finish()
        }

        edit_profile_picBttn.setOnClickListener {
            openImagePicker()
        }

        editBttn.setOnClickListener {
            updateProfile()
        }

    }

    private fun updateProfile() {
        val firstName = findViewById<TextInputEditText>(R.id.firstname).text.toString().trim()
        val lastName = findViewById<TextInputEditText>(R.id.lastname).text.toString().trim()
        val password = findViewById<TextInputEditText>(R.id.newpassword).text.toString().trim()
        val confirmPassword = findViewById<TextInputEditText>(R.id.confirmpass).text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required to fill in", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val usersReference = databaseReference.getReference("UsersTbl/$uid")

            // Show ProgressBar
            progressBar.visibility = View.VISIBLE

            // Update password
            if (password.length >= 6) {
                if (password == confirmPassword) {

                    // Update profile picture
                    if (::imageUri.isInitialized) {
                        val imageRef = storageReference.child("profile_pictures/${imageUri.lastPathSegment}")
                        imageRef.putFile(imageUri)
                            .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
                                imageRef.downloadUrl.addOnSuccessListener { uri: Uri ->
                                    usersReference.child("profile_pic").setValue(uri.toString())
                                }
                            }
                    }

                    // Update other profile information
                    usersReference.child("first_name").setValue(firstName)
                    usersReference.child("last_name").setValue(lastName)

                    currentUser.updatePassword(password)
                        .addOnCompleteListener { task ->
                            // Hide ProgressBar
                            progressBar.visibility = View.GONE

                            if (task.isSuccessful) {
                                val intent = Intent(this, VendorProfileDashboard::class.java)
                                startActivity(intent)
                                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    // Hide ProgressBar
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Hide ProgressBar
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getContent.launch(intent)
    }
}