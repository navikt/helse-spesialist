package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asInstant
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.asLocalDateOrNull
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.asUUIDOrNull
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.saksbehandlerhandling.HandlingType
import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling
import no.nav.helse.spesialist.domain.saksbehandlerhandling.SaksbehandlerhandlingId
import java.util.*

class SaksbehandlerhandlingRiver : TransaksjonellRiver() {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "saksbehandlerhandling")
        }

    override fun validations(): River.PacketValidation =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "saksbehandlerIdent", "handlingId", "tidspunkt", "kreverTotrinnsvurdering")
            it.requireAny("handlingType", HandlingType.entries.map { type -> type.name })
            it.interestedIn("skjæringstidspunkt", "behandlingId")
        }

    override fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(packet["fødselsnummer"].asString())
        val kreverTotrinnsvurdering = packet["kreverTotrinnsvurdering"].asBoolean()

        val totrinnsvurdering =
            if (kreverTotrinnsvurdering) {
                transaksjon.totrinnsvurderingRepository.finnAktivForPersonOrNull(identitetsnummer.value)
                    ?: Totrinnsvurdering.ny(fødselsnummer = identitetsnummer.value).also(transaksjon.totrinnsvurderingRepository::lagre)
            } else {
                null
            }

        val saksbehandlerhandling =
            Saksbehandlerhandling.ny(
                id = SaksbehandlerhandlingId(UUID.randomUUID()),
                identitetsnummer = identitetsnummer,
                totrinnsvurderingId = totrinnsvurdering?.id(),
                handlingId = packet["handlingId"].asUUID(),
                handlingType = enumValueOf<HandlingType>(packet["handlingType"].asString()),
                saksbehandlerIdent = packet["saksbehandlerIdent"].asString(),
                tidspunkt = packet["tidspunkt"].asInstant(),
                skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDateOrNull(),
                behandlingId = packet["behandlingId"].asUUIDOrNull()?.let { SpleisBehandlingId(it) },
            )

        transaksjon.saksbehandlerhandlingRepository.lagre(saksbehandlerhandling)
    }
}
