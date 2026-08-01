package com.esde.companion.domain.model

/**
 * Both the requested pool (what eligibility asked for) and, after fallback, the resolved
 * pool (what was actually loaded) - comparing two resolved values is how the coordinator
 * decides whether a pool transition is a no-op (e.g. two systems both falling back to
 * [General]) versus a real switch requiring a new track.
 */
sealed class MusicPool {
    data object General : MusicPool()

    data class PerSystem(val systemShortName: String) : MusicPool()
}
