package com.example.wisatajogja.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.ProfileRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.ActivityEditProfilBinding
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.ui.common.CropImageActivity
import com.example.wisatajogja.ui.common.PhotoPickerDelegate
import com.example.wisatajogja.util.hideKeyboard
import com.example.wisatajogja.util.loadAvatar
import com.example.wisatajogja.util.snack
import com.example.wisatajogja.util.toast
import kotlinx.coroutines.launch
import java.io.File

class EditProfilActivity : BaseActivity() {

    private lateinit var binding: ActivityEditProfilBinding
    private lateinit var photoPicker: PhotoPickerDelegate
    private lateinit var cropLauncher: ActivityResultLauncher<Intent>
    private val repository = ProfileRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isLoggedIn) {
            finish()
            return
        }
        binding = ActivityEditProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val path = result.data?.getStringExtra(CropImageActivity.EXTRA_OUTPUT_PATH)
                if (!path.isNullOrBlank()) uploadPhotoFile(File(path))
            }
        }
        photoPicker = PhotoPickerDelegate(
            caller = this,
            context = this,
            onPicked = ::openCropEditor,
            onError = { message -> binding.root.snack(message) },
        )
        bindProfile()
        binding.buttonBack.setOnClickListener { finish() }
        binding.cardAvatar.setOnClickListener { photoPicker.showSourceDialog() }
        binding.badgeCamera.setOnClickListener { photoPicker.showSourceDialog() }
        binding.buttonSave.setOnClickListener { saveProfile() }
    }

    private fun bindProfile() = with(binding) {
        val user = SessionManager.currentUser ?: return@with
        inputName.setText(user.name)
        inputUsername.setText(user.username)
        inputEmail.setText(user.email)
        inputPhone.setText(user.phone)
        inputCity.setText(user.city)
        inputBio.setText(user.bio)
        if (user.photoUrl.isNullOrBlank()) {
            imageAvatar.setPadding(28, 28, 28, 28)
            imageAvatar.setImageResource(R.drawable.ic_person)
            imageAvatar.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this@EditProfilActivity, R.color.cream_light)
            )
        } else {
            imageAvatar.setPadding(0, 0, 0, 0)
            imageAvatar.imageTintList = null
            imageAvatar.loadAvatar(user.photoUrl)
        }
    }

    private fun openCropEditor(uri: Uri) {
        val intent = Intent(this, CropImageActivity::class.java)
            .putExtra(CropImageActivity.EXTRA_SOURCE_URI, uri.toString())
            .putExtra(CropImageActivity.EXTRA_ASPECT_RATIO, 1f)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        cropLauncher.launch(intent)
    }

    private fun uploadPhotoFile(file: File) {
        binding.root.snack(getString(R.string.profile_photo_uploading))
        lifecycleScope.launch {
            runCatching { repository.uploadPhoto(file) }
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

    private fun saveProfile() {
        with(binding) {
        val name = inputName.text?.toString()?.trim().orEmpty()
        val username = inputUsername.text?.toString()?.trim().orEmpty()
        var valid = true
        if (name.length < 3) {
            inputNameLayout.error = getString(R.string.error_name_short)
            valid = false
        } else inputNameLayout.error = null
        if (!username.matches(Regex("^[a-z0-9._]{3,30}$"))) {
            inputUsernameLayout.error = "Username 3-30 karakter tanpa spasi"
            valid = false
        } else inputUsernameLayout.error = null
        if (!valid) return

        binding.root.hideKeyboard()
        buttonSave.isEnabled = false
        buttonSave.text = getString(R.string.common_loading)
        lifecycleScope.launch {
            runCatching {
                repository.updateProfile(
                    name = name,
                    username = username,
                    phone = inputPhone.text?.toString().orEmpty(),
                    city = inputCity.text?.toString().orEmpty(),
                    bio = inputBio.text?.toString().orEmpty(),
                )
            }.onSuccess {
                toast(getString(R.string.edit_profile_success))
                finish()
            }.onFailure { error ->
                buttonSave.isEnabled = true
                buttonSave.text = getString(R.string.edit_profile_save)
                inputUsernameLayout.error = (error as? ApiException)?.message
                if (inputUsernameLayout.error.isNullOrBlank()) {
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
                }
            }
        }
        }
    }
}
