package com.example.concessionarioapp.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.concessionarioapp.R

class FotoAdapter(
    private val foto: MutableList<Uri>,
    private val onRemoveFoto: (Int) -> Unit,
    private val onAddFoto: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FOTO = 0
        private const val TYPE_ADD_BUTTON = 1
        private const val MAX_FOTO = 5
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < foto.size) TYPE_FOTO else TYPE_ADD_BUTTON
    }

    override fun getItemCount(): Int {
        // Mostra il pulsante "aggiungi" solo se non si è raggiunto il limite
        return foto.size + if (foto.size < MAX_FOTO) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FOTO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_foto_preview, parent, false)
                FotoViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_add_foto, parent, false)
                AddFotoViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FotoViewHolder -> {
                val fotoUri = foto[position]
                Glide.with(holder.itemView.context)
                    .load(fotoUri)
                    .centerCrop()
                    .into(holder.imageView)

                holder.removeButton.setOnClickListener {
                    onRemoveFoto(position)
                }
            }
            is AddFotoViewHolder -> {
                holder.addButton.setOnClickListener {
                    onAddFoto()
                }
            }
        }
    }

    fun addFoto(uri: Uri) {
        if (foto.size < MAX_FOTO) {
            foto.add(uri)
            notifyDataSetChanged()
        }
    }

    fun removeFoto(position: Int) {
        if (position in 0 until foto.size) {
            foto.removeAt(position)
            notifyDataSetChanged()
        }
    }

    inner class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val removeButton: ImageButton = itemView.findViewById(R.id.removeButton)
    }

    inner class AddFotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val addButton: ImageButton = itemView.findViewById(R.id.addFotoButton)
    }
}