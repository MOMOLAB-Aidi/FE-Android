package com.example.momolabfe.ui.record.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.momolabfe.databinding.ItemPhotoBinding

class AlbumAdapter(
    private val onClick: (Uri) -> Unit
) : ListAdapter<Uri, AlbumAdapter.PhotoViewHolder>(Diff) {

    private var selectedPos: Int = RecyclerView.NO_POSITION

    object Diff : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Uri, newItem: Uri) = true
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri, isSelected: Boolean) {
            Glide.with(binding.image)
                .load(uri)
                .centerCrop()
                .into(binding.image)

            binding.selectionOverlay.isVisible = isSelected

            binding.root.setOnClickListener {
                if (bindingAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                // 선택 상태 갱신
                val prev = selectedPos
                selectedPos = bindingAdapterPosition
                if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
                notifyItemChanged(selectedPos)

                // 상단 미리보기 콜백
                onClick(uri)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPos)
    }
}
