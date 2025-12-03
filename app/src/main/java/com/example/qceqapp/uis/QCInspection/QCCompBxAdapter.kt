package com.example.qceqapp.uis.QCInspection

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.qceqapp.R
import com.example.qceqapp.data.model.Entities

class QCCompBxAdapter(
    context: Context,
    private val compositions: List<Entities.BoxCompositionResponse>,
    private val selectedComposition: Entities.BoxCompositionResponse? = null
) : ArrayAdapter<Entities.BoxCompositionResponse>(context, 0, compositions) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_box_composition,
            parent,
            false
        )

        val composition = getItem(position)

        composition?.let {
            val tvBarcode = view.findViewById<TextView>(R.id.tvBarcode)
            val tvFloCod = view.findViewById<TextView>(R.id.tvFloCod)
            val tvComposition = view.findViewById<TextView>(R.id.tvComposition)
            val tvIssues = view.findViewById<TextView>(R.id.tvIssues)
            tvBarcode.text = it.inbNumBox ?: "-"
            tvFloCod.text = it.floCod ?: "-"
            tvComposition.text = it.composition ?: "-"
            val issuesCount = it.issues?.size ?: 0
            tvIssues.text = if (issuesCount > 0) issuesCount.toString() else "-"

            if (issuesCount > 0) {
                tvIssues.setTextColor(Color.parseColor("#F44336")) // Rojo
            } else {
                tvIssues.setTextColor(Color.parseColor("#4CAF50")) // Verde
            }
            if (selectedComposition != null &&
                it.inbNumBox == selectedComposition.inbNumBox &&
                it.floCod == selectedComposition.floCod) {
                view.setBackgroundColor(Color.parseColor("#E3F2FD"))
            } else {
                view.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        return view
    }
}