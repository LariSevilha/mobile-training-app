package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.view.WindowManager

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

        // Referência à tabela de detalhes
        val detailsTable = findViewById<TableLayout>(R.id.details_text)

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
        loadDayDetails(apiKey, deviceId, dayOfWeek, dataType, detailsTable)
    }

    private fun loadDayDetails(
        apiKey: String,
        deviceId: String,
        dayOfWeek: String,
        dataType: String,
        detailsTable: TableLayout
    ) {
        val authHeader = "Bearer $apiKey"
        Log.d("DayDetailsScreen", "Carregando dados para $dayOfWeek com tipo $dataType")
        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d("DayDetailsScreen", "Dados recebidos: $planilha")
                    if (planilha.error == null) {
                        // Limpar tabela existente
                        detailsTable.removeAllViews()

                        // Verificar se há PDF para o dia
                        val backendDay = mapDayToBackendFormat(dayOfWeek)
                        val weeklyPdf = planilha.weekly_pdfs?.find { it.weekday == backendDay }
                        if (weeklyPdf != null && weeklyPdf.pdf_url != null) {
                            val row = TableRow(this@DayDetailsScreen)
                            val textView = TextView(this@DayDetailsScreen).apply {
                                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                                text = "PDF disponível para $dayOfWeek.\nClique aqui para visualizar: ${weeklyPdf.pdf_url}"
                                textSize = 14f
                                setTextColor(0xFFFFFFFF.toInt())
                                gravity = android.view.Gravity.CENTER
                                setPadding(8, 8, 8, 8)
                                setOnClickListener {
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(weeklyPdf.pdf_url))
                                    startActivity(intent)
                                }
                            }
                            row.addView(textView)
                            detailsTable.addView(row)
                            return
                        }

                        when (dataType) {
                            "TRAINING" -> {
                                // Cabeçalho da tabela
                                val headerRow = TableRow(this@DayDetailsScreen).apply {
                                    setBackgroundColor(0xFF2D2D2D.toInt())
                                    setPadding(8, 8, 8, 8)
                                }
                                listOf("Exercício", "Séries", "Repetições", "Vídeo").forEach { header ->
                                    val textView = TextView(this@DayDetailsScreen).apply {
                                        layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                                        text = header
                                        textSize = 14f
                                        setTextColor(0xFFCEAC5E.toInt())
                                        gravity = android.view.Gravity.CENTER
                                        setPadding(8, 8, 8, 8)
                                    }
                                    headerRow.addView(textView)
                                }
                                detailsTable.addView(headerRow)

                                // Dados de treinos
                                val trainings = planilha.trainings?.filter { it.weekday == backendDay } ?: emptyList()
                                if (trainings.isEmpty()) {
                                    val row = TableRow(this@DayDetailsScreen)
                                    val textView = TextView(this@DayDetailsScreen).apply {
                                        layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                                        text = "Nenhum treino disponível para $dayOfWeek."
                                        textSize = 14f
                                        setTextColor(0xFFFFFFFF.toInt())
                                        gravity = android.view.Gravity.CENTER
                                        setPadding(8, 8, 8, 8)
                                    }
                                    row.addView(textView)
                                    detailsTable.addView(row)
                                } else {
                                    trainings.forEach { training ->
                                        val row = TableRow(this@DayDetailsScreen).apply {
                                            setPadding(8, 8, 8, 8)
                                        }
                                        listOf(
                                            training.exercise_name ?: "Não especificado",
                                            training.serie_amount ?: "N/A",
                                            training.repeat_amount ?: "N/A",
                                            if (training.video.isNullOrEmpty()) "N/A" else "Ver vídeo"
                                        ).forEachIndexed { index, value ->
                                            val textView = TextView(this@DayDetailsScreen).apply {
                                                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                                                text = value
                                                textSize = 14f
                                                setTextColor(0xFFFFFFFF.toInt())
                                                gravity = android.view.Gravity.CENTER
                                                setPadding(8, 8, 8, 8)
                                                if (index == 3 && !training.video.isNullOrEmpty()) {
                                                    setOnClickListener {
                                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(training.video))
                                                        startActivity(intent)
                                                    }
                                                }
                                            }
                                            row.addView(textView)
                                        }
                                        detailsTable.addView(row)
                                    }
                                }
                            }
                            "DIET" -> {
                                // Cabeçalho da tabela
                                val headerRow = TableRow(this@DayDetailsScreen).apply {
                                    setBackgroundColor(0xFF2D2D2D.toInt())
                                    setPadding(8, 8, 8, 8)
                                }
                                listOf("Refeição", "Itens").forEach { header ->
                                    val textView = TextView(this@DayDetailsScreen).apply {
                                        layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                                        text = header
                                        textSize = 14f
                                        setTextColor(0xFFCEAC5E.toInt())
                                        gravity = android.view.Gravity.CENTER
                                        setPadding(8, 8, 8, 8)
                                    }
                                    headerRow.addView(textView)
                                }
                                detailsTable.addView(headerRow)

                                // Dados de refeições
                                val meals = planilha.meals?.filter { it.weekday == backendDay } ?: emptyList()
                                if (meals.isEmpty()) {
                                    val row = TableRow(this@DayDetailsScreen)
                                    val textView = TextView(this@DayDetailsScreen).apply {
                                        layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                                        text = "Nenhuma refeição disponível para $dayOfWeek."
                                        textSize = 14f
                                        setTextColor(0xFFFFFFFF.toInt())
                                        gravity = android.view.Gravity.CENTER
                                        setPadding(8, 8, 8, 8)
                                    }
                                    row.addView(textView)
                                    detailsTable.addView(row)
                                } else {
                                    meals.forEach { meal ->
                                        val row = TableRow(this@DayDetailsScreen).apply {
                                            setPadding(8, 8, 8, 8)
                                        }
                                        listOf(
                                            meal.meal_type ?: "Não especificada",
                                            if (meal.comidas.isNullOrEmpty()) "Nenhum item disponível"
                                            else meal.comidas.joinToString(", ") { comida ->
                                                "${comida.name ?: "Item desconhecido"} (${comida.amount ?: "N/A"})"
                                            }
                                        ).forEach { value ->
                                            val textView = TextView(this@DayDetailsScreen).apply {
                                                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                                                text = value
                                                textSize = 14f
                                                setTextColor(0xFFFFFFFF.toInt())
                                                gravity = android.view.Gravity.CENTER
                                                setPadding(8, 8, 8, 8)
                                            }
                                            row.addView(textView)
                                        }
                                        detailsTable.addView(row)
                                    }
                                }
                            }
                            else -> {
                                val row = TableRow(this@DayDetailsScreen)
                                val textView = TextView(this@DayDetailsScreen).apply {
                                    layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                                    text = "Tipo de dados inválido."
                                    textSize = 14f
                                    setTextColor(0xFFFFFFFF.toInt())
                                    gravity = android.view.Gravity.CENTER
                                    setPadding(8, 8, 8, 8)
                                }
                                row.addView(textView)
                                detailsTable.addView(row)
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