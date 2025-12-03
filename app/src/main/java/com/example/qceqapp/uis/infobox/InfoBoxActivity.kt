package com.example.qceqapp.uis.infobox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.databinding.ActivityInfoBoxBinding

class InfoBoxActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInfoBoxBinding
    private lateinit var adapter: InfoBoxAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBoxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val idToInspect = intent.getStringExtra("ID_TO_INSPECT")

        val selectedBoxes =
            intent.getParcelableArrayListExtra<Entities.BoxToInspect>("SELECTED_BOXES")

        binding.tvTitle.text = "Inspection #$idToInspect"

        adapter = InfoBoxAdapter(selectedBoxes ?: emptyList())
        binding.recyclerBoxes.layoutManager = LinearLayoutManager(this)
        binding.recyclerBoxes.adapter = adapter
    }
}
