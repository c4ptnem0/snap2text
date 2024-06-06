package com.example.snap2text.ui.snap2Text

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.snap2text.databinding.FragmentSnapBinding

class Snap2TextFragment : Fragment() {

    private var _binding: FragmentSnapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSnapBinding.inflate(inflater, container, false)
        val image2TextBtn = binding.image2TextBtn
        val speech2TextBtn = binding.speech2TextBtn

        // navigate to Image to Text Activity
        image2TextBtn.setOnClickListener {
            val intent = Intent(requireContext(), Image2TextActivity::class.java)
            startActivity(intent)
        }

        // navigate to Speech to Text Activity
        speech2TextBtn.setOnClickListener {
            val intent = Intent(requireContext(), Speech2TextActivity::class.java)
            startActivity(intent)
        }



        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}