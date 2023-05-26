package com.pl.dac.veterinarian.model

data class PetRequest (
    val status: PetStatus,
    val name: String,
    val breed: String,
    val age: Int
)
