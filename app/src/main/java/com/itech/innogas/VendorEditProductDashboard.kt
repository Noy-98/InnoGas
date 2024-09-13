package com.itech.innogas

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream

class VendorEditProductDashboard : AppCompatActivity() {

    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var productsData: ProductsDBStructure

    private lateinit var currentUsersId: String
    private lateinit var currentPId: String

    private val PICK_IMAGE_REQUEST = 1
    private lateinit var selectedImageUri: Uri
    private lateinit var ProgressBar: ProgressBar


    private lateinit var productName: TextInputEditText
    private lateinit var productStock: TextInputEditText
    private lateinit var productType: TextInputEditText
    private lateinit var productPrice: TextInputEditText
    private lateinit var productDescription: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vendor_edit_product_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        productsData = intent.getSerializableExtra("productsData") as ProductsDBStructure
        currentPId = intent.getStringExtra("p_id") ?: ""

        ProgressBar = findViewById(R.id.editProgressBar)
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        storageReference = FirebaseStorage.getInstance().reference

        val currentUser = auth.currentUser
        currentUsersId = currentUser?.uid ?: ""


        productName = findViewById(R.id.product_name)
        productStock = findViewById(R.id.product_stock)
        productType = findViewById(R.id.product_type)
        productPrice = findViewById(R.id.product_price)
        productDescription = findViewById(R.id.product_description)

        val backButton = findViewById<FloatingActionButton>(R.id.go_back_bttn)
        val editProfileButton = findViewById<AppCompatButton>(R.id.save_bttn)
        val editProfileImageButton = findViewById<AppCompatButton>(R.id.add_pic_bttn)

        backButton.setOnClickListener {
            finish()
        }

        editProfileImageButton.setOnClickListener {
            openImagePicker()
        }

        editProfileButton.setOnClickListener {
            val pn = productName.text.toString().trim()
            val ps = productStock.text.toString().trim()
            val pt = productType.text.toString().trim()
            val pp = productPrice.text.toString().trim()
            val pd = productDescription.text.toString().trim()

            ProgressBar.visibility = View.VISIBLE

            if (pn.isEmpty() || ps.isEmpty() || pt.isEmpty() || pp.isEmpty() || pd.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                ProgressBar.visibility = View.GONE
            } else {

                val updatedproductData = ProductsDBStructure(
                    p_id = currentPId,
                    product_name = pn,
                    product_stock = ps,
                    product_type = pt,
                    product_price = pp,
                    product_description = pd
                )
                uploadImage(updatedproductData)
            }
        }
    }

    private fun uploadImage(updatedproductData: ProductsDBStructure) {
        val imageRef = storageReference.child("product_pictures/${System.currentTimeMillis()}.jpg")
        val imageView = findViewById<ShapeableImageView>(R.id.product_image)

        // Check if image has a valid drawable
        val drawable = imageView.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            val uploadTask = imageRef.putBytes(imageData)
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    ProgressBar.visibility = View.GONE
                    task.exception?.let {
                        throw it
                    }
                }
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUrl = task.result.toString()
                    updatedproductData.product_image = downloadUrl
                    saveProductData(updatedproductData)
                } else {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    ProgressBar.visibility = View.GONE
                }
            }
        } else {
            Toast.makeText(this, "Invalid image format", Toast.LENGTH_SHORT).show()
            ProgressBar.visibility = View.GONE
        }
    }

    private fun saveProductData(updatedproductData: ProductsDBStructure) {
        val updates = hashMapOf<String, Any>(
            "/UsersTbl/$currentUsersId/ProductsTbl/$currentPId" to updatedproductData,
            "/ProductsTbl/$currentPId" to updatedproductData
        )

        databaseReference.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val intent = Intent(this, VendorViewDetailsDashboard::class.java)
                startActivity(intent)
                Toast.makeText(this, "Products successfully Changed!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to update Products: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                Log.e("Products", "Error updating Products", task.exception)
            }
            ProgressBar.visibility = View.GONE
        }
    }

    private fun openImagePicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data!!
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)

            val imageView = findViewById<ShapeableImageView>(R.id.product_image)
            imageView.setImageBitmap(bitmap)
            imageView.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }
}