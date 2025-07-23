package com.example.concessionarioapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.concessionarioapp.classes.Annuncio
import com.example.concessionarioapp.R

class AnnuncioEvidenzaAdapter(
    private val onAnnuncioClick: (Annuncio) -> Unit
) : RecyclerView.Adapter<AnnuncioEvidenzaAdapter.AnnuncioEvidenzaViewHolder>() {

    private val annunci = mutableListOf<Annuncio>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnuncioEvidenzaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_annuncio_evidenza, parent, false)
        return AnnuncioEvidenzaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnnuncioEvidenzaViewHolder, position: Int) {
        val annuncio = annunci[position]
        holder.bind(annuncio)
        holder.itemView.setOnClickListener {
            onAnnuncioClick(annuncio)
        }
    }

    override fun getItemCount(): Int = annunci.size

    fun updateAnnunci(newList: List<Annuncio>) {
        annunci.clear()
        annunci.addAll(newList)
        notifyDataSetChanged()
    }

    inner class AnnuncioEvidenzaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //faccio la stessa cosa di annuncioAdapter ma con un layout diverso
        private val titoloTextView: TextView = itemView.findViewById(R.id.titoloAnnuncioTextView)
        private val prezzoTextView: TextView = itemView.findViewById(R.id.prezzoAnnuncioTextView)
        private val dettagliTextView: TextView = itemView.findViewById(R.id.dettagliAnnuncioTextView)
        private val annuncioImageView: ImageView = itemView.findViewById(R.id.immagineAnnuncioImageView)

        fun bind(annuncio: Annuncio) {
            titoloTextView.text = annuncio.titolo
            prezzoTextView.text = String.format("€ %.2f", annuncio.prezzo)

            // Formatta i dettagli dell'auto
            val dettagli = "${annuncio.anno} • ${annuncio.carburante} • ${annuncio.chilometraggio} km"
            dettagliTextView.text = dettagli

            if(!annuncio.imageUrl.isNullOrEmpty()){
                Glide.with(itemView.context)
                    .load(annuncio.imageUrl)
                    .placeholder(R.drawable.placeholder) // Immagine mostrata durante il caricamento
                    .error(R.drawable.placeholder) // Immagine mostrata in caso di errore
                    .into(annuncioImageView)
            } else {
                // Se l'URL è nullo o vuoto, mostra il placeholder
                annuncioImageView.setImageResource(R.drawable.placeholder)
            }
        }
    }
}