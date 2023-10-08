package com.onixen.audioplayer.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.BottomSheetLayoutBinding

class ModalBottomSheetPlayer(private val title: String): Fragment(R.layout.bottom_sheet_layout) {
    private var _binding: BottomSheetLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLayoutBinding.inflate(inflater, container, false)

        binding.trackTitle.text = title

        binding.root.setOnClickListener {
            requireActivity().supportFragmentManager
            .beginTransaction()
            .addSharedElement(it, it.transitionName)
            .replace(R.id.fragmentContainer, PlayerFragmentV2.getInstance())
            .commit()
        }

        return binding.root
    }

    companion object {
        const val TAG = "bottom_sheet_player"
    }
}