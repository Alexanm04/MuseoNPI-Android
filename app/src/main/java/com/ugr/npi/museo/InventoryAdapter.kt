package com.ugr.npi.museo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ugr.npi.museo.databinding.ItemInventoryBinding

class InventoryAdapter(
    private var objects: List<MuseoObject>,
    private val onItemClick: (MuseoObject) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    private var filteredObjects: List<MuseoObject> = objects

    class ViewHolder(val binding: ItemInventoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredObjects[position]
        holder.binding.tvObjectName.text = item.getNombre()
        holder.binding.tvObjectCategory.text = item.getCategoria()
        
        val context = holder.binding.root.context
        val imageResId = context.resources.getIdentifier(item.imagen, "drawable", context.packageName)
        if (imageResId != 0) {
            holder.binding.ivObject.setImageResource(imageResId)
        } else {
            holder.binding.ivObject.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = filteredObjects.size

    fun filter(query: String) {
        filteredObjects = if (query.isEmpty()) {
            objects
        } else {
            objects.filter {
                it.getNombre().contains(query, ignoreCase = true) || 
                it.getCategoria().contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}