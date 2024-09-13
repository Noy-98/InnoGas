package com.itech.innogas

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CustomerEditProfileDashboard : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val databaseReference = FirebaseDatabase.getInstance().getReference("UsersTbl")
    private val storageReference = FirebaseStorage.getInstance().reference

    private lateinit var progressBar: ProgressBar
    private var imageUri: Uri? = null

    // Image picker result handler
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
        setContentView(R.layout.activity_customer_edit_profile_dashboard)

        // Initialize Firebase authentication
        auth = FirebaseAuth.getInstance()

        // Initialize ProgressBar
        progressBar = findViewById(R.id.profileProgressBar)

        // Set up event listeners for UI elements
        findViewById<AppCompatButton>(R.id.edit_bttn).setOnClickListener {
            updateProfile()
        }
        findViewById<ShapeableImageView>(R.id.profilepic).setOnClickListener {
            openImagePicker()
        }

        val backBttn = findViewById<FloatingActionButton>(R.id.go_back_bttn)

        backBttn.setOnClickListener {
            finish()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getContent.launch(intent)
    }

    // Main function to update the profile
    private fun updateProfile() {
        val firstName = findViewById<TextInputEditText>(R.id.firstname).text.toString().trim()
        val lastName = findViewById<TextInputEditText>(R.id.lastname).text.toString().trim()
        val newPassword = findViewById<TextInputEditText>(R.id.newpassword).text.toString().trim()
        val confirmPassword = findViewById<TextInputEditText>(R.id.confirmpass).text.toString().trim()

        // Validate input fields
        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "First and Last Name are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.isNotEmpty() && newPassword.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.isNotEmpty() && newPassword != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Start profile update in the background using coroutines
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Perform all updates in sequence
                if (imageUri != null) {
                    uploadProfilePicture(imageUri!!)
                }
                updateProfileDetails(firstName, lastName)
                if (newPassword.isNotEmpty()) {
                    updatePassword(newPassword)
                }

                // Success feedback on the main thread
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@CustomerEditProfileDashboard, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Handle any errors on the main thread
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@CustomerEditProfileDashboard, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Uploads the profile picture to Firebase Storage
    private suspend fun uploadProfilePicture(uri: Uri) {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        val imageRef = storageReference.child("profile_pictures/${uri.lastPathSegment}")

        // Upload image to Firebase Storage
        val uploadTask = imageRef.putFile(uri).await()
        val downloadUrl = imageRef.downloadUrl.await()

        // Update the profile picture URL in Firebase Database
        val uid = currentUser.uid
        databaseReference.child(uid).child("profile_pic").setValue(downloadUrl.toString()).await()
    }

    // Updates the first name and last name in Firebase Realtime Database
    private suspend fun updateProfileDetails(firstName: String, lastName: String) {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        val uid = currentUser.uid

        // Update first and last name in Firebase Database
        val updates = mapOf(
            "first_name" to firstName,
            "last_name" to lastName
        )
        databaseReference.child(uid).updateChildren(updates).await()
    }

    // Updates the user's password in Firebase Authentication
    private suspend fun updatePassword(newPassword: String) {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        currentUser.updatePassword(newPassword).await()
    }
}
