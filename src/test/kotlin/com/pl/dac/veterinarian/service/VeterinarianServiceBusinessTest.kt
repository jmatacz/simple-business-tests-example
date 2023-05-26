package com.pl.dac.veterinarian.service

import com.pl.dac.veterinarian.DiseaseNotFoundException
import com.pl.dac.veterinarian.PetNotFoundException
import com.pl.dac.veterinarian.PetShouldNotBeCuredException
import com.pl.dac.veterinarian.PetShouldNotBeDiagnosedException
import com.pl.dac.veterinarian.model.Disease
import com.pl.dac.veterinarian.model.DiseaseId
import com.pl.dac.veterinarian.model.Medicine
import com.pl.dac.veterinarian.model.MedicineId
import com.pl.dac.veterinarian.model.Pet
import com.pl.dac.veterinarian.model.PetId
import com.pl.dac.veterinarian.model.PetRequest
import com.pl.dac.veterinarian.model.PetStatus
import com.pl.dac.veterinarian.repository.DiseaseRepository
import com.pl.dac.veterinarian.repository.MedicineRepository
import com.pl.dac.veterinarian.repository.PetDiseaseRepository
import com.pl.dac.veterinarian.repository.PetMedicineRepository
import com.pl.dac.veterinarian.repository.PetRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID

private val petRepository = PetRepository()
private val diseaseRepository = DiseaseRepository()
private val petDiseaseRepository = PetDiseaseRepository()
private val medicineRepository = MedicineRepository()
private val petMedicineRepository = PetMedicineRepository()

private val veterinarianService = VeterinarianService(petRepository, diseaseRepository, petDiseaseRepository, medicineRepository, petMedicineRepository)

internal class VeterinarianServiceBusinessTest : StringSpec({
    "Should diagnose existing, unhealthy pet with existing disease" {
        val pet = pet(PetStatus.UNHEALTHY)
            .savedInDatabase()
        val disease = disease("Toxoplasmosis")
            .savedInDatabase()
        val medicine = disease.isCuredByMedicine("Pyrimethamine")

        val recommendedMedicines = veterinarianService.diagnosePet(pet, disease)

        pet.shouldBeDiagnosedOnlyWith(disease)
        recommendedMedicines.shouldRecommendOnly(medicine.id)
    }

    "Should diagnose existing, already being cured pet with still existing disease" {
        val pet = pet(PetStatus.CURE_IN_PROGRESS)
            .savedInDatabase()
        val disease = disease("Toxoplasmosis")
            .savedInDatabase()
        val medicine = disease.isCuredByMedicine("Pyrimethamine")

        val recommendedMedicines = veterinarianService.diagnosePet(pet, disease)

        pet.shouldBeDiagnosedOnlyWith(disease)
        recommendedMedicines.shouldRecommendOnly(medicine.id)
    }

    "Should not diagnose healthy pet" {
        val pet = pet(PetStatus.HEALTHY)
            .savedInDatabase()
        val disease = disease("Cat Tapeworm")
            .savedInDatabase()

        shouldThrow<PetShouldNotBeDiagnosedException> {
            veterinarianService.diagnosePet(pet, disease)
        }
    }

    "Should not diagnose pet which does not exist" {
        val pet = pet(PetStatus.UNHEALTHY)
            .notSavedInDatabase()
        val disease = disease("Roundworms")
            .savedInDatabase()

        shouldThrow<PetNotFoundException> {
            veterinarianService.diagnosePet(pet, disease)
        }
    }

    "Should not diagnose existing pet with a disease that does not exist" {
        val pet = pet(PetStatus.UNHEALTHY)
            .savedInDatabase()
        val nonExistentDisease = disease("Crazy ears disease")
            .notSavedInDatabase()

        shouldThrow<DiseaseNotFoundException> {
            veterinarianService.diagnosePet(pet, nonExistentDisease)
        }
    }

    "Should cure existing, diagnosed pet with medicines" {
        val pet = pet(PetStatus.DIAGNOSED)
            .savedInDatabase()
        val disease = disease("Cryptosporidium")
            .savedInDatabase()
        val medicine = disease.isCuredByMedicine("Nitazoxanide")

        veterinarianService.curePet(pet, listOf(medicine))

        pet.shouldBeCuredWith(medicine)
    }

    "Should not cure non existing pet" {
        val pet = pet(PetStatus.UNHEALTHY)
            .notSavedInDatabase()
        val disease = disease("Rabies")
            .savedInDatabase()
        val medicine = disease.isCuredByMedicine("Postexposure prophylaxis")

        shouldThrow<PetNotFoundException> {
            veterinarianService.curePet(pet, listOf(medicine))
        }
    }

    "Should not cure un-diagnosed pet" {
        val pet = pet(undiagnosed())
            .savedInDatabase()
        val disease = disease("Salmonellosis")
            .savedInDatabase()
        val medicine = disease.isCuredByMedicine("Ceftriaxone")

        shouldThrow<PetShouldNotBeCuredException> {
            veterinarianService.curePet(pet, listOf(medicine))
        }
    }

    "Should mark pet as healthy" {
        val pet = pet(withAnyStatus())
            .savedInDatabase()

        veterinarianService.markPetAsHealthy(pet)

        pet.shouldBeHealthy()
    }

    "Should not mark non existing pet as healthy" {
        val pet = pet(PetStatus.UNHEALTHY)
            .notSavedInDatabase()

        shouldThrow<PetNotFoundException> {
            veterinarianService.markPetAsHealthy(pet)
        }
    }
})

