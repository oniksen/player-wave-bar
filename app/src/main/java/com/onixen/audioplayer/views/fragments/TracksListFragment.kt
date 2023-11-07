package com.onixen.audioplayer.views.fragments

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.oniksen.playgroundmvi_pattern.intents.PlayerIntent
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.TracksListFragmentBinding
import com.onixen.audioplayer.model.MediaPlayer
import com.onixen.audioplayer.model.data.TrackInfoV2
import com.onixen.audioplayer.viewModels.PlayerViewModelV2
import com.onixen.audioplayer.views.adapters.TracksAdapterV2
import java.net.URI

class TracksListFragment: Fragment(R.layout.tracks_list_fragment) {
    private var _binding: TracksListFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var selectMediaLauncher: ActivityResultLauncher<String>

    private val playerVm: PlayerViewModelV2 by activityViewModels()
    private lateinit var modalSheet: ModalBottomSheetPlayer
    private val trackListV2: MutableList<Pair<android.media.MediaPlayer, TrackInfoV2>> = mutableListOf()
    private lateinit var adapter: TracksAdapterV2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TracksListFragmentBinding.inflate(inflater, container, false)

        val item1 = with(R.raw.blue_light) {
            Pair(createPlayer(this), getMetadata(this))
        }
        val item2 = with(R.raw.disappearer) {
            Pair(createPlayer(this), getMetadata(this))
        }
        trackListV2.apply {
            val searchFirstItemRes = this.find { track -> track.second.title == item1.second.title }
            if (searchFirstItemRes == null) {
                add(item1)
            }
            val searchSecondItemRes = this.find { track -> track.second.title == item2.second.title }
            if (searchSecondItemRes == null) {
                add(item2)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TracksAdapterV2(trackListV2) { bind, player, retriever ->
            openPlayerFragmentV2(player, retriever)
        }
        binding.recyclerTracksView.adapter = adapter

        initActivityResultLauncher()

        binding.mainToolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_track -> {
                    selectMediaLauncher.launch("audio/*")
                    true
                }
                else -> { false }
            }
        }
    }

    private fun openPlayerFragmentV2(player: android.media.MediaPlayer, trackInfo: TrackInfoV2) {
        // Если это новый плеер (трек)
        Log.d(TAG,"openPlayerFragmentV2: old player = ${playerVm.fetchPlayerInfo()}, new player = $trackInfo")
        if (playerVm.fetchPlayerInfo()?.copy(art = null) != trackInfo.copy(art = null)) {
            playerVm.sendIntent(PlayerIntent.Stop)
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

    private fun initActivityResultLauncher() {
        selectMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            val player = android.media.MediaPlayer()
            it?.let {
                player.setDataSource(requireContext(), it)
            }
            player.prepare()

            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(context, it)

            val trackArt = metadataRetriever.embeddedPicture?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            val metadata = TrackInfoV2(
                title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                art = trackArt,
                duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt()
            )

            trackListV2.add(Pair(player, metadata))
            adapter.notifyItemInserted(trackListV2.size - 1)
        }
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