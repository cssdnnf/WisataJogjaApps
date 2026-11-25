package com.example.wisatajogja.ui.reservasi

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wisatajogja.R
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.ProfileRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.ActivityReservasiSayaBinding
import com.example.wisatajogja.ui.auth.LoginActivity
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.util.snack
import kotlinx.coroutines.launch

class ReservasiSayaActivity : BaseActivity() {

    private lateinit var binding: ActivityReservasiSayaBinding
    private val repository = ProfileRepository()
    private val adapter = ReservationHistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityReservasiSayaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        binding.recyclerReservations.layoutManager = LinearLayoutManager(this)
        binding.recyclerReservations.adapter = adapter
        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonEmptyAction.setOnClickListener { finish() }
        binding.swipeRefresh.setOnRefreshListener { loadReservations() }
        loadReservations()
    }

    private fun loadReservations() {
        binding.progressBar.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        lifecycleScope.launch {
            runCatching { repository.reservations() }
                .onSuccess { items ->
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    adapter.submitList(items)
                    binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    val upcoming = items.count { it.status == "menunggu" || it.status == "dikonfirmasi" }
                    binding.textSummary.text = buildString {
                        append(getString(R.string.reservation_history_count, items.size))
                        if (upcoming > 0) {
                            append(" · ")
                            append(getString(R.string.reservation_history_upcoming, upcoming))
                        }
                    }
                    binding.textSummary.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                }
                .onFailure { error ->
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.emptyState.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.common_error_network))
                }
        }
    }
}
