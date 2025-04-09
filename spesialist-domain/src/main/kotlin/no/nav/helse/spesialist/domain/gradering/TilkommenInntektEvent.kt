package no.nav.helse.spesialist.domain.gradering

import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.gradering.TilkommenInntektEvent.Metadata
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.SortedSet

sealed interface TilkommenInntektEvent {
    val metadata: Metadata

    data class Metadata(
        val tilkommenInntektId: TilkommenInntektId,
        val sekvensnummer: Int,
        val tidspunkt: Instant,
        val utførtAvSaksbehandlerIdent: String,
        val notatTilBeslutter: String,
        val totrinnsvurderingId: TotrinnsvurderingId,
    )

    data class Endringer(
        val organisasjonsnummer: Endring<String>?,
        val fom: Endring<LocalDate>?,
        val tom: Endring<LocalDate>?,
        val periodebeløp: Endring<BigDecimal>?,
        val dager: Endring<SortedSet<LocalDate>>?,
    )
}

data class TilkommenInntektOpprettetEvent(
    override val metadata: Metadata,
    val organisasjonsnummer: String,
    val periode: Periode,
    val periodebeløp: BigDecimal,
    val dager: SortedSet<LocalDate>,
) : TilkommenInntektEvent

data class TilkommenInntektEndretEvent(
    override val metadata: Metadata,
    val endringer: TilkommenInntektEvent.Endringer,
) : TilkommenInntektEvent

data class TilkommenInntektFjernetEvent(
    override val metadata: Metadata,
) : TilkommenInntektEvent

data class TilkommenInntektGjenopprettetEvent(
    override val metadata: Metadata,
    val endringer: TilkommenInntektEvent.Endringer,
) : TilkommenInntektEvent

data class Endring<T>(val fra: T, val til: T)
