package com.onixen.audioplayer.views.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.TracksListFragmentBinding
import com.onixen.audioplayer.model.MediaPlayer
import com.onixen.audioplayer.views.adapters.TracksAdapter

class TracksListFragment: Fragment(R.layout.tracks_list_fragment) {
    private var _binding: TracksListFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TracksListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tracksList = mutableListOf<MediaPlayer>().apply {
            add(MediaPlayer(requireContext(), R.raw.blue_light))
        }
        binding.recyclerTracksView.adapter = TracksAdapter(tracksList) { bind, player ->
            openPlayerFragment(bind.previewCard.transitionName, bind.previewCard, player)
        }
    }
    private fun openPlayerFragment(transitionName: String, view: View, player: MediaPlayer) {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .addSharedElement(view, transitionName)
            .replace(R.id.fragmentContainer, PlayerFragment.getInstance(player))
            .commit()
    }

    companion object {
        private const val TAG = "tracks_list_fragment"

        private var instance: TracksListFragment? = null
        fun getInstance(): TracksListFragment {
            return if (instance == null) {
                instance = TracksListFragment()
                instance as TracksListFragment
            } else {
                instance as TracksListFragment
            }
        }
    }
}