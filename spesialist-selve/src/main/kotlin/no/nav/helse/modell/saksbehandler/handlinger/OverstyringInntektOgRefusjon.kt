package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.mediator.meldinger.Personhendelse
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
    val oid: UUID,
    val arbeidsgivere: List<OverstyrtArbeidsgiver>,
    val skjæringstidspunkt: LocalDate,
    val opprettet: LocalDateTime,
    private val json: String,
) : Personhendelse {
    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
}

internal class OverstyrInntektOgRefusjonCommand(
    id: UUID,
    fødselsnummer: String,
    oid: UUID,
    skjæringstidspunkt: LocalDate,
    arbeidsgivere: List<OverstyrtArbeidsgiver>,
    opprettet: LocalDateTime,
    overstyringDao: OverstyringDao,
    overstyringMediator: OverstyringMediator,
    json: String
) : MacroCommand() {
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

}
