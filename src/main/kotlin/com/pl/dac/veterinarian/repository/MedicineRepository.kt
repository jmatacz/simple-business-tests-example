package com.pl.dac.veterinarian.repository

import com.pl.dac.veterinarian.model.DiseaseId
import com.pl.dac.veterinarian.model.Medicine
import com.pl.dac.veterinarian.model.MedicineId
import java.util.UUID

class MedicineRepository {
    private val memory: MutableMap<MedicineId, Medicine> = mutableMapOf()

    fun getMedicinesForDisease(id: DiseaseId): Collection<Medicine> =
        memory.values.filter { it.recommendedForDisease == id }

    fun addMedicineForDisease(diseaseId: DiseaseId, medicineName: String): MedicineId {
        val medicineId = MedicineId(UUID.randomUUID())
        val medicine = Medicine(medicineId, medicineName, diseaseId)
        memory[medicineId] = medicine

        return medicineId
    }
}
