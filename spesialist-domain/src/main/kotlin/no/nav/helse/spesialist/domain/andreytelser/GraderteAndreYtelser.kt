package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant
import java.util.UUID

@JvmInline
value class GraderteAndreYtelserId(
    val value: UUID,
) : ValueObject

class GraderteAndreYtelser private constructor(
    opprettetEvent: GraderteAndreYtelserOpprettetEvent,
) : AggregateRoot<GraderteAndreYtelserId>(opprettetEvent.metadata.graderteAndreYtelserId) {
    private val _events: MutableList<GraderteAndreYtelserEvent> = mutableListOf(opprettetEvent)
    val events: List<GraderteAndreYtelserEvent> get() = _events

    val identitetsnummer: Identitetsnummer = Identitetsnummer.fraString(opprettetEvent.fødselsnummer)
    val perioder: List<GraderteAndreYtelserPeriode> = opprettetEvent.graderteAndreYtelserPerioder
    val graderteAndreYtelserType: GraderteAndreYtelserType = opprettetEvent.graderteAndreYtelserType

    companion object {
        fun fraLagring(events: List<GraderteAndreYtelserEvent>): GraderteAndreYtelser = GraderteAndreYtelser(events.first() as GraderteAndreYtelserOpprettetEvent)

        fun ny(
            identitetsnummer: Identitetsnummer,
            saksbehandlerIdent: NAVIdent,
            notatTilBeslutter: String,
            totrinnsvurderingId: TotrinnsvurderingId,
            graderteAndreYtelserPerioder: List<GraderteAndreYtelserPeriode>,
            graderteAndreYtelserType: GraderteAndreYtelserType,
        ) = GraderteAndreYtelser(
            GraderteAndreYtelserOpprettetEvent(
                metadata =
                    GraderteAndreYtelserEvent.Metadata(
                        graderteAndreYtelserId = GraderteAndreYtelserId(UUID.randomUUID()),
                        sekvensnummer = 1,
                        tidspunkt = Instant.now(),
                        utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                        notatTilBeslutter = notatTilBeslutter,
                        totrinnsvurderingId = totrinnsvurderingId,
                    ),
                fødselsnummer = identitetsnummer.value,
                graderteAndreYtelserPerioder = graderteAndreYtelserPerioder,
                graderteAndreYtelserType = graderteAndreYtelserType,
            ),
        )
    }
}

enum class GraderteAndreYtelserType {
    FORELDREPENGER,
    SVANGERSKAPSPENGER,
    OMSORGSPENGER,
    PLEIEPENGER,
    OPPLARINGSPENGER,
}
