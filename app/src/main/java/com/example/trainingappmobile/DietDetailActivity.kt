package com.example.trainingappmobile

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DietDetailActivity : ComponentActivity() {

    private lateinit var weekdayText: TextView
    private lateinit var mealsContainer: LinearLayout
    private lateinit var backButton: LinearLayout

    companion object {
        private const val TAG = "DietDetailActivity"
    }
    private fun setupMaximumSecurity() {
        try {
            // Configurar FLAG_SECURE para prevenir screenshots
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )

            // Desabilitar screenshots na tela de recentes (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setRecentsScreenshotEnabled(false)
            }

            // Configurar desenho das barras do sistema
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            }

            // Adicionar proteção contra gravação de tela
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            Log.d(TAG, "Segurança configurada com sucesso")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar segurança: ${e.message}", e)
            // Não fazer crash da aplicação por causa da segurança
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMaximumSecurity()
        setContentView(R.layout.activity_diet_detail)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Initialize views
        try {
            weekdayText = findViewById(R.id.weekday_text) ?: throw IllegalStateException("weekday_text not found")
            mealsContainer = findViewById(R.id.meals_container) ?: throw IllegalStateException("meals_container not found")
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

        // Set initial weekday
        weekdayText.text = dayOfWeek

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

        when {
            matchingMeals.isNotEmpty() -> {
                Log.d(TAG, "Displaying ${matchingMeals.size} meals for '$dayOfWeek'")
                updateDietUI(matchingMeals, dayOfWeek)
            }
            meals.isNotEmpty() -> {
                Log.w(TAG, "No specific meal found for '$dayOfWeek'. Available weekdays: ${meals.map { it.weekday }}")
                updateNoDataUI("No meal found for $dayOfWeek")
            }
            else -> {
                Log.w(TAG, "No meal found in planilha")
                updateNoDataUI("No meal registered")
            }
        }
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
            .replace(" ", "")
            .trim()
    }

    private fun getPortugueseDayName(normalizedDay: String): String {
        val dayMappings = mapOf(
            "segunda" to "Segunda-feira",
            "terca" to "Terça-feira",
            "quarta" to "Quarta-feira",
            "quinta" to "Quinta-feira",
            "sexta" to "Sexta-feira",
            "sabado" to "Sábado",
            "domingo" to "Domingo"
        )
        return dayMappings[normalizedDay] ?: normalizedDay.replaceFirstChar { it.uppercaseChar() }
    }

    private fun isDayMatch(targetDay: String, mealDay: String): Boolean {
        val normalizedTarget = normalizeDayName(targetDay)
        val normalizedMeal = normalizeDayName(mealDay)

        Log.d(TAG, "Comparing days: '$normalizedTarget' vs '$normalizedMeal'")

        val dayMappings = mapOf(
            "segunda" to listOf("segunda", "segunda-feira", "seg", "monday", "mon", "segundafeira"),
            "terca" to listOf("terça", "terça-feira", "ter", "tuesday", "tue", "tercafeira", "terçafeira"),
            "quarta" to listOf("quarta", "quarta-feira", "qua", "wednesday", "wed", "quartafeira"),
            "quinta" to listOf("quinta", "quinta-feira", "qui", "thursday", "thu", "quintafeira"),
            "sexta" to listOf("sexta", "sexta-feira", "sex", "friday", "fri", "sextafeira"),
            "sabado" to listOf("sábado", "sabado", "sab", "saturday", "sat", "sabadofeira", "sábadofeira"),
            "domingo" to listOf("domingo", "dom", "sunday", "sun", "domingofeira")
        )

        val targetVariants = dayMappings.entries.find { it.value.contains(normalizedTarget) }?.value ?: emptyList()
        val match = targetVariants.contains(normalizedMeal)

        if (match) {
            Log.d(TAG, "Match found for '$normalizedTarget' in variants: $targetVariants")
        } else {
            Log.w(TAG, "No match for '$normalizedTarget' vs '$normalizedMeal'. Target variants: $targetVariants")
        }

        return match
    }

    private fun updateDietUI(meals: List<Meal>, dayOfWeek: String) {
        Log.d(TAG, "Updating UI with ${meals.size} meals")

        // Clear existing views in the container
        mealsContainer.removeAllViews()

        // Set weekday to the intent's DAY_OF_WEEK (already in Portuguese)
        weekdayText.text = dayOfWeek

        if (meals.isEmpty()) {
            updateNoDataUI("Nenhuma refeição cadastrada")
            return
        }

        // Add a card for each meal
        meals.forEach { meal ->
            // Inflate a new card layout
            val cardView = LayoutInflater.from(this).inflate(R.layout.meal_card, null)

            // Find views in the card
            val mealTypeText = cardView.findViewById<TextView>(R.id.meal_type_text)
            val ingredientsText = cardView.findViewById<TextView>(R.id.ingredients_text)

            // Set meal data
            mealTypeText.text = meal.getMealTypeSafe()
            val comidas = meal.getComidasSafe()
            ingredientsText.text = if (comidas.isNotEmpty()) {
                comidas.joinToString("\n") { it.getFullDescription() }
            } else {
                "Nenhuma comida cadastrada"
            }

            // Add card to container
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.bottomMargin = (16 * resources.displayMetrics.density).toInt() // Convert 16dp to pixels
            mealsContainer.addView(cardView, layoutParams)

            Log.d(TAG, "Added card for meal: ${meal.getMealTypeSafe()}")
        }
    }

    private fun updateNoDataUI(message: String) {
        Log.d(TAG, "Updating UI with error message: $message")

        mealsContainer.removeAllViews()
        val errorView = LayoutInflater.from(this).inflate(R.layout.meal_card, null)
        val mealTypeText = errorView.findViewById<TextView>(R.id.meal_type_text)
        val ingredientsText = errorView.findViewById<TextView>(R.id.ingredients_text)

        mealTypeText.text = "Dados Indisponíveis"
        ingredientsText.text = message
        weekdayText.text = "-"

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.bottomMargin = (16 * resources.displayMetrics.density).toInt()
        mealsContainer.addView(errorView, layoutParams)
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