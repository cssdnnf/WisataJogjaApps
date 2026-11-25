package com.example.wisatajogja.ui.main.rekomendasi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.DestinationRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.FragmentRekomendasiBinding
import com.example.wisatajogja.ui.auth.LoginActivity
import com.example.wisatajogja.ui.detail.DetailWisataActivity
import com.example.wisatajogja.util.snack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RekomendasiFragment : Fragment() {

    private var _binding: FragmentRekomendasiBinding? = null
    private val binding get() = _binding!!

    private val repository = DestinationRepository()
    private val adapter = ExploreAdapter(::openDetail, ::toggleFavorite)
    private var searchJob: Job? = null
    private var firstLoad = true
    private var areaFilter: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRekomendasiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        areaFilter = arguments?.getString(ARG_AREA)
        binding.recyclerDestinations.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerDestinations.adapter = adapter

        val initialCategory = arguments?.getString(ARG_CATEGORY)
        selectCategory(initialCategory)
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            loadDestinations()
        }
        binding.inputSearch.doAfterTextChanged {
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(350)
                loadDestinations()
            }
        }
        binding.swipeRefresh.setOnRefreshListener { loadDestinations() }
        loadDestinations()
    }

    private fun selectCategory(category: String?) {
        val chipId = when (category) {
            "Pantai" -> R.id.chipBeach
            "Budaya" -> R.id.chipCulture
            "Alam" -> R.id.chipNature
            else -> R.id.chipAll
        }
        binding.chipGroupCategory.check(chipId)
    }

    private fun selectedCategory(): String? = when (binding.chipGroupCategory.checkedChipId) {
        R.id.chipBeach -> "Pantai"
        R.id.chipCulture -> "Budaya"
        R.id.chipNature -> "Alam"
        else -> null
    }

    private fun loadDestinations() {
        val category = selectedCategory()
        val query = binding.inputSearch.text?.toString()?.trim().orEmpty()
        binding.progressBar.visibility = if (firstLoad) View.VISIBLE else View.GONE
        lifecycleScope.launch {
            runCatching { repository.explore(category, query, areaFilter, limit = 30) }
                .onSuccess { items ->
                    firstLoad = false
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    adapter.submitList(items)
                    binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    binding.textSubtitle.text = buildString {
                        append(getString(R.string.explore_subtitle, items.size))
                        areaFilter?.let { append(" · Area $it") }
                    }
                }
                .onFailure { error ->
                    firstLoad = false
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.emptyState.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.common_error_network))
                }
        }
    }

    private fun toggleFavorite(item: Destination) {
        if (!SessionManager.isLoggedIn) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            return
        }
        lifecycleScope.launch {
            runCatching { repository.toggleFavorite(item.id) }
                .onSuccess { favorite ->
                    adapter.submitList(
                        adapter.currentList.map { if (it.id == item.id) it.copy(isFavorite = favorite) else it }
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
        if (!firstLoad && _binding != null) loadDestinations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_AREA = "arg_area"

        fun newInstance(category: String? = null, area: String? = null): RekomendasiFragment =
            RekomendasiFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category)
                    putString(ARG_AREA, area)
                }
            }
    }
}
