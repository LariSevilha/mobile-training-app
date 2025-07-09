package com.example.trainingappmobile

import android.util.Log
import com.google.gson.annotations.SerializedName

data class PlanilhaResponse(
    val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("registration_date") val registrationDate: String? = null,
    @SerializedName("expiration_date") val expirationDate: String? = null,
    @SerializedName("plan_type") val planType: String? = null,
    @SerializedName("plan_duration") val planDuration: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("trainings") val trainings: List<Training>? = null,
    @SerializedName("meals") val meals: List<Meal>? = null,
    @SerializedName("weekly_pdfs") val weeklyPdfs: List<WeeklyPdf>? = null
) {
    companion object {
        private const val TAG = "PlanilhaResponse"
    }

    fun hasError(): Boolean = !error.isNullOrEmpty()

    fun getTrainingsSafe(): List<Training> {
        val trainings = trainings ?: emptyList()
        Log.d(TAG, "getTrainingsSafe: ${trainings.size} treinos encontrados")
        return trainings
    }

    fun getMealsSafe(): List<Meal> {
        val meals = meals ?: emptyList()
        Log.d(TAG, "getMealsSafe: ${meals.size} refeições encontradas")
        return meals
    }

    fun getWeeklyPdfsSafe(): List<WeeklyPdf> {
        val pdfs = weeklyPdfs ?: emptyList()
        Log.d(TAG, "getWeeklyPdfsSafe: ${pdfs.size} PDFs encontrados")
        return pdfs
    }
}

data class Training(
    val id: Int? = null,
    @SerializedName("serie_amount") val serieAmount: String? = null,
    @SerializedName("repeat_amount") val repeatAmount: String? = null,
    @SerializedName("exercise_name") val exerciseName: String? = null,
    @SerializedName("video") val video: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("weekday") val weekday: String? = null,
    @SerializedName("photo_urls") val photoUrls: List<String>? = null
) {
    fun getExerciseNameSafe(): String = exerciseName ?: "Nome do exercício não definido"
    fun getSeriesRepetitionsText(): String = "Séries: ${serieAmount ?: "0"}, Repetições: ${repeatAmount ?: "0"}"
    fun hasVideo(): Boolean = !video.isNullOrEmpty()
    fun hasPhotos(): Boolean = !photoUrls.isNullOrEmpty()
    fun getPhotoUrlsSafe(): List<String> = photoUrls ?: emptyList()
}

data class Meal(
    val id: Int? = null,
    @SerializedName("meal_type") val mealType: String? = null,
    @SerializedName("weekday") val weekday: String? = null,
    @SerializedName("comidas") val comidas: List<Comida>? = null
) {
    fun getMealTypeSafe(): String = mealType ?: "Tipo de refeição não definido"
    fun getComidasSafe(): List<Comida> = comidas ?: emptyList()
}

data class Comida(
    val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("amount") val amount: String? = null
) {
    fun getFullDescription(): String = "${name ?: "Comida não definida"} - Quantidade: ${amount ?: "0"}"
}

data class WeeklyPdf(
    val id: Int? = null,
    @SerializedName("weekday") val weekday: String? = null,
    @SerializedName("pdf_url") val pdfUrl: String? = null
) {
    fun getWeekdaySafe(): String = weekday ?: "Dia não definido"
}

