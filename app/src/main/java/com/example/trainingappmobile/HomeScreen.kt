package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.view.View

class HomeScreen : ComponentActivity() {

    private lateinit var greetingText: TextView
    private lateinit var trainingButton: LinearLayout
    private lateinit var dietButton: LinearLayout
    private lateinit var pdfCard: LinearLayout
    private lateinit var noDataText: TextView
    private lateinit var planExpiryText: TextView

    private var currentPlanilhaData: PlanilhaResponse? = null

    companion object {
        private const val TAG = "HomeScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initializeViews()
        setupClickListeners()
        loadUserData()
    }

    private fun initializeViews() {
        try {
            greetingText = findViewById(R.id.greeting_text)
            trainingButton = findViewById(R.id.training_button)
            dietButton = findViewById(R.id.diet_button)
            pdfCard = findViewById(R.id.pdf_card)
            noDataText = findViewById(R.id.no_data_text)
            planExpiryText = findViewById(R.id.plan_expiry_text)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            throw e // Re-throw to prevent app from continuing in bad state
        }
    }

    private fun setupClickListeners() {
        trainingButton.setOnClickListener {
            if (hasTrainingData()) {
                openDaysOfWeekScreen("training")
            } else {
                showNoDataMessage("Nenhum treino disponível")
            }
        }

        dietButton.setOnClickListener {
            if (hasDietData()) {
                openDaysOfWeekScreen("diet")
            } else {
                showNoDataMessage("Nenhuma dieta disponível")
            }
        }

        pdfCard.setOnClickListener {
            openPdfViewer()
        }
    }

    private fun loadUserData() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
        val deviceId = prefs.getString("device_id", null)

        Log.d(TAG, "Token: $token")
        Log.d(TAG, "Device ID: $deviceId")

        if (token.isNullOrEmpty() || deviceId.isNullOrEmpty()) {
            Log.e(TAG, "Token or Device ID not found or empty")
            showNoDataMessage("Authentication error")
            return
        }

        val apiService = RetrofitClient.apiService
        val call = apiService.getPlanilha("Bearer $token", deviceId)

        call.enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                if (response.isSuccessful) {
                    val planilhaResponse = response.body()
                    if (planilhaResponse != null) {
                        currentPlanilhaData = planilhaResponse
                        updateUI(planilhaResponse)
                    } else {
                        Log.e(TAG, "Empty response from API")
                        showNoDataMessage("Data not available")
                    }
                } else {
                    Log.e(TAG, "Request error: ${response.code()} - ${response.message()}")
                    showNoDataMessage("Error loading data")
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e(TAG, "Request failed: ${t.message}", t)
                showNoDataMessage("Connection error")
            }
        })
    }

    private fun updateUI(planilhaResponse: PlanilhaResponse) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val userName = planilhaResponse.name ?: "Usuário"
                greetingText.text = "Bem-vindo(a), $userName!"

                // Removendo a lógica condicional de visibilidade
                trainingButton.visibility = View.VISIBLE
                dietButton.visibility = View.VISIBLE
                pdfCard.visibility = View.VISIBLE
                noDataText.visibility = View.GONE // Removendo a exibição automática de "Nenhum dado disponível"

                planilhaResponse.expirationDate?.let { expirationDate ->
                    planExpiryText.text = "Data de expiração: $expirationDate"
                    planExpiryText.visibility = View.VISIBLE
                } ?: run { planExpiryText.visibility = View.GONE }

                Log.d(TAG, "UI updated successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error updating UI: ${e.message}", e)
                showNoDataMessage("Error displaying data")
            }
        }
    }

    private fun hasTrainingData(): Boolean {
        return currentPlanilhaData?.getTrainingsSafe()?.isNotEmpty() == true
    }

    private fun hasDietData(): Boolean {
        return currentPlanilhaData?.getMealsSafe()?.isNotEmpty() == true
    }

    private fun hasPdfData(): Boolean {
        val pdfs = currentPlanilhaData?.getWeeklyPdfsSafe() ?: emptyList()
        val hasValidPdf = pdfs.any { it.hasValidUrl() }
        Log.d(TAG, "hasPdfData: $hasValidPdf, PDFs count: ${pdfs.size}")
        return hasValidPdf
    }

    private fun openDaysOfWeekScreen(type: String) {
        val intent = Intent(this, DaysOfWeekScreen::class.java)
        intent.putExtra("SCREEN_TYPE", type)
        startActivity(intent)
    }

    private fun openPdfViewer() {
        try {
            val pdfs = currentPlanilhaData?.getWeeklyPdfsSafe() ?: emptyList()
            val validPdf = pdfs.find { it.hasValidUrl() }

            if (validPdf == null) {
                Log.e(TAG, "No valid PDF found")
                showNoDataMessage("PDF not available")
                return
            }

            val intent = Intent(this, PdfViewerScreen::class.java)
            intent.putExtra("PDF_URL", validPdf.pdfUrl)
            validPdf.weekday?.let { intent.putExtra("WEEKDAY", it) }
            startActivity(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF: ${e.message}", e)
            showNoDataMessage("Error opening PDF")
        }
    }

    private fun clearUserData() {
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showNoDataMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            noDataText.text = message
            noDataText.visibility = View.VISIBLE
            Toast.makeText(this@HomeScreen, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentPlanilhaData == null) {
            loadUserData()
        }
    }
}