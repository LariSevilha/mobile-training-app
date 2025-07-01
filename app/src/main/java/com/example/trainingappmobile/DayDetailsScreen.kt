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

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK") ?: "Segunda-feira"
        val dataType = intent.getStringExtra("DATA_TYPE") ?: "TRAINING"

        val dayTitle = findViewById<TextView>(R.id.day_title)
        dayTitle.text = "Detalhes de $dayOfWeek"

        val detailsTable = findViewById<TableLayout>(R.id.details_text)

        val backButton = findViewById<LinearLayout>(R.id.back_button)
        backButton.setOnClickListener { finish() }

        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("auth_token", null)
        val deviceId = sharedPrefs.getString("device_id", null)

        Log.d("DayDetailsScreen", "apiKey: $apiKey, deviceId: $deviceId")

        if (apiKey == null || deviceId == null) {
            Log.e("DayDetailsScreen", "apiKey or deviceId is null")
            Toast.makeText(this, "Por favor, faça login novamente", Toast.LENGTH_SHORT).show()
            sharedPrefs.edit().clear().apply()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

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
        Log.d("DayDetailsScreen", "Loading data for $dayOfWeek with type $dataType")
        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d("DayDetailsScreen", "Data received: $planilha")
                    if (!planilha.hasError()) {
                        detailsTable.removeAllViews()

                        val backendDay = mapDayToBackendFormat(dayOfWeek)
                        val weeklyPdf = planilha.getWeeklyPdfsSafe().find { it.weekday == backendDay }
                        if (weeklyPdf != null && weeklyPdf.pdfUrl != null) {
                            val row = TableRow(this@DayDetailsScreen)
                            val textView = TextView(this@DayDetailsScreen).apply {
                                layoutParams = TableRow.LayoutParams(
                                    TableRow.LayoutParams.MATCH_PARENT,
                                    TableRow.LayoutParams.WRAP_CONTENT
                                )
                                text = "PDF disponível para $dayOfWeek.\nClique aqui para visualizar: ${weeklyPdf.pdfUrl}"
                                textSize = 14f
                                setTextColor(0xFFFFFFFF.toInt())
                                gravity = android.view.Gravity.CENTER
                                setPadding(8, 8, 8, 8)
                                setOnClickListener {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(weeklyPdf.pdfUrl))
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(this@DayDetailsScreen, "Não foi possível abrir o PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            row.addView(textView)
                            detailsTable.addView(row)
                            return
                        }

                        when (dataType) {
                            "TRAINING" -> {
                                val headerRow = TableRow(this@DayDetailsScreen).apply {
                                    setBackgroundColor(0xFF2D2D2D.toInt())
                                    setPadding(8, 8, 8, 8)
                                    layoutParams = TableRow.LayoutParams(
                                        TableRow.LayoutParams.MATCH_PARENT,
                                        TableRow.LayoutParams.WRAP_CONTENT
                                    )
                                }
                                listOf("Exercício", "Séries", "Repetições", "Vídeo").forEach { header ->
                                    val textView = TextView(this@DayDetailsScreen).apply {
                                        layoutParams = TableRow.LayoutParams(
                                            0,
                                            TableRow.LayoutParams.WRAP_CONTENT,
                                            1f
                                        )
                                        text = header
                                        textSize = 14f
                                        setTextColor(0xFFCEAC5E.toInt())
                                        gravity = android.view.Gravity.CENTER
                                        setPadding(8, 8, 8, 8)
                                    }
                                    headerRow.addView(textView)
                                }
                                detailsTable.addView(headerRow)

                                val trainings = planilha.getTrainingsSafe().filter { it.weekday == backendDay }
                                if (trainings.isEmpty()) {
                                    val row = TableRow(this@DayDetailsScreen).apply {
                                        layoutParams = TableRow.LayoutParams(
                                            TableRow.LayoutParams.MATCH_PARENT,
                                            TableRow.LayoutParams.WRAP_CONTENT
                                        )
                                    }
                                    val textView = TextView(this@DayDetailsScreen).apply {
                                        layoutParams = TableRow.LayoutParams(
                                            TableRow.LayoutParams.MATCH_PARENT,
                                            TableRow.LayoutParams.WRAP_CONTENT
                                        )
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
                                            layoutParams = TableRow.LayoutParams(
                                                TableRow.LayoutParams.MATCH_PARENT,
                                                TableRow.LayoutParams.WRAP_CONTENT
                                            )
                                        }

                                        val columnValues = listOf(
                                            training.exerciseName ?: "Não especificado",
                                            training.serieAmount?.toString() ?: "N/A",
                                            training.repeatAmount?.toString() ?: "N/A",
                                            if (training.video.isNullOrEmpty()) "N/A" else "Ver vídeo"
                                        )

                                        columnValues.forEachIndexed { index, value ->
                                            val textView = TextView(this@DayDetailsScreen).apply {
                                                layoutParams = TableRow.LayoutParams(
                                                    0,
                                                    TableRow.LayoutParams.WRAP_CONTENT,
                                                    1f
                                                )
                                                text = value
                                                textSize = 14f
                                                setTextColor(0xFFFFFFFF.toInt())
                                                gravity = android.view.Gravity.CENTER
                                                setPadding(8, 8, 8, 8)

                                                when (index) {
                                                    0 -> {
                                                        setOnClickListener {
                                                            openExerciseDetail(training)
                                                        }
                                                        setTextColor(0xFFCEAC5E.toInt())
                                                    }
                                                    3 -> {
                                                        if (!training.video.isNullOrEmpty()) {
                                                            setOnClickListener {
                                                                try {
                                                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(training.video))
                                                                    startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(this@DayDetailsScreen, "Não foi possível abrir o vídeo", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
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
                                val headerRow = TableRow(this@DayDetailsScreen).apply {
                                    setBackgroundColor(0xFF2D2D2D.toInt())
                                    setPadding(8, 8, 8, 8)
                                    layoutParams = TableRow.LayoutParams(
                                        TableRow.LayoutParams.MATCH_PARENT,
                                        TableRow.LayoutParams.WRAP_CONTENT
                                    )
                                }
                                listOf("Refeição", "Itens").forEach { header ->
                                    val textView = TextView(this@DayDetailsScreen).apply {
                                        layoutParams = TableRow.LayoutParams(
                                            0,
                                            TableRow.LayoutParams.WRAP_CONTENT,
                                            1f
                                        )
                                        text = header
                                        textSize = 14f
                                        setTextColor(0xFFCEAC5E.toInt())
                                        gravity = android.view.Gravity.CENTER
                                        setPadding(8, 8, 8, 8)
                                    }
                                    headerRow.addView(textView)
                                }
                                detailsTable.addView(headerRow)

                                val meals = planilha.getMealsSafe().filter { it.weekday == backendDay }
                                if (meals.isEmpty()) {
                                    val row = TableRow(this@DayDetailsScreen).apply {
                                        layoutParams = TableRow.LayoutParams(
                                            TableRow.LayoutParams.MATCH_PARENT,
                                            TableRow.LayoutParams.WRAP_CONTENT
                                        )
                                    }
                                    val textView = TextView(this@DayDetailsScreen).apply {
                                        layoutParams = TableRow.LayoutParams(
                                            TableRow.LayoutParams.MATCH_PARENT,
                                            TableRow.LayoutParams.WRAP_CONTENT
                                        )
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
                                            layoutParams = TableRow.LayoutParams(
                                                TableRow.LayoutParams.MATCH_PARENT,
                                                TableRow.LayoutParams.WRAP_CONTENT
                                            )
                                        }
                                        val comidaText = if (meal.comidas.isNullOrEmpty()) {
                                            "Nenhum item disponível"
                                        } else {
                                            meal.comidas.joinToString(", ") { comida ->
                                                "${comida.name ?: "Item desconhecido"} (${comida.amount ?: "N/A"})"
                                            }
                                        }
                                        listOf(
                                            meal.mealType ?: "Não especificada",
                                            comidaText
                                        ).forEach { value ->
                                            val textView = TextView(this@DayDetailsScreen).apply {
                                                layoutParams = TableRow.LayoutParams(
                                                    0,
                                                    TableRow.LayoutParams.WRAP_CONTENT,
                                                    1f
                                                )
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
                                val row = TableRow(this@DayDetailsScreen).apply {
                                    layoutParams = TableRow.LayoutParams(
                                        TableRow.LayoutParams.MATCH_PARENT,
                                        TableRow.LayoutParams.WRAP_CONTENT
                                    )
                                }
                                val textView = TextView(this@DayDetailsScreen).apply {
                                    layoutParams = TableRow.LayoutParams(
                                        TableRow.LayoutParams.MATCH_PARENT,
                                        TableRow.LayoutParams.WRAP_CONTENT
                                    )
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
                        Toast.makeText(this@DayDetailsScreen, planilha.error, Toast.LENGTH_SHORT).show()
                        if (planilha.error == "Conta expirada. Entre em contato com o administrador.") {
                            navigateToLogin()
                        } else {
                            finish()
                        }
                    }
                } else {
                    Toast.makeText(this@DayDetailsScreen, "Erro ao carregar dados: ${response.message()}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e("DayDetailsScreen", "Request failed: ${t.message}", t)
                Toast.makeText(this@DayDetailsScreen, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun openExerciseDetail(training: Any) {
        val intent = Intent(this, ExerciseDetailActivity::class.java).apply {
            putExtra("EXERCISE_NAME", getExerciseName(training))
            putExtra("EXERCISE_DESCRIPTION", getExerciseDescription(training))
            putExtra("EXERCISE_VIDEO", getExerciseVideo(training))
            putExtra("EXERCISE_PHOTOS", getExercisePhotos(training))
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("DayDetailsScreen", "Failed to start ExerciseDetailActivity: ${e.message}", e)
            Toast.makeText(this, "Erro ao abrir detalhes do exercício", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getExerciseName(training: Any): String {
        return try {
            val field = training.javaClass.getDeclaredField("exerciseName")
            field.isAccessible = true
            field.get(training) as? String ?: "Nome não disponível"
        } catch (e: Exception) {
            Log.w("DayDetailsScreen", "Failed to get exerciseName: ${e.message}")
            "Nome não disponível"
        }
    }

    private fun getExerciseDescription(training: Any): String {
        return try {
            val field = training.javaClass.getDeclaredField("description")
            field.isAccessible = true
            field.get(training) as? String ?: "Descrição não disponível"
        } catch (e: Exception) {
            try {
                val field = training.javaClass.getDeclaredField("howToDo")
                field.isAccessible = true
                field.get(training) as? String ?: "Descrição não disponível"
            } catch (e2: Exception) {
                Log.w("DayDetailsScreen", "Failed to get description: ${e.message}")
                "Descrição não disponível"
            }
        }
    }

    private fun getExerciseVideo(training: Any): String? {
        return try {
            val field = training.javaClass.getDeclaredField("video")
            field.isAccessible = true
            field.get(training) as? String
        } catch (e: Exception) {
            Log.w("DayDetailsScreen", "Failed to get video: ${e.message}")
            null
        }
    }

    private fun getExercisePhotos(training: Any): Array<String>? {
        return try {
            val field = training.javaClass.getDeclaredField("photos")
            field.isAccessible = true
            val photos = field.get(training)
            when (photos) {
                is Array<*> -> photos.map { it.toString() }.toTypedArray()
                is List<*> -> photos.map { it.toString() }.toTypedArray()
                else -> null
            }
        } catch (e: Exception) {
            Log.w("DayDetailsScreen", "Failed to get photos: ${e.message}")
            null
        }
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
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
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