private fun Pet.shouldBeHealthy() =
    petRepository.get(id)
        .shouldNotBeNull()
        .status.shouldBe(PetStatus.HEALTHY)

private fun withAnyStatus() =
    PetStatus.values().random()

fun undiagnosed(): PetStatus {
    val undiagnosedStatuses = PetStatus.values().toMutableList() - PetStatus.DIAGNOSED
    return undiagnosedStatuses.random()
}

private fun Pet.shouldBeCuredWith(medicine: Medicine) {
    petRepository.get(id)
        .shouldNotBeNull()
        .status.shouldBe(PetStatus.CURE_IN_PROGRESS)

    petMedicineRepository.getMedicinesForPet(id)
        .shouldContainOnly(medicine.id)
}

private fun Disease.notSavedInDatabase(): Disease {
    diseaseRepository.exist(id).shouldBeFalse()
    return this
}

private fun Pet.notSavedInDatabase(): Pet {
    petRepository.exist(id).shouldBeFalse()
    return this
}

private fun Collection<Medicine>.shouldRecommendOnly(medicineId: MedicineId) =
    map { it.id }.shouldContainOnly(medicineId)

private fun Pet.shouldBeDiagnosedOnlyWith(disease: Disease): Pet {
    petRepository.get(id)
        .shouldNotBeNull()
        .status.shouldBe(PetStatus.DIAGNOSED)

    petDiseaseRepository.getDiseasesForPet(id)
        .shouldContainOnly(disease.id)

    return this
}

private fun Disease.isCuredByMedicine(medicineName: String): Medicine {
    val medicineId = medicineRepository.addMedicineForDisease(id, medicineName)
    return Medicine(medicineId, medicineName, id)
}

private fun disease(diseaseName: String): Disease =
    Disease(DiseaseId(UUID.randomUUID()), diseaseName)

private fun Disease.savedInDatabase(): Disease {
    val diseaseId = diseaseRepository.insert(name)
    return copy(id = diseaseId)
}

private fun Pet.savedInDatabase(): Pet {
    val petRequest = PetRequest(status, name, breed, age)
    val petId = petRepository.insertPet(petRequest)

    return copy(id = petId)
}

private fun pet(petStatus: PetStatus): Pet =
    Pet(
        id = PetId(UUID.randomUUID()),
        status = petStatus,
        name = "Pet name",
        breed = "Pet breed",
        age = 8
    )
