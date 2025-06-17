package com.example.trainingappmobile

import com.google.gson.annotations.SerializedName

data class PlanilhaResponse(
    val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("registration_date") val registrationDate: String? = null,
    @SerializedName("expiration_date") val expirationDate: String? = null,
    @SerializedName("plan_type") val planType: String? = null,
    @SerializedName("plan_duration") val planDuration: Int? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("trainings") val trainings: List<Training>? = null,
    @SerializedName("meals") val meals: List<Meal>? = null,
    @SerializedName("weekly_pdfs") val weeklyPdfs: List<WeeklyPdf>? = null
) {
    fun hasError(): Boolean = !error.isNullOrEmpty()
    fun getTrainingsSafe(): List<Training> = trainings ?: emptyList()
    fun getMealsSafe(): List<Meal> = meals ?: emptyList()
    fun getWeeklyPdfsSafe(): List<WeeklyPdf> = weeklyPdfs ?: emptyList()
}

data class Training(
    val id: Int? = null,
    @SerializedName("serie_amount") val serieAmount: Int? = null,
    @SerializedName("repeat_amount") val repeatAmount: Int? = null,
    @SerializedName("exercise_name") val exerciseName: String? = null,
    @SerializedName("video") val video: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("weekday") val weekday: String? = null,
    @SerializedName("photo_urls") val photoUrls: List<String>? = null
) {
    fun getExerciseNameSafe(): String = exerciseName ?: "Exercício não especificado"
    fun hasVideo(): Boolean = !video.isNullOrEmpty()
    fun hasPhotos(): Boolean = !photoUrls.isNullOrEmpty()
    fun getPhotoUrlsSafe(): List<String> = photoUrls ?: emptyList()
    fun getSeriesRepetitionsText(): String {
        val series = serieAmount ?: 0
        val reps = repeatAmount ?: 0
        return if (series > 0 && reps > 0) {
            "${series}x${reps}"
        } else {
            "Não especificado"
        }
    }
}

data class Meal(
    val id: Int? = null,
    @SerializedName("meal_type") val mealType: String? = null,
    @SerializedName("weekday") val weekday: String? = null,
    @SerializedName("comidas") val comidas: List<Comida>? = null
) {
    fun getMealTypeSafe(): String = mealType ?: "Refeição"
    fun getComidasSafe(): List<Comida> = comidas ?: emptyList()
    fun hasComidas(): Boolean = !comidas.isNullOrEmpty()
}

data class Comida(
    val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("amount") val amount: Int? = null
) {
    fun getNameSafe(): String = name ?: "Alimento não especificado"
    fun getAmountText(): String {
        return if (amount != null && amount > 0) {
            "${amount}g"
        } else {
            "Quantidade não especificada"
        }
    }
    fun getFullDescription(): String = "${getNameSafe()} - ${getAmountText()}"
}

data class WeeklyPdf(
    val id: Int? = null,
    @SerializedName("weekday") val weekday: String? = null,
    @SerializedName("pdf_url") val pdfUrl: String? = null
) {
    fun hasValidUrl(): Boolean = !pdfUrl.isNullOrEmpty()
    fun getWeekdaySafe(): String = weekday ?: "Dia não especificado"
}

enum class Weekday(val displayName: String) {
    MONDAY("Segunda-feira"),
    TUESDAY("Terça-feira"),
    WEDNESDAY("Quarta-feira"),
    THURSDAY("Quinta-feira"),
    FRIDAY("Sexta-feira"),
    SATURDAY("Sábado"),
    SUNDAY("Domingo");

    companion object {
        fun fromString(value: String?): Weekday? {
            return values().find {
                it.name.equals(value, ignoreCase = true) ||
                        it.displayName.equals(value, ignoreCase = true)
            }
        }
    }
}

enum class MealType(val displayName: String) {
    BREAKFAST("Café da manhã"),
    LUNCH("Almoço"),
    SNACK("Lanche"),
    DINNER("Jantar"),
    SUPPER("Ceia");

    companion object {
        fun fromString(value: String?): MealType? {
            return values().find {
                it.name.equals(value, ignoreCase = true) ||
                        it.displayName.equals(value, ignoreCase = true)
            }
        }
    }
}