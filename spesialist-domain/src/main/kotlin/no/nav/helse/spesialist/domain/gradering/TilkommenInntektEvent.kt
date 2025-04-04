package no.nav.helse.spesialist.domain.gradering

import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

sealed interface TilkommenInntektEvent {
    val tilkommenInntektId: TilkommenInntektId
    val sekvensNummer: Int
    val tidspunkt: Instant
    val utførtAvSaksbehandlerOid: SaksbehandlerOid
    val notatTilBeslutter: String

    data class Endringer(
        val organisasjonsnummer: Endring<String>?,
        val periode: Endring<Periode>?,
        val periodebeløp: Endring<BigDecimal>?,
        val dager: Endring<Set<LocalDate>>?,
    )
}

data class TilkommenInntektOpprettetEvent(
    override val tilkommenInntektId: TilkommenInntektId,
    override val sekvensNummer: Int,
    override val tidspunkt: Instant,
    override val utførtAvSaksbehandlerOid: SaksbehandlerOid,
    override val notatTilBeslutter: String,
    val organisasjonsnummer: String,
    val periode: Periode,
    val periodebeløp: BigDecimal,
    val dager: Set<LocalDate>,
) : TilkommenInntektEvent

data class TilkommenInntektEndretEvent(
    override val tilkommenInntektId: TilkommenInntektId,
    override val sekvensNummer: Int,
    override val tidspunkt: Instant,
    override val utførtAvSaksbehandlerOid: SaksbehandlerOid,
    override val notatTilBeslutter: String,
    val endringer: TilkommenInntektEvent.Endringer,
) : TilkommenInntektEvent

data class TilkommenInntektFjernetEvent(
    override val tilkommenInntektId: TilkommenInntektId,
    override val sekvensNummer: Int,
    override val tidspunkt: Instant,
    override val utførtAvSaksbehandlerOid: SaksbehandlerOid,
    override val notatTilBeslutter: String,
) : TilkommenInntektEvent

data class TilkommenInntektGjenopprettetEvent(
    override val tilkommenInntektId: TilkommenInntektId,
    override val sekvensNummer: Int,
    override val tidspunkt: Instant,
    override val utførtAvSaksbehandlerOid: SaksbehandlerOid,
    override val notatTilBeslutter: String,
    val endringer: TilkommenInntektEvent.Endringer,
) : TilkommenInntektEvent

data class Endring<T>(val fra: T, val til: T)
