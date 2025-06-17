package com.example.trainingappmobile

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DayDetailsScreen : AppCompatActivity() {

    companion object {
        private const val TAG = "DayDetailsScreen"
        private const val PREF_NAME = "auth"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEVICE_ID = "device_id"
    }

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var trainingRecyclerView: RecyclerView
    private lateinit var mealRecyclerView: RecyclerView
    private var selectedDay: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_day_details)

        sharedPrefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        selectedDay = intent.getStringExtra("selected_day") ?: ""

        initViews()
        loadDayDetails()
    }

    private fun initViews() {
        // Initialize your RecyclerViews here
        // trainingRecyclerView = findViewById(R.id.training_recycler_view)
        // mealRecyclerView = findViewById(R.id.meal_recycler_view)

        // trainingRecyclerView.layoutManager = LinearLayoutManager(this)
        // mealRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadDayDetails() {
        val apiKey = sharedPrefs.getString(KEY_API_KEY, null)
        val deviceId = sharedPrefs.getString(KEY_DEVICE_ID, null)

        if (apiKey == null || deviceId == null) {
            showToast("Erro de autenticação")
            finish()
            return
        }

        val call = RetrofitClient.apiService.getPlanilha("Bearer $apiKey", deviceId)
        call.enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                if (response.isSuccessful) {
                    handlePlanilhaSuccess(response.body())
                } else {
                    handlePlanilhaError(response.code(), response.message())
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e(TAG, "Falha ao carregar planilha", t)
                showToast("Falha na conexão. Verifique sua internet.")
            }
        })
    }

    private fun handlePlanilhaSuccess(planilhaResponse: PlanilhaResponse?) {
        if (planilhaResponse == null) {
            showToast("Erro ao carregar dados")
            return
        }

        if (planilhaResponse.hasError()) {
            showToast(planilhaResponse.error ?: "Erro desconhecido")
            return
        }

        // Use the safe methods to get data
        val trainings = planilhaResponse.getTrainingsSafe()
            .filter { it.weekday?.equals(selectedDay, ignoreCase = true) == true }

        val meals = planilhaResponse.getMealsSafe()
            .filter { it.weekday?.equals(selectedDay, ignoreCase = true) == true }

        val weeklyPdfs = planilhaResponse.getWeeklyPdfsSafe()
            .filter { it.weekday?.equals(selectedDay, ignoreCase = true) == true }

        // Process trainings
        trainings.forEach { training ->
            Log.d(TAG, "Exercise: ${training.getExerciseNameSafe()}")
            Log.d(TAG, "Series/Reps: ${training.getSeriesRepetitionsText()}")
            Log.d(TAG, "Has video: ${training.hasVideo()}")
            Log.d(TAG, "Has photos: ${training.hasPhotos()}")

            // Access properties correctly
            val exerciseName = training.exerciseName // Use property name, not snake_case
            val repeatAmount = training.repeatAmount // Use property name, not snake_case
            val photoUrls = training.getPhotoUrlsSafe() // Use safe method

            Log.d(TAG, "Direct access - Exercise: $exerciseName, Repeats: $repeatAmount")
            Log.d(TAG, "Photo URLs: $photoUrls")
        }

        // Process meals
        meals.forEach { meal ->
            Log.d(TAG, "Meal type: ${meal.getMealTypeSafe()}")

            meal.getComidasSafe().forEach { comida ->
                Log.d(TAG, "Food: ${comida.getFullDescription()}")
            }
        }

        // Process PDFs
        weeklyPdfs.forEach { pdf ->
            Log.d(TAG, "PDF for ${pdf.getWeekdaySafe()}: ${pdf.pdfUrl}")
        }

        // Setup your adapters here with the filtered data
        // setupTrainingAdapter(trainings)
        // setupMealAdapter(meals)
    }

    private fun handlePlanilhaError(code: Int, message: String) {
        Log.e(TAG, "Erro ao carregar planilha: $code - $message")
        when (code) {
            401 -> {
                showToast("Sessão expirada. Faça login novamente.")
                // Navigate back to login
            }
            403 -> showToast("Acesso negado")
            404 -> showToast("Planilha não encontrada")
            else -> showToast("Erro no servidor. Tente novamente")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}