package com.onixen.audioplayer.views.fragments

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.TracksListFragmentBinding
import com.onixen.audioplayer.model.MediaPlayer
import com.onixen.audioplayer.model.data.TrackInfoV2
import com.onixen.audioplayer.viewModels.PlayerViewModelV2
import com.onixen.audioplayer.views.adapters.TracksAdapterV2

class TracksListFragment: Fragment(R.layout.tracks_list_fragment) {
    private var _binding: TracksListFragmentBinding? = null
    private val binding get() = _binding!!

    private val playerVm: PlayerViewModelV2 by activityViewModels()
    private lateinit var modalSheet: ModalBottomSheetPlayer

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
        val trackListV2 = mutableListOf<Pair<android.media.MediaPlayer, TrackInfoV2>>().apply {
            with(R.raw.blue_light) {
                add(Pair(createPlayer(this), getMetadata(this)))
            }
        }
        /*binding.recyclerTracksView.adapter = TracksAdapter(tracksList) { bind, player ->
            openPlayerFragment(bind.previewCard.transitionName, bind.previewCard, player)
        }*/
        binding.recyclerTracksView.adapter = TracksAdapterV2(trackListV2) { bind, player, retriever ->
            openPlayerFragmentV2(player, retriever)
        }
    }
    private fun openPlayerFragment(transitionName: String, view: View, player: MediaPlayer) {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .addSharedElement(view, transitionName)
            .replace(R.id.fragmentContainer, PlayerFragment.getInstance(player))
            .commit()
    }
    private fun openPlayerFragmentV2(player: android.media.MediaPlayer, trackInfo: TrackInfoV2) {
        // Если это новый плеер (трек)
        Log.d(TAG,"openPlayerFragmentV2: old player = ${playerVm.fetchPlayerInfo()}, new player = $trackInfo")
        if (playerVm.fetchPlayerInfo()?.copy(art = null) != trackInfo.copy(art = null)) {
            Log.d(TAG, "openPlayerFragmentV2: attach new player")
            playerVm.attachTrackData(player, trackInfo)
        }
        modalSheet = ModalBottomSheetPlayer(trackInfo.title.toString())
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.playerBottomSheet, modalSheet)
            .commit()
    }
    private fun createPlayer(resId: Int): android.media.MediaPlayer {
        return android.media.MediaPlayer.create(requireContext(), resId)
    }
    private fun getMetadata(resId: Int): TrackInfoV2 {
        val uri = Uri.parse("android.resource://" + requireContext().packageName + "/" + resId)
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(context, uri)

        val trackArt = metadataRetriever.embeddedPicture?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }

        return TrackInfoV2(
            title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
            artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            art = trackArt,
            duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt()
        )
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