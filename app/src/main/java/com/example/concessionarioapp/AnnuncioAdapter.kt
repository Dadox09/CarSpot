package com.example.concessionarioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AnnuncioAdapter(private var annunci: MutableList<Annuncio>, 
private val onAnnuncioClick: (Annuncio) -> Unit) : RecyclerView.Adapter<AnnuncioAdapter.AnnuncioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnuncioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_annuncio, parent, false)
        return AnnuncioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnnuncioViewHolder, position: Int) {
        val annuncio = annunci[position]
        holder.bind(annuncio)
        holder.itemView.setOnClickListener {
            onAnnuncioClick(annuncio)
        }
    }

    override fun getItemCount(): Int {
        return annunci.size
    }

    fun updateList(newList: List<Annuncio>) {
        annunci.clear()
        annunci.addAll(newList)
        notifyDataSetChanged()
    }

    inner class AnnuncioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titoloTextView: TextView = itemView.findViewById(R.id.titoloAnnuncioTextView)
        private val descrizioneTextView: TextView = itemView.findViewById(R.id.descrizioneAnnuncioTextView)
        private val prezzoTextView: TextView = itemView.findViewById(R.id.prezzoAnnuncioTextView)

        fun bind(annuncio: Annuncio) {
            titoloTextView.text = annuncio.titolo
            descrizioneTextView.text = annuncio.descrizione
            prezzoTextView.text = String.format("€ %.2f", annuncio.prezzo)
        }
    }
}
