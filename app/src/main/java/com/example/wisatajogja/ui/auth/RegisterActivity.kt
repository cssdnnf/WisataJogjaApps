package com.example.wisatajogja.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.AuthRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.ActivityRegisterBinding
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.ui.main.MainActivity
import com.example.wisatajogja.util.hideKeyboard
import com.example.wisatajogja.util.toast
import kotlinx.coroutines.launch

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val repository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        binding.buttonBack.setOnClickListener { finish() }
        binding.actionLogin.setOnClickListener { finish() }
        binding.buttonRegister.setOnClickListener { submit() }
    }

    private fun submit() {
        with(binding) {
        val name = inputName.text?.toString()?.trim().orEmpty()
        val email = inputEmail.text?.toString()?.trim().orEmpty()
        val password = inputPassword.text?.toString().orEmpty()
        val confirm = inputConfirm.text?.toString().orEmpty()
        var valid = true
        if (name.length < 3) {
            inputNameLayout.error = getString(R.string.error_name_short)
            valid = false
        } else inputNameLayout.error = null
        if (!email.contains("@") || email.length < 6) {
            inputEmailLayout.error = getString(R.string.error_email_invalid)
            valid = false
        } else inputEmailLayout.error = null
        if (password.length < 6) {
            inputPasswordLayout.error = getString(R.string.error_password_short)
            valid = false
        } else inputPasswordLayout.error = null
        if (password != confirm) {
            inputConfirmLayout.error = getString(R.string.register_password_mismatch)
            valid = false
        } else inputConfirmLayout.error = null
        if (!checkTerms.isChecked) {
            toast(getString(R.string.register_terms_required))
            valid = false
        }
        if (!valid) return

        binding.root.hideKeyboard()
        buttonRegister.isEnabled = false
        buttonRegister.text = getString(R.string.common_loading)
        lifecycleScope.launch {
            runCatching { repository.register(name, email, password) }
                .onSuccess { result ->
                    SessionManager.saveSession(result.token, result.user)
                    toast(getString(R.string.register_success))
                    startActivity(
                        Intent(this@RegisterActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
                .onFailure { error ->
                    buttonRegister.isEnabled = true
                    buttonRegister.text = getString(R.string.register_action)
                    val message = (error as? ApiException)?.message ?: getString(R.string.error_generic)
                    toast(message)
                }
        }
        }
    }
}
