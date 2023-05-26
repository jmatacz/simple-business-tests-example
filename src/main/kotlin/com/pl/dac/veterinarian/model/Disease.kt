package com.pl.dac.veterinarian.model

import java.util.UUID

data class Disease(
    val id: DiseaseId,
    val name: String
)

@JvmInline
value class DiseaseId(val value: UUID)
