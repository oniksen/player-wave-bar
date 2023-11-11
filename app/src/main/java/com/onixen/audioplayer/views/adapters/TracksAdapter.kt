package com.onixen.audioplayer.views.adapters

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.onixen.audioplayer.databinding.TrackListItemBinding
import com.onixen.audioplayer.model.data.TrackInfo

class TracksAdapter(
    private val list: List<Pair<MediaPlayer, TrackInfo>>,
    private val escape: (TrackListItemBinding, MediaPlayer, TrackInfo) -> Unit
): RecyclerView.Adapter<TracksAdapter.ViewHolder>() {
    class ViewHolder(private val binding: TrackListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(player: MediaPlayer, trackInfo: TrackInfo, escape: (TrackListItemBinding, MediaPlayer, TrackInfo) -> Unit) {
            binding.smallImg.setImageBitmap(trackInfo.art)
            binding.trackTitle.text = trackInfo.title
            binding.trackAuthor.text = trackInfo.artist

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