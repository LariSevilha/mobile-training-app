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

    companion object {
        private const val TAG = "PlanilhaScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planilha)

        initializeViews()
        setupClickListeners()

        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("auth_token", null)
        val deviceId = sharedPrefs.getString("device_id", null)
        Log.d(TAG, "Credenciais: apiKey=$apiKey, deviceId=$deviceId")

        if (apiKey == null || deviceId == null) {
            handleMissingCredentials()
            return
        }

        val dataType = intent.getStringExtra("DATA_TYPE") ?: "ALL"
        Log.d(TAG, "Tipo de dados: $dataType")
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
            Log.d(TAG, "Botão de logout clicado")
            performLogout()
        }

        findViewById<Button>(R.id.back_button)?.setOnClickListener {
            Log.d(TAG, "Botão Voltar clicado")
            finish()
        }
    }

    private fun handleMissingCredentials() {
        Log.e(TAG, "apiKey ou deviceId está nulo")
        Toast.makeText(this, "Por favor, faça login novamente", Toast.LENGTH_SHORT).show()
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun loadPlanilha(apiKey: String, deviceId: String, dataType: String) {
        val authHeader = "Bearer $apiKey"
        Log.d(TAG, "Carregando planilha com authHeader=$authHeader, deviceId=$deviceId")

        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                Log.d(TAG, "Resposta recebida: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    handleSuccessfulResponse(response.body()!!, dataType)
                } else {
                    handleErrorResponse(response)
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e(TAG, "Falha na requisição: ${t.message}", t)
                Toast.makeText(this@PlanilhaScreen, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleSuccessfulResponse(planilha: PlanilhaResponse, dataType: String) {
        Log.d(TAG, "Dados da planilha: $planilha")

        if (planilha.hasError()) {
            Log.e(TAG, "Erro na resposta: ${planilha.error}")
            Toast.makeText(this, planilha.error, Toast.LENGTH_SHORT).show()
            return
        }

        val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK")
        planilhaTitle.text = when {
            dayOfWeek != null -> "${if (dataType == "TRAINING") "Treinos" else "Dieta"} de ${planilha.name ?: "Usuário"} - $dayOfWeek"
            else -> "${if (dataType == "TRAINING") "Treinos" else "Dieta"} de ${planilha.name ?: "Usuário"}"
        }

        val items = createPlanilhaItems(planilha, dataType, dayOfWeek)
        adapter = PlanilhaAdapter(items)
        planilhaRecyclerView.adapter = adapter
        Log.d(TAG, "RecyclerView atualizado com ${items.size} itens")
    }

    private fun createPlanilhaItems(planilha: PlanilhaResponse, dataType: String, dayOfWeek: String? = null): List<PlanilhaItem> {
        val items = mutableListOf<PlanilhaItem>()

        if (dataType == "TRAINING" || dataType == "ALL") {
            val trainings = planilha.getTrainingsSafe()
                .filter { dayOfWeek == null || it.weekday?.equals(dayOfWeek, ignoreCase = true) == true }

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
                    details = "Nenhum treino cadastrado${dayOfWeek?.let { " para $it" } ?: ""}.",
                    type = "training"
                ))
            }
        }

        if (dataType == "DIET" || dataType == "ALL") {
            val meals = planilha.getMealsSafe()
                .filter { dayOfWeek == null || it.weekday?.equals(dayOfWeek, ignoreCase = true) == true }

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
                    details = "Nenhuma refeição cadastrada${dayOfWeek?.let { " para $it" } ?: ""}.",
                    type = "meal"
                ))
            }
        }

        return items
    }

    private fun buildTrainingDetails(training: Training): String {
        val details = StringBuilder()
        details.append(training.getSeriesRepetitionsText())
        if (training.hasVideo()) {
            details.append(" (Vídeo disponível)")
        }
        if (!training.description.isNullOrEmpty()) {
            details.append("\n${training.description}")
        }
        if (!training.weekday.isNullOrEmpty()) {
            details.append("\nDia: ${training.weekday}")
        }
        return details.toString()
    }

    private fun buildMealDetails(meal: Meal): String {
        val comidas = meal.getComidasSafe()
        return if (comidas.isNotEmpty()) {
            val comidasText = comidas.joinToString(", ") { comida ->
                comida.getFullDescription()
            }
            val details = StringBuilder(comidasText)
            if (!meal.weekday.isNullOrEmpty()) {
                details.append("\nDia: ${meal.weekday}")
            }
            details.toString()
        } else {
            "Nenhuma comida cadastrada."
        }
    }

    private fun handleErrorResponse(response: Response<PlanilhaResponse>) {
        Log.e(TAG, "Resposta não foi bem-sucedida: ${response.code()} - ${response.message()}")
        try {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "Corpo de erro: $errorBody")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler corpo de erro: ${e.message}")
        }
        Toast.makeText(this, "Erro ao carregar a planilha: ${response.message()}", Toast.LENGTH_SHORT).show()
    }

    private fun performLogout() {
        Log.d(TAG, "Iniciando logout")
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val apiKey = sharedPreferences.getString("auth_token", null)
        val deviceId = sharedPreferences.getString("device_id", null)
        Log.d(TAG, "apiKey: $apiKey, deviceId: $deviceId")

        if (apiKey != null && deviceId != null) {
            val authHeader = "Bearer $apiKey"
            RetrofitClient.apiService.logout(authHeader, deviceId).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d(TAG, "Logout bem-sucedido: ${response.code()}")
                    clearSessionAndNavigateToLogin()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e(TAG, "Falha no logout: ${t.message}")
                    clearSessionAndNavigateToLogin()
                }
            })
        } else {
            Log.w(TAG, "Nenhuma api_key ou device_id encontrado")
            clearSessionAndNavigateToLogin()
        }
    }

    private fun clearSessionAndNavigateToLogin() {
        Log.d(TAG, "Limpando sessão e navegando para login")
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}