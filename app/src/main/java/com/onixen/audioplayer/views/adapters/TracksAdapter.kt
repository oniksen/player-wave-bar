package com.onixen.audioplayer.views.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.onixen.audioplayer.databinding.TrackListItemBinding
import com.onixen.audioplayer.model.MediaPlayer

class TracksAdapter(
    private val list: List<MediaPlayer>,
    private val escape: (TrackListItemBinding, MediaPlayer) -> Unit
): RecyclerView.Adapter<TracksAdapter.ViewHolder>() {
    class ViewHolder(private val binding: TrackListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(player: MediaPlayer, escape: (TrackListItemBinding, MediaPlayer) -> Unit) {
            val metadata = player.getMetadata()
            binding.smallImg.setImageBitmap(metadata.art)

            binding.previewCard.setOnClickListener {
                Toast.makeText(binding.root.context, "${metadata.title} is clicked.", Toast.LENGTH_SHORT).show()

                escape.invoke(binding, player)
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
        holder.bind(list[position], escape)
    }

}