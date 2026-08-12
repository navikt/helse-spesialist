package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserEvent.Metadata
import java.time.Instant

sealed interface GraderteAndreYtelserEvent {
    val metadata: Metadata

    data class Metadata(
        val graderteAndreYtelserId: GraderteAndreYtelserId,
        val sekvensnummer: Int,
        val tidspunkt: Instant,
        val utførtAvSaksbehandlerIdent: NAVIdent,
        val notatTilBeslutter: String,
        val totrinnsvurderingId: TotrinnsvurderingId,
    )

    data class Endringer(
        val graderteAndreYtelserPerioder: Endring<List<GraderteAndreYtelserPeriode>>?,
        val graderteAndreYtelserType: Endring<GraderteAndreYtelserType>?,
    )
}

data class GraderteAndreYtelserOpprettetEvent(
    override val metadata: Metadata,
    val fødselsnummer: String,
    val graderteAndreYtelserPerioder: List<GraderteAndreYtelserPeriode>,
    val graderteAndreYtelserType: GraderteAndreYtelserType,
) : GraderteAndreYtelserEvent

data class GraderteAndreYtelserEndretEvent(
    override val metadata: Metadata,
    val endringer: GraderteAndreYtelserEvent.Endringer,
) : GraderteAndreYtelserEvent

data class GraderteAndreYtelserFjernetEvent(
    override val metadata: Metadata,
) : GraderteAndreYtelserEvent

data class GraderteAndreYtelserGjenopprettetEvent(
    override val metadata: Metadata,
    val endringer: GraderteAndreYtelserEvent.Endringer,
) : GraderteAndreYtelserEvent

data class Endring<T>(
    val fra: T,
    val til: T,
)
