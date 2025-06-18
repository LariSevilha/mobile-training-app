package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlanilhaScreen : ComponentActivity() {

    private lateinit var planilhaTitle: TextView
    private lateinit var planilhaRecyclerView: RecyclerView
    private lateinit var adapter: PlanilhaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planilha)

        initializeViews()
        setupClickListeners()

        val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("api_key", null)
        val deviceId = sharedPrefs.getString("device_id", null)
        Log.d("PlanilhaScreen", "Credenciais: apiKey=$apiKey, deviceId=$deviceId")

        if (apiKey == null || deviceId == null) {
            handleMissingCredentials()
            return
        }

        val dataType = intent.getStringExtra("DATA_TYPE") ?: "ALL"
        Log.d("PlanilhaScreen", "Tipo de dados: $dataType")
        loadPlanilha(apiKey, deviceId, dataType)
    }

    private fun initializeViews() {
        planilhaTitle = findViewById(R.id.planilha_title)
        planilhaRecyclerView = findViewById(R.id.planilha_recycler_view)
        planilhaRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PlanilhaAdapter(emptyList())
        planilhaRecyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.logout_button)?.setOnClickListener {
            Log.d("PlanilhaScreen", "Botão de logout clicado")
            performLogout()
        }

        findViewById<Button>(R.id.back_button)?.setOnClickListener {
            Log.d("PlanilhaScreen", "Botão Voltar clicado")
            finish()
        }
    }

    private fun handleMissingCredentials() {
        Log.e("PlanilhaScreen", "apiKey ou deviceId está nulo")
        Toast.makeText(this, "Por favor, faça login novamente", Toast.LENGTH_SHORT).show()
        val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun loadPlanilha(apiKey: String, deviceId: String, dataType: String) {
        val authHeader = "Bearer $apiKey"
        Log.d("PlanilhaScreen", "Carregando planilha com authHeader=$authHeader, deviceId=$deviceId")

        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                Log.d("PlanilhaScreen", "Resposta recebida: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    handleSuccessfulResponse(response.body()!!, dataType)
                } else {
                    handleErrorResponse(response)
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e("PlanilhaScreen", "Falha na requisição: ${t.message}", t)
                Toast.makeText(this@PlanilhaScreen, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleSuccessfulResponse(planilha: PlanilhaResponse, dataType: String) {
        Log.d("PlanilhaScreen", "Dados da planilha: $planilha")

        if (planilha.hasError()) {
            Log.e("PlanilhaScreen", "Erro na resposta: ${planilha.error}")
            Toast.makeText(this, planilha.error, Toast.LENGTH_SHORT).show()
            return
        }

        // Configurar título
        planilhaTitle.text = when (dataType) {
            "TRAINING" -> "Treinos de ${planilha.name ?: "Usuário"}"
            "DIET" -> "Dieta de ${planilha.name ?: "Usuário"}"
            else -> "Planilha de ${planilha.name ?: "Usuário"}"
        }

        // Criar lista de itens
        val items = createPlanilhaItems(planilha, dataType)

        // Atualizar adapter
        adapter = PlanilhaAdapter(items)
        planilhaRecyclerView.adapter = adapter
        Log.d("PlanilhaScreen", "RecyclerView atualizado com ${items.size} itens")
    }

    private fun createPlanilhaItems(planilha: PlanilhaResponse, dataType: String): List<PlanilhaItem> {
        val items = mutableListOf<PlanilhaItem>()

        // Adicionar treinos
        if (dataType == "TRAINING" || dataType == "ALL") {
            addTrainingItems(planilha, items)
        }

        // Adicionar refeições
        if (dataType == "DIET" || dataType == "ALL") {
            addMealItems(planilha, items)
        }

        return items
    }

    private fun addTrainingItems(planilha: PlanilhaResponse, items: MutableList<PlanilhaItem>) {
        val trainings = planilha.getTrainingsSafe()

        if (trainings.isNotEmpty()) {
            trainings.forEach { training ->
                val details = buildTrainingDetails(training)
                items.add(PlanilhaItem(
                    title = training.getExerciseNameSafe(),
                    details = details,
                    type = "training"
                ))
            }
        } else {
            items.add(PlanilhaItem(
                title = "Treinos",
                details = "Nenhum treino cadastrado.",
                type = "training"
            ))
        }
    }

    private fun buildTrainingDetails(training: Training): String {
        val details = StringBuilder()

        // Adicionar séries e repetições
        details.append(training.getSeriesRepetitionsText())

        // Adicionar vídeo se disponível
        if (training.hasVideo()) {
            details.append(" (Vídeo disponível)")
        }

        // Adicionar descrição se disponível
        if (!training.description.isNullOrEmpty()) {
            details.append("\n${training.description}")
        }

        // Adicionar dia da semana se disponível
        if (!training.weekday.isNullOrEmpty()) {
            details.append("\nDia: ${training.weekday}")
        }

        return details.toString()
    }

    private fun addMealItems(planilha: PlanilhaResponse, items: MutableList<PlanilhaItem>) {
        val meals = planilha.getMealsSafe()

        if (meals.isNotEmpty()) {
            meals.forEach { meal ->
                val details = buildMealDetails(meal)
                items.add(PlanilhaItem(
                    title = meal.getMealTypeSafe(),
                    details = details,
                    type = "meal"
                ))
            }
        } else {
            items.add(PlanilhaItem(
                title = "Refeições",
                details = "Nenhuma refeição cadastrada.",
                type = "meal"
            ))
        }
    }

    private fun buildMealDetails(meal: Meal): String {
        val comidas = meal.getComidasSafe()

        return if (comidas.isNotEmpty()) {
            val comidasText = comidas.joinToString(", ") { comida ->
                comida.getFullDescription()
            }

            val details = StringBuilder(comidasText)

            // Adicionar dia da semana se disponível
            if (!meal.weekday.isNullOrEmpty()) {
                details.append("\nDia: ${meal.weekday}")
            }

            details.toString()
        } else {
            "Nenhuma comida cadastrada."
        }
    }

    private fun handleErrorResponse(response: Response<PlanilhaResponse>) {
        Log.e("PlanilhaScreen", "Resposta não foi bem-sucedida: ${response.code()} - ${response.message()}")

        try {
            val errorBody = response.errorBody()?.string()
            Log.e("PlanilhaScreen", "Corpo de erro: $errorBody")
        } catch (e: Exception) {
            Log.e("PlanilhaScreen", "Erro ao ler corpo de erro: ${e.message}")
        }

        Toast.makeText(this, "Erro ao carregar a planilha: ${response.message()}", Toast.LENGTH_SHORT).show()
    }

    private fun performLogout() {
        Log.d("PlanilhaScreen", "Iniciando logout")
        val sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE)
        val apiKey = sharedPreferences.getString("api_key", null)
        val deviceId = sharedPreferences.getString("device_id", null)
        Log.d("PlanilhaScreen", "apiKey: $apiKey, deviceId: $deviceId")

        if (apiKey != null && deviceId != null) {
            val authHeader = "Bearer $apiKey"
            // Corrigido: usando ResponseBody ao invés de Void
            RetrofitClient.apiService.logout(authHeader, deviceId).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("PlanilhaScreen", "Logout bem-sucedido: ${response.code()}")
                    clearSessionAndNavigateToLogin()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("PlanilhaScreen", "Falha no logout: ${t.message}")
                    clearSessionAndNavigateToLogin()
                }
            })
        } else {
            Log.w("PlanilhaScreen", "Nenhuma api_key ou device_id encontrado")
            clearSessionAndNavigateToLogin()
        }
    }

    private fun clearSessionAndNavigateToLogin() {
        Log.d("PlanilhaScreen", "Limpando sessão e navegando para login")
        val sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}