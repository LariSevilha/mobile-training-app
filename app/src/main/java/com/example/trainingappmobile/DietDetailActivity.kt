package com.example.trainingappmobile

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DietDetailActivity : ComponentActivity() {

    private lateinit var mealTitle: TextView
    private lateinit var ingredientsText: TextView
    private lateinit var backButton: LinearLayout

    companion object {
        private const val TAG = "DietDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diet_detail)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Initialize views
        try {
            mealTitle = findViewById(R.id.meal_title) ?: throw IllegalStateException("meal_title not found")
            ingredientsText = findViewById(R.id.ingredients_text) ?: throw IllegalStateException("ingredients_text not found")
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
        mealTitle.text = "Dieta de $dayOfWeek"

        // Load diet details
        loadDietDetails(dayOfWeek)
    }

    private fun loadDietDetails(dayOfWeek: String) {
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
        Log.d(TAG, "Loading diet details with authHeader=$authHeader, deviceId=$deviceId")

        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                Log.d(TAG, "API response received: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d(TAG, "Planilha data received: $planilha")

                    if (planilha.hasError()) {
                        Log.e(TAG, "Error in response: ${planilha.error}")
                        Toast.makeText(this@DietDetailActivity, planilha.error, Toast.LENGTH_SHORT).show()
                        updateNoDataUI("Error loading data: ${planilha.error}")
                        return
                    }

                    handleDietData(planilha, dayOfWeek)
                } else {
                    Log.e(TAG, "Response unsuccessful: ${response.code()} - ${response.message()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error body: $errorBody")
                    Toast.makeText(this@DietDetailActivity, "Error loading details", Toast.LENGTH_SHORT).show()
                    updateNoDataUI("Error loading data")
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e(TAG, "Request failed: ${t.message}", t)
                Toast.makeText(this@DietDetailActivity, "Connection failed: ${t.message}", Toast.LENGTH_SHORT).show()
                updateNoDataUI("Connection failed")
            }
        })
    }

    private fun handleDietData(planilha: PlanilhaResponse, dayOfWeek: String) {
        val meals = planilha.getMealsSafe()
        Log.d(TAG, "Total meals: ${meals.size}")

        meals.forEachIndexed { index, meal ->
            Log.d(TAG, "Meal $index: weekday='${meal.weekday}', type='${meal.mealType}'")
        }

        val matchingMeals = meals.filter { meal ->
            meal.weekday?.let { weekday ->
                isDayMatch(dayOfWeek, weekday)
            } ?: false
        }

        Log.d(TAG, "Meals found for '$dayOfWeek': ${matchingMeals.size}")

        val meal = when {
            matchingMeals.isNotEmpty() -> {
                Log.d(TAG, "Using specific meal for '$dayOfWeek'")
                matchingMeals.first()
            }
            meals.isNotEmpty() -> {
                Log.w(TAG, "No specific meal found for '$dayOfWeek'. Available: ${meals.map { it.weekday }}")
                updateNoDataUI("No meal found for $dayOfWeek")
                return
            }
            else -> {
                Log.w(TAG, "No meal found in planilha")
                updateNoDataUI("No meal registered")
                return
            }
        }

        Log.d(TAG, "Selected meal: ${meal.mealType} for ${meal.weekday}")
        updateDietUI(meal)
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

    private fun isDayMatch(targetDay: String, mealDay: String): Boolean {
        val normalizedTarget = normalizeDayName(targetDay)
        val normalizedMeal = normalizeDayName(mealDay)

        Log.d(TAG, "Comparing days: '$normalizedTarget' vs '$normalizedMeal'")

        val dayMappings = mapOf(
            "segunda" to listOf("segunda", "segunda-feira", "seg", "monday", "mon"),
            "terça" to listOf("terça", "terça-feira", "ter", "tuesday", "tue"),
            "quarta" to listOf("quarta", "quarta-feira", "qua", "wednesday", "wed"),
            "quinta" to listOf("quinta", "quinta-feira", "qui", "thursday", "thu"),
            "sexta" to listOf("sexta", "sexta-feira", "sex", "friday", "fri"),
            "sábado" to listOf("sábado", "sabado", "sab", "saturday", "sat"),
            "domingo" to listOf("domingo", "dom", "sunday", "sun")
        )

        if (normalizedTarget == normalizedMeal) {
            Log.d(TAG, "Exact match found")
            return true
        }

        for ((key, variants) in dayMappings) {
            if (variants.contains(normalizedTarget) && variants.contains(normalizedMeal)) {
                Log.d(TAG, "Match found for $key")
                return true
            }
        }

        val targetWords = normalizedTarget.split(" ", "-")
        val mealWords = normalizedMeal.split(" ", "-")

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

    private fun updateDietUI(meal: Meal) {
        Log.d(TAG, "Updating UI with meal: ${meal.mealType}")

        mealTitle.text = meal.getMealTypeSafe()
        val comidas = meal.getComidasSafe()
        ingredientsText.text = if (comidas.isNotEmpty()) {
            comidas.joinToString("\n") { it.getFullDescription() }
        } else {
            "Nenhuma comida cadastrada"
        }
    }

    private fun updateNoDataUI(message: String) {
        Log.d(TAG, "Updating UI with error message: $message")

        mealTitle.text = "Dados Indisponíveis"
        ingredientsText.text = message
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