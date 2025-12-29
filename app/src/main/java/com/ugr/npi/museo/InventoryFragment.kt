package com.ugr.npi.museo

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ugr.npi.museo.databinding.FragmentInventoryBinding

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: InventoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val jsonString = context?.assets?.open("objetos.json")?.bufferedReader().use { it?.readText() }
        val listType = object : TypeToken<List<MuseoObject>>() {}.type
        val objects: List<MuseoObject> = Gson().fromJson(jsonString, listType)

        adapter = InventoryAdapter(objects) { museoObject ->
            val bundle = Bundle().apply {
                putParcelable("museoObject", museoObject)
            }
            findNavController().navigate(R.id.action_inventoryFragment_to_objectDetailFragment, bundle)
        }

        binding.rvInventory.layoutManager = LinearLayoutManager(context)
        binding.rvInventory.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}