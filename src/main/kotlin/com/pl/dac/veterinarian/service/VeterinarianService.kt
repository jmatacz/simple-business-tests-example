package com.pl.dac.veterinarian.service

import com.pl.dac.veterinarian.DiseaseNotFoundException
import com.pl.dac.veterinarian.PetNotFoundException
import com.pl.dac.veterinarian.PetShouldNotBeCuredException
import com.pl.dac.veterinarian.PetShouldNotBeDiagnosedException
import com.pl.dac.veterinarian.model.Disease
import com.pl.dac.veterinarian.model.Medicine
import com.pl.dac.veterinarian.model.Pet
import com.pl.dac.veterinarian.model.PetStatus
import com.pl.dac.veterinarian.repository.DiseaseRepository
import com.pl.dac.veterinarian.repository.MedicineRepository
import com.pl.dac.veterinarian.repository.PetDiseaseRepository
import com.pl.dac.veterinarian.repository.PetMedicineRepository
import com.pl.dac.veterinarian.repository.PetRepository

class VeterinarianService(
    private val petRepository: PetRepository,
    private val diseaseRepository: DiseaseRepository,
    private val petDiseaseRepository: PetDiseaseRepository,
    private val medicineRepository: MedicineRepository,
    private val petMedicineRepository: PetMedicineRepository
) {

    fun diagnosePet(pet: Pet, disease: Disease): Collection<Medicine> {
        if (!pet.existsInDatabase()) {
            throw PetNotFoundException(pet)
        }

        if (!disease.existsInDatabase()) {
            throw DiseaseNotFoundException(disease)
        }

        if (pet.shouldBeDiagnosed()) {
            disease.assignTo(pet)
        } else {
            throw PetShouldNotBeDiagnosedException(pet)
        }

        return disease.getRecommendedMedicines()
    }

    fun curePet(pet: Pet, medicines: Collection<Medicine>) {
        if (!pet.existsInDatabase()) {
            throw PetNotFoundException(pet)
        }

        if (pet.shouldBeCured()) {
            pet.cureInProgressUsing(medicines)
        } else {
            throw PetShouldNotBeCuredException(pet)
        }
    }

    fun markPetAsHealthy(pet: Pet) {
        if (!pet.existsInDatabase()) {
            throw PetNotFoundException(pet)
        }

        pet.markAsHealthy()
    }

    private fun Pet.existsInDatabase(): Boolean =
        petRepository.exist(id)

    private fun Disease.existsInDatabase(): Boolean =
        diseaseRepository.exist(id)

    private fun Pet.shouldBeDiagnosed(): Boolean =
        status == PetStatus.UNHEALTHY || status == PetStatus.CURE_IN_PROGRESS

    private fun Pet.shouldBeCured(): Boolean =
        status == PetStatus.DIAGNOSED

    private fun Disease.assignTo(pet: Pet) {
        petDiseaseRepository.insert(pet.id, id)
        petRepository.updateStatus(pet.id, PetStatus.DIAGNOSED)
    }

    private fun Disease.getRecommendedMedicines(): Collection<Medicine> =
        medicineRepository.getMedicinesForDisease(id)

    private fun Pet.cureInProgressUsing(medicines: Collection<Medicine>) {
        val medicineIds = medicines.map { it.id }
        petMedicineRepository.insert(id, medicineIds)
        petRepository.updateStatus(id, PetStatus.CURE_IN_PROGRESS)
    }

    private fun Pet.markAsHealthy() {
        petRepository.updateStatus(id, PetStatus.HEALTHY)
    }
}
