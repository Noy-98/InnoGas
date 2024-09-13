package com.itech.innogas

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class CustomerViewDetailsDashboard : AppCompatActivity() {

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
        setContentView(R.layout.activity_customer_view_details_dashboard)
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

        val backButton = findViewById<FloatingActionButton>(R.id.go_back_bttn)

        populateFields()

        backButton.setOnClickListener {
            val intent = Intent(this, CustomerDashboard::class.java)
            startActivity(intent)
            finish()
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