package com.example.wisatajogja.ui.common

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.databinding.ActivityCropImageBinding
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.util.PhotoUtils
import com.example.wisatajogja.util.hideKeyboard
import com.example.wisatajogja.util.snack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class CropImageActivity : BaseActivity() {

    private lateinit var binding: ActivityCropImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        val aspectRatio = intent.getFloatExtra(EXTRA_ASPECT_RATIO, 1f).takeIf { it > 0f } ?: 1f
        val isProfile = abs(aspectRatio - 1f) < 0.05f
        binding.textCropTitle.setText(if (isProfile) R.string.crop_title_profile else R.string.crop_title_destination)
        binding.textCropSubtitle.setText(if (isProfile) R.string.crop_subtitle_profile else R.string.crop_subtitle_destination)
        binding.cropView.setAspectRatio(aspectRatio)
        binding.cropProgress.visibility = View.VISIBLE
        binding.buttonSave.isEnabled = false
        binding.buttonZoomIn.isEnabled = false
        binding.buttonZoomOut.isEnabled = false

        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonCancel.setOnClickListener { finish() }
        binding.buttonZoomOut.setOnClickListener { binding.cropView.zoomBy(0.8f) }
        binding.buttonZoomIn.setOnClickListener { binding.cropView.zoomBy(1.25f) }
        binding.buttonSave.setOnClickListener { saveImage() }

        val uri = intent.getStringExtra(EXTRA_SOURCE_URI)?.let(Uri::parse)
        if (uri == null) {
            binding.root.snack(getString(R.string.crop_failed))
            finish()
            return
        }
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                PhotoUtils.loadOrientedBitmap(this@CropImageActivity, uri, maxDimension = 2400)
            }
            if (bitmap == null) {
                binding.cropProgress.visibility = View.GONE
                binding.root.snack(getString(R.string.crop_failed))
                finish()
                return@launch
            }
            binding.cropView.setImage(bitmap)
            binding.cropProgress.visibility = View.GONE
            binding.buttonSave.isEnabled = true
            binding.buttonZoomIn.isEnabled = true
            binding.buttonZoomOut.isEnabled = true
        }
    }

    private fun saveImage() = with(binding) {
        buttonSave.isEnabled = false
        buttonSave.text = getString(R.string.crop_processing)
        cropProgress.visibility = View.VISIBLE
        binding.root.hideKeyboard()
        val file = cropView.createCroppedFile(this@CropImageActivity)
        if (file == null) {
            cropProgress.visibility = View.GONE
            buttonSave.isEnabled = true
            buttonSave.setText(R.string.crop_apply)
            root.snack(getString(R.string.crop_failed))
            return@with
        }
        setResult(
            RESULT_OK,
            Intent().putExtra(EXTRA_OUTPUT_PATH, file.absolutePath),
        )
        finish()
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.cropView.release()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SOURCE_URI = "extra_source_uri"
        const val EXTRA_ASPECT_RATIO = "extra_aspect_ratio"
        const val EXTRA_OUTPUT_PATH = "extra_output_path"
    }
}
