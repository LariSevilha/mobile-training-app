package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DayDetailsScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_details)

        // Recuperar o dia e o tipo de dados
        val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK") ?: "Segunda-feira"
        val dataType = intent.getStringExtra("DATA_TYPE") ?: "TRAINING"

        // Atualizar o título
        val dayTitle = findViewById<TextView>(R.id.day_title)
        dayTitle.text = "Detalhes de $dayOfWeek"

        // Referência ao texto de detalhes
        val detailsText = findViewById<TextView>(R.id.details_text)

        // Referência ao botão de voltar
        val backButton = findViewById<LinearLayout>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        // Verificar autenticação
        val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("api_key", null)
        val deviceId = sharedPrefs.getString("device_id", null)

        if (apiKey == null || deviceId == null) {
            Toast.makeText(this, "Por favor, faça login novamente", Toast.LENGTH_SHORT).show()
            sharedPrefs.edit().clear().apply()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // Carregar os dados do dia
        loadDayDetails(apiKey, deviceId, dayOfWeek, dataType, detailsText)
    }

    private fun loadDayDetails(
        apiKey: String,
        deviceId: String,
        dayOfWeek: String,
        dataType: String,
        detailsText: TextView
    ) {
        val authHeader = "Bearer $apiKey"
        Log.d("DayDetailsScreen", "Carregando dados para $dayOfWeek com tipo $dataType")
        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d("DayDetailsScreen", "Dados recebidos: $planilha")
                    if (planilha.error == null) {
                        val backendDay = mapDayToBackendFormat(dayOfWeek)
                        val details = when (dataType) {
                            "TRAINING" -> {
                                val trainings = planilha.trainings?.filter { it.weekday == backendDay } ?: emptyList()
                                if (trainings.isEmpty()) {
                                    "Nenhum treino disponível para $dayOfWeek."
                                } else {
                                    trainings.joinToString("\n\n") { training ->
                                        "Treino: ${training.exercise_name ?: "Não especificado"}\n" +
                                                "Séries: ${training.serie_amount ?: "N/A"}, Repetições: ${training.repeat_amount ?: "N/A"}" +
                                                if (training.video.isNullOrEmpty()) "" else "\nVídeo: ${training.video}"
                                    }
                                }
                            }
                            "DIET" -> {
                                val meals = planilha.meals?.filter { it.weekday == backendDay } ?: emptyList()
                                if (meals.isEmpty()) {
                                    "Nenhuma refeição disponível para $dayOfWeek."
                                } else {
                                    meals.joinToString("\n\n") { meal ->
                                        "Refeição: ${meal.meal_type ?: "Não especificada"}\n" +
                                                "Itens: ${
                                                    if (meal.comidas.isNullOrEmpty()) "Nenhum item disponível"
                                                    else meal.comidas.joinToString(", ") { comida ->
                                                        "${comida.name ?: "Item desconhecido"} (${comida.amount ?: "N/A"})"
                                                    }
                                                }"
                                    }
                                }
                            }
                            else -> "Tipo de dados inválido."
                        }
                        detailsText.text = details
                    } else {
                        Toast.makeText(this@DayDetailsScreen, planilha.error, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@DayDetailsScreen, "Erro ao carregar dados: ${response.message()}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e("DayDetailsScreen", "Falha na requisição: ${t.message}", t)
                Toast.makeText(this@DayDetailsScreen, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun mapDayToBackendFormat(dayOfWeek: String): String {
        return when (dayOfWeek) {
            "Segunda-feira" -> "monday"
            "Terça-feira" -> "tuesday"
            "Quarta-feira" -> "wednesday"
            "Quinta-feira" -> "thursday"
            "Sexta-feira" -> "friday"
            "Sábado" -> "saturday"
            "Domingo" -> "sunday"
            else -> "monday"
        }
    }

    private fun navigateToLogin() {
        val sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}