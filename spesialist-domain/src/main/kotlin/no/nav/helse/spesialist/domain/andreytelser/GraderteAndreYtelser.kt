package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant
import java.util.*

@JvmInline
value class GraderteAndreYtelserId(
    val value: UUID,
) : ValueObject

class GraderteAndreYtelser private constructor(
    opprettetEvent: GraderteAndreYtelserOpprettetEvent,
) : AggregateRoot<GraderteAndreYtelserId>(opprettetEvent.metadata.graderteAndreYtelserId) {
    val events: List<GraderteAndreYtelserEvent> = listOf(opprettetEvent)

    val identitetsnummer: Identitetsnummer = Identitetsnummer.fraString(opprettetEvent.fødselsnummer)
    val totrinnsvurderingId: TotrinnsvurderingId = opprettetEvent.metadata.totrinnsvurderingId
    val graderteAndreYtelserType: GraderteAndreYtelserType = opprettetEvent.graderteAndreYtelserType
    val versjon: Int = opprettetEvent.metadata.sekvensnummer

    companion object {
        fun fraLagring(event: GraderteAndreYtelserOpprettetEvent): GraderteAndreYtelser = GraderteAndreYtelser(event)

        fun ny(
            identitetsnummer: Identitetsnummer,
            saksbehandlerIdent: NAVIdent,
            notatTilBeslutter: String,
            totrinnsvurderingId: TotrinnsvurderingId,
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
