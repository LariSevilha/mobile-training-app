package com.example.trainingappmobile

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.EditText
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
        private const val PREF_NAME = "auth"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEVICE_ID = "device_id"
    }

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: LinearLayout
    private lateinit var logoutButton: LinearLayout
    private var apiKey: String? = null
    private var deviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initSharedPreferences()

        apiKey = sharedPrefs.getString(KEY_API_KEY, null)
        deviceId = sharedPrefs.getString(KEY_DEVICE_ID, null)

        if (apiKey != null && deviceId != null) {
            showLoggedInState()
            navigateToHome()
        } else {
            showLoginState()
        }
    }

    private fun initViews() {
        emailEditText = findViewById(R.id.email_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        loginButton = findViewById(R.id.login_button)
        logoutButton = findViewById(R.id.logout_button)
    }

    private fun initSharedPreferences() {
        sharedPrefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
    }

    private fun setupLoginButton() {
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInput(email, password)) {
                val deviceId = getSecureDeviceId()
                login(email, password, deviceId)
            }
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
            this.apiKey = body.apiKey
            this.deviceId = deviceId

            saveCredentials(body.apiKey, deviceId)
            showLoggedInState()
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

    private fun saveCredentials(apiKey: String, deviceId: String) {
        sharedPrefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()
    }

    private fun logout() {
        val apiKey = this.apiKey ?: sharedPrefs.getString(KEY_API_KEY, null)
        val deviceId = this.deviceId ?: sharedPrefs.getString(KEY_DEVICE_ID, null)

        if (apiKey != null && deviceId != null) {
            performLogout(apiKey, deviceId)
        } else {
            clearCredentialsAndShowLogin()
        }
    }

    private fun performLogout(apiKey: String, deviceId: String) {
        logoutButton.isEnabled = false

        val call = RetrofitClient.apiService.logout("Bearer $apiKey", deviceId)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                logoutButton.isEnabled = true

                if (response.isSuccessful) {
                    clearCredentialsAndShowLogin()
                    showToast("Logout realizado com sucesso")
                } else {
                    Log.e(TAG, "Erro no logout: ${response.code()} - ${response.message()}")
                    clearCredentialsAndShowLogin()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                logoutButton.isEnabled = true
                Log.e(TAG, "Falha no logout", t)
                clearCredentialsAndShowLogin()
            }
        })
    }

    private fun clearCredentialsAndShowLogin() {
        sharedPrefs.edit().clear().apply()
        apiKey = null
        deviceId = null
        showLoginState()
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
        logoutButton.visibility = View.GONE
        setupLoginButton()
    }

    private fun showLoggedInState() {
        logoutButton.visibility = View.VISIBLE
        loginButton.visibility = View.GONE
        emailEditText.visibility = View.GONE
        passwordEditText.visibility = View.GONE
        setupLogoutButton()
    }

    private fun setupLogoutButton() {
        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeScreen::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}