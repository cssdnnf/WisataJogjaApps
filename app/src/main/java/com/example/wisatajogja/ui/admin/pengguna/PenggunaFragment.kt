package com.example.wisatajogja.ui.admin.pengguna

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.AdminUser
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.AdminRepository
import com.example.wisatajogja.databinding.FragmentPenggunaBinding
import com.example.wisatajogja.util.formatNumber
import com.example.wisatajogja.util.snack
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PenggunaFragment : Fragment() {

    private var _binding: FragmentPenggunaBinding? = null
    private val binding get() = _binding!!

    private val repository = AdminRepository()
    private val adapter = AdminUserAdapter(::showActions)
    private var page = 1
    private var hasMore = true
    private var loading = false
    private var total = 0
    private var searchJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerUsers.layoutManager = layoutManager
        binding.recyclerUsers.adapter = adapter
        binding.inputSearch.doAfterTextChanged {
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(400)
                loadPage(reset = true)
            }
        }
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, _ -> loadPage(reset = true) }
        binding.recyclerUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && hasMore && !loading) {
                    val last = layoutManager.findLastVisibleItemPosition()
                    if (last >= adapter.itemCount - 4) loadPage(reset = false)
                }
            }
        })
        loadPage(reset = true)
    }

    private fun selectedStatus(): String = when (binding.chipGroupStatus.checkedChipId) {
        R.id.chipActive -> "aktif"
        R.id.chipInactive -> "nonaktif"
        else -> ""
    }

    private fun loadPage(reset: Boolean) {
        if (loading) return
        loading = true
        if (reset) {
            page = 1
            hasMore = true
            binding.progressBar.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
        val requestedPage = page
        val query = binding.inputSearch.text?.toString()?.trim().orEmpty()
        lifecycleScope.launch {
            runCatching { repository.users(query, selectedStatus(), requestedPage) }
                .onSuccess { result ->
                    loading = false
                    binding.progressBar.visibility = View.GONE
                    total = result.total
                    if (reset) adapter.submit(result.items) else adapter.append(result.items)
                    page = requestedPage + 1
                    hasMore = requestedPage * 20 < total
                    binding.textSubtitle.text = getString(R.string.users_subtitle, formatNumber(total))
                    binding.emptyState.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                }
                .onFailure { error ->
                    loading = false
                    binding.progressBar.visibility = View.GONE
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.common_error_network))
                }
        }
    }

    private fun showActions(user: AdminUser) {
        if (user.role == "admin") {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(user.name)
                .setMessage("Akun admin tidak dapat dinonaktifkan dari halaman ini. Gunakan menu akun admin untuk mengubah profil atau kata sandi.")
                .setPositiveButton(R.string.common_close, null)
                .show()
            return
        }
        val nextStatus = if (user.status == "aktif") "nonaktif" else "aktif"
        val actionLabel = if (nextStatus == "nonaktif") getString(R.string.users_deactivate) else getString(R.string.users_activate)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(user.name)
            .setMessage("${user.email}\n${formatNumber(user.savedCount)} tersimpan · ${formatNumber(user.reservationCount)} reservasi")
            .setPositiveButton(actionLabel) { _, _ -> updateStatus(user, nextStatus) }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun updateStatus(user: AdminUser, status: String) {
        lifecycleScope.launch {
            runCatching { repository.updateUserStatus(user.id, status) }
                .onSuccess { message ->
                    binding.root.snack(message)
                    loadPage(reset = true)
                }
                .onFailure { error ->
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
