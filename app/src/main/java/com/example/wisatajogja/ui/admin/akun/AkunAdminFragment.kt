package com.example.wisatajogja.ui.admin.akun

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.repository.AuthRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.FragmentAkunAdminBinding
import com.example.wisatajogja.ui.auth.LoginActivity
import com.example.wisatajogja.ui.main.MainActivity
import com.example.wisatajogja.ui.profile.EditProfilActivity
import com.example.wisatajogja.ui.profile.UbahSandiActivity
import com.example.wisatajogja.util.loadAvatar
import com.example.wisatajogja.util.snack
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AkunAdminFragment : Fragment() {

    private var _binding: FragmentAkunAdminBinding? = null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAkunAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindProfile()
        binding.rowOpenTraveler.setOnClickListener {
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }
        binding.rowEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfilActivity::class.java))
        }
        binding.rowChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), UbahSandiActivity::class.java))
        }
        binding.rowHelp.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_account_help)
                .setMessage("Kelola katalog wisata melalui tab Wisata. Gunakan tombol tambah untuk membuat destinasi baru, atau ketuk destinasi untuk memperbarui data. Status draf tidak akan tampil di aplikasi wisatawan sampai diterbitkan.")
                .setPositiveButton(R.string.common_close, null)
                .show()
        }
        binding.cardAvatar.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfilActivity::class.java))
        }
        binding.buttonLogout.setOnClickListener { confirmLogout() }
        refreshUser()
    }

    private fun bindProfile() = with(binding) {
        val user = SessionManager.currentUser ?: return@with
        textName.text = user.name
        textEmail.text = user.email
        if (user.photoUrl.isNullOrBlank()) {
            textAvatarInitial.visibility = View.VISIBLE
            textAvatarInitial.text = user.name.firstOrNull()?.uppercase() ?: "A"
            imageAvatar.visibility = View.GONE
        } else {
            textAvatarInitial.visibility = View.GONE
            imageAvatar.visibility = View.VISIBLE
            imageAvatar.loadAvatar(user.photoUrl)
        }
    }

    private fun refreshUser() {
        lifecycleScope.launch {
            runCatching { authRepository.refreshCurrentUser() }
                .onSuccess {
                    SessionManager.updateUser(it)
                    bindProfile()
                }
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.common_logout_title)
            .setMessage(R.string.common_logout_message)
            .setPositiveButton(R.string.common_logout) { _, _ ->
                lifecycleScope.launch {
                    authRepository.logout()
                    startActivity(
                        Intent(requireContext(), LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    requireActivity().finish()
                }
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            bindProfile()
            refreshUser()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
