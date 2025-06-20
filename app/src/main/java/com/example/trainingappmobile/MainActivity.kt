package com.example.trainingappmobile

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREF_NAME = "app_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_SAVED_PASSWORD = "saved_password"
    }

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: LinearLayout
    private lateinit var rememberMeCheckbox: CheckBox
    private lateinit var passwordToggle: ImageView
    private var authToken: String? = null
    private var deviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initSharedPreferences()
        setupListeners()

        authToken = sharedPrefs.getString(KEY_AUTH_TOKEN, null)
        deviceId = sharedPrefs.getString(KEY_DEVICE_ID, null)

        if (authToken != null && deviceId != null) {
            navigateToHome()
        } else {
            loadSavedCredentials()
            showLoginState()
        }
    }

    private fun initViews() {
        emailEditText = findViewById(R.id.email_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        loginButton = findViewById(R.id.login_button)
        rememberMeCheckbox = findViewById(R.id.remember_me_checkbox)
        passwordToggle = findViewById(R.id.password_toggle)
    }

    private fun initSharedPreferences() {
        sharedPrefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInput(email, password)) {
                val deviceId = getSecureDeviceId()
                saveCredentialsIfRemembered(email, password)
                login(email, password, deviceId)
            }
        }

        passwordToggle.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        when {
            email.isEmpty() -> {
                emailEditText.error = "Email é obrigatório"
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailEditText.error = "Email inválido"
                return false
            }
            password.isEmpty() -> {
                passwordEditText.error = "Senha é obrigatória"
                return false
            }
            password.length < 6 -> {
                passwordEditText.error = "Senha deve ter pelo menos 6 caracteres"
                return false
            }
            else -> return true
        }
    }

    private fun getSecureDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_device"
    }

    private fun login(email: String, password: String, deviceId: String) {
        loginButton.isEnabled = false

        val call = RetrofitClient.apiService.login(email, password, deviceId)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                loginButton.isEnabled = true

                if (response.isSuccessful) {
                    handleLoginSuccess(response.body(), deviceId)
                } else {
                    handleLoginError(response.code(), response.message())
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                loginButton.isEnabled = true
                Log.e(TAG, "Falha no login", t)
                showToast("Falha na conexão. Verifique sua internet.")
            }
        })
    }

    private fun handleLoginSuccess(body: LoginResponse?, deviceId: String) {
        if (body?.apiKey != null) {
            this.authToken = body.apiKey
            this.deviceId = deviceId

            saveCredentials(body.apiKey, deviceId)
            navigateToHome()
        } else {
            Log.e(TAG, "Resposta de login inválida: ${body?.error}")
            showToast(body?.error ?: "Erro interno. Tente novamente.")
        }
    }

    private fun handleLoginError(code: Int, message: String) {
        Log.e(TAG, "Erro no login: $code - $message")
        when (code) {
            401 -> showToast("Email ou senha incorretos")
            403 -> showToast("Conta bloqueada. Contate o suporte")
            429 -> showToast("Muitas tentativas. Tente mais tarde")
            else -> showToast("Erro no servidor. Tente novamente")
        }
    }

    private fun saveCredentials(authToken: String, deviceId: String) {
        sharedPrefs.edit()
            .putString(KEY_AUTH_TOKEN, authToken)
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()
    }

    private fun saveCredentialsIfRemembered(email: String, password: String) {
        if (rememberMeCheckbox.isChecked) {
            sharedPrefs.edit()
                .putBoolean(KEY_REMEMBER_ME, true)
                .putString(KEY_SAVED_EMAIL, email)
                .putString(KEY_SAVED_PASSWORD, password)
                .apply()
        } else {
            sharedPrefs.edit()
                .putBoolean(KEY_REMEMBER_ME, false)
                .remove(KEY_SAVED_EMAIL)
                .remove(KEY_SAVED_PASSWORD)
                .apply()
        }
    }

    private fun loadSavedCredentials() {
        val rememberMe = sharedPrefs.getBoolean(KEY_REMEMBER_ME, false)
        rememberMeCheckbox.isChecked = rememberMe
        if (rememberMe) {
            val savedEmail = sharedPrefs.getString(KEY_SAVED_EMAIL, "")
            val savedPassword = sharedPrefs.getString(KEY_SAVED_PASSWORD, "")
            emailEditText.setText(savedEmail)
            passwordEditText.setText(savedPassword)
        }
    }

    private fun clearCredentials() {
        sharedPrefs.edit().clear().apply()
        authToken = null
        deviceId = null
        clearInputFields()
    }

    private fun clearInputFields() {
        emailEditText.text.clear()
        passwordEditText.text.clear()
        emailEditText.error = null
        passwordEditText.error = null
    }

    private fun showLoginState() {
        loginButton.visibility = View.VISIBLE
        emailEditText.visibility = View.VISIBLE
        passwordEditText.visibility = View.VISIBLE
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeScreen::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun togglePasswordVisibility() {
        val isPasswordVisible = passwordEditText.inputType == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        if (isPasswordVisible) {
            passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordToggle.setImageResource(R.drawable.ic_eye) // Replace with closed eye drawable
        } else {
            passwordEditText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordToggle.setImageResource(R.drawable.ic_eye_open) // Replace with open eye drawable
        }
        passwordEditText.setSelection(passwordEditText.text.length) // Move cursor to end
    }
}