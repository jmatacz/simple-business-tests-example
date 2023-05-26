package com.pl.dac.veterinarian.repository

import com.pl.dac.veterinarian.model.MedicineId
import com.pl.dac.veterinarian.model.PetId

class PetMedicineRepository {
    private val memory: MutableMap<PetId, MutableList<MedicineId>> = mutableMapOf()

    fun insert(petId: PetId, medicineIds: List<MedicineId>) {
        val medicines = memory[petId] ?: mutableListOf()
        medicines.addAll(medicineIds)
        memory[petId] = medicines
    }

    fun getMedicinesForPet(petId: PetId): List<MedicineId> =
        memory[petId] ?: emptyList()
}
