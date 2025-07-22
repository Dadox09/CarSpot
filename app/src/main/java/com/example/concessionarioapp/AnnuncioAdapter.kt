package com.example.concessionarioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AnnuncioAdapter(private var annunci: MutableList<Annuncio>, 
private val onAnnuncioClick: (Annuncio) -> Unit,
private val onLikeClick: (Annuncio) -> Unit,
private val showLikes: Boolean = true) : RecyclerView.Adapter<AnnuncioAdapter.AnnuncioViewHolder>() {

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

    override fun getItemCount(): Int = annunci.size
    
    // Metodo semplice per aggiornare la lista di annunci
    fun updateAnnunci(newAnnunci: List<Annuncio>) {
        annunci.clear()
        annunci.addAll(newAnnunci)
        notifyDataSetChanged()
    }

    inner class AnnuncioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titoloTextView: TextView = itemView.findViewById(R.id.titoloAnnuncioTextView)
        private val descrizioneTextView: TextView = itemView.findViewById(R.id.descrizioneAnnuncioTextView)
        private val prezzoTextView: TextView = itemView.findViewById(R.id.prezzoAnnuncioTextView)
        private val dettagliTextView: TextView = itemView.findViewById(R.id.dettagliAnnuncioTextView)
        private val likesCountTextView: TextView = itemView.findViewById(R.id.likesCountTextView)
        private val likeIconImageView: ImageView = itemView.findViewById(R.id.likeIconImageView)
        private val likesContainer: LinearLayout = itemView.findViewById(R.id.likesContainer)

        fun bind(annuncio: Annuncio) {
            titoloTextView.text = annuncio.titolo
            descrizioneTextView.text = annuncio.descrizione
            prezzoTextView.text = String.format("€ %.2f", annuncio.prezzo)

            val dettagli = "${annuncio.anno} • ${annuncio.carburante} • ${annuncio.chilometraggio} km"
            dettagliTextView.text = dettagli

            if (showLikes) {
                likesContainer.visibility = View.VISIBLE
                updateLikesDisplay(annuncio)

                likesContainer.setOnClickListener {
                    onLikeClick(annuncio)
                }
            } else {
                likesContainer.visibility = View.GONE
            }
        }
        
        private fun updateLikesDisplay(annuncio: Annuncio) {
            likesCountTextView.text = annuncio.likes.toString()

            val currentUserId = getCurrentUserId()
            val hasLiked = annuncio.likedBy.contains(currentUserId)

            likeIconImageView.setImageResource(
                if (hasLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty
            )
        }
        
        private fun getCurrentUserId(): String {
            // Ottieni l'ID dell'utente corrente da Firebase Auth
            return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
    }
}


