package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import java.time.LocalDate
import java.util.UUID

data class MinimumSykdomsgrad(
    val aktorId: String,
    val fodselsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vurdering: Boolean,
    val begrunnelse: String,
    val arbeidsgivere: List<Arbeidsgiver>,
    val initierendeVedtaksperiodeId: UUID,
) : HandlingFraApi {
    data class Arbeidsgiver(
        val organisasjonsnummer: String,
        val berørtVedtaksperiodeId: UUID,
    )
}
