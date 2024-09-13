package com.kid.productscanner.presentation.input_quantity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kid.productscanner.databinding.ItemInputQuantityBinding
import com.kid.productscanner.repository.cache.room.entity.Pack

class PacksFoundAdapter(private val packs: List<Pack>) :
    RecyclerView.Adapter<PacksFoundAdapter.ViewHolder>() {


    class ViewHolder(val binding: ItemInputQuantityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemInputQuantityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.pack = packs[position]
    }

    override fun getItemCount(): Int {
        return packs.size
    }
}