package com.pl.dac.veterinarian.repository

import com.pl.dac.veterinarian.model.DiseaseId
import com.pl.dac.veterinarian.model.PetId

class PetDiseaseRepository {
    private val memory: MutableMap<PetId, MutableList<DiseaseId>> = mutableMapOf()

    fun insert(petId: PetId, diseaseId: DiseaseId) {
        val diseases = memory[petId] ?: mutableListOf()
        diseases.add(diseaseId)
        memory[petId] = diseases
    }

    fun getDiseasesForPet(petId: PetId): List<DiseaseId> =
        memory[petId] ?: listOf()
}
