package com.onixen.audioplayer.views.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.transition.MaterialContainerTransform
import com.oniksen.playgroundmvi_pattern.intents.PlayerIntent
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.BottomSheetLayoutBinding
import com.onixen.audioplayer.extentions.msToUserFriendlyStr
import com.onixen.audioplayer.model.data.TrackInfo
import com.onixen.audioplayer.states.PlayerState
import com.onixen.audioplayer.viewModels.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModalBottomSheetPlayer(private val title: String): Fragment(R.layout.bottom_sheet_layout) {
    private var DURATION_TIME = 700L

    private var _binding: BottomSheetLayoutBinding? = null
    private val binding get() = _binding!!
    private val playerVM by activityViewModels<PlayerViewModel>()
    private var trackDuration: Int = -1

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            withContext(Dispatchers.Main) { bindInfo(playerVM.prepareView()) }
            playerActionsHandler()
        }
    }

    private suspend fun bindInfo(trackInfo: TrackInfo) {
        Log.i(TAG, "bindInfo()")
        Log.w(TAG, "bindInfo: inside lifecyle scope.")
        delay(100)
        Log.w(TAG, "bindInfo: inside lifecyle scope after delay.")
        binding.apply {
            trackDuration = trackInfo.duration!!
            playerBar.prepare(trackInfo.duration, trackInfo.currentTime)
            Log.d(TAG, "bindInfo: trackInfo.currentTime = ${trackInfo.currentTime.msToUserFriendlyStr()}")
            addRewindFlowListener()
            addCurrentTimeListener()
        }
    }
    private fun playerActionsHandler() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                setActionPlayTrackBtn()
                playerVM.playerState.collect {
                    it?.let {
                        when(it) {
                            is PlayerState.Started -> { startTrack(it.currentPos) }
                            is PlayerState.Paused -> { pauseTrack(it.currentPos) }
                            is PlayerState.Stoped -> { stopTrack() }
                        }
                    }
                }
            }
        }
    }
    private fun setActionPlayTrackBtn() {
        Log.d(TAG, "setActionPlayTrackBtn()")
        binding.startBtn.setOnClickListener {
            playerVM.sendIntent(PlayerIntent.Play)
        }
        binding.playPauseBtn.setImageResource(R.drawable.play_arrow)
    }
    private fun setActionPauseTrackBtn() {
        Log.d(TAG, "setActionPauseTrackBtn()")
        binding.startBtn.setOnClickListener {
            playerVM.sendIntent(PlayerIntent.Pause)
        }
        binding.playPauseBtn.setImageResource(R.drawable.pause)
    }
    /**
     * The function starts or restores the playback of the track depending on the current position of the tracks.
     * Also changes the state of the interface.
     * @param currentPos Number of milliseconds played.
     * */
    private fun startTrack(currentPos: Int) {
        setActionPauseTrackBtn()
        binding.playerBar.startAnimation()
    }
    /** The function pauses the playback of the track and also changes the state of the interface. */
    private fun pauseTrack(pos: Int?) {
        setActionPlayTrackBtn()
        Log.i(TAG, "pauseTrack()")
        binding.playerBar.pauseAnimation(pos!!)
    }
    private fun stopTrack() {
        setActionPlayTrackBtn()
        Log.i(TAG, "stopTrack()")
        binding.playerBar.stopAnimation()
    }
    private fun addRewindFlowListener() {
        lifecycleScope.launch {
            binding.playerBar.getRewindTimeFlow().collect {
                Log.d(TAG, "addRewindFlowListener: it = $it")
                if (it > 0)
                    playerVM.sendIntent(PlayerIntent.Rewind(it))
            }
        }
    }
    private fun addCurrentTimeListener() {
        Log.d(TAG, "addCurrentTimeListener:")
        lifecycleScope.launch {
            binding.playerBar.getCurrentTimeFlow().collect {
                if (it == trackDuration) {
                    playerVM.sendIntent(PlayerIntent.Stop)
                }
            }
        }
    }

    companion object {
        const val TAG = "bottom_sheet_player"
    }
}