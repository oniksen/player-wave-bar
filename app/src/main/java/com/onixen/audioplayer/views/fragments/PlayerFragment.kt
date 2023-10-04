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
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.transition.MaterialContainerTransform
import com.oniksen.playgroundmvi_pattern.intents.PlayerIntent
import com.onixen.audioplayer.states.PlayerState
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.PlayerFragmentBinding
import com.onixen.audioplayer.extentions.dp
import com.onixen.audioplayer.model.MediaPlayer
import com.onixen.audioplayer.model.data.TrackInfo
import com.onixen.audioplayer.extentions.msToUserFriendlyStr
import com.onixen.audioplayer.viewModels.PlayerViewModel
import com.onixen.audioplayer.views.interfaces.PlayerView
import kotlinx.coroutines.launch

class PlayerFragment(private val player: MediaPlayer): Fragment(R.layout.player_fragment), PlayerView {
    private var _binding: PlayerFragmentBinding? = null
    private val binding get() = _binding!!

    private var startBannerWidthDp: Int = 0
    private var fullBannerWidthDp = 0

    private lateinit var metadata: TrackInfo
    private val playerVM by activityViewModels<PlayerViewModel>()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        startBannerWidthDp = START_BANNER_WIDTH.dp(requireContext())
        fullBannerWidthDp = FULL_BANNER_WIDTH.dp(requireContext())

        metadata = player.getMetadata()
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerFragmentBinding.inflate(inflater, container, false)

        bindMetadata(metadata)

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
            Log.d(TAG, "onCreateView: player state = ${playerVM.playerState.value}")
            /*when(playerVM.playerState.value) {
                is PlayerState.Started -> {  }
                else -> { playerVM.sendIntent(PlayerIntent.Prepare) }
            }*/
            playerVM.sendIntent(PlayerIntent.Prepare)
            playerActionsHandler()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playerVM.attachPlayerView(this, player)
        binding.backBtn.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, TracksListFragment.getInstance())
                .commit()
        }
        currentTimeListener()
        rewindTimeListener()
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
    private fun bindMetadata(metadata: TrackInfo) {
        binding.apply {
            fullImg.setImageBitmap(metadata.art)
            currentTrackTime.text = "0"
            fullTrackTime.text = metadata.duration?.toInt()?.msToUserFriendlyStr()
        }

    }
    private fun playerActionsHandler() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                playerVM.playerState.collect {
                    when(it) {
                        is PlayerState.Recovered -> { trackIsRecovered(it.currentPos, it.metadata) }
                        is PlayerState.Prepared -> {
                            trackIsPrepared(it.info)
                            setActionPlayTrackBtn()
                        }
                        is PlayerState.Started -> {
                            trackIsPlayed()
                            setActionPauseTrackBtn()
                        }
                        is PlayerState.Stopped -> {
                            setActionPlayTrackBtn()
                        }
                        is PlayerState.Paused -> {
                            trackIsPaused()
                            setActionPlayTrackBtn()
                        }
                        is PlayerState.Resumed -> {
                            trackIsResumed()
                            setActionPauseTrackBtn()
                        }
                    }
                }
            }
        }
    }
    private fun currentTimeListener() {
        lifecycleScope.launch {
            binding.playerBar.getCurrentTimeFlow().collect {
                binding.currentTrackTime.text = it.msToUserFriendlyStr()
            }
        }
    }
    private fun rewindTimeListener() {
        lifecycleScope.launch {
            binding.playerBar.getRewindTimeFlow().collect {
                if (it >= 0)
                    playerVM.sendIntent(PlayerIntent.Rewind(it))
            }
        }
    }
    private fun trackIsPrepared(info: TrackInfo) {
        Log.d(TAG, "trackIsPrepared()")
        binding.playerBar.setTrackDuration(info.duration?.toInt()!!)
        binding.fullTrackTime.text = info.duration.toInt().msToUserFriendlyStr()
        info.art?.let {
            // binding.fullImg.setImageBitmap(it)
        }
    }
    private fun trackIsPlayed() {
        Log.d(TAG, "trackIsPlayed()")
        binding.apply {
            playPauseBtn.setImageResource(R.drawable.pause)
            playerBar.startAnimation()
        }
    }
    private fun trackIsResumed() {
        Log.d(TAG, "trackIsResumed()")
        binding.apply {
            playPauseBtn.setImageResource(R.drawable.pause)
            playerBar.resumeAnimation()
        }
    }
    private fun trackIsPaused() {
        Log.d(TAG, "trackIsPaused: ")
        binding.apply {
            playPauseBtn.setImageResource(R.drawable.play_arrow)
            playerBar.pauseAnimation()
        }
    }
    private fun trackIsRecovered(currentPos: Int, metadata: TrackInfo) {
        Log.d(TAG, "trackIsRecovered: currentPos = $currentPos")
        binding.apply {
            playerBar.setTrackDuration(metadata.duration!!.toInt())
            playerBar.setTrackPosition(100000)
            playerBar.startAnimation()
        }
        setActionPauseTrackBtn()
    }
    private fun setActionPlayTrackBtn() {
        binding.startBtn.setOnClickListener {
            playerVM.sendIntent(PlayerIntent.Play)
        }
    }
    private fun setActionPauseTrackBtn() {
        binding.startBtn.setOnClickListener {
            playerVM.sendIntent(PlayerIntent.Pause)
        }
    }
    override fun returnContext(): Context {
        return requireContext()
    }

    companion object {
        private const val TAG = "player_fragment"

        private const val DURATION_TIME = 700L
        private const val START_BANNER_WIDTH = 270
        private const val FULL_BANNER_WIDTH = 300

        private var instance: PlayerFragment? = null
        fun getInstance(player: MediaPlayer): PlayerFragment {
            return if (instance == null) {
                instance = PlayerFragment(player)
                instance as PlayerFragment
            } else {
                instance as PlayerFragment
            }
        }
    }
}