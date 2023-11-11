package com.onixen.audioplayer.views.fragments

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.oniksen.playgroundmvi_pattern.intents.PlayerIntent
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.TracksListFragmentBinding
import com.onixen.audioplayer.model.data.TrackInfo
import com.onixen.audioplayer.viewModels.PlayerViewModel
import com.onixen.audioplayer.views.adapters.TracksAdapter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class TracksListFragment: Fragment(R.layout.tracks_list_fragment) {
    private var _binding: TracksListFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var selectMediaLauncher: ActivityResultLauncher<String>

    private val playerVm: PlayerViewModel by activityViewModels()
    private lateinit var modalSheet: ModalBottomSheetPlayer
    private val trackList: MutableList<Pair<MediaPlayer, TrackInfo>> = mutableListOf()
    private lateinit var adapter: TracksAdapter

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

        adapter = TracksAdapter(trackList) { bind, player, retriever ->
            showBottomPlayerSheet(player, retriever)
        }
        binding.recyclerTracksView.adapter = adapter

        initActivityResultLauncher()

        binding.mainToolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_track -> {
                    selectMediaLauncher.launch("audio/*")
                    // openFilePicker()
                    true
                }
                else -> { false }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showSavedTracks()
    }

    private fun showBottomPlayerSheet(player: MediaPlayer, trackInfo: TrackInfo) {
        // Если это новый плеер (трек)
        if (playerVm.fetchPlayerInfo()?.copy(art = null) != trackInfo.copy(art = null)) {
            playerVm.sendIntent(PlayerIntent.Stop)
            Log.d(TAG, "openPlayerFragmentV2: attach new player")
            playerVm.attachTrackData(player, trackInfo)
        }
        modalSheet = ModalBottomSheetPlayer(trackInfo.title.toString())
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.playerBottomSheet, modalSheet)
            .addToBackStack(TAG)
            .commit()
    }

    private fun initActivityResultLauncher() {
        selectMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            it?.let {
                Log.d(TAG, "initActivityResultLauncher: $it")
                saveMediaFile(it)
            }
        }
    }

    /**
     * Getting the necessary track metadata by its id from project resources.
     * */
    private fun getTrackMetadata(resId: Int): TrackInfo {
        val uri = Uri.parse("android.resource://" + requireContext().packageName + "/" + resId)
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(context, uri)

        val trackArt = metadataRetriever.embeddedPicture?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }

        return TrackInfo(
            title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
            artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            art = trackArt,
            duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt()
        )
    }
    /**
     * Creating an instance of the player by the absolute path of the audio file.
     * */
    private fun createPlayer(absolutePath: String): MediaPlayer {
        return MediaPlayer().apply {
            setDataSource(absolutePath)
            prepare()
        }
    }
    /**
     * The function of collecting a set of URI tracks stored in the application memory.
     * */
    private fun saveMediaFile(uri: Uri) {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val outputStream: OutputStream = FileOutputStream(createMediaFile())

        inputStream?.copyTo(outputStream, bufferSize = DEFAULT_BUFFER_SIZE)

        outputStream.close()
        inputStream?.close()
    }

    private fun createMediaFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)

        return File.createTempFile("MEDIA_$timeStamp", ".mp3", storageDir)
    }
    private fun showSavedTracks() {
        Log.d(TAG, "showSavedTracks()")

        getSavedFiles().forEach {
            val mediaPlayer = createPlayer(it.absolutePath)

            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(it.absolutePath)

            val trackArt = metadataRetriever.embeddedPicture?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }

            val trackInfo = TrackInfo(
                title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                art = trackArt,
                duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt()
            )

            val trackExist = trackList.find { item -> item.second.copy(art = null) == trackInfo.copy(art = null) }
            if (trackExist == null) {
                trackList.add(
                    Pair(
                        mediaPlayer,
                        trackInfo
                    )
                )
                adapter.notifyItemInserted(trackList.size - 1)
            }
        }
    }
    private fun getSavedFiles(): List<File> {
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val files = storageDir?.listFiles()
        return files?.toList() ?: emptyList()
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