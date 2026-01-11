package com.ugr.npi.museo

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardMessage: CardView = view.findViewById(R.id.card_message)
        val textMessage: TextView = view.findViewById(R.id.text_message)
        val layout: LinearLayout = view as LinearLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.textMessage.text = message.text

        val params = holder.cardMessage.layoutParams as LinearLayout.LayoutParams
        if (message.isUser) {
            holder.layout.gravity = Gravity.END
            holder.cardMessage.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_blue_light))
            params.marginStart = 64
            params.marginEnd = 0
        } else {
            holder.layout.gravity = Gravity.START
            holder.cardMessage.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            params.marginStart = 0
            params.marginEnd = 64
        }
        holder.cardMessage.layoutParams = params
    }

    override fun getItemCount() = messages.size
}
