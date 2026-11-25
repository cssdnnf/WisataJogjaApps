package com.example.wisatajogja.ui.main.beranda

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.DestinationRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.data.weather.WeatherRepository
import com.example.wisatajogja.databinding.FragmentBerandaBinding
import com.example.wisatajogja.ui.auth.LoginActivity
import com.example.wisatajogja.ui.detail.DetailWisataActivity
import com.example.wisatajogja.ui.main.MainActivity
import com.example.wisatajogja.ui.main.beranda.BerandaDestinationAdapter
import com.example.wisatajogja.ui.main.beranda.WideDestinationAdapter
import com.example.wisatajogja.util.formatTemperature
import com.example.wisatajogja.util.seniorGreeting
import com.example.wisatajogja.util.snack
import com.example.wisatajogja.util.todayLabel
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaBinding? = null
    private val binding get() = _binding!!

    private val destinationRepository = DestinationRepository()
    private val weatherRepository = WeatherRepository()

    private val featuredAdapter = BerandaDestinationAdapter(
        onClick = ::openDetail,
        onFavorite = ::toggleFavorite,
    )
    private val recommendationAdapter = WideDestinationAdapter(
        onClick = ::openDetail,
        onFavorite = ::toggleFavorite,
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBerandaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupLists()
        setupActions()
    }

    private fun setupHeader() = with(binding) {
        textGreeting.text = seniorGreeting().uppercase()
        textUserName.text = buildString {
            append(SessionManager.currentUser?.name?.substringBefore(' ') ?: getString(R.string.home_guest_name))
            append('.')
        }
        textDate.text = todayLabel()
    }

    private fun setupLists() = with(binding) {
        recyclerFeatured.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerFeatured.adapter = featuredAdapter
        recyclerRecommendations.layoutManager = LinearLayoutManager(requireContext())
        recyclerRecommendations.adapter = recommendationAdapter
    }

    private fun setupActions() = with(binding) {
        actionSeeAll.setOnClickListener { (activity as? MainActivity)?.openExplore() }
        buttonNotifications.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_notifications)
                .setMessage("Belum ada notifikasi baru. Reservasi dan pengingat perjalananmu akan tampil di sini.")
                .setPositiveButton(R.string.common_close, null)
                .show()
        }
        cardCommunity.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_community_title)
                .setMessage("Komunitas lokal membuka pengalaman yang tidak ditemukan di tur biasa: jeep trail warga Timang, pemandu sejarah Taman Sari, hingga Pokdarwis pantai. Setiap layanan komunitas dapat ditemukan di halaman detail destinasi.")
                .setPositiveButton(R.string.home_community_action) { _, _ ->
                    (activity as? MainActivity)?.openExplore()
                }
                .setNegativeButton(R.string.common_close, null)
                .show()
        }
        swipeRefresh.setOnRefreshListener {
            loadData(forceWeather = true)
        }
    }

    private fun loadData(forceWeather: Boolean) {
        binding.progressBar.visibility = View.VISIBLE
        loadingWeather(forceWeather)
        lifecycleScope.launch {
            runCatching {
                coroutineScope {
                    val featured = async { destinationRepository.explore(featuredOnly = true, limit = 6) }
                    val all = async { destinationRepository.explore(limit = 8) }
                    val areas = async { destinationRepository.areas() }
                    Triple(featured.await(), all.await(), areas.await())
                }
            }.onSuccess { (featured, all, areas) ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                featuredAdapter.submitList(featured)
                recommendationAdapter.submitList(all.take(4))
                binding.emptyState.visibility = if (featured.isEmpty() && all.isEmpty()) View.VISIBLE else View.GONE
                buildAreaChips(areas.map { it.first })
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.root.snack((error as? ApiException)?.message ?: getString(R.string.common_error_network))
                buildAreaChips(emptyList())
            }
        }
    }

    private fun loadingWeather(force: Boolean) {
        lifecycleScope.launch {
            runCatching { weatherRepository.load(force) }
                .onSuccess { weather ->
                    binding.textTemperature.text = formatTemperature(weather.temperature)
                    binding.textWeatherDescription.text = weather.description
                    binding.textWeatherSource.text = getString(R.string.home_weather_source, weather.source, weather.updatedAt)
                    binding.textWind.text = "${String.format(java.util.Locale("id", "ID"), "%.1f", weather.windSpeed)} km/j"
                    binding.textHumidity.text = "${weather.humidity}%"
                    binding.textUv.text = String.format(java.util.Locale("id", "ID"), "%.1f", weather.uvIndex)
                    val lower = weather.description.lowercase()
                    binding.iconWeather.setImageResource(
                        if (lower.contains("hujan") || lower.contains("berawan") || lower.contains("kabut") || lower.contains("gerimis")) {
                            R.drawable.ic_cloud
                        } else {
                            R.drawable.ic_wb_sunny
                        }
                    )
                }
                .onFailure {
                    binding.textTemperature.text = "—"
                    binding.textWeatherDescription.text = getString(R.string.home_weather_unavailable)
                    binding.textWeatherSource.text = getString(R.string.home_weather_error)
                }
        }
    }

    private fun buildAreaChips(areas: List<String>) {
        val chipGroup = binding.chipGroupAreas
        chipGroup.removeAllViews()
        val safeAreas = areas.ifEmpty { listOf("Bantul", "Gunungkidul", "Sleman", "Kota Yogyakarta", "Kulon Progo") }
        safeAreas.forEach { area ->
            val chip = Chip(requireContext()).apply {
                text = area
                isCheckable = false
                isClickable = true
                setTextAppearance(R.style.TextAppearance_WisataJogja_Caption)
                setTextColor(ContextCompat.getColor(context, R.color.green_dark))
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.cream_light)
                )
                chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.line)
                )
                chipStrokeWidth = resources.displayMetrics.density
                chipCornerRadius = 22f * resources.displayMetrics.density
                setOnClickListener { (activity as? MainActivity)?.openExplore(area = area) }
            }
            chipGroup.addView(chip)
        }
    }

    private fun toggleFavorite(item: Destination) {
        if (!SessionManager.isLoggedIn) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            return
        }
        lifecycleScope.launch {
            runCatching { destinationRepository.toggleFavorite(item.id) }
                .onSuccess { favorite ->
                    featuredAdapter.submitList(
                        featuredAdapter.currentList.map { if (it.id == item.id) it.copy(isFavorite = favorite) else it }
                    )
                    recommendationAdapter.submitList(
                        recommendationAdapter.currentList.map { if (it.id == item.id) it.copy(isFavorite = favorite) else it }
                    )
                    binding.root.snack(getString(if (favorite) R.string.detail_favorite_on else R.string.saved_removed))
                }
                .onFailure {
                    binding.root.snack((it as? ApiException)?.message ?: getString(R.string.error_generic))
                }
        }
    }

    private fun openDetail(item: Destination) {
        startActivity(
            Intent(requireContext(), DetailWisataActivity::class.java)
                .putExtra(DetailWisataActivity.EXTRA_DESTINATION_ID, item.id)
        )
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            setupHeader()
            loadData(forceWeather = false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
