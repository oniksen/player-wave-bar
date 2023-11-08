package com.onixen.audioplayer.views.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialContainerTransform
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.BottomSheetLayoutBinding

class ModalBottomSheetPlayer(private val title: String): Fragment(R.layout.bottom_sheet_layout) {
    private var DURATION_TIME = 700L

    private var _binding: BottomSheetLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)

        DURATION_TIME = resources.getInteger(R.integer.animation_duration).toLong()
        val transition = MaterialContainerTransform().apply {
            duration = DURATION_TIME
            scrimColor = Color.TRANSPARENT
        }
        sharedElementEnterTransition = transition
        sharedElementReturnTransition = transition
    }
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
            .replace(R.id.playerBottomSheet, PlayerFragment.getInstance())
            .addToBackStack(TAG)
            .commit()
        }

        return binding.root
    }

    companion object {
        const val TAG = "bottom_sheet_player"
    }
}