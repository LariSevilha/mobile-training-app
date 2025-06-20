package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.view.View
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HomeScreen : ComponentActivity() {

    private lateinit var greetingText: TextView
    private lateinit var trainingButton: LinearLayout
    private lateinit var dietButton: LinearLayout
    private lateinit var pdfCard: LinearLayout
    private lateinit var noDataText: TextView
    private lateinit var planExpiryText: TextView
    private lateinit var alertIcon: ImageView
    private lateinit var logoutIcon: ImageView

    private lateinit var trainingCardView: CardView
    private lateinit var dietCardView: CardView
    private lateinit var pdfCardView: CardView

    private var currentPlanilhaData: PlanilhaResponse? = null

    companion object {
        private const val TAG = "HomeScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initializeViews()
        setupClickListeners()
        loadUserData()
    }

    private fun initializeViews() {
        greetingText = findViewById(R.id.greeting_text)
        trainingButton = findViewById(R.id.training_button)
        dietButton = findViewById(R.id.diet_button)
        pdfCard = findViewById(R.id.pdf_card)
        noDataText = findViewById(R.id.no_data_text)
        planExpiryText = findViewById(R.id.plan_expiry_text)
        alertIcon = findViewById(R.id.alert_icon)
        logoutIcon = findViewById(R.id.logout_icon)

        trainingCardView = findViewById(R.id.training_card)
        dietCardView = findViewById(R.id.diet_card)
        pdfCardView = findViewById(R.id.pdf_card_container)
    }

    private fun setupClickListeners() {
        trainingButton.setOnClickListener {
            if (hasTrainingData()) {
                openDaysOfWeekScreen("training")
            } else {
                showNoDataMessage("Nenhum treino disponível")
            }
        }

        dietButton.setOnClickListener {
            if (hasDietData()) {
                openDaysOfWeekScreen("diet")
            } else {
                showNoDataMessage("Nenhuma dieta disponível")
            }
        }

        pdfCard.setOnClickListener {
            openPdfViewer()
        }

        alertIcon.setOnClickListener {
            showRemainingDaysDialog()
        }

        logoutIcon.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showRemainingDaysDialog() {
        val expirationDate = currentPlanilhaData?.expirationDate
        if (expirationDate.isNullOrEmpty()) {
            Toast.makeText(this, "Data de expiração não disponível", Toast.LENGTH_SHORT).show()
            return
        }

        val remainingDays = calculateRemainingDays(expirationDate)
        val message = when {
            remainingDays > 1 -> "Restam $remainingDays dias para o fim do seu plano"
            remainingDays == 1 -> "Resta 1 dia para o fim do seu plano"
            remainingDays == 0 -> "Seu plano expira hoje!"
            else -> "Seu plano expirou há ${Math.abs(remainingDays)} dias"
        }

        val title = when {
            remainingDays > 7 -> "Status do Plano - Ativo"
            remainingDays > 0 -> "Status do Plano - Atenção"
            else -> "Status do Plano - Expirado"
        }

        val iconRes = when {
            remainingDays > 7 -> android.R.drawable.ic_dialog_info
            remainingDays > 0 -> android.R.drawable.ic_dialog_alert
            else -> android.R.drawable.ic_dialog_alert
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setIcon(iconRes)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja realmente sair da sua conta?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sair") { _, _ -> clearUserData() }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun calculateRemainingDays(expirationDate: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val expiry = dateFormat.parse(expirationDate)
            val today = Calendar.getInstance().time
            if (expiry != null) {
                val diffInMillis = expiry.time - today.time
                TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS).toInt()
            } else 0
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao calcular data de expiração: ${e.message}")
            0
        }
    }

    private fun updateAlertIcon() {
        val expirationDate = currentPlanilhaData?.expirationDate
        if (expirationDate.isNullOrEmpty()) {
            alertIcon.visibility = View.GONE
            return
        }

        // Sempre mostrar o ícone quando há data de expiração
        alertIcon.visibility = View.VISIBLE
        val remainingDays = calculateRemainingDays(expirationDate)

        // Definir cor baseada nos dias restantes
        val colorRes = when {
            remainingDays > 7 -> R.color.gold_primary // Verde/Dourado para mais de 7 dias
            remainingDays > 0 -> android.R.color.holo_orange_dark // Laranja para entre 1-7 dias
            else -> android.R.color.holo_red_dark // Vermelho para expirado
        }

        // Aplicar a cor ao ícone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alertIcon.setColorFilter(getColor(colorRes))
        } else {
            alertIcon.setColorFilter(ContextCompat.getColor(this, colorRes))
        }

        Log.d(TAG, "Ícone de alerta atualizado - Dias restantes: $remainingDays")
    }

    private fun loadUserData() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
        val deviceId = prefs.getString("device_id", null)

        if (token.isNullOrEmpty() || deviceId.isNullOrEmpty()) {
            showNoDataMessage("Erro de autenticação")
            return
        }

        val apiService = RetrofitClient.apiService
        val call = apiService.getPlanilha("Bearer $token", deviceId)

        call.enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                if (response.isSuccessful) {
                    val planilhaResponse = response.body()
                    if (planilhaResponse != null) {
                        currentPlanilhaData = planilhaResponse
                        updateUI(planilhaResponse)
                        updateAlertIcon() // Sempre atualizar o ícone
                    } else {
                        showNoDataMessage("Dados indisponíveis")
                        // Mesmo sem dados, tentar mostrar o ícone se houver data de expiração
                        updateAlertIcon()
                    }
                } else {
                    showNoDataMessage("Erro ao carregar dados")
                    Log.e(TAG, "Erro na resposta: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                showNoDataMessage("Erro de conexão")
                Log.e(TAG, "Falha na requisição: ${t.message}")
            }
        })
    }

    private fun updateUI(planilhaResponse: PlanilhaResponse) {
        CoroutineScope(Dispatchers.Main).launch {
            val userName = planilhaResponse.name ?: "Usuário"
            greetingText.text = "Bem-vindo(a), $userName!"

            val hasTraining = hasTrainingData()
            val hasDiet = hasDietData()
            val hasPdf = hasPdfData()

            when {
                hasPdf && !hasTraining && !hasDiet -> {
                    pdfCardView.visibility = View.VISIBLE
                    trainingCardView.visibility = View.GONE
                    dietCardView.visibility = View.GONE
                    noDataText.visibility = View.GONE
                }
                (hasTraining || hasDiet) && !hasPdf -> {
                    trainingCardView.visibility = if (hasTraining) View.VISIBLE else View.GONE
                    dietCardView.visibility = if (hasDiet) View.VISIBLE else View.GONE
                    pdfCardView.visibility = View.GONE
                    noDataText.visibility = View.GONE
                }
                hasPdf && (hasTraining || hasDiet) -> {
                    pdfCardView.visibility = View.VISIBLE
                    trainingCardView.visibility = View.GONE
                    dietCardView.visibility = View.GONE
                    noDataText.text = "Atenção: Dados mistos (PDF priorizado)"
                    noDataText.visibility = View.VISIBLE
                }
                else -> {
                    trainingCardView.visibility = View.GONE
                    dietCardView.visibility = View.GONE
                    pdfCardView.visibility = View.GONE
                    noDataText.text = "Nenhum dado disponível"
                    noDataText.visibility = View.VISIBLE
                }
            }

            // Sempre tentar mostrar a data de expiração se disponível
            planilhaResponse.expirationDate?.let { expirationDate ->
                planExpiryText.text = "Data de expiração: $expirationDate"
                planExpiryText.parent?.let { parent ->
                    if (parent is CardView) {
                        parent.visibility = View.VISIBLE
                    }
                }
                planExpiryText.visibility = View.VISIBLE
                Log.d(TAG, "Data de expiração definida: $expirationDate")
            } ?: run {
                planExpiryText.visibility = View.GONE
                planExpiryText.parent?.let { parent ->
                    if (parent is CardView) {
                        parent.visibility = View.GONE
                    }
                }
                Log.d(TAG, "Nenhuma data de expiração disponível")
            }
        }
    }

    private fun hasTrainingData(): Boolean {
        val trainings = currentPlanilhaData?.getTrainingsSafe() ?: emptyList()
        return trainings.isNotEmpty()
    }

    private fun hasDietData(): Boolean {
        val meals = currentPlanilhaData?.getMealsSafe() ?: emptyList()
        return meals.isNotEmpty()
    }

    private fun hasPdfData(): Boolean {
        val pdfs = currentPlanilhaData?.getWeeklyPdfsSafe() ?: emptyList()
        val hasValidPdf = pdfs.any { it.hasValidUrl() }

        pdfs.forEachIndexed { index, pdf ->
            Log.d(TAG, "PDF $index: url=${pdf.pdfUrl}, valid=${pdf.hasValidUrl()}")
        }

        return hasValidPdf
    }

    private fun openDaysOfWeekScreen(type: String) {
        val intent = Intent(this, DaysOfWeekScreen::class.java)
        intent.putExtra("SCREEN_TYPE", type)
        startActivity(intent)
    }

    private fun openPdfViewer() {
        val pdfs = currentPlanilhaData?.getWeeklyPdfsSafe() ?: emptyList()
        val validPdf = pdfs.find { it.hasValidUrl() }

        if (validPdf == null) {
            showNoDataMessage("PDF não disponível")
            return
        }

        val intent = Intent(this, PdfViewerScreen::class.java)
        intent.putExtra("PDF_URL", validPdf.getFullUrl())
        validPdf.weekday?.let { intent.putExtra("WEEKDAY", it) }
        startActivity(intent)
    }

    private fun clearUserData() {
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showNoDataMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            noDataText.text = message
            noDataText.visibility = View.VISIBLE
            Toast.makeText(this@HomeScreen, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentPlanilhaData == null) {
            loadUserData()
        } else {
            // Se já tem dados, apenas atualizar o ícone
            updateAlertIcon()
        }
    }
}

// Extensões para validar PDF
fun WeeklyPdf.hasValidUrl(): Boolean {
    return !pdfUrl.isNullOrEmpty() && Uri.parse(getFullUrl()).isHierarchical
}

fun WeeklyPdf.getFullUrl(): String {
    return if (pdfUrl?.startsWith("http") == true) {
        pdfUrl!!
    } else {
        "${RetrofitClient.BASE_URL}${pdfUrl ?: ""}"
    }
}