package com.example.wisatajogja.ui.admin.ringkasan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wisatajogja.R
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.AdminRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.FragmentRingkasanBinding
import com.example.wisatajogja.ui.admin.AdminMainActivity
import com.example.wisatajogja.ui.navigation.AdminTab
import com.example.wisatajogja.util.formatNumber
import com.example.wisatajogja.util.seniorGreeting
import com.example.wisatajogja.util.todayLabel
import kotlinx.coroutines.launch
import java.util.Locale

class RingkasanFragment : Fragment() {

    private var _binding: FragmentRingkasanBinding? = null
    private val binding get() = _binding!!
    private val repository = AdminRepository()
    private val popularAdapter = PopularDestinationAdapter()
    private val usersAdapter = RecentUsersAdapter()
    private val reservationsAdapter = RecentReservationsAdapter()
    private var loadedOnce = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRingkasanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerPopular.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPopular.adapter = popularAdapter
        binding.recyclerRecentUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecentUsers.adapter = usersAdapter
        binding.recyclerRecentReservations.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecentReservations.adapter = reservationsAdapter

        val user = SessionManager.currentUser
        binding.textAvatarInitial.text = user?.name?.firstOrNull()?.uppercase() ?: "A"
        binding.textGreeting.text = getString(R.string.admin_greeting, user?.name?.substringBefore(' ') ?: "Admin")
        binding.textDate.text = "${seniorGreeting().removeSuffix(",")}, ${todayLabel()}"
        binding.actionManageDestinations.setOnClickListener {
            (activity as? AdminMainActivity)?.openTab(AdminTab.DESTINATIONS)
        }
        binding.actionManageUsers.setOnClickListener {
            (activity as? AdminMainActivity)?.openTab(AdminTab.USERS)
        }
        binding.buttonRetry.setOnClickListener { loadSummary() }
        binding.swipeRefresh.setOnRefreshListener { loadSummary() }
        loadSummary()
    }

    private fun loadSummary() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorState.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.summary() }
                .onSuccess { summary ->
                    loadedOnce = true
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    val metrics = summary.metrics
                    binding.textVisitValue.text = formatNumber(metrics.visits30)
                    binding.textVisitDelta.text = String.format(Locale("id", "ID"), "%+.0f%%", metrics.visitsPercent)
                    binding.textUsersValue.text = formatNumber(metrics.totalUsers)
                    binding.textUsersDelta.text = "+${formatNumber(metrics.newUsers)} bulan ini"
                    binding.textDestinationsValue.text = formatNumber(metrics.publishedDestinations)
                    binding.textDestinationsDelta.text = "dari ${metrics.totalDestinations} · ${metrics.draftDestinations} draf"
                    binding.textSavedValue.text = formatNumber(metrics.totalSaved)
                    binding.textSavedDelta.text = "${formatNumber(metrics.totalReviews)} ulasan · ${formatNumber(metrics.totalReservations)} reservasi"
                    binding.chartView.setData(summary.chart)
                    binding.chartView.contentDescription = "Tren kunjungan ${summary.chart.sumOf { it.visits }} dalam 14 hari terakhir."
                    popularAdapter.submit(summary.popularDestinations)
                    usersAdapter.submit(summary.latestUsers)
                    reservationsAdapter.submit(summary.latestReservations)
                }
                .onFailure { error ->
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.errorState.visibility = if (!loadedOnce) View.VISIBLE else View.GONE
                    binding.textError.text = (error as? ApiException)?.message ?: getString(R.string.common_error_network)
                }
        }
    }

    override fun onResume() {
        super.onResume()
        if (loadedOnce && _binding != null) loadSummary()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
