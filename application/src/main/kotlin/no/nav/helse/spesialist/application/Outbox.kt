package no.nav.helse.spesialist.application

import no.nav.helse.MeldingPubliserer
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.spesialist.domain.Identitetsnummer
import java.util.UUID

class Outbox(
    private val versjonAvKode: String,
) {
    private val outbox = mutableListOf<OutboxMelding>()

    private sealed interface OutboxMelding

    private data class OutboxBehovListe(
        val hendelseId: UUID,
        val commandContextId: UUID,
        val identitetsnummer: Identitetsnummer,
        val behov: List<Behov>,
    ) : OutboxMelding

    fun leggTil(
        hendelseId: UUID,
        commandContextId: UUID,
        identitetsnummer: Identitetsnummer,
        behov: List<Behov>,
    ) {
        outbox.add(
            OutboxBehovListe(
                hendelseId = hendelseId,
                commandContextId = commandContextId,
                identitetsnummer = identitetsnummer,
                behov = behov,
            ),
        )
    }

    private data class OutboxKommandokjedeEndretEvent(
        val identitetsnummer: Identitetsnummer,
        val event: KommandokjedeEndretEvent,
        val hendelseNavn: String,
    ) : OutboxMelding

    fun leggTil(
        identitetsnummer: Identitetsnummer,
        event: KommandokjedeEndretEvent,
        hendelseNavn: String,
    ) {
        outbox.add(
            OutboxKommandokjedeEndretEvent(
                identitetsnummer = identitetsnummer,
                event = event,
                hendelseNavn = hendelseNavn,
            ),
        )
    }

    private data class OutboxSubsumsjon(
        val identitetsnummer: Identitetsnummer,
        val subsumsjonEvent: SubsumsjonEvent,
        val versjonAvKode: String,
    ) : OutboxMelding

    fun leggTil(
        identitetsnummer: Identitetsnummer,
        subsumsjonEvent: SubsumsjonEvent,
    ) {
        outbox.add(
            OutboxSubsumsjon(
                identitetsnummer = identitetsnummer,
                subsumsjonEvent = subsumsjonEvent,
                versjonAvKode = versjonAvKode,
            ),
        )
    }

    private data class OutboxUtgåendeHendelse(
        val identitetsnummer: Identitetsnummer,
        val hendelse: UtgåendeHendelse,
        val årsak: String,
    ) : OutboxMelding

    fun leggTil(
        identitetsnummer: Identitetsnummer,
        hendelse: UtgåendeHendelse,
        årsak: String,
    ) {
        outbox.add(
            OutboxUtgåendeHendelse(
                identitetsnummer = identitetsnummer,
                hendelse = hendelse,
                årsak = årsak,
            ),
        )
    }

    fun sendAlle(meldingPubliserer: MeldingPubliserer) {
        outbox.forEach {
            when (it) {
                is OutboxBehovListe ->
                    meldingPubliserer.publiser(
                        hendelseId = it.hendelseId,
                        commandContextId = it.commandContextId,
                        fødselsnummer = it.identitetsnummer.value,
                        behov = it.behov,
                    )

                is OutboxKommandokjedeEndretEvent ->
                    meldingPubliserer.publiser(
                        fødselsnummer = it.identitetsnummer.value,
                        event = it.event,
                        hendelseNavn = it.hendelseNavn,
                    )

                is OutboxSubsumsjon ->
                    meldingPubliserer.publiser(
                        fødselsnummer = it.identitetsnummer.value,
                        subsumsjonEvent = it.subsumsjonEvent,
                        versjonAvKode = it.versjonAvKode,
                    )

                is OutboxUtgåendeHendelse ->
                    meldingPubliserer.publiser(
                        fødselsnummer = it.identitetsnummer.value,
                        hendelse = it.hendelse,
                        årsak = it.årsak,
                    )
            }
        }
    }
}
