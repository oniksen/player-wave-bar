package com.onixen.audioplayer.views.fragments

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialContainerTransform
import com.onixen.audioplayer.R
import com.onixen.audioplayer.databinding.PlayerFragmentBinding
import com.onixen.audioplayer.extentions.dp
import com.onixen.audioplayer.model.MediaPlayer
import com.onixen.audioplayer.model.data.TrackInfo
import com.onixen.audioplayer.extentions.msToUserFriendlyStr

class PlayerFragment(private val player: MediaPlayer): Fragment(R.layout.player_fragment) {
    private var _binding: PlayerFragmentBinding? = null
    private val binding get() = _binding!!

    private var startBannerWidthDp: Int = 0
    private var fullBannerWidthDp = 0

    private lateinit var metadata: TrackInfo

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
            /*if (playerVM.playerState.value == PlayerState.Start)
                playerVM.sendIntent(PlayerIntent.Recover)
            else
                playerVM.sendIntent(PlayerIntent.Prepare)

            playerActionsHandler()*/
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backBtn.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, TracksListFragment.getInstance())
                .commit()
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
    private fun bindMetadata(metadata: TrackInfo) {
        binding.apply {
            fullImg.setImageBitmap(metadata.art)
            currentTrackTime.text = "0"
            fullTrackTime.text = metadata.duration?.toInt()?.msToUserFriendlyStr()
        }

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