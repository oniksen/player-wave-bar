package com.onixen.audioplayer.views.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.PlayerFragmentBinding

class PlayerFragment: Fragment(R.layout.player_fragment) {
    private var _binding: PlayerFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        private const val TAG = "player_fragment"

        private const val DURATION_TIME = 700L
        private const val START_BANNER_WIDTH = 270
        private const val FULL_BANNER_WIDTH = 300

        private var instance: PlayerFragment? = null
        fun getInstance(): PlayerFragment {
            return if (instance == null) {
                instance = PlayerFragment()
                instance as PlayerFragment
            } else {
                instance as PlayerFragment
            }
        }
    }
}