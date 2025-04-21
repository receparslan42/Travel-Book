package com.receparslan.travelbook.view

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.receparslan.travelbook.R
import com.receparslan.travelbook.adapter.RecyclerAdapter
import com.receparslan.travelbook.databinding.ActivityMainBinding
import com.receparslan.travelbook.model.Location
import com.receparslan.travelbook.roomDB.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding // ViewBinding

    private lateinit var locationList: List<Location> // List of Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Room Database
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "locations").build()
        val locationDAO = db.locationDAO()

        lifecycleScope.launch(Dispatchers.IO) {
            locationList = locationDAO.getAll().firstOrNull() ?: ArrayList()
            withContext(Dispatchers.Main) {
                binding.recyclerView.adapter = RecyclerAdapter(locationList)
                binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
            }
        }
    }

    // Inflate the menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    // Handle the menu item click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when {
            // Start MapsActivity with isOld = false to add a new place
            item.itemId == R.id.addAPlace -> {
                val intent = Intent(this, MapsActivity::class.java)
                intent.putExtra("isOld", false)
                startActivity(intent)

                return true
            }

            else -> return false
        }
    }
}