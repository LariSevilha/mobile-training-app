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

        planilhaTitle = findViewById(R.id.planilha_title)
        planilhaRecyclerView = findViewById(R.id.planilha_recycler_view)
        planilhaRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PlanilhaAdapter(emptyList())
        planilhaRecyclerView.adapter = adapter

        // Configurar o botão de logout
        findViewById<Button>(R.id.logout_button)?.setOnClickListener {
            Log.d("PlanilhaScreen", "Botão de logout clicado")
            // performLogout()
        }

        // Configurar o botão de voltar
        findViewById<Button>(R.id.back_button)?.setOnClickListener {
            Log.d("PlanilhaScreen", "Botão Voltar clicado")
            finish()
        }

        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("auth_token", null)
        val deviceId = sharedPrefs.getString("device_id", null)
        Log.d("PlanilhaScreen", "Credenciais: auth_token=$apiKey, deviceId=$deviceId")

        if (apiKey == null || deviceId == null) {
            Log.e("PlanilhaScreen", "auth_token ou deviceId está nulo")
            Toast.makeText(this, "Por favor, faça login novamente", Toast.LENGTH_SHORT).show()
            sharedPrefs.edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Obter o tipo de dados (TRAINING ou DIET)
        val dataType = intent.getStringExtra("DATA_TYPE") ?: "ALL"
        Log.d("PlanilhaScreen", "Tipo de dados: $dataType")
        loadPlanilha(apiKey, deviceId, dataType)
    }

    private fun loadPlanilha(apiKey: String, deviceId: String, dataType: String) {
        val authHeader = "Bearer $apiKey"
        Log.d("PlanilhaScreen", "Carregando planilha com authHeader=$authHeader, deviceId=$deviceId")
        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                Log.d("PlanilhaScreen", "Resposta recebida: ${response.code()} - ${response.raw()}")
                Log.d("PlanilhaScreen", "Corpo da resposta: ${response.body()}")
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d("PlanilhaScreen", "Dados da planilha: $planilha")
                    if (planilha.error == null) {
                        planilhaTitle.text = when (dataType) {
                            "TRAINING" -> "Treinos de ${planilha.name ?: "Usuário"}"
                            "DIET" -> "Dieta de ${planilha.name ?: "Usuário"}"
                            else -> "Planilha de ${planilha.name ?: "Usuário"}"
                        }

                        val items = mutableListOf<PlanilhaItem>()
                        if (dataType == "TRAINING" || dataType == "ALL") {
                            if (!planilha.trainings.isNullOrEmpty()) {
                                planilha.trainings.forEach { training ->
                                    val details = StringBuilder()
                                    details.append("${training.getSeriesRepetitionsText()} séries x ${training.getSeriesRepetitionsText()} repetições")
                                    if (!training.video.isNullOrEmpty()) {
                                        details.append(" (Vídeo: ${training.video})")
                                    }
                                    items.add(PlanilhaItem(
                                        title = training.exerciseName.toString(),
                                        details = details.toString(),
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
                        if (dataType == "DIET" || dataType == "ALL") {
                            if (!planilha.meals.isNullOrEmpty()) {
                                planilha.meals.forEach { meal ->
                                    val details = if (!meal.comidas.isNullOrEmpty()) {
                                        meal.comidas.joinToString(", ") { comida ->
                                            "${comida.name} (${comida.amount})"
                                        }
                                    } else {
                                        "Nenhuma comida cadastrada."
                                    }
                                    items.add(PlanilhaItem(
                                        title = meal.mealType.toString(),
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

                        adapter = PlanilhaAdapter(items)
                        planilhaRecyclerView.adapter = adapter
                        Log.d("PlanilhaScreen", "RecyclerView atualizado com ${items.size} itens")
                    } else {
                        Log.e("PlanilhaScreen", "Erro na resposta: ${planilha.error}")
                        Toast.makeText(this@PlanilhaScreen, planilha.error, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("PlanilhaScreen", "Resposta não foi bem-sucedida: ${response.code()} - ${response.message()}")
                    Log.e("PlanilhaScreen", "Corpo de erro: ${response.errorBody()?.string()}")
                    Toast.makeText(this@PlanilhaScreen, "Erro ao carregar a planilha: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e("PlanilhaScreen", "Falha na requisição: ${t.message}", t)
                Toast.makeText(this@PlanilhaScreen, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun clearSessionAndNavigateToLogin() {
        Log.d("PlanilhaScreen", "Limpando sessão e navegando para login")
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}