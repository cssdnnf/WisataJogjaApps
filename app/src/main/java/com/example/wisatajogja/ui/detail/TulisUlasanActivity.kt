package com.example.wisatajogja.ui.detail

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.DestinationRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.ActivityTulisUlasanBinding
import com.example.wisatajogja.ui.auth.LoginActivity
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.util.hideKeyboard
import com.example.wisatajogja.util.snack
import com.example.wisatajogja.util.toast
import kotlinx.coroutines.launch

class TulisUlasanActivity : BaseActivity() {

    private lateinit var binding: ActivityTulisUlasanBinding
    private val repository = DestinationRepository()
    private var destinationId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityTulisUlasanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        destinationId = intent.getIntExtra(EXTRA_DESTINATION_ID, 0)
        if (destinationId <= 0) {
            finish()
            return
        }
        binding.textDestinationName.text = intent.getStringExtra(EXTRA_DESTINATION_NAME).orEmpty()
        val initialRating = intent.getIntExtra(EXTRA_RATING, 5).coerceIn(1, 5)
        binding.ratingBar.rating = initialRating.toFloat()
        binding.inputComment.setText(intent.getStringExtra(EXTRA_COMMENT).orEmpty())
        binding.textRatingLabel.setText(ratingLabel(initialRating))

        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            binding.textRatingLabel.setText(ratingLabel(rating.toInt().coerceIn(1, 5)))
        }
        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonSubmit.setOnClickListener { submit() }
    }

    private fun submit() = with(binding) {
        val rating = ratingBar.rating.toInt().coerceIn(1, 5)
        val comment = inputComment.text?.toString()?.trim().orEmpty()
        if (comment.length < 5) {
            inputCommentLayout.error = getString(R.string.review_comment_short)
            return@with
        }
        inputCommentLayout.error = null
        hideKeyboardFromRoot()
        buttonSubmit.isEnabled = false
        buttonSubmit.text = getString(R.string.review_sending)
        lifecycleScope.launch {
            runCatching { repository.submitReview(destinationId, rating, comment) }
                .onSuccess {
                    toast(getString(R.string.review_success))
                    setResult(RESULT_OK)
                    finish()
                }
                .onFailure { error ->
                    buttonSubmit.isEnabled = true
                    buttonSubmit.setText(R.string.review_submit)
                    root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
                }
        }
    }

    private fun hideKeyboardFromRoot() {
        binding.root.hideKeyboard()
    }

    private fun ratingLabel(rating: Int): Int = when (rating) {
        1 -> R.string.review_rating_1
        2 -> R.string.review_rating_2
        3 -> R.string.review_rating_3
        4 -> R.string.review_rating_4
        else -> R.string.review_rating_5
    }

    companion object {
        const val EXTRA_DESTINATION_ID = "extra_destination_id"
        const val EXTRA_DESTINATION_NAME = "extra_destination_name"
        const val EXTRA_RATING = "extra_rating"
        const val EXTRA_COMMENT = "extra_comment"
    }
}
