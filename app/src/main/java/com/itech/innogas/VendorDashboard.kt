package com.itech.innogas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class VendorDashboard : AppCompatActivity() {

    private lateinit var productsAdapter: ProductsAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var productList: MutableList<ProductsDBStructure>
    private lateinit var storageReference: StorageReference
    private lateinit var searchBox: EditText
    private lateinit var noPostText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vendor_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.home
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            if (item.itemId == R.id.home) {
                // Handle Home item
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.scanner) {
                startActivity(Intent(applicationContext, VendorScannerDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.profile) {
                startActivity(Intent(applicationContext, VendorProfileDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.logout) {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                return@setOnItemSelectedListener true
            }
            false
        }

        // Initialize RecyclerView and Adapter
        val recyclerView: RecyclerView = findViewById(R.id.productList)
        productList = mutableListOf()
        productsAdapter = ProductsAdapter(this, productList)
        recyclerView.adapter = productsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize Firebase database reference
        val currentUser = FirebaseAuth.getInstance().currentUser

        storageReference = FirebaseStorage.getInstance().reference

        searchBox = findViewById(R.id.search_box)
        noPostText = findViewById(R.id.no_post_text)

        val addProductsBttn = findViewById<ImageView>(R.id.add_products_bttn)

        loadUsersProfile()

        addProductsBttn.setOnClickListener {
            val intent = Intent(this, VendorAddProductDashboard::class.java)
            startActivity(intent)
        }

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(charSequence.toString())
            }

            override fun afterTextChanged(s: Editable?) {}

        })

        if (currentUser != null) {
            val uid = currentUser.uid
            databaseReference = FirebaseDatabase.getInstance().getReference("UsersTbl/$uid/ProductsTbl")

            // Retrieve boardings data from Firebase
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        productList.clear()
                        for (productSnapshot in snapshot.children) {
                            val products = productSnapshot.getValue(ProductsDBStructure::class.java)
                            if (products != null) {
                                productList.add(products)
                            }
                        }
                        productsAdapter.notifyDataSetChanged()

                        if (productList.isEmpty()) {
                            noPostText.visibility = View.VISIBLE
                        } else {
                            noPostText.visibility = View.GONE
                        }

                    } else {
                        noPostText.visibility = View.VISIBLE  // Show the text if no data exists
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        }
    }

    private fun filterProducts(query: String) {
        val filteredList = ArrayList<ProductsDBStructure>()

        for (products in productList) {
            val search = "${products.product_name?.orEmpty()}".toLowerCase()
            if (search.contains(query.toLowerCase())) {
                filteredList.add(products)
            }
        }

        productsAdapter.filterList(filteredList)
    }

    private fun loadUsersProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null){
            val uid = currentUser.uid
            val usersReference = FirebaseDatabase.getInstance().getReference("UsersTbl/$uid")

            usersReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val users = snapshot.getValue(UserDBStructure::class.java)
                        if (users != null) {
                            val profileImageView =
                                findViewById<ShapeableImageView>(R.id.profile_pic)
                            Glide.with(this@VendorDashboard)
                                .load(users.profile_pic)
                                .into(profileImageView)

                            findViewById<TextView>(R.id.fname).text = users.first_name
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@VendorDashboard, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }
}