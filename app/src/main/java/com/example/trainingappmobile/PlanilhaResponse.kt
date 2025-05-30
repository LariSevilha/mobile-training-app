package com.example.trainingappmobile

data class PlanilhaResponse(
    val id: Int?,
    val name: String?,
    val email: String?,
    val role: String?,
    val trainings: List<Training>?,
    val meals: List<Meal>?,
    val error: String?,
    val weekly_pdfs: List<WeeklyPdf>?
)

data class Training(
    val id: Int?,
    val exercise_name: String, // Made nullable to match backend JSON
    val serie_amount: String?, // Made nullable to handle null from backend
    val repeat_amount: String?, // Made nullable to handle null from backend
    val video: String?,
    val weekday: String?, // Using 'weekday' to match backend, removed 'dayOfWeek' and 'exercise'
    val description: String?, // Added from backend
    val photo_urls: List<String>?, // Added from backend to handle photos
    val sets: Int? = null, // Added as optional, but may conflict with serie_amount
    val reps: Int? = null // Added as optional, but may conflict with repeat_amount
) {
    fun getSerieAmountInt(): Int = serie_amount?.toIntOrNull() ?: 0
    fun getRepeatAmountInt(): Int = repeat_amount?.toIntOrNull() ?: 0
}

data class Meal(
    val id: Int?,
    val meal_type: String,
    val comidas: List<Comida>?,
    val weekday: String?, // Using 'weekday' to match backend, removed 'dayOfWeek'
    val description: String?, // Added to match potential backend expansion
    val time: String?
)

data class Comida(
    val name: String?,
    val amount: String?,
    val id: Int?
)

data class WeeklyPdf(
    val id: Int?,
    val weekday: String?,
    val pdf_url: String?
)