package com.example.gastromaps.adapters

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.gastromaps.R
import com.example.gastromaps.activity.HappyPlaceDetailActivity
import com.example.gastromaps.databinding.ItemHappyPlaceBinding
import com.example.gastromaps.models.HappyPlaceModel

class HappyPlacesAdapter(
    private val context: Context,
    private var list: ArrayList<HappyPlaceModel>
) : RecyclerView.Adapter<HappyPlacesAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemHappyPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: HappyPlaceModel) {
            binding.apply {
                Log.d("HappyPlacesAdapter", "Loading image: ${model.image}")

                Glide.with(context)
                    .load(model.image)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e("HappyPlacesAdapter", "Failed to load image: ${e?.message}")
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d("HappyPlacesAdapter", "Successfully loaded image")
                            return false
                        }
                    })
                    .placeholder(R.drawable.add_screen_image_placeholder)
                    .error(R.drawable.add_screen_image_placeholder)
                    .into(ivPlaceImage)

                tvTitle.text = model.title
                tvDescription.text = model.description

                // Click listener to open detail view
                root.setOnClickListener {
                    val intent = Intent(context, HappyPlaceDetailActivity::class.java)
                    intent.putExtra(HappyPlaceDetailActivity.EXTRA_PLACE_DETAILS, model)
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHappyPlaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = list[position]
        holder.bind(model)
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: ArrayList<HappyPlaceModel>) {
        Log.d("HappyPlacesAdapter", "Updating data with ${newList.size} items")
        list = newList
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): HappyPlaceModel {
        return list[position]
    }

    fun removeItem(position: Int) {
        list.removeAt(position)
        notifyItemRemoved(position)

        // Notify of changes in subsequent items
        if (position < list.size) {
            notifyItemRangeChanged(position, list.size - position)
        }
    }
}