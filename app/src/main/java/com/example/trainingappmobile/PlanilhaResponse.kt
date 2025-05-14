package com.example.trainingappmobile

data class PlanilhaResponse(
    val id: Int?,
    val name: String?,
    val email: String?,
    val role: String?,
    val trainings: List<Training>?,
    val meals: List<Meal>?,
    val error: String?
)

data class Training(
    val id: Int?,
    val exercise_name: String,
    val serie_amount: String,
    val repeat_amount: String,
    val video: String?,
    val dayOfWeek: String,
    val weekday: String?,
    val exercise: String,
    val sets: Int,
    val reps: Int
) {
    fun getSerieAmountInt(): Int = serie_amount.toIntOrNull() ?: 0
    fun getRepeatAmountInt(): Int = repeat_amount.toIntOrNull() ?: 0
}

data class Meal(
    val id: Int?,
    val meal_type: String,
    val comidas: List<Comida>?,
    val dayOfWeek: String,
    val description: String,
    val time: String,
    val weekday: String?,
)

data class Comida(
    val name: String,
    val amount: String,
    val id: Int?,
)

