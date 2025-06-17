package com.example.trainingappmobile

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeScreen : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeScreen"
        private const val PREF_NAME = "auth"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEVICE_ID = "device_id"
    }

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_home)

        sharedPrefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        loadUserData()
    }

    private fun loadUserData() {
        val apiKey = sharedPrefs.getString(KEY_API_KEY, null)
        val deviceId = sharedPrefs.getString(KEY_DEVICE_ID, null)

        if (apiKey == null || deviceId == null) {
            navigateToLogin()
            return
        }

        val call = RetrofitClient.apiService.getPlanilha("Bearer $apiKey", deviceId)
        call.enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                if (response.isSuccessful) {
                    handleDataSuccess(response.body())
                } else {
                    handleDataError(response.code(), response.message())
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e(TAG, "Falha ao carregar dados", t)
                showToast("Falha na conexão. Verifique sua internet.")
            }
        })
    }

    private fun handleDataSuccess(planilhaResponse: PlanilhaResponse?) {
        if (planilhaResponse == null) {
            showToast("Erro ao carregar dados")
            return
        }

        if (planilhaResponse.hasError()) {
            showToast(planilhaResponse.error ?: "Erro desconhecido")
            return
        }

        // Display user information
        Log.d(TAG, "User: ${planilhaResponse.name}")
        Log.d(TAG, "Email: ${planilhaResponse.email}")
        Log.d(TAG, "Plan: ${planilhaResponse.planType}")

        // Process data using safe methods
        val trainings = planilhaResponse.getTrainingsSafe()
        val meals = planilhaResponse.getMealsSafe()
        val pdfs = planilhaResponse.getWeeklyPdfsSafe()

        Log.d(TAG, "Total trainings: ${trainings.size}")
        Log.d(TAG, "Total meals: ${meals.size}")
        Log.d(TAG, "Total PDFs: ${pdfs.size}")

        // Setup your UI with the loaded data
        setupUI(planilhaResponse)
    }

    private fun setupUI(planilhaResponse: PlanilhaResponse) {
        // Implement your UI setup here
        // Example: Setup day buttons to navigate to DayDetailsScreen

        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

        days.forEach { day ->
            // Setup click listeners for day buttons
            // Button click would call: navigateToDayDetails(day)
        }
    }

    private fun navigateToDayDetails(day: String) {
        val intent = Intent(this, DayDetailsScreen::class.java)
        intent.putExtra("selected_day", day)
        startActivity(intent)
    }

    private fun handleDataError(code: Int, message: String) {
        Log.e(TAG, "Erro ao carregar dados: $code - $message")
        when (code) {
            401 -> {
                showToast("Sessão expirada. Faça login novamente.")
                navigateToLogin()
            }
            403 -> showToast("Acesso negado")
            else -> showToast("Erro no servidor. Tente novamente")
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}