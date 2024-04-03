package no.nav.helse.modell.saksbehandler.handlinger.dto

import no.nav.helse.modell.vilkårsprøving.LovhjemmelDto
import java.time.LocalDate
import java.util.UUID

data class OverstyrtTidslinjeDto(
    val id: UUID,
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjedagDto>,
    val begrunnelse: String,
)

data class OverstyrtTidslinjedagDto(
    val dato: LocalDate,
    val type: String,
    val fraType: String,
    val grad: Int?,
    val fraGrad: Int?,
    val lovhjemmel: LovhjemmelDto?,
)
