package com.itech.innogas

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream

class VendorAddProductDashboard : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var currentUsersId: String
    private lateinit var pid: String
    private lateinit var ProgressBar: ProgressBar
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var selectedImageUri: Uri
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vendor_add_product_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val currentUser = auth.currentUser
        currentUsersId = currentUser?.uid ?: ""

        val newProductRef = FirebaseDatabase.getInstance().reference.child("ProductsTbl").push()
        pid = newProductRef.key ?: ""

        val productName = findViewById<TextInputEditText>(R.id.product_name)
        val productStock = findViewById<TextInputEditText>(R.id.product_stock)
        val productType = findViewById<TextInputEditText>(R.id.product_type)
        val productPrice = findViewById<TextInputEditText>(R.id.product_price)
        val productDescription = findViewById<TextInputEditText>(R.id.product_description)
        val addPicBttn = findViewById<AppCompatButton>(R.id.add_pic_bttn)
        val saveBttn = findViewById<AppCompatButton>(R.id.save_bttn)
        val backBttn = findViewById<FloatingActionButton>(R.id.go_back_bttn)
        ProgressBar = findViewById(R.id.addProductProgressBar)

        storageReference = FirebaseStorage.getInstance().reference

        backBttn.setOnClickListener {
            val intent = Intent(this, VendorDashboard::class.java)
            startActivity(intent)
            finish()
        }

        addPicBttn.setOnClickListener {
            openImageChooser()
        }

        saveBttn.setOnClickListener {
            val pn = productName.text.toString().trim()
            val ps = productStock.text.toString().trim()
            val pt = productType.text.toString().trim()
            val pp = productPrice.text.toString().trim()
            val pd = productDescription.text.toString().trim()

            ProgressBar.visibility = View.VISIBLE

            if (pn.isEmpty() || ps.isEmpty() || pt.isEmpty() || pp.isEmpty() || pd.isEmpty()) {
                if (pn.isEmpty()) {
                    productName.error = "Please enter Product Name"
                }
                if (ps.isEmpty()) {
                    productStock.error = "Please enter Product Stock"
                }
                if (pt.isEmpty()) {
                    productType.error = "Please enter Product Type"
                }
                if (pp.isEmpty()) {
                    productPrice.error = "Please enter Product Price"
                }
                if (pd.isEmpty()) {
                    productDescription.error = "Please enter Product Description"
                }
                Toast.makeText(this, "All fields are Required!", Toast.LENGTH_SHORT).show()
                ProgressBar.visibility = View.GONE
            } else {

                val productsData =ProductsDBStructure(
                    p_id = pid,
                    product_name = pn,
                    product_stock = ps,
                    product_type = pt,
                    product_price = pp,
                    product_description = pd,
                )
                uploadImage(productsData)
            }
        }
    }

    private fun uploadImage(productsData: ProductsDBStructure) {

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
                    productsData.product_image = downloadUrl
                    saveProductData(productsData)
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

    private fun saveProductData(productsData: ProductsDBStructure) {
        val updates = hashMapOf<String, Any>(
            "/UsersTbl/$currentUsersId/ProductsTbl/$pid" to productsData,
            "/ProductsTbl/$pid" to productsData
        )

        database.reference.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Products Upload successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, VendorDashboard::class.java))
            } else {
                Toast.makeText(this, "Failed to Upload Products", Toast.LENGTH_SHORT).show()
            }
            ProgressBar.visibility = View.GONE
        }
    }

    private fun openImageChooser() {
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