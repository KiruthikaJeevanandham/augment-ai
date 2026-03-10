package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class VersionResponse(val version: String, val gitCommit: String)
