package com.onixen.audioplayer.views.adapters

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.onixen.audioplayer.databinding.TrackListItemBinding
import com.onixen.audioplayer.model.data.TrackInfo
import com.onixen.audioplayer.model.data.TrackInfoV2

class TracksAdapterV2(
    private val list: List<Pair<MediaPlayer, TrackInfoV2>>,
    private val escape: (TrackListItemBinding, MediaPlayer, TrackInfoV2) -> Unit
): RecyclerView.Adapter<TracksAdapterV2.ViewHolder>() {
    class ViewHolder(private val binding: TrackListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(player: MediaPlayer, trackInfo: TrackInfoV2, escape: (TrackListItemBinding, MediaPlayer, TrackInfoV2) -> Unit) {
            binding.smallImg.setImageBitmap(trackInfo.art)

            binding.previewCard.setOnClickListener {
                escape.invoke(binding, player, trackInfo)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val listItemViewBinding = TrackListItemBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(listItemViewBinding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position].first, list[position].second, escape)
    }

}