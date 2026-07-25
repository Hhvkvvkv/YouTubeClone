package com.ytclone.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.ytclone.R
import com.ytclone.adapters.VideoAdapter
import com.ytclone.api.YouTubeApi
import com.ytclone.models.Category
import com.ytclone.ui.player.PlayerActivity
import com.ytclone.ui.search.SearchActivity
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerVideos: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private var selectedCategoryIndex = 0

    private val categories = listOf(
        Category("all", "كلّ المحتوى", null),
        Category("podcast", "بودكاست", null),
        Category("music", "موسيقى", null),
        Category("mixes", "ميكسات", null),
        Category("live", "مباشر", null),
        Category("gaming", "ألعاب فيديو", null),
        Category("news", "أخبار", null),
        Category("sports", "رياضة", null),
        Category("learning", "تعليم", null),
        Category("fashion", "أزياء وجمال", null)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipGroup = view.findViewById(R.id.chipGroup)
        recyclerVideos = view.findViewById(R.id.recyclerVideos)

        setupChips()
        setupRecyclerView()
        setupTopBar(view)
        loadVideos()
    }

    private fun setupTopBar(view: View) {
        view.findViewById<View>(R.id.btnSearch)?.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }
    }

    private fun setupChips() {
        chipGroup.removeAllViews()
        categories.forEachIndexed { index, category ->
            val chip = Chip(requireContext()).apply {
                text = category.title
                isCheckable = true
                isChecked = index == selectedCategoryIndex
                setTextColor(
                    if (isChecked) resources.getColor(R.color.youtube_dark, null)
                    else resources.getColor(R.color.youtube_chip_text, null)
                )
                setChipBackgroundColorResource(
                    if (isChecked) R.color.youtube_chip_selected
                    else R.color.youtube_chip_bg
                )
                textSize = 14f
                setOnClickListener {
                    selectedCategoryIndex = index
                    updateChipColors()
                    loadVideos()
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun updateChipColors() {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            chip.setTextColor(
                if (i == selectedCategoryIndex) resources.getColor(R.color.youtube_dark, null)
                else resources.getColor(R.color.youtube_chip_text, null)
            )
            chip.setChipBackgroundColorResource(
                if (i == selectedCategoryIndex) R.color.youtube_chip_selected
                else R.color.youtube_chip_bg
            )
        }
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoAdapter(
            onVideoClick = { video ->
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("video_id", video.videoId)
                    putExtra("title", video.title)
                    putExtra("channel_name", video.channelName)
                }
                startActivity(intent)
            }
        )
        recyclerVideos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
        }
    }

    private fun loadVideos() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val categoryId = categories[selectedCategoryIndex].id
                val videos = YouTubeApi.getHomeFeed(categoryId)
                videoAdapter.submitList(videos)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "خطأ في تحميل الفيديوهات", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
