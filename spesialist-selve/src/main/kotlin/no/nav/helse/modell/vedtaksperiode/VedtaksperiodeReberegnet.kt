package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.VedtaksperiodeReberegnetPeriodehistorikk
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao

internal class VedtaksperiodeReberegnet(
    override val id: UUID,
    private val fødselsnummer: String,
    vedtaksperiodeId: UUID,
    commandContextDao: CommandContextDao,
    private val json: String,
    oppgaveMediator: OppgaveMediator,
    periodehistorikkDao: PeriodehistorikkDao,
    utbetalingDao: UtbetalingDao,
) : Hendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    override val commands: List<Command> = listOf(
        VedtaksperiodeReberegnetPeriodehistorikk(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingDao = utbetalingDao,
            periodehistorikkDao = periodehistorikkDao
        ),
        AvbrytCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            commandContextDao = commandContextDao,
            oppgaveMediator = oppgaveMediator
        )
    )

}
