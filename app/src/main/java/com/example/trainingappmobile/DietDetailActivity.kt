package com.example.trainingappmobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
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
    private lateinit var preparationText: TextView
    private lateinit var mealImage: ImageView
    private lateinit var videoLink: TextView
    private lateinit var backButton: LinearLayout

    companion object {
        private const val TAG = "DietDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diet_detail)

        // Inicializar views
        try {
            mealTitle = findViewById(R.id.meal_title)
            ingredientsText = findViewById(R.id.ingredients_text)
            preparationText = findViewById(R.id.preparation_text)
            mealImage = findViewById(R.id.meal_image)
            videoLink = findViewById(R.id.video_link)
            backButton = findViewById(R.id.back_button_detail)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar views: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar a tela", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar botão de voltar
        backButton.setOnClickListener {
            Log.d(TAG, "Botão Voltar clicado")
            finish()
        }

        // Recuperar dados do Intent
        val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK") ?: "Dia não especificado"
        Log.d(TAG, "Dados recebidos - Dia: $dayOfWeek")

        // Configurar título inicial
        mealTitle.text = "Dieta de $dayOfWeek"

        // Carregar dados da dieta
        loadDietDetails(dayOfWeek)
    }

    private fun loadDietDetails(dayOfWeek: String) {
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("auth_token", null)
        val deviceId = sharedPrefs.getString("device_id", null)
        Log.d(TAG, "Credenciais: auth_token=$apiKey, deviceId=$deviceId")

        if (apiKey == null || deviceId == null) {
            Log.e(TAG, "auth_token ou deviceId está nulo")
            Toast.makeText(this, "Por favor, faça login novamente", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val authHeader = "Bearer $apiKey"
        Log.d(TAG, "Carregando detalhes da dieta com authHeader=$authHeader, deviceId=$deviceId")

        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                Log.d(TAG, "Resposta recebida: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d(TAG, "Dados da planilha recebidos: $planilha")

                    if (planilha.hasError()) {
                        Log.e(TAG, "Erro na resposta: ${planilha.error}")
                        Toast.makeText(this@DietDetailActivity, planilha.error, Toast.LENGTH_SHORT).show()
                        updateNoDataUI("Erro ao carregar dados: ${planilha.error}")
                        return
                    }

                    handleDietData(planilha, dayOfWeek)
                } else {
                    Log.e(TAG, "Resposta não foi bem-sucedida: ${response.code()} - ${response.message()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error body: $errorBody")
                    Toast.makeText(this@DietDetailActivity, "Erro ao carregar detalhes", Toast.LENGTH_SHORT).show()
                    updateNoDataUI("Erro ao carregar dados")
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e(TAG, "Falha na requisição: ${t.message}", t)
                Toast.makeText(this@DietDetailActivity, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
                updateNoDataUI("Falha na conexão")
            }
        })
    }

    private fun handleDietData(planilha: PlanilhaResponse, dayOfWeek: String) {
        val meals = planilha.getMealsSafe()
        Log.d(TAG, "Total de refeições: ${meals.size}")

        meals.forEachIndexed { index, meal ->
            Log.d(TAG, "Refeição $index: weekday='${meal.weekday}', type='${meal.mealType}'")
        }

        val matchingMeals = meals.filter { meal ->
            meal.weekday?.let { weekday ->
                isDayMatch(dayOfWeek, weekday)
            } ?: false
        }

        Log.d(TAG, "Refeições encontradas para '$dayOfWeek': ${matchingMeals.size}")

        val meal = when {
            matchingMeals.isNotEmpty() -> {
                Log.d(TAG, "Usando refeição específica para '$dayOfWeek'")
                matchingMeals.first()
            }
            meals.isNotEmpty() -> {
                Log.w(TAG, "Nenhuma refeição específica encontrada para '$dayOfWeek'. Disponíveis: ${meals.map { it.weekday }}")
                updateNoDataUI("Nenhuma refeição encontrada para $dayOfWeek")
                return
            }
            else -> {
                Log.w(TAG, "Nenhuma refeição encontrada na planilha")
                updateNoDataUI("Nenhuma refeição cadastrada")
                return
            }
        }

        Log.d(TAG, "Refeição selecionada: ${meal.mealType} para ${meal.weekday}")
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

        Log.d(TAG, "Comparando dias: '$normalizedTarget' vs '$normalizedMeal'")

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
            Log.d(TAG, "Match exato encontrado")
            return true
        }

        for ((key, variants) in dayMappings) {
            if (variants.contains(normalizedTarget) && variants.contains(normalizedMeal)) {
                Log.d(TAG, "Match por mapeamento encontrado para $key")
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
                        Log.d(TAG, "Match por substring encontrado: '$targetWord' e '$mealWord'")
                        return true
                    }
                }
            }
        }

        Log.d(TAG, "Nenhum match encontrado")
        return false
    }

    private fun updateDietUI(meal: Meal) {
        Log.d(TAG, "Atualizando UI com refeição: ${meal.mealType}")

        mealTitle.text = meal.getMealTypeSafe()
        val comidas = meal.getComidasSafe()
        ingredientsText.text = if (comidas.isNotEmpty()) {
            comidas.joinToString("\n") { it.getFullDescription() }
        } else {
            "Nenhuma comida cadastrada"
        }
        preparationText.text = meal.getComidasSafe().firstOrNull()?.name ?: "Instruções de preparo não disponíveis"

        mealImage.visibility = View.GONE
        videoLink.visibility = View.GONE
    }

    private fun updateNoDataUI(message: String) {
        Log.d(TAG, "Atualizando UI com mensagem de erro: $message")

        mealTitle.text = "Dados Indisponíveis"
        ingredientsText.text = message
        preparationText.text = "-"
        mealImage.visibility = View.GONE
        videoLink.visibility = View.GONE
    }
}