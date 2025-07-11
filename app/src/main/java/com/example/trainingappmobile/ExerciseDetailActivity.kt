package com.example.trainingappmobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ExerciseDetailActivity : ComponentActivity() {

    private lateinit var weekdayText: TextView
    private lateinit var exercisesContainer: LinearLayout
    private lateinit var backButton: LinearLayout

    companion object {
        private const val TAG = "ExerciseDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_details)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Initialize views
        try {
            weekdayText = findViewById(R.id.weekday_text) ?: throw IllegalStateException("weekday_text not found")
            exercisesContainer = findViewById(R.id.exercises_container) ?: throw IllegalStateException("exercises_container not found")
            backButton = findViewById(R.id.back_button_detail) ?: throw IllegalStateException("back_button_detail not found")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar a tela", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up back button
        backButton.setOnClickListener {
            Log.d(TAG, "Botão Voltar clicado")
            finish()
            overridePendingTransition(android.R.anim.fade_out, android.R.anim.fade_in)
        }

        // Retrieve day of week from Intent
        val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK") ?: "Dia não especificado"
        Log.d(TAG, "Dia recebido: $dayOfWeek")

        // Set initial weekday in Portuguese
        weekdayText.text = translateDayToPortuguese(dayOfWeek)

        // Load training details
        loadTrainingDetails(dayOfWeek)
    }

    private fun translateDayToPortuguese(day: String): String {
        return when (day.lowercase().trim()) {
            "monday", "mon", "segunda", "segunda-feira", "seg", "segundafeira" -> "Segunda-feira"
            "tuesday", "tue", "terça", "terça-feira", "ter", "tercafeira", "terçafeira" -> "Terça-feira"
            "wednesday", "wed", "quarta", "quarta-feira", "qua", "quartafeira" -> "Quarta-feira"
            "thursday", "thu", "quinta", "quinta-feira", "qui", "quintafeira" -> "Quinta-feira"
            "friday", "fri", "sexta", "sexta-feira", "sex", "sextafeira" -> "Sexta-feira"
            "saturday", "sat", "sábado", "sabado", "sab", "sabadofeira", "sábadofeira" -> "Sábado"
            "sunday", "sun", "domingo", "dom", "domingofeira" -> "Domingo"
            else -> day.replaceFirstChar { it.uppercaseChar() }
        }
    }

    private fun loadTrainingDetails(dayOfWeek: String) {
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
        Log.d(TAG, "Carregando detalhes do treino com authHeader=$authHeader, deviceId=$deviceId")

        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                Log.d(TAG, "Resposta recebida: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d(TAG, "Dados da planilha recebidos: $planilha")

                    if (planilha.hasError()) {
                        Log.e(TAG, "Erro na resposta: ${planilha.error}")
                        Toast.makeText(this@ExerciseDetailActivity, planilha.error, Toast.LENGTH_SHORT).show()
                        updateNoDataUI("Erro ao carregar dados: ${planilha.error}")
                        return
                    }

                    handleTrainingData(planilha, dayOfWeek)
                } else {
                    Log.e(TAG, "Resposta não foi bem-sucedida: ${response.code()} - ${response.message()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Corpo do erro: $errorBody")
                    Toast.makeText(this@ExerciseDetailActivity, "Erro ao carregar detalhes", Toast.LENGTH_SHORT).show()
                    updateNoDataUI("Erro ao carregar dados")
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e(TAG, "Falha na requisição: ${t.message}", t)
                Toast.makeText(this@ExerciseDetailActivity, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
                updateNoDataUI("Falha na conexão")
            }
        })
    }

    private fun handleTrainingData(planilha: PlanilhaResponse, dayOfWeek: String) {
        val trainings = planilha.getTrainingsSafe()
        Log.d(TAG, "Total de treinos: ${trainings.size}")

        trainings.forEachIndexed { index, training ->
            Log.d(TAG, "Treino $index: weekday='${training.weekday}', exerciseName='${training.exerciseName}', video='${training.video}', photoUrls='${training.photoUrls}'")
        }

        val matchingTrainings = trainings.filter { training ->
            training.weekday?.let { weekday ->
                isDayMatch(dayOfWeek, weekday)
            } ?: false
        }

        Log.d(TAG, "Treinos encontrados para '$dayOfWeek': ${matchingTrainings.size}")

        when {
            matchingTrainings.isNotEmpty() -> {
                Log.d(TAG, "Exibindo ${matchingTrainings.size} treinos para '$dayOfWeek'")
                updateTrainingUI(matchingTrainings, dayOfWeek)
            }
            trainings.isNotEmpty() -> {
                Log.w(TAG, "Nenhum treino específico encontrado para '$dayOfWeek'. Disponíveis: ${trainings.map { it.weekday }}")
                updateNoDataUI("Nenhum treino encontrado para ${translateDayToPortuguese(dayOfWeek)}")
            }
            else -> {
                Log.w(TAG, "Nenhum treino encontrado na planilha")
                updateNoDataUI("Nenhum treino cadastrado")
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

    private fun isDayMatch(targetDay: String, trainingDay: String): Boolean {
        val normalizedTarget = normalizeDayName(targetDay)
        val normalizedTraining = normalizeDayName(trainingDay)

        Log.d(TAG, "Comparando dias: '$normalizedTarget' vs '$normalizedTraining'")

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
        val match = targetVariants.contains(normalizedTraining)

        if (match) {
            Log.d(TAG, "Correspondência encontrada para '$normalizedTarget' em variantes: $targetVariants")
        } else {
            Log.w(TAG, "Nenhuma correspondência para '$normalizedTarget' vs '$normalizedTraining'. Variantes alvo: $targetVariants")
        }

        return match
    }

    private fun updateTrainingUI(trainings: List<Training>, dayOfWeek: String) {
        Log.d(TAG, "Atualizando UI com ${trainings.size} treinos para '$dayOfWeek'")

        // Clear existing views in the container
        exercisesContainer.removeAllViews()

        // Set weekday in Portuguese
        weekdayText.text = translateDayToPortuguese(dayOfWeek)

        if (trainings.isEmpty()) {
            updateNoDataUI("Nenhum treino cadastrado")
            return
        }

        // Add a card for each training
        trainings.forEach { training ->
            // Inflate a new card layout
            val cardView = LayoutInflater.from(this).inflate(R.layout.exercise_card, null)

            // Find views in the card
            val exerciseNameText = cardView.findViewById<TextView>(R.id.exercise_name_text)
            val seriesText = cardView.findViewById<TextView>(R.id.series_text)
            val repetitionsText = cardView.findViewById<TextView>(R.id.repetitions_text)
            val noVideoText = cardView.findViewById<TextView>(R.id.no_video_text)
            val videoLinkContainer = cardView.findViewById<LinearLayout>(R.id.video_link_container)
            val videoLinkText = cardView.findViewById<TextView>(R.id.video_link_text)

            // Set training data
            exerciseNameText.text = training.getExerciseNameSafe()
            seriesText.text = training.getSeriesRepetitionsText()

            // Handle video link
            Log.d(TAG, "Processando vídeo para ${training.getExerciseNameSafe()}: '${training.video}'")
            if (training.hasVideo()) {
                videoLinkContainer.visibility = View.VISIBLE
                noVideoText.visibility = View.GONE
                videoLinkText.setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(training.video))
                        startActivity(intent)
                        Log.d(TAG, "Abrindo vídeo: ${training.video}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao abrir vídeo: ${e.message}", e)
                        Toast.makeText(this, "Não foi possível abrir o vídeo: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                videoLinkContainer.visibility = View.GONE
                noVideoText.visibility = View.VISIBLE
                Log.w(TAG, "Vídeo não disponível para ${training.getExerciseNameSafe()}: '${training.video}'")
            }

            // Add card to container
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.bottomMargin = (16 * resources.displayMetrics.density).toInt() // Convert 16dp to pixels
            exercisesContainer.addView(cardView, layoutParams)

            Log.d(TAG, "Card adicionado para treino: ${training.getExerciseNameSafe()}")
        }
    }

    private fun updateNoDataUI(message: String) {
        Log.d(TAG, "Atualizando UI com mensagem de erro: $message")

        exercisesContainer.removeAllViews()
        val errorView = LayoutInflater.from(this).inflate(R.layout.exercise_card, null)
        val exerciseNameText = errorView.findViewById<TextView>(R.id.exercise_name_text)
        val seriesText = errorView.findViewById<TextView>(R.id.series_text)
        val repetitionsText = errorView.findViewById<TextView>(R.id.repetitions_text)
        val noVideoText = errorView.findViewById<TextView>(R.id.no_video_text)
        val videoLinkContainer = errorView.findViewById<LinearLayout>(R.id.video_link_container)

        exerciseNameText.text = "Dados Indisponíveis"
        seriesText.text = message
        repetitionsText.text = ""
        noVideoText.visibility = View.VISIBLE
        videoLinkContainer.visibility = View.GONE
        weekdayText.text = "-"

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.bottomMargin = (16 * resources.displayMetrics.density).toInt()
        exercisesContainer.addView(errorView, layoutParams)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_out, android.R.anim.fade_in)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart chamado")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume chamado")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause chamado")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop chamado")
    }
}