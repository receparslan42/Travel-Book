package com.receparslan.travelbook.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.receparslan.travelbook.databinding.RecyclerRowBinding
import com.receparslan.travelbook.model.Location
import com.receparslan.travelbook.view.MapsActivity

class RecyclerAdapter(private val locationList: List<Location>) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    private lateinit var binding: RecyclerRowBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return locationList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.locationIDTextView.text = String.format(locationList[position].id.toString())
        holder.locationNameTextView.text = locationList[position].name
        holder.itemView.setOnClickListener {
            val intent = Intent(binding.root.context, MapsActivity::class.java)
            intent.putExtra("locationID", locationList[position].id)
            intent.putExtra("isOld", true)
            binding.root.context.startActivity(intent)
        }
    }

    class ViewHolder(binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root) {
        val locationIDTextView = binding.locationIDTextView
        val locationNameTextView = binding.locationNameTextView
    }
}