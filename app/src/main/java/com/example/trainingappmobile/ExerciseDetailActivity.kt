package com.example.trainingappmobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ExerciseDetailActivity : ComponentActivity() {

    private lateinit var exerciseTitle: TextView
    private lateinit var seriesCount: TextView
    private lateinit var repetitionsCount: TextView
    private lateinit var weekdayText: TextView
    private lateinit var videoContainer: LinearLayout
    private lateinit var videoLink: LinearLayout
    private lateinit var backButton: LinearLayout
    private var currentTraining: Training? = null

    companion object {
        private const val TAG = "ExerciseDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_details)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Initialize views
        try {
            exerciseTitle = findViewById(R.id.exercise_title) ?: throw IllegalStateException("exercise_title not found")
            seriesCount = findViewById(R.id.series_count) ?: throw IllegalStateException("series_count not found")
            repetitionsCount = findViewById(R.id.repetitions_count) ?: throw IllegalStateException("repetitions_count not found")
            weekdayText = findViewById(R.id.weekday_text) ?: throw IllegalStateException("weekday_text not found")
            videoContainer = findViewById(R.id.video_container) ?: throw IllegalStateException("video_container not found")
            videoLink = findViewById(R.id.video_link) ?: throw IllegalStateException("video_link not found")
            backButton = findViewById(R.id.back_button_detail) ?: throw IllegalStateException("back_button_detail not found")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            Toast.makeText(this, "Error loading screen: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up back button
        backButton.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            finish()
            overridePendingTransition(android.R.anim.fade_out, android.R.anim.fade_in)
        }

        // Retrieve day of week from Intent
        val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK") ?: "Dia não especificado"
        Log.d(TAG, "Received data - Day: $dayOfWeek")

        // Set initial title
        exerciseTitle.text = "Treino de $dayOfWeek"
        weekdayText.text = dayOfWeek

        // Load training details
        loadTrainingDetails(dayOfWeek)
    }

    private fun loadTrainingDetails(dayOfWeek: String) {
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("auth_token", null)
        val deviceId = sharedPrefs.getString("device_id", null)
        Log.d(TAG, "Credentials: auth_token=$apiKey, deviceId=$deviceId")

        if (apiKey == null || deviceId == null) {
            Log.e(TAG, "auth_token or deviceId is null")
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val authHeader = "Bearer $apiKey"
        Log.d(TAG, "Loading training details with authHeader=$authHeader, deviceId=$deviceId")

        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                Log.d(TAG, "API response received: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d(TAG, "Planilha data received: $planilha")

                    if (planilha.hasError()) {
                        Log.e(TAG, "Error in response: ${planilha.error}")
                        Toast.makeText(this@ExerciseDetailActivity, planilha.error, Toast.LENGTH_SHORT).show()
                        updateNoDataUI("Error loading data: ${planilha.error}")
                        return
                    }

                    handleTrainingData(planilha, dayOfWeek)
                } else {
                    Log.e(TAG, "Response unsuccessful: ${response.code()} - ${response.message()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error body: $errorBody")
                    Toast.makeText(this@ExerciseDetailActivity, "Error loading details", Toast.LENGTH_SHORT).show()
                    updateNoDataUI("Error loading data")
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e(TAG, "Request failed: ${t.message}", t)
                Toast.makeText(this@ExerciseDetailActivity, "Connection failed: ${t.message}", Toast.LENGTH_SHORT).show()
                updateNoDataUI("Connection failed")
            }
        })
    }

    private fun handleTrainingData(planilha: PlanilhaResponse, dayOfWeek: String) {
        val trainings = planilha.getTrainingsSafe()
        Log.d(TAG, "Total trainings: ${trainings.size}")

        trainings.forEachIndexed { index, training ->
            Log.d(TAG, "Training $index: weekday='${training.weekday}', exerciseName='${training.exerciseName}'")
        }

        val matchingTrainings = trainings.filter { training ->
            training.weekday?.let { weekday ->
                isDayMatch(dayOfWeek, weekday)
            } ?: false
        }

        Log.d(TAG, "Trainings found for '$dayOfWeek': ${matchingTrainings.size}")

        val training = when {
            matchingTrainings.isNotEmpty() -> {
                Log.d(TAG, "Using specific training for '$dayOfWeek'")
                matchingTrainings.first()
            }
            trainings.isNotEmpty() -> {
                Log.w(TAG, "No specific training found for '$dayOfWeek'. Available: ${trainings.map { it.weekday }}")
                updateNoDataUI("No training found for $dayOfWeek")
                return
            }
            else -> {
                Log.w(TAG, "No training found in planilha")
                updateNoDataUI("No training registered")
                return
            }
        }

        currentTraining = training
        Log.d(TAG, "Selected training: ${training.exerciseName} for ${training.weekday}")
        updateTrainingUI(training)
    }

    private fun normalizeDayName(day: String): String {
        return day.lowercase()
            .replace("á", "a")
            .replace("ã", "a")
            .replace("ç", "c")
            .replace("é", "e")
            .replace("ê", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ô", "o")
            .replace("ú", "u")
            .replace("ü", "u")
            .replace("-feira", "")
            .trim()
    }

    private fun isDayMatch(targetDay: String, trainingDay: String): Boolean {
        val normalizedTarget = normalizeDayName(targetDay)
        val normalizedTraining = normalizeDayName(trainingDay)

        Log.d(TAG, "Comparing days: '$normalizedTarget' vs '$normalizedTraining'")

        val dayMappings = mapOf(
            "segunda" to listOf("segunda", "segunda-feira", "seg", "monday", "mon"),
            "terça" to listOf("terça", "terça-feira", "ter", "tuesday", "tue"),
            "quarta" to listOf("quarta", "quarta-feira", "qua", "wednesday", "wed"),
            "quinta" to listOf("quinta", "quinta-feira", "qui", "thursday", "thu"),
            "sexta" to listOf("sexta", "sexta-feira", "sex", "friday", "fri"),
            "sábado" to listOf("sábado", "sabado", "sab", "saturday", "sat"),
            "domingo" to listOf("domingo", "dom", "sunday", "sun")
        )

        if (normalizedTarget == normalizedTraining) {
            Log.d(TAG, "Exact match found")
            return true
        }

        for ((key, variants) in dayMappings) {
            if (variants.contains(normalizedTarget) && variants.contains(normalizedTraining)) {
                Log.d(TAG, "Match found for $key")
                return true
            }
        }

        val targetWords = normalizedTarget.split(" ", "-")
        val mealWords = normalizedTraining.split(" ", "-")

        for (targetWord in targetWords) {
            if (targetWord.length >= 3) {
                for (mealWord in mealWords) {
                    if (mealWord.length >= 3 &&
                        (targetWord.contains(mealWord) || mealWord.contains(targetWord))) {
                        Log.d(TAG, "Substring match found: '$targetWord' and '$mealWord'")
                        return true
                    }
                }
            }
        }

        Log.d(TAG, "No match found")
        return false
    }

    private fun updateTrainingUI(training: Training) {
        Log.d(TAG, "Updating UI with training: ${training.exerciseName}")

        exerciseTitle.text = training.exerciseName ?: "Nome não disponível"
        seriesCount.text = training.serieAmount?.toString() ?: "0"
        repetitionsCount.text = training.repeatAmount?.toString() ?: "0"
        weekdayText.text = training.weekday ?: "Dia não especificado"

        // Handle video link
        if (!training.video.isNullOrEmpty()) {
            videoContainer.visibility = View.VISIBLE
            videoLink.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(training.video))
                    startActivity(intent)
                    Log.d(TAG, "Opening video: ${training.video}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening video: ${e.message}", e)
                    Toast.makeText(this, "Não foi possível abrir o vídeo", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            videoContainer.visibility = View.GONE
        }
    }

    private fun updateNoDataUI(message: String) {
        Log.d(TAG, "Updating UI with error message: $message")

        exerciseTitle.text = "Dados Indisponíveis"
        seriesCount.text = "-"
        repetitionsCount.text = "-"
        weekdayText.text = "-"
        videoContainer.visibility = View.GONE
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_out, android.R.anim.fade_in)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called")
    }
}