package com.example.wisatajogja.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.AuthRepository
import com.example.wisatajogja.databinding.ActivityResetPasswordBinding
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.util.hideKeyboard
import com.example.wisatajogja.util.toast
import kotlinx.coroutines.launch

class ResetPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val repository = AuthRepository()
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        email = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        binding.inputCode.setText(intent.getStringExtra(EXTRA_CODE).orEmpty())
        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonReset.setOnClickListener { reset() }
    }

    private fun reset() {
        with(binding) {
        val code = inputCode.text?.toString()?.trim().orEmpty()
        val password = inputPassword.text?.toString().orEmpty()
        val confirm = inputConfirm.text?.toString().orEmpty()
        var valid = true
        if (code.length != 6) {
            inputCodeLayout.error = getString(R.string.common_required)
            valid = false
        } else inputCodeLayout.error = null
        if (password.length < 6) {
            inputPasswordLayout.error = getString(R.string.error_password_short)
            valid = false
        } else inputPasswordLayout.error = null
        if (password != confirm) {
            inputConfirmLayout.error = getString(R.string.register_password_mismatch)
            valid = false
        } else inputConfirmLayout.error = null
        if (!valid) return

        binding.root.hideKeyboard()
        buttonReset.isEnabled = false
        buttonReset.text = getString(R.string.common_loading)
        lifecycleScope.launch {
            runCatching { repository.resetPassword(email, code, password) }
                .onSuccess {
                    toast(getString(R.string.reset_success))
                    startActivity(
                        Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
                .onFailure { error ->
                    buttonReset.isEnabled = true
                    buttonReset.text = getString(R.string.reset_action)
                    toast((error as? ApiException)?.message ?: getString(R.string.error_generic))
                }
        }
        }
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_CODE = "extra_code"
    }
}
