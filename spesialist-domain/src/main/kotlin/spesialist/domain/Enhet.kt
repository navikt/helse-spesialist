package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.ValueObject

data class Enhet(
    val enhetNr: String,
    val navn: String,
    val type: String,
) : ValueObject
