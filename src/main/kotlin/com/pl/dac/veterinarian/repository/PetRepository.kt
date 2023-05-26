package com.pl.dac.veterinarian.repository

import com.pl.dac.veterinarian.model.Pet
import com.pl.dac.veterinarian.model.PetId
import com.pl.dac.veterinarian.model.PetRequest
import com.pl.dac.veterinarian.model.PetStatus
import java.util.UUID

class PetRepository {
    private val memory: MutableMap<PetId, Pet> = mutableMapOf()

    fun insertPet(petRequest: PetRequest): PetId {
        val petId = PetId(UUID.randomUUID())
        val pet = petRequest.toPet(petId)
        memory[petId] = pet

        return petId
    }

    fun exist(id: PetId): Boolean =
        memory.containsKey(id)

    fun updateStatus(id: PetId, status: PetStatus) {
        memory[id]?.let {
            val updatedPet = it.copy(status = status)
            memory[id] = updatedPet
        }
    }

    private fun PetRequest.toPet(petId: PetId) =
        Pet(petId, status, name, breed, age)

    fun get(petId: PetId): Pet? =
        memory[petId]
}
