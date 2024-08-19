package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import java.util.UUID

data class AnnulleringData(
    val aktorId: String,
    val fodselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val begrunnelser: List<String>,
    val arsaker: List<AnnulleringArsak>?,
    val kommentar: String?,
) : HandlingFraApi

data class AnnulleringArsak(
    val _key: String,
    val arsak: String,
)
