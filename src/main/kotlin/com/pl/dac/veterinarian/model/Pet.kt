package com.pl.dac.veterinarian.model

import java.util.UUID

data class Pet(
    val id: PetId,
    val status: PetStatus,
    val name: String,
    val breed: String,
    val age: Int
)

@JvmInline
value class PetId(val value: UUID)

enum class PetStatus {
    UNHEALTHY, DIAGNOSED, CURE_IN_PROGRESS, HEALTHY
}
