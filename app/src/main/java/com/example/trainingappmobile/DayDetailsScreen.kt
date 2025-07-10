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
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DayDetailsScreen : ComponentActivity() {

    private lateinit var exerciseTitle: TextView
    private lateinit var seriesCount: TextView
    private lateinit var repetitionsCount: TextView
    private lateinit var weekdayText: TextView
    private lateinit var howToDoText: TextView
    private lateinit var exerciseImage: ImageView
    private lateinit var noImageText: TextView
    private lateinit var videoContainer: LinearLayout
    private lateinit var videoLink: LinearLayout
    private var backButton: LinearLayout? = null

    companion object {
        private const val TAG = "DayDetailsScreen"

        // Mapeamento dos dias da semana para diferentes formatos
        private val dayMappings = mapOf(
            "segunda" to listOf("segunda", "segunda-feira", "seg", "monday", "mon"),
            "terça" to listOf("terça", "terça-feira", "ter", "tuesday", "tue"),
            "quarta" to listOf("quarta", "quarta-feira", "qua", "wednesday", "wed"),
            "quinta" to listOf("quinta", "quinta-feira", "qui", "thursday", "thu"),
            "sexta" to listOf("sexta", "sexta-feira", "sex", "friday", "fri"),
            "sábado" to listOf("sábado", "sabado", "sab", "saturday", "sat"),
            "domingo" to listOf("domingo", "dom", "sunday", "sun")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_details)

        // Inicializar views
        try {
            exerciseTitle = findViewById(R.id.exercise_title)
            seriesCount = findViewById(R.id.series_count)
            repetitionsCount = findViewById(R.id.repetitions_count)
            weekdayText = findViewById(R.id.weekday_text)
            howToDoText = findViewById(R.id.how_to_do_text)
            exerciseImage = findViewById(R.id.exercise_image)
            noImageText = findViewById(R.id.no_image_text)
            videoContainer = findViewById(R.id.video_container)
            videoLink = findViewById(R.id.video_link)
            backButton = findViewById(R.id.back_button_detail)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar views: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar a tela", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar botão de voltar
        backButton?.setOnClickListener {
            Log.d(TAG, "Botão Voltar clicado")
            finish()
        } ?: run {
            Log.w(TAG, "back_button_detail não encontrado no layout")
        }

        // Configurar clique no link de vídeo
        videoLink.setOnClickListener {
            Log.d(TAG, "Link de vídeo clicado")
            // Será configurado após carregar os dados
        }

        // Recuperar dados do Intent
        val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK") ?: "Dia não especificado"
        val dataType = intent.getStringExtra("DATA_TYPE") ?: "TRAINING"
        Log.d(TAG, "Dados recebidos - Dia: $dayOfWeek, Tipo: $dataType")

        // Configurar título inicial
        exerciseTitle.text = when (dataType) {
            "TRAINING" -> "Treino de $dayOfWeek"
            "DIET" -> "Dieta de $dayOfWeek"
            else -> "Detalhes de $dayOfWeek"
        }

        // Carregar dados
        loadDayDetails(dayOfWeek, dataType)
    }

    private fun loadDayDetails(dayOfWeek: String, dataType: String) {
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
        Log.d(TAG, "Carregando detalhes do dia com authHeader=$authHeader, deviceId=$deviceId")

        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                Log.d(TAG, "Resposta recebida: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d(TAG, "Dados da planilha recebidos: $planilha")

                    if (planilha.hasError()) {
                        Log.e(TAG, "Erro na resposta: ${planilha.error}")
                        Toast.makeText(this@DayDetailsScreen, planilha.error, Toast.LENGTH_SHORT).show()
                        updateNoDataUI("Erro ao carregar dados: ${planilha.error}")
                        return
                    }

                    if (dataType == "TRAINING") {
                        handleTrainingData(planilha, dayOfWeek)
                    } else if (dataType == "DIET") {
                        handleDietData(planilha, dayOfWeek)
                    }
                } else {
                    Log.e(TAG, "Resposta não foi bem-sucedida: ${response.code()} - ${response.message()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error body: $errorBody")
                    Toast.makeText(this@DayDetailsScreen, "Erro ao carregar detalhes", Toast.LENGTH_SHORT).show()
                    updateNoDataUI("Erro ao carregar dados")
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e(TAG, "Falha na requisição: ${t.message}", t)
                Toast.makeText(this@DayDetailsScreen, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
                updateNoDataUI("Falha na conexão")
            }
        })
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

        Log.d(TAG, "Comparando dias: '$normalizedTarget' vs '$normalizedTraining'")

        // Verificação exata
        if (normalizedTarget == normalizedTraining) {
            Log.d(TAG, "Match exato encontrado")
            return true
        }

        // Verificação usando mapeamentos
        for ((key, variants) in dayMappings) {
            if (variants.contains(normalizedTarget) && variants.contains(normalizedTraining)) {
                Log.d(TAG, "Match por mapeamento encontrado para $key")
                return true
            }
        }

        // Verificação por substring (mais restritiva)
        val targetWords = normalizedTarget.split(" ", "-")
        val trainingWords = normalizedTraining.split(" ", "-")

        for (targetWord in targetWords) {
            if (targetWord.length >= 3) { // Só considera palavras com 3+ caracteres
                for (trainingWord in trainingWords) {
                    if (trainingWord.length >= 3 &&
                        (targetWord.contains(trainingWord) || trainingWord.contains(targetWord))) {
                        Log.d(TAG, "Match por substring encontrado: '$targetWord' e '$trainingWord'")
                        return true
                    }
                }
            }
        }

        Log.d(TAG, "Nenhum match encontrado")
        return false
    }

    private fun handleTrainingData(planilha: PlanilhaResponse, dayOfWeek: String) {
        val trainings = planilha.getTrainingsSafe()
        Log.d(TAG, "Total de treinos: ${trainings.size}")

        // Log de todos os treinos para debug
        trainings.forEachIndexed { index, training ->
            Log.d(TAG, "Treino $index: weekday='${training.weekday}', exercise='${training.exerciseName}'")
        }

        // Buscar treino específico para o dia
        val matchingTrainings = trainings.filter { training ->
            training.weekday?.let { weekday ->
                isDayMatch(dayOfWeek, weekday)
            } ?: false
        }

        Log.d(TAG, "Treinos encontrados para '$dayOfWeek': ${matchingTrainings.size}")

        val training = when {
            matchingTrainings.isNotEmpty() -> {
                Log.d(TAG, "Usando treino específico para '$dayOfWeek'")
                matchingTrainings.first()
            }
            trainings.isNotEmpty() -> {
                Log.w(TAG, "Nenhum treino específico encontrado para '$dayOfWeek'. Disponíveis: ${trainings.map { it.weekday }}")
                updateNoDataUI("Nenhum treino encontrado para $dayOfWeek")
                return
            }
            else -> {
                Log.w(TAG, "Nenhum treino encontrado na planilha")
                updateNoDataUI("Nenhum treino cadastrado")
                return
            }
        }

        Log.d(TAG, "Treino selecionado: ${training.exerciseName} para ${training.weekday}")
        updateTrainingUI(training)
    }

    private fun handleDietData(planilha: PlanilhaResponse, dayOfWeek: String) {
        val meals = planilha.getMealsSafe()
        Log.d(TAG, "Total de refeições: ${meals.size}")

        // Log de todas as refeições para debug
        meals.forEachIndexed { index, meal ->
            Log.d(TAG, "Refeição $index: weekday='${meal.weekday}', type='${meal.mealType}'")
        }

        // Buscar refeições específicas para o dia
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

    private fun updateTrainingUI(training: Training) {
        Log.d(TAG, "Atualizando UI com treino: ${training.exerciseName}")

        exerciseTitle.text = training.getExerciseNameSafe()
        seriesCount.text = training.serieAmount ?: "0"
        repetitionsCount.text = training.repeatAmount ?: "0"
        weekdayText.text = training.weekday ?: "Dia não especificado"
        howToDoText.text = training.description ?: "Nenhuma instrução disponível"

        // Mostrar campos de séries e repetições
        seriesCount.visibility = View.VISIBLE
        repetitionsCount.visibility = View.VISIBLE
        // Tornar os containers visíveis
        try {
            seriesCount.parent?.let { parent ->
                (parent as? View)?.visibility = View.VISIBLE
            }
            repetitionsCount.parent?.let { parent ->
                (parent as? View)?.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao mostrar containers de séries/repetições: ${e.message}")
        }

        // Configurar imagem
        val photoUrls = training.getPhotoUrlsSafe()
        Log.d(TAG, "URLs de fotos: $photoUrls")

        if (photoUrls.isNotEmpty()) {
            val photoUrl = photoUrls.first()
            exerciseImage.visibility = View.VISIBLE
            noImageText.visibility = View.GONE

            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_error)
                .into(exerciseImage)
        } else {
            exerciseImage.visibility = View.GONE
            noImageText.visibility = View.VISIBLE
            noImageText.text = "Nenhuma imagem disponível"
        }

        // Configurar vídeo
        if (training.hasVideo()) {
            Log.d(TAG, "Vídeo disponível: ${training.video}")
            videoContainer.visibility = View.VISIBLE
            videoLink.setOnClickListener {
                Log.d(TAG, "Abrindo vídeo: ${training.video}")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(training.video))
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao abrir vídeo: ${e.message}", e)
                    Toast.makeText(this, "Erro ao abrir vídeo", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d(TAG, "Nenhum vídeo disponível")
            videoContainer.visibility = View.GONE
        }
    }

    private fun updateDietUI(meal: Meal) {
        Log.d(TAG, "Atualizando UI com refeição: ${meal.mealType}")

        exerciseTitle.text = meal.getMealTypeSafe()
        weekdayText.text = meal.weekday ?: "Dia não especificado"

        // Ocultar campos de séries e repetições para dieta
        seriesCount.visibility = View.GONE
        repetitionsCount.visibility = View.GONE
        // Ocultar os containers dos campos também
        try {
            seriesCount.parent?.let { parent ->
                (parent as? View)?.visibility = View.GONE
            }
            repetitionsCount.parent?.let { parent ->
                (parent as? View)?.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao ocultar container de séries/repetições: ${e.message}")
        }

        // Configurar descrição com comidas
        val comidas = meal.getComidasSafe()
        Log.d(TAG, "Comidas: $comidas")

        val descricao = if (comidas.isNotEmpty()) {
            comidas.joinToString("\n") { it.getFullDescription() }
        } else {
            "Nenhuma comida cadastrada"
        }
        howToDoText.text = descricao

        // Dieta não tem imagem ou vídeo
        exerciseImage.visibility = View.GONE
        noImageText.visibility = View.VISIBLE
        noImageText.text = "Nenhuma imagem disponível para dieta"
        videoContainer.visibility = View.GONE
    }

    private fun updateNoDataUI(message: String) {
        Log.d(TAG, "Atualizando UI com mensagem de erro: $message")

        exerciseTitle.text = "Dados Indisponíveis"
        seriesCount.text = "-"
        repetitionsCount.text = "-"
        weekdayText.text = "-"
        howToDoText.text = message
        exerciseImage.visibility = View.GONE
        noImageText.visibility = View.VISIBLE
        noImageText.text = "Nenhuma imagem disponível"
        videoContainer.visibility = View.GONE
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed chamado")
        super.onBackPressed()
        finish()
    }
}