package com.otomemo.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class ImagePagerActivity : AppCompatActivity() {

    private lateinit var images: List<String>
    private lateinit var adapter: PagerAdapter
    private lateinit var textIndex: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_pager)

        images = intent.getStringArrayExtra(EXTRA_IMAGES)?.toList() ?: emptyList()
        val start = intent.getIntExtra(EXTRA_INDEX, 0)
        textIndex = findViewById(R.id.textPagerIndex)

        adapter = PagerAdapter()
        val recycler = findViewById<RecyclerView>(R.id.recyclerPager)
        recycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(recycler)
        recycler.adapter = adapter
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lm = rv.layoutManager as? LinearLayoutManager ?: return
                    val pos = lm.findFirstCompletelyVisibleItemPosition()
                    if (pos in images.indices) updateIndexCounter(pos)
                }
            }
        })
        if (start in images.indices) recycler.scrollToPosition(start)
        updateIndexCounter(if (start in images.indices) start else 0)
    }

    private fun updateIndexCounter(position: Int) {
        if (images.isEmpty()) return
        textIndex.text = "${position + 1}/${images.size}"
    }

    private inner class PagerAdapter : RecyclerView.Adapter<PagerAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.imagePager)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pager, parent, false)
            return Holder(view)
        }

        override fun getItemCount() = images.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.image.setImageBitmap(ImageStore.load(this@ImagePagerActivity, images[position]))
        }
    }

    companion object {
        const val EXTRA_IMAGES = "extra_images"
        const val EXTRA_INDEX = "extra_index"
    }
}
