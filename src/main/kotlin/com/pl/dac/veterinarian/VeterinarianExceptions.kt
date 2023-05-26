package com.pl.dac.veterinarian

import com.pl.dac.veterinarian.model.Disease
import com.pl.dac.veterinarian.model.Pet

class PetNotFoundException(pet: Pet) : RuntimeException("Pet ${pet.id.value} not found")

class DiseaseNotFoundException(disease: Disease) : RuntimeException("Disease ${disease.id.value} not found")

class PetShouldNotBeDiagnosedException(pet: Pet) : RuntimeException("Pet ${pet.id.value} has incorrect status ${pet.status} and should not be diagnosed")

class PetShouldNotBeCuredException(pet: Pet) : RuntimeException("Pet ${pet.id.value} has incorrect status ${pet.status} and should not be cured")
