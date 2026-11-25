package com.example.wisatajogja.ui.profile

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.ProfileRepository
import com.example.wisatajogja.databinding.ActivityUbahSandiBinding
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.util.hideKeyboard
import com.example.wisatajogja.util.snack
import com.example.wisatajogja.util.toast
import kotlinx.coroutines.launch

class UbahSandiActivity : BaseActivity() {

    private lateinit var binding: ActivityUbahSandiBinding
    private val repository = ProfileRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUbahSandiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonSave.setOnClickListener { save() }
    }

    private fun save() {
        with(binding) {
        val current = inputCurrent.text?.toString().orEmpty()
        val newPassword = inputNew.text?.toString().orEmpty()
        val confirm = inputConfirm.text?.toString().orEmpty()
        var valid = true
        if (current.length < 6) {
            inputCurrentLayout.error = getString(R.string.error_password_short)
            valid = false
        } else inputCurrentLayout.error = null
        if (newPassword.length < 6) {
            inputNewLayout.error = getString(R.string.error_password_short)
            valid = false
        } else inputNewLayout.error = null
        if (newPassword != confirm) {
            inputConfirmLayout.error = getString(R.string.register_password_mismatch)
            valid = false
        } else inputConfirmLayout.error = null
        if (!valid) return

        binding.root.hideKeyboard()
        buttonSave.isEnabled = false
        buttonSave.text = getString(R.string.common_loading)
        lifecycleScope.launch {
            runCatching { repository.changePassword(current, newPassword) }
                .onSuccess {
                    toast(getString(R.string.password_success))
                    finish()
                }
                .onFailure { error ->
                    buttonSave.isEnabled = true
                    buttonSave.text = getString(R.string.password_action)
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
                }
        }
        }
    }
}
