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
import android.view.WindowManager
import android.view.View

class DayDetailsScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_details)

        // Impedir capturas de tela
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // Recuperar o dia e o tipo de dados
        val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK") ?: "Segunda-feira"
        val dataType = intent.getStringExtra("DATA_TYPE") ?: "TRAINING"

        // Atualizar o título
        val dayTitle = findViewById<TextView>(R.id.day_title)
        dayTitle.text = "Detalhes de $dayOfWeek"

        // Referência ao container de treinos
        val trainingsContainer = findViewById<LinearLayout>(R.id.trainings_container)

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
        loadDayDetails(apiKey, deviceId, dayOfWeek, dataType, trainingsContainer)
    }

    private fun loadDayDetails(
        apiKey: String,
        deviceId: String,
        dayOfWeek: String,
        dataType: String,
        trainingsContainer: LinearLayout
    ) {
        val authHeader = "Bearer $apiKey"
        Log.d("DayDetailsScreen", "Carregando dados para $dayOfWeek com tipo $dataType")
        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d("DayDetailsScreen", "Dados recebidos: $planilha")
                    if (planilha.error == null) {
                        // Limpar container
                        trainingsContainer.removeAllViews()

                        // Verificar se há PDF para o dia
                        val backendDay = mapDayToBackendFormat(dayOfWeek)
                        val weeklyPdf = planilha.weekly_pdfs?.find { it.weekday == backendDay }
                        if (weeklyPdf != null && weeklyPdf.pdf_url != null) {
                            val pdfView = layoutInflater.inflate(R.layout.item_training, trainingsContainer, false)
                            pdfView.findViewById<TextView>(R.id.exercise_name).text = "PDF disponível para $dayOfWeek"
                            pdfView.findViewById<TextView>(R.id.series_text).visibility = View.GONE
                            pdfView.findViewById<TextView>(R.id.reps_text).apply {
                                text = "Clique para visualizar"
                                setTextColor(0xFFCEAC5E.toInt())
                                setOnClickListener {
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(weeklyPdf.pdf_url))
                                    startActivity(intent)
                                }
                            }
                            trainingsContainer.addView(pdfView)
                            return
                        }

                        when (dataType) {
                            "TRAINING" -> {
                                val trainings = planilha.trainings?.filter { it.weekday == backendDay } ?: emptyList()
                                if (trainings.isEmpty()) {
                                    val textView = TextView(this@DayDetailsScreen).apply {
                                        layoutParams = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        )
                                        text = "Nenhum treino disponível para $dayOfWeek."
                                        textSize = 14f
                                        setTextColor(0xFFFFFFFF.toInt())
                                        gravity = android.view.Gravity.CENTER
                                        setPadding(8, 8, 8, 8)
                                    }
                                    trainingsContainer.addView(textView)
                                } else {
                                    trainings.forEach { training ->
                                        val trainingView = layoutInflater.inflate(R.layout.item_training, trainingsContainer, false)
                                        trainingView.findViewById<TextView>(R.id.exercise_name).text = training.exercise_name ?: "Não especificado"
                                        trainingView.findViewById<TextView>(R.id.series_text).text = "Séries: ${training.serie_amount ?: "N/A"}"
                                        trainingView.findViewById<TextView>(R.id.reps_text).text = "Repetições: ${training.repeat_amount ?: "N/A"}"
                                        trainingView.setOnClickListener {
                                            val intent = Intent(this@DayDetailsScreen, ExerciseDetailActivity::class.java).apply {
                                                putExtra("EXERCISE_NAME", training.exercise_name)
                                                putExtra("EXERCISE_DESCRIPTION", training.description)
                                                putExtra("EXERCISE_VIDEO", training.video)
                                                putExtra("EXERCISE_PHOTOS", training.photo_urls?.toTypedArray())
                                            }
                                            startActivity(intent)
                                        }
                                        trainingsContainer.addView(trainingView)
                                    }
                                }
                            }
                            "DIET" -> {
                                val meals = planilha.meals?.filter { it.weekday == backendDay } ?: emptyList()
                                if (meals.isEmpty()) {
                                    val textView = TextView(this@DayDetailsScreen).apply {
                                        layoutParams = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        )
                                        text = "Nenhuma refeição disponível para $dayOfWeek."
                                        textSize = 14f
                                        setTextColor(0xFFFFFFFF.toInt())
                                        gravity = android.view.Gravity.CENTER
                                        setPadding(8, 8, 8, 8)
                                    }
                                    trainingsContainer.addView(textView)
                                } else {
                                    meals.forEach { meal ->
                                        val mealView = layoutInflater.inflate(R.layout.item_training, trainingsContainer, false)
                                        mealView.findViewById<TextView>(R.id.exercise_name).text = meal.meal_type ?: "Não especificada"
                                        mealView.findViewById<TextView>(R.id.series_text).text = "Itens: ${
                                            if (meal.comidas.isNullOrEmpty()) "Nenhum item disponível"
                                            else meal.comidas.joinToString(", ") { comida ->
                                                "${comida.name ?: "Item desconhecido"} (${comida.amount ?: "N/A"})"
                                            }
                                        }"
                                        mealView.findViewById<TextView>(R.id.reps_text).visibility = View.GONE
                                        trainingsContainer.addView(mealView)
                                    }
                                }
                            }
                            else -> {
                                val textView = TextView(this@DayDetailsScreen).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                                    text = "Tipo de dados inválido."
                                    textSize = 14f
                                    setTextColor(0xFFFFFFFF.toInt())
                                    gravity = android.view.Gravity.CENTER
                                    setPadding(8, 8, 8, 8)
                                }
                                trainingsContainer.addView(textView)
                            }
                        }
                    } else {
                        if (planilha.error == "Conta expirada. Entre em contato com o administrador.") {
                            Toast.makeText(this@DayDetailsScreen, planilha.error, Toast.LENGTH_LONG).show()
                            navigateToLogin()
                        } else {
                            Toast.makeText(this@DayDetailsScreen, planilha.error, Toast.LENGTH_SHORT).show()
                            finish()
                        }
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