package com.example.gastromaps.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gastromaps.R
import com.example.gastromaps.adapters.HappyPlacesAdapter
import com.example.gastromaps.databinding.ActivityMainBinding
import com.example.gastromaps.firebase.FirestoreManager
import com.example.gastromaps.utils.SwipeToCallback
import com.google.firebase.auth.FirebaseAuth

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firestoreManager: FirestoreManager
    private lateinit var happyPlacesAdapter: HappyPlacesAdapter
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.title = "My Favorite Places"

        // Check if user is signed in
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firestoreManager = FirestoreManager()
        setupRecyclerView()
        setupSwipeHandlers()
        getHappyPlacesListFromFirestore()

        binding.fabAddHappyPlace.setOnClickListener {
            val intent = Intent(this@MainActivity, AddHappyPlaceActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isFirstLoad) {
            getHappyPlacesListFromFirestore()
        }
        isFirstLoad = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        binding.rvHappyPlacesList.layoutManager = LinearLayoutManager(this)
        binding.rvHappyPlacesList.setHasFixedSize(true)
        happyPlacesAdapter = HappyPlacesAdapter(this, ArrayList())
        binding.rvHappyPlacesList.adapter = happyPlacesAdapter
    }

    private fun setupSwipeHandlers() {
        val swipeHandler = object : SwipeToCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val place = happyPlacesAdapter.getItemAt(position)

                when (direction) {
                    ItemTouchHelper.RIGHT -> { // Edit
                        val intent = Intent(this@MainActivity, AddHappyPlaceActivity::class.java)
                        intent.putExtra(EXTRA_PLACE_DETAILS, place)
                        startActivity(intent)
                        // Restore the item since we're not removing it
                        happyPlacesAdapter.notifyItemChanged(position)
                    }
                    ItemTouchHelper.LEFT -> { // Delete
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Delete Place")
                            .setMessage("Are you sure you want to delete ${place.title}?")
                            .setPositiveButton("Delete") { dialog, _ ->
                                showProgressDialog(resources.getString(R.string.please_wait))

                                firestoreManager.deleteHappyPlace(
                                    place.id,
                                    onSuccess = {
                                        hideProgressDialog()
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Place deleted successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        happyPlacesAdapter.removeItem(position)
                                    },
                                    onFailure = { e ->
                                        hideProgressDialog()
                                        happyPlacesAdapter.notifyItemChanged(position)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Error deleting place: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                                happyPlacesAdapter.notifyItemChanged(position)
                            }
                            .setOnCancelListener {
                                happyPlacesAdapter.notifyItemChanged(position)
                            }
                            .show()
                    }
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvHappyPlacesList)
    }

    private fun getHappyPlacesListFromFirestore() {
        showProgressDialog(resources.getString(R.string.please_wait))

        firestoreManager.getUserHappyPlaces(
            onSuccess = { happyPlacesList ->
                hideProgressDialog()
                Log.d("MainActivity", "Got ${happyPlacesList.size} places from Firestore")

                if (happyPlacesList.isNotEmpty()) {
                    binding.rvHappyPlacesList.visibility = View.VISIBLE
                    binding.tvNoRecordsAvailable.visibility = View.GONE
                    happyPlacesAdapter.updateData(ArrayList(happyPlacesList))
                } else {
                    binding.rvHappyPlacesList.visibility = View.GONE
                    binding.tvNoRecordsAvailable.visibility = View.VISIBLE
                }
            },
            onFailure = { exception ->
                hideProgressDialog()
                Log.e("MainActivity", "Error loading places", exception)
                binding.rvHappyPlacesList.visibility = View.GONE
                binding.tvNoRecordsAvailable.visibility = View.VISIBLE
                binding.tvNoRecordsAvailable.text = "Error loading places: ${exception.message}"
                Toast.makeText(
                    this@MainActivity,
                    exception.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    companion object {
        const val EXTRA_PLACE_DETAILS = "extra_place_details"
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        doubleBackToExit()
    }
}