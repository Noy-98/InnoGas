package com.itech.innogas

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CustomerMonitoringDashboard : AppCompatActivity() {

    private lateinit var temperatureTextView: TextView
    private lateinit var gasLevelTextView: TextView
    private lateinit var gasTypeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_monitoring_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.monitoring
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            if (item.itemId == R.id.home) {
                startActivity(Intent(applicationContext, CustomerDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.monitoring) {
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.notification) {
                startActivity(Intent(applicationContext, CustomerNotificationDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.profile) {
                startActivity(Intent(applicationContext, CustomerProfileDashboard::class.java))
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

        // Initialize TextViews
        temperatureTextView = findViewById(R.id.temperature)
        gasLevelTextView = findViewById(R.id.gasLevel)
        gasTypeTextView = findViewById(R.id.gasType)

        // Reference to Firebase Realtime Database
        val database = FirebaseDatabase.getInstance()
        val sensorRef = database.getReference("InnoGasDevice/Sensor")


        // Attach a listener to read data from Firebase
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Extract data from the snapshot
                val temperature = snapshot.child("temperature").getValue(Float::class.java)
                val gasLevel = snapshot.child("gasLevel").getValue(Int::class.java)
                val gasType = snapshot.child("gasType").getValue(String::class.java)

                // Update UI elements
                temperatureTextView.text = String.format("%.1f°C", temperature ?: 0.0)
                gasLevelTextView.text = gasLevel?.toString() ?: "0"
                gasTypeTextView.text = gasType ?: "Unknown"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database read error
                error.toException().printStackTrace()
            }
        })
    }
}