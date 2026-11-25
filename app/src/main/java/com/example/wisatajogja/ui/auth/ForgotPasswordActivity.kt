package com.example.wisatajogja.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.AuthRepository
import com.example.wisatajogja.databinding.ActivityForgotPasswordBinding
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.util.hideKeyboard
import com.example.wisatajogja.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ForgotPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val repository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        binding.inputEmail.setText(intent.getStringExtra(EXTRA_EMAIL).orEmpty())
        binding.buttonBack.setOnClickListener { finish() }
        binding.actionLogin.setOnClickListener { finish() }
        binding.buttonSend.setOnClickListener { sendCode() }
    }

    private fun sendCode() {
        with(binding) {
        val email = inputEmail.text?.toString()?.trim().orEmpty()
        if (!email.contains("@") || email.length < 6) {
            inputEmailLayout.error = getString(R.string.error_email_invalid)
            return
        }
        inputEmailLayout.error = null
        binding.root.hideKeyboard()
        buttonSend.isEnabled = false
        buttonSend.text = getString(R.string.common_loading)
        lifecycleScope.launch {
            runCatching { repository.forgotPassword(email) }
                .onSuccess { result ->
                    buttonSend.isEnabled = true
                    buttonSend.text = getString(R.string.forgot_action)
                    val code = result.resetCode
                    if (code == null) {
                        MaterialAlertDialogBuilder(this@ForgotPasswordActivity)
                            .setTitle(R.string.forgot_success)
                            .setMessage("Jika email terdaftar, kode pemulihan telah dibuat. Periksa email atau coba email lain.")
                            .setPositiveButton(R.string.common_close, null)
                            .show()
                    } else {
                        MaterialAlertDialogBuilder(this@ForgotPasswordActivity)
                            .setTitle(R.string.forgot_code_title)
                            .setMessage(getString(R.string.forgot_code_message, code))
                            .setPositiveButton(R.string.forgot_continue) { _, _ ->
                                startActivity(
                                    Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                                        .putExtra(ResetPasswordActivity.EXTRA_EMAIL, email)
                                        .putExtra(ResetPasswordActivity.EXTRA_CODE, code)
                                )
                            }
                            .setNegativeButton(R.string.common_cancel, null)
                            .show()
                    }
                }
                .onFailure { error ->
                    buttonSend.isEnabled = true
                    buttonSend.text = getString(R.string.forgot_action)
                    toast((error as? ApiException)?.message ?: getString(R.string.error_generic))
                }
        }
        }
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }
}
