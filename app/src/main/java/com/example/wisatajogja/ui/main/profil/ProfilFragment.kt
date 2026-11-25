package com.example.wisatajogja.ui.main.profil

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.User
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.AuthRepository
import com.example.wisatajogja.data.repository.ProfileRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.FragmentProfilBinding
import com.example.wisatajogja.ui.admin.AdminMainActivity
import com.example.wisatajogja.ui.auth.LoginActivity
import com.example.wisatajogja.ui.common.CropImageActivity
import com.example.wisatajogja.ui.common.PhotoPickerDelegate
import com.example.wisatajogja.ui.profile.EditProfilActivity
import com.example.wisatajogja.ui.profile.UbahSandiActivity
import com.example.wisatajogja.ui.reservasi.ReservasiSayaActivity
import com.example.wisatajogja.util.formatNumber
import com.example.wisatajogja.util.loadAvatar
import com.example.wisatajogja.util.snack
import com.example.wisatajogja.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    private val profileRepository = ProfileRepository()
    private val authRepository = AuthRepository()
    private lateinit var photoPicker: PhotoPickerDelegate

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(CropImageActivity.EXTRA_OUTPUT_PATH)
            if (!path.isNullOrBlank() && _binding != null) uploadPhotoFile(File(path))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoPicker = PhotoPickerDelegate(
            caller = this,
            context = requireContext(),
            onPicked = ::openCropEditor,
            onError = { message -> binding.root.snack(message) },
        )
        bindProfile()
        bindActions()
        refreshUser()
    }

    private fun bindProfile() = with(binding) {
        val user = SessionManager.currentUser
        val guest = !SessionManager.isLoggedIn
        if (guest) {
            textName.text = getString(R.string.profile_guest_name)
            textEmail.text = getString(R.string.profile_guest_email)
            textVisited.text = "0"
            textSaved.text = "0"
            textReviews.text = "0"
            imageAvatar.setImageResource(R.drawable.ic_person)
            imageAvatar.imageTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.cream_light)
            )
            imageAvatar.setPadding(28, 28, 28, 28)
            badgeCamera.visibility = View.GONE
            buttonEditProfile.visibility = View.GONE
            buttonLoginGuest.visibility = View.VISIBLE
            cardAccountMenu.visibility = View.GONE
            textAccountLabel.visibility = View.GONE
            buttonLogout.visibility = View.GONE
            rowAdminPanel.visibility = View.GONE
        } else {
            val safeUser = user ?: return@with
            textName.text = safeUser.name
            textEmail.text = safeUser.email
            textVisited.text = formatNumber(safeUser.stats.visited)
            textSaved.text = formatNumber(safeUser.stats.saved)
            textReviews.text = formatNumber(safeUser.stats.reviews)
            if (safeUser.photoUrl.isNullOrBlank()) {
                imageAvatar.setPadding(28, 28, 28, 28)
                imageAvatar.setImageResource(R.drawable.ic_person)
                imageAvatar.imageTintList = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.cream_light)
                )
            } else {
                imageAvatar.setPadding(0, 0, 0, 0)
                imageAvatar.imageTintList = null
                imageAvatar.loadAvatar(safeUser.photoUrl)
            }
            badgeCamera.visibility = View.VISIBLE
            buttonEditProfile.visibility = View.VISIBLE
            buttonLoginGuest.visibility = View.GONE
            cardAccountMenu.visibility = View.VISIBLE
            textAccountLabel.visibility = View.VISIBLE
            buttonLogout.visibility = View.VISIBLE
            rowAdminPanel.visibility = if (safeUser.isAdmin) View.VISIBLE else View.GONE
        }

        switchNotifications.setOnCheckedChangeListener(null)
        switchNotifications.isChecked = SessionManager.notificationsEnabled
        switchNotifications.setOnCheckedChangeListener { _, checked ->
            SessionManager.notificationsEnabled = checked
            val message = if (checked) R.string.profile_notifications_on else R.string.profile_notifications_off
            binding.root.snack(getString(message))
        }
    }

    private fun bindActions() = with(binding) {
        val requireLogin = {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        cardAvatar.setOnClickListener {
            if (SessionManager.isLoggedIn) photoPicker.showSourceDialog() else requireLogin()
        }
        badgeCamera.setOnClickListener {
            if (SessionManager.isLoggedIn) photoPicker.showSourceDialog() else requireLogin()
        }
        buttonLoginGuest.setOnClickListener { requireLogin() }
        buttonEditProfile.setOnClickListener { openEditProfile() }
        rowEditProfile.setOnClickListener { openEditProfile() }
        rowChangePassword.setOnClickListener {
            if (SessionManager.isLoggedIn) {
                startActivity(Intent(requireContext(), UbahSandiActivity::class.java))
            } else requireLogin()
        }
        rowReservations.setOnClickListener {
            if (SessionManager.isLoggedIn) {
                startActivity(Intent(requireContext(), ReservasiSayaActivity::class.java))
            } else requireLogin()
        }
        rowHelp.setOnClickListener { showHelp() }
        rowPrivacy.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_privacy)
                .setMessage(R.string.profile_privacy_message)
                .setPositiveButton(R.string.common_close, null)
                .show()
        }
        rowAdminPanel.setOnClickListener {
            startActivity(Intent(requireContext(), AdminMainActivity::class.java))
        }
        buttonLogout.setOnClickListener { confirmLogout() }
    }

    private fun openCropEditor(uri: android.net.Uri) {
        val intent = Intent(requireContext(), CropImageActivity::class.java)
            .putExtra(CropImageActivity.EXTRA_SOURCE_URI, uri.toString())
            .putExtra(CropImageActivity.EXTRA_ASPECT_RATIO, 1f)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        cropLauncher.launch(intent)
    }

    private fun uploadPhotoFile(file: File) {
        binding.root.snack(getString(R.string.profile_photo_uploading))
        lifecycleScope.launch {
            runCatching { profileRepository.uploadPhoto(file) }
                .onSuccess {
                    file.delete()
                    bindProfile()
                    toast(getString(R.string.profile_photo_success))
                }
                .onFailure { error ->
                    file.delete()
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
                }
        }
    }

    private fun refreshUser() {
        if (!SessionManager.isLoggedIn) return
        lifecycleScope.launch {
            runCatching { authRepository.refreshCurrentUser() }
                .onSuccess { user ->
                    SessionManager.updateUser(user)
                    bindProfile()
                }
        }
    }

    private fun openEditProfile() {
        if (!SessionManager.isLoggedIn) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        } else {
            startActivity(Intent(requireContext(), EditProfilActivity::class.java))
        }
    }

    private fun showHelp() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_help)
            .setMessage(R.string.profile_help_message)
            .setPositiveButton("Kirim email") { _, _ ->
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:halo@wisatajogja.id"))
                try {
                    startActivity(intent)
                } catch (error: ActivityNotFoundException) {
                    binding.root.snack("Tidak ada aplikasi email di perangkat ini.")
                }
            }
            .setNegativeButton(R.string.common_close, null)
            .show()
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
