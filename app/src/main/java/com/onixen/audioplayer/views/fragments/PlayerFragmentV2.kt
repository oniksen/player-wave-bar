package com.onixen.audioplayer.views.fragments

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.transition.MaterialContainerTransform
import com.oniksen.playgroundmvi_pattern.intents.PlayerIntent
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.PlayerFragmentV2Binding
import com.onixen.audioplayer.extentions.dp
import com.onixen.audioplayer.extentions.msToUserFriendlyStr
import com.onixen.audioplayer.model.data.TrackInfoV2
import com.onixen.audioplayer.states.PlayerStateV2
import com.onixen.audioplayer.viewModels.PlayerViewModelV2
import com.onixen.audioplayer.views.interfaces.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerFragmentV2: Fragment(R.layout.player_fragment_v2), PlayerView {
    private var DURATION_TIME = 700L

    private var _binding: PlayerFragmentV2Binding? = null
    private val binding get() = _binding!!

    private var trackDuration: Int = -1

    private var startBannerWidthDp: Int = 0
    private var fullBannerWidthDp = 0

    private val playerVM by activityViewModels<PlayerViewModelV2>()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        DURATION_TIME = resources.getInteger(R.integer.animation_duration).toLong()
        startBannerWidthDp = START_BANNER_WIDTH.dp(requireContext())
        fullBannerWidthDp = FULL_BANNER_WIDTH.dp(requireContext())
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerFragmentV2Binding.inflate(inflater, container, false)

        binding.backBtn.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.trackCard.layoutParams.width = startBannerWidthDp

        val transition = MaterialContainerTransform().apply {
            duration = DURATION_TIME
            scrimColor = Color.TRANSPARENT
        }
        sharedElementEnterTransition = transition
        sharedElementReturnTransition = transition

        val animatorSet = AnimatorSet()

        val imgOffsetAnimation = createImgOffsetAnimation()
        val backBtnAnimation = createBackBtnAnimation()
        val widthAnimation = createWidthBannerAnimation()

        animatorSet.playTogether(widthAnimation, imgOffsetAnimation, backBtnAnimation)
        animatorSet.start()
        animatorSet.doOnEnd {
            Log.d(TAG, "onCreateView: End of animation.")
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

    private fun createImgOffsetAnimation(): ValueAnimator? {
        val startTranslation = -30.dp(requireContext()).toFloat()
        val dx = (fullBannerWidthDp - startBannerWidthDp).toFloat()
        return ValueAnimator.ofFloat(startTranslation, (startTranslation + dx / 2)).apply {
            duration = DURATION_TIME
            //interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                binding.fullImg.translationX = it.animatedValue as Float
            }
        }
    }
    private fun createBackBtnAnimation(): ValueAnimator? {
        val animation = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURATION_TIME
            addUpdateListener {
                binding.backBtn.alpha = it.animatedValue as Float
            }
        }
        return animation
    }
    private fun createWidthBannerAnimation(): ValueAnimator? {
        val animator = ValueAnimator.ofInt(startBannerWidthDp, fullBannerWidthDp).apply {
            duration = DURATION_TIME

            addUpdateListener {
                binding.trackCard.layoutParams.width = it.animatedValue as Int
                binding.trackCard.requestLayout()
            }
        }
        return animator
    }

    private fun playerActionsHandler() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                setActionPlayTrackBtn()
                playerVM.playerState.collect {
                    it?.let {
                        when(it) {
                            is PlayerStateV2.Started -> { startTrack(it.currentPos) }
                            is PlayerStateV2.Paused -> { pauseTrack(it.currentPos) }
                            is PlayerStateV2.Stoped -> { stopTrack() }
                        }
                    }
                }
            }
        }
    }
    private suspend fun bindInfo(trackInfo: TrackInfoV2) {
        Log.i(TAG, "bindInfo()")
        Log.w(TAG, "bindInfo: inside lifecyle scope.")
        delay(100)
        Log.w(TAG, "bindInfo: inside lifecyle scope after delay.")
        binding.apply {
            trackDuration = trackInfo.duration!!
            playerBar.prepare(trackInfo.duration, trackInfo.currentTime)
            Log.d(TAG, "bindInfo: trackInfo.currentTime = ${trackInfo.currentTime.msToUserFriendlyStr()}")
            
            fullTrackTime.text = trackInfo.duration.toInt().msToUserFriendlyStr()
            currentTrackTime.text = trackInfo.currentTime.msToUserFriendlyStr()
            fullImg.setImageBitmap(trackInfo.art)
            addRewindFlowListener()
            addCurrentTimeListener()
        }
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
                if (it > 0) {
                    binding.currentTrackTime.text = it.msToUserFriendlyStr()
                }
                if (it == trackDuration) {
                    playerVM.sendIntent(PlayerIntent.Stop)
                }
            }
        }
    }

    override fun returnContext(): Context {
        return requireContext()
    }

    companion object {
        private const val TAG = "player_fragment_v2"
        // private const val DURATION_TIME =
        private const val START_BANNER_WIDTH = 270
        private const val FULL_BANNER_WIDTH = 300

        private var instance: PlayerFragmentV2? = null
        fun getInstance(): PlayerFragmentV2 {
            return if (instance == null) {
                instance = PlayerFragmentV2()
                instance as PlayerFragmentV2
            } else {
                instance as PlayerFragmentV2
            }
        }
    }
}