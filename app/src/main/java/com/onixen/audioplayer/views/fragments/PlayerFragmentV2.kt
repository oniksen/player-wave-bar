package com.onixen.audioplayer.views.fragments

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.onixen.audioplayer.model.data.TrackInfo
import com.onixen.audioplayer.model.data.TrackInfoV2
import com.onixen.audioplayer.states.PlayerState
import com.onixen.audioplayer.states.PlayerStateV2
import com.onixen.audioplayer.viewModels.PlayerViewModel
import com.onixen.audioplayer.viewModels.PlayerViewModelV2
import com.onixen.audioplayer.views.interfaces.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.log

class PlayerFragmentV2: Fragment(R.layout.player_fragment_v2), PlayerView {
    private var _binding: PlayerFragmentV2Binding? = null
    private val binding get() = _binding!!

    private var startBannerWidthDp: Int = 0
    private var fullBannerWidthDp = 0

    private val playerVM by activityViewModels<PlayerViewModelV2>()

    override fun onAttach(context: Context) {
        super.onAttach(context)

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
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, TracksListFragment.getInstance())
                .commit()
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
                            is PlayerStateV2.Started -> {
                                setActionPauseTrackBtn()
                                if (it.currentPos == 0) {
                                    startTrack()
                                } else {
                                    resumeTrack(currentPos = it.currentPos)
                                }
                            }
                            is PlayerStateV2.Paused -> {
                                setActionPlayTrackBtn()
                                pauseTrack()
                            }
                        }
                    }
                }
            }
        }
    }
    private suspend fun bindInfo(trackInfo: TrackInfoV2) {
        Log.i(TAG, "bindInfo()")
        Log.w(TAG, "bindInfo: inside lifecyle scope.")
        delay(10)
        Log.w(TAG, "bindInfo: inside lifecyle scope after delay.")
        binding.apply {
            playerBar.setTrackPosition(trackInfo.currentTime)
            Log.d(TAG, "bindInfo: track position = ${trackInfo.currentTime}")
            playerBar.setTrackDuration(trackInfo.duration!!)
            Log.d(TAG, "bindInfo: track duration = ${trackInfo.duration}")
            fullTrackTime.text = trackInfo.duration.toInt().msToUserFriendlyStr()
            currentTrackTime.text = trackInfo.currentTime.msToUserFriendlyStr()
            fullImg.setImageBitmap(trackInfo.art)
            addRewindFlowListener()
            addCurrentTimeListener()
        }
    }
    private fun startTrack() {
        Log.i(TAG, "startTrack()")
        binding.playerBar.startAnimation()
    }
    private fun resumeTrack(currentPos: Int) {
        Log.i(TAG, "resumeTrack($currentPos)")
        binding.playerBar.resumeAnimation()
    }
    private fun pauseTrack() {
        Log.i(TAG, "pauseTrack()")
        binding.playerBar.pauseAnimation()
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
        lifecycleScope.launch {
            binding.playerBar.getCurrentTimeFlow().collect {
                if (it > 0) {
                    binding.currentTrackTime.text = it.msToUserFriendlyStr()
                }
            }
        }
    }

    override fun returnContext(): Context {
        return requireContext()
    }

    companion object {
        private const val TAG = "player_fragment_v2"
        private const val DURATION_TIME = 700L
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