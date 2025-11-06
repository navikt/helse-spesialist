@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiArbeidstidsvurderingRequest(
    val aktørId: String,
    val fødselsnummer: String,
    val perioderVurdertOk: List<ApiDatoPeriode>,
    val perioderVurdertIkkeOk: List<ApiDatoPeriode>,
    val begrunnelse: String,
    val arbeidsgivere: List<Arbeidsgiver>,
    val initierendeVedtaksperiodeId: UUID,
) {
    @Serializable
    data class Arbeidsgiver(
        val organisasjonsnummer: String,
        val berørtVedtaksperiodeId: UUID,
    )
}
