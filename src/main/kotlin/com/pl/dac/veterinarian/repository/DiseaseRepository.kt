package com.pl.dac.veterinarian.repository

import com.pl.dac.veterinarian.model.Disease
import com.pl.dac.veterinarian.model.DiseaseId
import java.util.UUID

class DiseaseRepository {
    private val memory: MutableMap<DiseaseId, Disease> = mutableMapOf()

    fun exist(id: DiseaseId): Boolean =
        memory.containsKey(id)

    fun insert(name: String): DiseaseId {
        val diseaseId = DiseaseId(UUID.randomUUID())
        memory[diseaseId] = Disease(diseaseId, name)

        return diseaseId
    }
}
