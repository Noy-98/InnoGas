package com.itech.innogas

import android.os.Bundle
import android.widget.TextView
import android.util.Log
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class VendorViewDetailsDashboard : AppCompatActivity() {

    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var productsData: ProductsDBStructure

    private lateinit var currentUsersId: String
    private lateinit var currentPId: String


    private lateinit var productName: TextView
    private lateinit var productStock: TextView
    private lateinit var productType: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vendor_view_details_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        productsData = intent.getSerializableExtra("productsData") as ProductsDBStructure
        currentPId = intent.getStringExtra("p_id") ?: ""

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

        val deleteButton = findViewById<AppCompatButton>(R.id.delete_bttn)
        val editButton = findViewById<AppCompatButton>(R.id.edit_bttn)
        val backButton = findViewById<FloatingActionButton>(R.id.go_back_bttn)

        populateFields()

        deleteButton.setOnClickListener {
            deleteproductsData()
        }

        editButton.setOnClickListener {
            val intent = Intent(this, VendorEditProductDashboard::class.java)
            intent.putExtra("p_id", currentPId)
            intent.putExtra("productsData", productsData)
            startActivity(intent)
        }

        backButton.setOnClickListener {
            val intent = Intent(this, VendorDashboard::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun deleteproductsData() {
        // References to both paths
        val userProductsRef = FirebaseDatabase.getInstance().getReference("UsersTbl").child(currentUsersId).child("ProductsTbl").child(currentPId)
        val globalProductsRef = FirebaseDatabase.getInstance().getReference("ProductsTbl").child(currentPId)

        // Create deletion tasks
        val userDeleteTask = userProductsRef.removeValue()
        val globalDeleteTask = globalProductsRef.removeValue()

        // Run both deletion tasks in parallel and wait for them to finish
        Tasks.whenAll(userDeleteTask, globalDeleteTask).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // If both deletions were successful, navigate back to the dashboard
                val intent = Intent(this, VendorDashboard::class.java)
                startActivity(intent)
                Toast.makeText(this, "Products deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                // Handle the failure of either task
                Toast.makeText(this, "Failed to delete Products: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                Log.e("DeleteProducts", "Error deleting data", task.exception)
            }
        }
    }

    private fun populateFields() {
        productName.setText(productsData.product_name)
        productStock.setText(productsData.product_stock)
        productType.setText(productsData.product_type)
        productPrice.setText(productsData.product_price)
        productDescription.setText(productsData.product_description)

        productsData.product_image?.let {
            val productsImageView = findViewById<ShapeableImageView>(R.id.product_image)
            Glide.with(this)
                .load(it)
                .into(productsImageView)
        }
    }
}