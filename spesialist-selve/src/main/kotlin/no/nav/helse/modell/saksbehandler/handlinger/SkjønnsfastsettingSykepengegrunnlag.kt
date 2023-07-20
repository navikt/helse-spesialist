package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.PersisterSkjønnsfastsettingSykepengegrunnlagCommand
import no.nav.helse.modell.kommando.PubliserOverstyringCommand
import no.nav.helse.modell.kommando.PubliserSubsumsjonCommand
import no.nav.helse.modell.kommando.ReserverPersonCommand
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.overstyring.SkjønnsfastsattArbeidsgiver
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao

/**
 * Tar vare på overstyring av inntekt fra saksbehandler og sletter den opprinnelige oppgaven i påvente av nytt
 * godkjenningsbehov fra spleis.
 *
 * Det er primært spleis som håndterer dette eventet.
 */
internal class SkjønnsfastsettingSykepengegrunnlag(
    override val id: UUID,
    private val fødselsnummer: String,
    oid: UUID,
    arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
    skjæringstidspunkt: LocalDate,
    opprettet: LocalDateTime,
    private val json: String,
    reservasjonDao: ReservasjonDao,
    oppgaveDao: OppgaveDao,
    tildelingDao: TildelingDao,
    overstyringDao: OverstyringDao,
    overstyringMediator: OverstyringMediator,
    versjonAvKode: String?,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        ReserverPersonCommand(oid, fødselsnummer, reservasjonDao, oppgaveDao, tildelingDao),
        PersisterSkjønnsfastsettingSykepengegrunnlagCommand(
            oid = oid,
            hendelseId = id,
            fødselsnummer = fødselsnummer,
            arbeidsgivere = arbeidsgivere,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            overstyringDao = overstyringDao
        ),
        PubliserOverstyringCommand(
            eventName = "skjønnsmessig_fastsettelse",
            hendelseId = id,
            json = json,
            overstyringMediator = overstyringMediator,
            overstyringDao = overstyringDao,
        ),
        PubliserSubsumsjonCommand(
            fødselsnummer = fødselsnummer,
            arbeidsgivere = arbeidsgivere,
            overstyringMediator = overstyringMediator,
            versjonAvKode = versjonAvKode
        )
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json

}
