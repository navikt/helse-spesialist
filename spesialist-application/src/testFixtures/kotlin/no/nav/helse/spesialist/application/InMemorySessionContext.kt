package no.nav.helse.spesialist.application

import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.DialogDao
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.GenerasjonDao
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.MetrikkDao
import no.nav.helse.db.NotatDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.PåVentDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingSaksbehandlerDao
import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.UtbetalingDao
import no.nav.helse.db.VedtakDao
import no.nav.helse.db.VedtaksperiodeRepository
import no.nav.helse.db.VergemålDao
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.kommando.CommandContext
import java.util.UUID

class InMemorySessionContext : SessionContext {
    override val arbeidsforholdDao: ArbeidsforholdDao
        get() = TODO("Not yet implemented")
    override val automatiseringDao: AutomatiseringDao
        get() = TODO("Not yet implemented")
    override val commandContextDao = object : CommandContextDao {
        override fun nyContext(meldingId: UUID) =
            CommandContext(UUID.randomUUID())

        override fun opprett(hendelseId: UUID, contextId: UUID) {
            TODO("Not yet implemented")
        }

        override fun ferdig(hendelseId: UUID, contextId: UUID) {
            TODO("Not yet implemented")
        }

        override fun suspendert(
            hendelseId: UUID,
            contextId: UUID,
            hash: UUID,
            sti: List<Int>
        ) {
            TODO("Not yet implemented")
        }

        override fun feil(hendelseId: UUID, contextId: UUID) {
            TODO("Not yet implemented")
        }

        override fun tidsbrukForContext(contextId: UUID): Int {
            TODO("Not yet implemented")
        }

        override fun avbryt(
            vedtaksperiodeId: UUID,
            contextId: UUID
        ): List<Pair<UUID, UUID>> {
            TODO("Not yet implemented")
        }

        override fun finnSuspendert(contextId: UUID): CommandContext? {
            TODO("Not yet implemented")
        }

        override fun finnSuspendertEllerFeil(contextId: UUID): CommandContext? {
            TODO("Not yet implemented")
        }

    }
    override val dialogDao: DialogDao
        get() = TODO("Not yet implemented")
    override val egenAnsattDao: EgenAnsattDao
        get() = TODO("Not yet implemented")
    override val generasjonDao: GenerasjonDao
        get() = TODO("Not yet implemented")
    override val meldingDao: MeldingDao
        get() = TODO("Not yet implemented")
    override val metrikkDao: MetrikkDao
        get() = TODO("Not yet implemented")
    override val notatDao: NotatDao
        get() = TODO("Not yet implemented")
    override val oppgaveDao: OppgaveDao
        get() = TODO("Not yet implemented")
    override val opptegnelseDao: OpptegnelseDao
        get() = TODO("Not yet implemented")
    override val periodehistorikkDao: PeriodehistorikkDao
        get() = TODO("Not yet implemented")
    override val personDao: PersonDao
        get() = TODO("Not yet implemented")
    override val påVentDao: PåVentDao
        get() = TODO("Not yet implemented")
    override val reservasjonDao: ReservasjonDao
        get() = TODO("Not yet implemented")
    override val risikovurderingDao: RisikovurderingDao
        get() = TODO("Not yet implemented")
    override val saksbehandlerDao: SaksbehandlerDao
        get() = TODO("Not yet implemented")
    override val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao
        get() = TODO("Not yet implemented")
    override val sykefraværstilfelleDao: SykefraværstilfelleDao
        get() = TODO("Not yet implemented")
    override val tildelingDao: TildelingDao
        get() = TODO("Not yet implemented")
    override val utbetalingDao: UtbetalingDao
        get() = TODO("Not yet implemented")
    override val vedtakDao: VedtakDao
        get() = TODO("Not yet implemented")
    override val vergemålDao: VergemålDao
        get() = TODO("Not yet implemented")
    override val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao
        get() = TODO("Not yet implemented")
    override val vedtaksperiodeRepository: VedtaksperiodeRepository
        get() = TODO("Not yet implemented")
    override val personRepository: InMemoryPersonRepository = InMemoryPersonRepository()
    override val stansAutomatiskBehandlingSaksbehandlerDao: StansAutomatiskBehandlingSaksbehandlerDao
        get() = TODO("Not yet implemented")
    override val totrinnsvurderingRepository: TotrinnsvurderingRepository
        get() = TODO("Not yet implemented")
    override val overstyringRepository: OverstyringRepository
        get() = TODO("Not yet implemented")
    override val notatRepository: NotatRepository
        get() = TODO("Not yet implemented")
    override val dialogRepository: DialogRepository
        get() = TODO("Not yet implemented")
    override val saksbehandlerRepository: SaksbehandlerRepository
        get() = TODO("Not yet implemented")
    override val avviksvurderingRepository: InMemoryAvviksvurderingRepository = InMemoryAvviksvurderingRepository()
    override val oppgaveRepository: OppgaveRepository
        get() = TODO("Not yet implemented")
    override val behandlingRepository: BehandlingRepository
        get() = TODO("Not yet implemented")
    override val tilkommenInntektRepository: TilkommenInntektRepository
        get() = TODO("Not yet implemented")
    override val arbeidsgiverRepository: InMemoryArbeidsgiverRepository = InMemoryArbeidsgiverRepository()
}
