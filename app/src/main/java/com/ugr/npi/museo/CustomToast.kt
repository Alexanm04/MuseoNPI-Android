package com.ugr.npi.museo

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.Gravity

/**
 * Utility helper to show custom styled toasts.
 * Replaces standard Android toasts with a custom layout (Icon + Text) defined in `custom_toast.xml`.
 */
object CustomToast {
    @Suppress("DEPRECATION")
    fun show(context: Context, message: String, isError: Boolean = false) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)

        val text: TextView = layout.findViewById(R.id.toast_text)
        text.text = message
        
        val icon: ImageView = layout.findViewById(R.id.toast_icon)
        if (isError) {
             icon.setImageResource(android.R.drawable.ic_delete)
        } else {
             icon.setImageResource(android.R.drawable.ic_dialog_info)
        }

        val toast = Toast(context)
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
}
