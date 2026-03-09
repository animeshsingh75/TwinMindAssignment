package com.example.twinmindassignment.model

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionResponse (
    val text: String
)