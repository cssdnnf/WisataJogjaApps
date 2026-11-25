package com.example.wisatajogja.ui.main.tersimpan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.DestinationRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.FragmentTersimpanBinding
import com.example.wisatajogja.ui.auth.LoginActivity
import com.example.wisatajogja.ui.detail.DetailWisataActivity
import com.example.wisatajogja.ui.main.MainActivity
import com.example.wisatajogja.util.snack
import kotlinx.coroutines.launch

class TersimpanFragment : Fragment() {

    private var _binding: FragmentTersimpanBinding? = null
    private val binding get() = _binding!!
    private val repository = DestinationRepository()
    private val adapter = SavedAdapter(::openDetail, ::removeSaved)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTersimpanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerSaved.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSaved.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { loadSaved() }
        binding.buttonEmptyAction.setOnClickListener {
            if (SessionManager.isGuest) {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
            } else {
                (activity as? MainActivity)?.openExplore()
            }
        }
        loadSaved()
    }

    private fun loadSaved() {
        if (!SessionManager.isLoggedIn) {
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            showGuestState()
            return
        }
        binding.progressBar.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        lifecycleScope.launch {
            runCatching { repository.savedDestinations() }
                .onSuccess { items ->
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    adapter.submitList(items)
                    binding.textSubtitle.text = if (items.isEmpty()) {
                        getString(R.string.saved_subtitle)
                    } else {
                        "${items.size} tempat tersimpan untuk perjalanan berikutnya."
                    }
                    showEmptyState(items.isEmpty())
                }
                .onFailure { error ->
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    showEmptyState(adapter.itemCount == 0)
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.common_error_network))
                }
        }
    }

    private fun showGuestState() {
        adapter.submitList(emptyList())
        binding.emptyState.visibility = View.VISIBLE
        binding.textEmptyTitle.setText(R.string.saved_guest_title)
        binding.textEmptyBody.setText(R.string.saved_guest_body)
        binding.buttonEmptyAction.setText(R.string.saved_login_action)
        binding.textSubtitle.setText(R.string.saved_guest_body)
    }

    private fun showEmptyState(empty: Boolean) {
        binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        if (empty) {
            binding.textEmptyTitle.setText(R.string.saved_empty_title)
            binding.textEmptyBody.setText(R.string.saved_empty_body)
            binding.buttonEmptyAction.setText(R.string.saved_explore_action)
        }
    }

    private fun removeSaved(item: Destination) {
        lifecycleScope.launch {
            runCatching { repository.toggleFavorite(item.id) }
                .onSuccess {
                    adapter.submitList(adapter.currentList.filterNot { it.id == item.id })
                    binding.textSubtitle.text = "${adapter.itemCount} tempat tersimpan untuk perjalanan berikutnya."
                    if (adapter.itemCount == 0) showEmptyState(true)
                    binding.root.snack(getString(R.string.saved_removed))
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
        if (_binding != null) loadSaved()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
