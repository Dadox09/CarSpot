package com.example.concessionarioapp.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.concessionarioapp.databinding.ItemChatMessageBinding
import com.example.concessionarioapp.databinding.ItemChatAnnuncioBinding
import com.example.concessionarioapp.viewmodels.ChatMessage
import com.example.concessionarioapp.classes.Annuncio
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val onAnnuncioClick: (Annuncio) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MESSAGE = 0
        private const val TYPE_MESSAGE_WITH_ANNUNCIO = 1
    }

    private var messages = listOf<ChatMessage>()
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].annuncio != null && !messages[position].isUser) {
            TYPE_MESSAGE_WITH_ANNUNCIO
        } else {
            TYPE_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_MESSAGE_WITH_ANNUNCIO -> {
                val binding = ItemChatAnnuncioBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                MessageWithAnnuncioViewHolder(binding)
            }
            else -> {
                val binding = ItemChatMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ChatViewHolder(binding)
            }
        }
    }

    inner class MessageWithAnnuncioViewHolder(private val binding: ItemChatAnnuncioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                // Messaggio di testo
                textViewMessage.text = message.content
                textViewTimestamp.text = dateFormat.format(Date(message.timestamp))

                // Dati annuncio
                message.annuncio?.let { annuncio ->
                    titoloAnnuncioTextView.text = annuncio.titolo
                    descrizioneAnnuncioTextView.text = annuncio.descrizione
                    prezzoAnnuncioTextView.text = "€ ${annuncio.prezzo}"

                    // Carica immagine con Glide
                    Glide.with(immagineAnnuncioImageView.context)
                        .load(annuncio.immagini.firstOrNull())
                        .placeholder(com.example.concessionarioapp.R.drawable.placeholder)
                        .error(com.example.concessionarioapp.R.drawable.placeholder)
                        .into(immagineAnnuncioImageView)

                    // Click listener per l'annuncio
                    annuncioCard.setOnClickListener {
                        onAnnuncioClick(annuncio)
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChatViewHolder -> holder.bind(messages[position])
            is MessageWithAnnuncioViewHolder -> holder.bind(messages[position])
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                textViewMessage.text = message.content
                textViewTimestamp.text = dateFormat.format(Date(message.timestamp))

                val layoutParams = containerMessage.layoutParams as LinearLayout.LayoutParams

                if (message.isUser) {
                    // Messaggio utente
                    containerMessage.setBackgroundResource(
                        com.example.concessionarioapp.R.drawable.bg_user_message
                    )
                    layoutParams.gravity = Gravity.END
                    layoutParams.marginStart = 100
                    layoutParams.marginEnd = 16

                } else {
                    // Messaggio bot
                    containerMessage.setBackgroundResource(
                        com.example.concessionarioapp.R.color.bg_bot_message
                    )
                    layoutParams.gravity = Gravity.START
                    layoutParams.marginStart = 16
                    layoutParams.marginEnd = 100
                }

                containerMessage.layoutParams = layoutParams
            }
        }
    }
}