package com.example.concessionarioapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.concessionarioapp.classes.Annuncio
import com.example.concessionarioapp.R
import com.google.firebase.auth.FirebaseAuth

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
        //salvo tutto il layout cosi da modificarlo
        private val titoloTextView: TextView = itemView.findViewById(R.id.titoloAnnuncioTextView)
        private val descrizioneTextView: TextView = itemView.findViewById(R.id.descrizioneAnnuncioTextView)
        private val prezzoTextView: TextView = itemView.findViewById(R.id.prezzoAnnuncioTextView)
        private val dettagliTextView: TextView = itemView.findViewById(R.id.dettagliAnnuncioTextView)
        private val likesCountTextView: TextView = itemView.findViewById(R.id.likesCountTextView)
        private val likeIconImageView: ImageView = itemView.findViewById(R.id.likeIconImageView)
        private val likesContainer: LinearLayout = itemView.findViewById(R.id.likesContainer)
        private val annuncioImageView: ImageView = itemView.findViewById(R.id.immagineAnnuncioImageView)

        fun bind(annuncio: Annuncio) {
            //popolo il layout con i dati dell'annuncio
            titoloTextView.text = annuncio.titolo
            descrizioneTextView.text = annuncio.descrizione
            prezzoTextView.text = String.format("€ %.2f", annuncio.prezzo)

            val dettagli = "${annuncio.anno} • ${annuncio.carburante} • ${annuncio.chilometraggio} km"
            dettagliTextView.text = dettagli

            // Mostra o nascondi il contatore di like in base al fragment dove sono
            if (showLikes) {
                likesContainer.visibility = View.VISIBLE
                updateLikesDisplay(annuncio)

                likesContainer.setOnClickListener {
                    onLikeClick(annuncio)
                }
            } else {
                likesContainer.visibility = View.GONE
            }

            // Immagine dell'annuncio
            if (annuncio.immagini.isNotEmpty()) {
                Glide.with(itemView.context) // Usare il contesto della view
                    .load(annuncio.immagini[0]) // Carica la prima immagine della lista
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(annuncioImageView) // Utilizza la view locale
            } else {
                // Se non ci sono immagini, mostra l'immagine di default
                annuncioImageView.setImageResource(R.drawable.placeholder)
            }

        }

        // Metodo per aggiornare la visualizzazione del contatore di like
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
            return FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
    }
}