package com.example.wisatajogja.ui.admin.wisata

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.AdminRepository
import com.example.wisatajogja.databinding.FragmentKelolaWisataBinding
import com.example.wisatajogja.util.snack
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class KelolaWisataFragment : Fragment() {

    private var _binding: FragmentKelolaWisataBinding? = null
    private val binding get() = _binding!!

    private val repository = AdminRepository()
    private val adapter = AdminDestinationAdapter(::openForm, ::confirmDelete)
    private var allDestinations: List<Destination> = emptyList()
    private var loadedOnce = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentKelolaWisataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerDestinations.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDestinations.adapter = adapter
        binding.buttonAdd.setOnClickListener {
            startActivity(Intent(requireContext(), FormWisataActivity::class.java))
        }
        binding.inputSearch.doAfterTextChanged { applyFilters() }
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, _ -> applyFilters() }
        loadDestinations()
    }

    private fun loadDestinations() {
        binding.progressBar.visibility = if (!loadedOnce) View.VISIBLE else View.GONE
        lifecycleScope.launch {
            runCatching { repository.destinations() }
                .onSuccess { items ->
                    loadedOnce = true
                    binding.progressBar.visibility = View.GONE
                    allDestinations = items
                    applyFilters()
                }
                .onFailure { error ->
                    binding.progressBar.visibility = View.GONE
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.common_error_network))
                }
        }
    }

    private fun applyFilters() {
        if (_binding == null) return
        val query = binding.inputSearch.text?.toString()?.trim().orEmpty().lowercase()
        val status = when (binding.chipGroupStatus.checkedChipId) {
            R.id.chipPublished -> "terbit"
            R.id.chipDraft -> "draf"
            else -> null
        }
        val filtered = allDestinations.filter { destination ->
            (query.isEmpty() || destination.name.lowercase().contains(query) || destination.area.lowercase().contains(query)) &&
                (status == null || destination.status == status)
        }
        adapter.submit(filtered)
        binding.textEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        val published = allDestinations.count { it.status == "terbit" }
        val draft = allDestinations.count { it.status == "draf" }
        binding.textSubtitle.text = getString(R.string.manage_subtitle, allDestinations.size, published, draft)
    }

    private fun openForm(item: Destination) {
        startActivity(
            Intent(requireContext(), FormWisataActivity::class.java)
                .putExtra(FormWisataActivity.EXTRA_DESTINATION_ID, item.id)
        )
    }

    private fun confirmDelete(item: Destination) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.manage_delete_title)
            .setMessage(getString(R.string.manage_delete_message, item.name))
            .setPositiveButton(R.string.manage_delete_action) { _, _ ->
                deleteDestination(item)
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun deleteDestination(item: Destination) {
        lifecycleScope.launch {
            runCatching { repository.deleteDestination(item.id) }
                .onSuccess {
                    allDestinations = allDestinations.filterNot { it.id == item.id }
                    applyFilters()
                    binding.root.snack(getString(R.string.manage_deleted))
                }
                .onFailure { error ->
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
                }
        }
    }

    override fun onResume() {
        super.onResume()
        if (loadedOnce && _binding != null) loadDestinations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
