package no.nav.helse.spesialist.domain.saksbehandlerhandling

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant
import java.time.LocalDate
import java.util.*


@JvmInline
value class SaksbehandlerhandlingId(
    val value: UUID,
) : ValueObject

class Saksbehandlerhandling private constructor(
    id: SaksbehandlerhandlingId,
    val identitetsnummer: Identitetsnummer,
    val handlingId: UUID,
    val handlingType: HandlingType,
    val saksbehandlerIdent: String,
    val tidspunkt: Instant,
    val totrinnsvurderingId: TotrinnsvurderingId?,
    val skjæringstidspunkt: LocalDate?,
    val behandlingId: SpleisBehandlingId?
    ) : AggregateRoot<SaksbehandlerhandlingId>(id) {
    companion object {
        fun ny(
            id: SaksbehandlerhandlingId,
            identitetsnummer: Identitetsnummer,
            handlingId: UUID,
            handlingType: HandlingType,
            saksbehandlerIdent: String,
            tidspunkt: Instant,
            skjæringstidspunkt: LocalDate?,
            behandlingId: SpleisBehandlingId?,
            totrinnsvurderingId: TotrinnsvurderingId?
        ) = Saksbehandlerhandling(
            id = id,
            identitetsnummer = identitetsnummer,
            handlingId = handlingId,
            handlingType = handlingType,
            saksbehandlerIdent = saksbehandlerIdent,
            tidspunkt = tidspunkt,
            skjæringstidspunkt = skjæringstidspunkt,
            behandlingId = behandlingId,
            totrinnsvurderingId = totrinnsvurderingId,
        )

        fun fraLagring(
            id: SaksbehandlerhandlingId,
            identitetsnummer: Identitetsnummer,
            handlingId: UUID,
            handlingType: HandlingType,
            saksbehandlerIdent: String,
            tidspunkt: Instant,
            skjæringstidspunkt: LocalDate?,
            behandlingId: SpleisBehandlingId?,
            totrinnsvurderingId: TotrinnsvurderingId?,
        ) = Saksbehandlerhandling(
            id = id,
            identitetsnummer = identitetsnummer,
            handlingId = handlingId,
            handlingType = handlingType,
            saksbehandlerIdent = saksbehandlerIdent,
            tidspunkt = tidspunkt,
            skjæringstidspunkt = skjæringstidspunkt,
            behandlingId = behandlingId,
            totrinnsvurderingId = totrinnsvurderingId,
        )
    }
}