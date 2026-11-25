package com.example.wisatajogja.ui.detail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.data.model.GalleryItem
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.DestinationRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.ActivityDetailWisataBinding
import com.example.wisatajogja.ui.auth.LoginActivity
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.ui.reservasi.ReservasiActivity
import com.example.wisatajogja.util.formatNumber
import com.example.wisatajogja.util.formatRating
import com.example.wisatajogja.util.formatRupiah
import com.example.wisatajogja.util.loadPhoto
import com.example.wisatajogja.util.snack
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

class DetailWisataActivity : BaseActivity() {

    private lateinit var binding: ActivityDetailWisataBinding
    private val repository = DestinationRepository()
    private val galleryAdapter = GalleryAdapter(::showGalleryImage)
    private val serviceAdapter = ServiceAdapter()
    private val reviewAdapter = ReviewAdapter()

    private val reviewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadDetail()
            binding.root.snack(getString(R.string.review_success))
        }
    }

    private var destination: Destination? = null
    private var destinationId: Int = 0
    private var favorite: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailWisataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        destinationId = intent.getIntExtra(EXTRA_DESTINATION_ID, 0)
        if (destinationId <= 0) {
            finish()
            return
        }
        setupLists()
        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonRetry.setOnClickListener {
            binding.errorState.visibility = View.GONE
            loadDetail()
        }
        binding.buttonFavorite.setOnClickListener { toggleFavorite() }
        binding.buttonShare.setOnClickListener { shareDestination() }
        binding.actionOpenMaps.setOnClickListener { openMaps() }
        binding.buttonReserve.setOnClickListener { openReservation() }
        loadDetail()
    }

    private fun setupLists() = with(binding) {
        recyclerGallery.layoutManager = LinearLayoutManager(this@DetailWisataActivity, LinearLayoutManager.HORIZONTAL, false)
        recyclerGallery.adapter = galleryAdapter
        recyclerServices.layoutManager = LinearLayoutManager(this@DetailWisataActivity, LinearLayoutManager.HORIZONTAL, false)
        recyclerServices.adapter = serviceAdapter
        recyclerReviews.layoutManager = LinearLayoutManager(this@DetailWisataActivity)
        recyclerReviews.adapter = reviewAdapter
    }

    private fun loadDetail() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.scrollContent.visibility = View.GONE
        binding.bottomBar.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.detail(id = destinationId) }
                .onSuccess { item ->
                    destination = item
                    favorite = item.isFavorite
                    binding.loadingOverlay.visibility = View.GONE
                    binding.scrollContent.visibility = View.VISIBLE
                    binding.bottomBar.visibility = View.VISIBLE
                    bind(item)
                }
                .onFailure { error ->
                    binding.loadingOverlay.visibility = View.GONE
                    binding.errorState.visibility = View.VISIBLE
                    binding.textError.text = (error as? ApiException)?.message ?: getString(R.string.common_error_network)
                }
        }
    }

    private fun bind(item: Destination) = with(binding) {
        imageHero.loadPhoto(item.imageUrl)
        textCategory.text = item.category.uppercase()
        textTitle.text = item.name
        textSubtitle.text = "${item.area} · Yogyakarta"
        textDescription.text = item.description
        textRating.text = formatRating(item.rating)
        textReviewCount.text = getString(R.string.detail_reviews_count, item.reviewCount)
        textPriceInfo.text = if (item.ticketPrice == 0) getString(R.string.common_free) else "${item.ticketPrice / 1000} rb"
        textHours.text = item.openHours.substringBefore(" -").trim()
        textBottomPrice.text = formatRupiah(item.ticketPrice)
        textCoordinates.text = "${item.area} · ${String.format(Locale.US, "%.4f", item.latitude)}, ${String.format(Locale.US, "%.4f", item.longitude)}"
        textReviewsTitle.setText(R.string.detail_reviews)
        updateFavoriteIcon()
        val gallery = item.gallery.ifEmpty {
            listOf(GalleryItem(id = 0, imageUrl = item.imageUrl.orEmpty()))
        }
        galleryAdapter.submit(gallery)
        textGalleryCount.text = "${gallery.size} foto"
        serviceAdapter.submit(item.services)
        reviewAdapter.submit(item.reviews)
        textNoReviews.visibility = if (item.reviews.isEmpty()) View.VISIBLE else View.GONE
        buttonWriteReview.text = getString(if (item.myReview != null) R.string.review_edit else R.string.review_write)
        buttonWriteReview.setOnClickListener { openReviewForm(item) }
        loadMap(item)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadMap(item: Destination) {
        binding.webViewMap.apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.cream_light))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url
                    if (url.host == "www.openstreetmap.org" && url.path == "/export/embed.html") return false
                    runCatching { view.context.startActivity(Intent(Intent.ACTION_VIEW, url)) }
                    return true
                }
            }
            val mapUrl = String.format(
                Locale.US,
                "https://www.openstreetmap.org/export/embed.html?bbox=%.5f,%.5f,%.5f,%.5f&layer=mapnik&marker=%.6f,%.6f",
                item.longitude - 0.012,
                item.latitude - 0.007,
                item.longitude + 0.012,
                item.latitude + 0.007,
                item.latitude,
                item.longitude,
            )
            loadUrl(mapUrl)
        }
    }

    private fun updateFavoriteIcon() = with(binding) {
        buttonFavorite.setImageResource(if (favorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        buttonFavorite.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this@DetailWisataActivity, if (favorite) R.color.terracotta else R.color.green_dark)
        )
    }

    private fun toggleFavorite() {
        if (!SessionManager.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        val item = destination ?: return
        lifecycleScope.launch {
            runCatching { repository.toggleFavorite(item.id) }
                .onSuccess { isFavorite ->
                    favorite = isFavorite
                    updateFavoriteIcon()
                    binding.root.snack(getString(if (isFavorite) R.string.detail_favorite_on else R.string.saved_removed))
                }
                .onFailure { error ->
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
                }
        }
    }

    private fun shareDestination() {
        val item = destination ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, item.name)
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.detail_share_text, item.name) +
                    "\nhttps://maps.google.com/?q=${item.latitude},${item.longitude}"
            )
        }
        runCatching { startActivity(Intent.createChooser(intent, getString(R.string.detail_share))) }
            .onFailure { binding.root.snack(getString(R.string.error_generic)) }
    }

    private fun openMaps() {
        val item = destination ?: return
        val label = Uri.encode(item.name)
        val uri = Uri.parse("geo:${item.latitude},${item.longitude}?q=${item.latitude},${item.longitude}($label)")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (error: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${item.latitude},${item.longitude}")
                )
            )
        }
    }

    private fun openReviewForm(item: Destination) {
        if (!SessionManager.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        reviewLauncher.launch(
            Intent(this, TulisUlasanActivity::class.java)
                .putExtra(TulisUlasanActivity.EXTRA_DESTINATION_ID, item.id)
                .putExtra(TulisUlasanActivity.EXTRA_DESTINATION_NAME, item.name)
                .putExtra(TulisUlasanActivity.EXTRA_RATING, item.myReview?.rating ?: 5)
                .putExtra(TulisUlasanActivity.EXTRA_COMMENT, item.myReview?.comment.orEmpty())
        )
    }

    private fun openReservation() {
        if (!SessionManager.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        val item = destination ?: return
        startActivity(
            Intent(this, ReservasiActivity::class.java)
                .putExtra(ReservasiActivity.EXTRA_DESTINATION_ID, item.id)
                .putExtra(ReservasiActivity.EXTRA_DESTINATION_NAME, item.name)
                .putExtra(ReservasiActivity.EXTRA_DESTINATION_AREA, item.area)
                .putExtra(ReservasiActivity.EXTRA_DESTINATION_IMAGE, item.imageUrl)
                .putExtra(ReservasiActivity.EXTRA_TICKET_PRICE, item.ticketPrice)
        )
    }

    private fun showGalleryImage(item: GalleryItem) {
        val image = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(12, 12, 12, 12)
            loadPhoto(item.imageUrl)
        }
        MaterialAlertDialogBuilder(this)
            .setView(image)
            .setPositiveButton(R.string.common_close, null)
            .show()
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.webViewMap.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_DESTINATION_ID = "extra_destination_id"
    }
}
