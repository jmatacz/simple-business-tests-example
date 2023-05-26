package com.pl.dac.veterinarian.model

import java.util.UUID

data class Medicine (
    val id: MedicineId,
    val name: String,
    val recommendedForDisease: DiseaseId
)

@JvmInline
value class MedicineId(val value: UUID)
