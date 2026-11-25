package com.example.wisatajogja.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.AuthRepository
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.data.weather.WeatherRepository
import com.example.wisatajogja.databinding.ActivityLoginBinding
import com.example.wisatajogja.ui.admin.AdminMainActivity
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.ui.main.MainActivity
import com.example.wisatajogja.util.hideKeyboard
import com.example.wisatajogja.util.loadPhoto
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()
    private val weatherRepository = WeatherRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager.isLoggedIn) {
            openHome(SessionManager.isAdmin)
            return
        }
        if (SessionManager.isGuest && intent.action == Intent.ACTION_MAIN) {
            openHome(false)
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        binding.imageLoginHero.loadPhoto(HERO_URL)
        bindActions()
        loadWeather()
    }

    private fun bindActions() = with(binding) {
        buttonLogin.setOnClickListener { submitLogin() }
        actionForgotPassword.setOnClickListener {
            startActivity(
                Intent(this@LoginActivity, ForgotPasswordActivity::class.java)
                    .putExtra(ForgotPasswordActivity.EXTRA_EMAIL, inputEmail.text?.toString().orEmpty())
            )
        }
        actionRegister.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }
        buttonGuest.setOnClickListener {
            SessionManager.enterGuestMode()
            openHome(false)
        }
        inputPassword.setOnEditorActionListener { _, _, _ ->
            submitLogin()
            true
        }
    }

    private fun submitLogin() {
        with(binding) {
        val email = inputEmail.text?.toString()?.trim().orEmpty()
        val password = inputPassword.text?.toString().orEmpty()
        var valid = true
        if (!email.contains("@") || email.length < 6) {
            inputEmailLayout.error = getString(R.string.error_email_invalid)
            valid = false
        } else {
            inputEmailLayout.error = null
        }
        if (password.length < 6) {
            inputPasswordLayout.error = getString(R.string.error_password_short)
            valid = false
        } else {
            inputPasswordLayout.error = null
        }
        if (!valid) return

        binding.root.hideKeyboard()
        buttonLogin.isEnabled = false
        buttonLogin.text = getString(R.string.common_loading)
        lifecycleScope.launch {
            runCatching { authRepository.login(email, password) }
                .onSuccess { result ->
                    SessionManager.saveSession(result.token, result.user)
                    openHome(result.user.isAdmin)
                }
                .onFailure { error ->
                    buttonLogin.isEnabled = true
                    buttonLogin.text = getString(R.string.login_action)
                    inputPasswordLayout.error = error.userMessage()
                }
        }
        }
    }

    private fun loadWeather() {
        lifecycleScope.launch {
            runCatching { weatherRepository.load() }
                .onSuccess { weather ->
                    binding.textHeroTemperature.text = "${weather.temperature}°"
                    binding.textHeroWeather.text = weather.description
                }
                .onFailure {
                    binding.textHeroTemperature.text = "—"
                    binding.textHeroWeather.text = getString(R.string.home_weather_unavailable)
                }
        }
    }

    private fun openHome(admin: Boolean) {
        val target = if (admin) AdminMainActivity::class.java else MainActivity::class.java
        startActivity(
            Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun Throwable.userMessage(): String =
        (this as? ApiException)?.message ?: getString(R.string.error_generic)

    companion object {
        private const val HERO_URL = "https://45.127.35.252/wisatajogja/assets/img/destinasi/hero-login.webp"
    }
}
