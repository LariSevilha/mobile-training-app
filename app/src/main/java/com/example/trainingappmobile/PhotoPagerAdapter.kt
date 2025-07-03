package com.example.trainingappmobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trainingappmobile.R

class PhotoPagerAdapter(
    private val photoUrls: List<String>,
    private val loadImage: (String) -> Unit
) : RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.photo_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val url = photoUrls[position]
        loadImage(url)
        Glide.with(holder.imageView.context)
            .load(url)
            .placeholder(android.R.color.transparent)
            .error(R.drawable.ic_error)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = photoUrls.size
}