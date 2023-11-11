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
    private var audioFilesDir: File? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        audioFilesDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
    }
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
     * Getting the necessary track metadata by its absolute path.
     * */
    private fun getTrackMetadata(absolutePath: String): TrackInfo {
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(absolutePath)

        val trackArt = metadataRetriever.embeddedPicture?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
        return File.createTempFile("MEDIA_$timeStamp", ".mp3", audioFilesDir)
    }
    private fun showSavedTracks() {
        Log.d(TAG, "showSavedTracks()")

        getSavedFiles().forEach {
            with(it.absolutePath) {
                val mediaPlayer = createPlayer(this)
                val trackInfo = getTrackMetadata(this)

                addTrackToList(trackInfo, mediaPlayer).let { isAdded ->
                    if (isAdded) adapter.notifyItemInserted(trackList.size - 1)
                    else Log.i(TAG, "showSavedTracks: This track already added.")
                }
            }
        }
    }
    /**
     * Get a list of audio files stored in the app's memory.
     * */
    private fun getSavedFiles(): List<File> {
        val files = audioFilesDir?.listFiles()
        return files?.toList() ?: emptyList()
    }
    /**
     * Add a track to the track list if it is not in it.
     * */
    private fun addTrackToList(trackInfo: TrackInfo, player: MediaPlayer): Boolean {
        val trackExist = trackList.find { item -> item.second.copy(art = null) == trackInfo.copy(art = null) }
        if (trackExist == null) {
            trackList.add(Pair(player, trackInfo))
            return true
        }
        return false
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