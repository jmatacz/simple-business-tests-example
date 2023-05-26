package com.pl.dac.veterinarian.service

import com.pl.dac.veterinarian.DiseaseNotFoundException
import com.pl.dac.veterinarian.PetNotFoundException
import com.pl.dac.veterinarian.PetShouldNotBeCuredException
import com.pl.dac.veterinarian.PetShouldNotBeDiagnosedException
import com.pl.dac.veterinarian.model.Disease
import com.pl.dac.veterinarian.model.DiseaseId
import com.pl.dac.veterinarian.model.Medicine
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

internal class VeterinarianServiceTest : StringSpec({
    "Should diagnose existing, unhealthy pet with existing disease" {
        val petRequest = PetRequest(
            status = PetStatus.UNHEALTHY,
            name = "Marcel",
            breed = "Birman Cat",
            age = 4
        )

        val petId = petRepository.insertPet(petRequest)
        val marcel = Pet(petId, petRequest.status, petRequest.name, petRequest.breed, petRequest.age)

        val diseaseName = "Toxoplasmosis"
        val diseaseId = diseaseRepository.insert(diseaseName)
        val toxoplasmosis = Disease(diseaseId, diseaseName)

        val pyrimethamineId = medicineRepository.addMedicineForDisease(diseaseId, medicineName = "Pyrimethamine")

        val recommendedMedicines = veterinarianService.diagnosePet(marcel, toxoplasmosis)

        recommendedMedicines.map { it.id }
            .shouldContainOnly(pyrimethamineId)

        petDiseaseRepository.getDiseasesForPet(petId)
            .shouldContainOnly(diseaseId)

        petRepository.get(petId)
            .shouldNotBeNull()
            .status.shouldBe(PetStatus.DIAGNOSED)
    }

    "Should diagnose existing, already being cured pet with still existing disease" {
        val petRequest = PetRequest(
            status = PetStatus.CURE_IN_PROGRESS,
            name = "Marcel",
            breed = "Birman Cat",
            age = 4
        )

        val petId = petRepository.insertPet(petRequest)
        val marcel = Pet(petId, petRequest.status, petRequest.name, petRequest.breed, petRequest.age)

        val diseaseName = "Toxoplasmosis"
        val diseaseId = diseaseRepository.insert(diseaseName)
        val toxoplasmosis = Disease(diseaseId, diseaseName)

        val pyrimethamineId = medicineRepository.addMedicineForDisease(diseaseId, medicineName = "Pyrimethamine")

        val recommendedMedicines = veterinarianService.diagnosePet(marcel, toxoplasmosis)

        recommendedMedicines.map { it.id }
            .shouldContainOnly(pyrimethamineId)

        petDiseaseRepository.getDiseasesForPet(petId)
            .shouldContainOnly(diseaseId)

        petRepository.get(petId)
            .shouldNotBeNull()
            .status.shouldBe(PetStatus.DIAGNOSED)
    }

    "Should not diagnose healthy pet" {
        val petRequest = PetRequest(
            status = PetStatus.HEALTHY,
            name = "Boniface",
            breed = "Havana Cat",
            age = 7
        )

        val petId = petRepository.insertPet(petRequest)
        val boniface = Pet(petId, petRequest.status, petRequest.name, petRequest.breed, petRequest.age)

        val diseaseName = "Cat Tapeworm"
        val diseaseId = diseaseRepository.insert(diseaseName)
        val catTapeworm = Disease(diseaseId, diseaseName)

        shouldThrow<PetShouldNotBeDiagnosedException> {
            veterinarianService.diagnosePet(boniface, catTapeworm)
        }
    }

    "Should not diagnose pet which does not exist" {
        val schrodinger = Pet(
            id = PetId(UUID.randomUUID()),
            status = PetStatus.UNHEALTHY,
            name = "Schrodinger",
            breed = "Abyssinian Cat",
            age = 8
        )
        petRepository.exist(schrodinger.id).shouldBeFalse()

        val diseaseName = "Roundworms"
        val diseaseId = diseaseRepository.insert(diseaseName)
        val roundworms = Disease(diseaseId, diseaseName)

        shouldThrow<PetNotFoundException> {
            veterinarianService.diagnosePet(schrodinger, roundworms)
        }
    }

    "Should not diagnose existing pet with a disease that does not exist" {
        val petRequest = PetRequest(
            status = PetStatus.UNHEALTHY,
            name = "Abby",
            breed = "Balinese-Javanese Cat",
            age = 12
        )

        val petId = petRepository.insertPet(petRequest)
        val abby = Pet(petId, petRequest.status, petRequest.name, petRequest.breed, petRequest.age)

        val nonExistentDisease = Disease(DiseaseId(UUID.randomUUID()), "Crazy ears disease")

        shouldThrow<DiseaseNotFoundException> {
            veterinarianService.diagnosePet(abby, nonExistentDisease)
        }
    }

    "Should cure existing, diagnosed pet with medicines" {
        val petRequest = PetRequest(
            status = PetStatus.DIAGNOSED,
            name = "Manfred",
            breed = "Korat Cat",
            age = 7
        )

        val petId = petRepository.insertPet(petRequest)
        val manfred = Pet(petId, petRequest.status, petRequest.name, petRequest.breed, petRequest.age)

        val diseaseName = "Cryptosporidium"
        val diseaseId = diseaseRepository.insert(diseaseName)

        val medicineName = "Nitazoxanide"
        val medicineId = medicineRepository.addMedicineForDisease(diseaseId, medicineName)
        val nitazoxanide = Medicine(medicineId, medicineName, diseaseId)

        veterinarianService.curePet(manfred, listOf(nitazoxanide))

        petMedicineRepository.getMedicinesForPet(petId)
            .shouldContainOnly(medicineId)

        petRepository.get(petId)
            .shouldNotBeNull()
            .status.shouldBe(PetStatus.CURE_IN_PROGRESS)
    }

    "Should not cure non existing pet" {
        val schrodinger = Pet(
            id = PetId(UUID.randomUUID()),
            status = PetStatus.UNHEALTHY,
            name = "Schrodinger",
            breed = "Abyssinian Cat",
            age = 8
        )
        petRepository.exist(schrodinger.id).shouldBeFalse()

        val diseaseName = "Rabies"
        val diseaseId = diseaseRepository.insert(diseaseName)

        val medicineName = "Postexposure prophylaxis"
        val medicineId = medicineRepository.addMedicineForDisease(diseaseId, medicineName)
        val postexposureProphylaxis = Medicine(medicineId, medicineName, diseaseId)

        shouldThrow<PetNotFoundException> {
            veterinarianService.curePet(schrodinger, listOf(postexposureProphylaxis))
        }
    }

    "Should not cure un-diagnosed pet" {
        val undiagnosedStatuses = PetStatus.values().toMutableList() - PetStatus.DIAGNOSED
        val undiagnosedRandomStatus = undiagnosedStatuses.random()
        val petRequest = PetRequest(
            status = undiagnosedRandomStatus,
            name = "Prince",
            breed = "Domestic Shorthair Cat",
            age = 2
        )

        val petId = petRepository.insertPet(petRequest)
        val prince = Pet(petId, petRequest.status, petRequest.name, petRequest.breed, petRequest.age)

        val diseaseName = "Salmonellosis"
        val diseaseId = diseaseRepository.insert(diseaseName)

        val medicineName = "Ceftriaxone"
        val medicineId = medicineRepository.addMedicineForDisease(diseaseId, medicineName)
        val ceftriaxone = Medicine(medicineId, medicineName, diseaseId)

        shouldThrow<PetShouldNotBeCuredException> {
            veterinarianService.curePet(prince, listOf(ceftriaxone))
        }
    }

    "Should mark pet as healthy" {
        val petStatus = PetStatus.values().random()
        val petRequest = PetRequest(
            status = petStatus,
            name = "Bobby M",
            breed = "Ragamuffin",
            age = 13
        )

        val petId = petRepository.insertPet(petRequest)
        val bobbyM = Pet(petId, petRequest.status, petRequest.name, petRequest.breed, petRequest.age)

        veterinarianService.markPetAsHealthy(bobbyM)

        petRepository.get(petId)
            .shouldNotBeNull()
            .status.shouldBe(PetStatus.HEALTHY)
    }

    "Should not mark non existing pet as healthy" {
        val schrodinger = Pet(
            id = PetId(UUID.randomUUID()),
            status = PetStatus.UNHEALTHY,
            name = "Schrodinger",
            breed = "Abyssinian Cat",
            age = 8
        )
        petRepository.exist(schrodinger.id).shouldBeFalse()

        shouldThrow<PetNotFoundException> {
            veterinarianService.markPetAsHealthy(schrodinger)
        }
    }
})
