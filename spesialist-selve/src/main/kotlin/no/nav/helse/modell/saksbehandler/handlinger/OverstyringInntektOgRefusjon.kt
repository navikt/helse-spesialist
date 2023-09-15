package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.PersisterOverstyringInntektOgRefusjonCommand
import no.nav.helse.modell.kommando.PubliserOverstyringCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.overstyring.OverstyrtArbeidsgiver

/**
 * Tar vare på overstyring av inntekt fra saksbehandler og sletter den opprinnelige oppgaven i påvente av nytt
 * godkjenningsbehov fra spleis.
 *
 * Det er primært spleis som håndterer dette eventet.
 */
internal class OverstyringInntektOgRefusjon(
    override val id: UUID,
    private val fødselsnummer: String,
    oid: UUID,
    arbeidsgivere: List<OverstyrtArbeidsgiver>,
    skjæringstidspunkt: LocalDate,
    opprettet: LocalDateTime,
    private val json: String,
    overstyringDao: OverstyringDao,
    overstyringMediator: OverstyringMediator,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        PersisterOverstyringInntektOgRefusjonCommand(
            oid = oid,
            hendelseId = id,
            fødselsnummer = fødselsnummer,
            arbeidsgivere = arbeidsgivere,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            overstyringDao = overstyringDao
        ),
        PubliserOverstyringCommand(
            eventName = "overstyr_inntekt_og_refusjon",
            hendelseId = id,
            json = json,
            overstyringMediator = overstyringMediator,
            overstyringDao = overstyringDao,
        )
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json

}
