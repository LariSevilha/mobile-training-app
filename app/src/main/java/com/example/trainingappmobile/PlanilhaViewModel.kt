package com.example.trainingappmobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData

class PlanilhaViewModel : ViewModel() {
    val planilhaData = MutableLiveData<PlanilhaResponse>()

    fun setPlanilhaData(data: PlanilhaResponse) {
        planilhaData.value = data
    }

    fun getTrainingsForDay(day: String): List<Training> {
        return planilhaData.value?.getTrainingsSafe()?.filter { it.weekday == day } ?: emptyList()
    }

    fun getMealsForDay(day: String): List<Meal> {
        return planilhaData.value?.getMealsSafe()?.filter { it.weekday == day } ?: emptyList()
    }

    fun getPdfForDay(day: String): WeeklyPdf? {
        return planilhaData.value?.getWeeklyPdfsSafe()?.find { it.weekday == day }
    }
}