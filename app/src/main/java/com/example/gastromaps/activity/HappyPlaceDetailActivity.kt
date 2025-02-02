package com.example.gastromaps.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.gastromaps.R
import com.example.gastromaps.databinding.ActivityHappyPlaceDetailBinding
import com.example.gastromaps.models.HappyPlaceModel

class HappyPlaceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHappyPlaceDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var happyPlaceDetail: HappyPlaceModel? = null

        if (intent.hasExtra(EXTRA_PLACE_DETAILS)) {
            happyPlaceDetail = intent.getParcelableExtra(EXTRA_PLACE_DETAILS)
        }

        if (happyPlaceDetail != null) {
            setSupportActionBar(binding.toolbarHappyPlaceDetail)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = happyPlaceDetail.title

            binding.toolbarHappyPlaceDetail.setNavigationOnClickListener {
                onBackPressed()
            }

            Log.d("Detail Activity", "Loading image: ${happyPlaceDetail.image}")

            Glide.with(this)
                .load(happyPlaceDetail.image)
                .centerCrop()
                .placeholder(R.drawable.add_screen_image_placeholder)
                .into(binding.ivPlaceImage)

            binding.tvDescription.text = happyPlaceDetail.description
            binding.tvLocation.text = happyPlaceDetail.location
            binding.tvDate.text = happyPlaceDetail.date
        }
    }

    companion object {
        const val EXTRA_PLACE_DETAILS = "extra_place_details"
    }
}