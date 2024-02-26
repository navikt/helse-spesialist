package no.nav.helse.modell.saksbehandler.handlinger.dto

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.vilkårsprøving.LovhjemmelDto

data class OverstyrtTidslinjeDto(
    val id: UUID,
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjedagDto>,
    val begrunnelse: String
)

data class OverstyrtTidslinjedagDto(
    val dato: LocalDate,
    val type: String,
    val fraType: String,
    val grad: Int?,
    val fraGrad: Int?,
    val lovhjemmel: LovhjemmelDto?
)