package no.nav.helse.spesialist.application

import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.DokumentDao
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.GodkjenningsbehovUtfall
import no.nav.helse.db.LegacyBehandlingDao
import no.nav.helse.db.MetrikkDao
import no.nav.helse.db.NotatDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.PåVentDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.StansAutomatiskBehandlingSaksbehandlerDao
import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.UtbetalingDao
import no.nav.helse.db.VergemålDao
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class InMemorySessionContext(
    override val notatRepository: InMemoryNotatRepository,
    override val oppgaveRepository: InMemoryOppgaveRepository,
    override val notatDao: NotatDao,
    override val oppgaveDao: OppgaveDao,
    override val legacyVedtaksperiodeRepository: InMemoryLegacyVedtaksperiodeRepository,
    override val dialogDao: InMemoryDialogDao,
    override val stansAutomatiskBehandlingDao: InMemoryStansAutomatiskBehandlingDao,
    override val annulleringRepository: InMemoryAnnulleringRepository,
    override val saksbehandlerRepository: InMemorySaksbehandlerRepository,
    override val dokumentDao: DokumentDao,
    override val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository
) : SessionContext {
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

        override fun ferdig(hendelseId: UUID, contextId: UUID) {}

        override fun suspendert(
            hendelseId: UUID,
            contextId: UUID,
            hash: UUID,
            sti: List<Int>
        ) {
        }

        override fun feil(hendelseId: UUID, contextId: UUID) {
            TODO("Not yet implemented")
        }

        override fun tidsbrukForContext(contextId: UUID) = 100

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
    override val egenAnsattDao = object : EgenAnsattDao {
        override fun erEgenAnsatt(fødselsnummer: String) = false
        override fun lagre(fødselsnummer: String, erEgenAnsatt: Boolean, opprettet: LocalDateTime) {}
    }
    override val legacyBehandlingDao: LegacyBehandlingDao
        get() = TODO("Not yet implemented")
    override val meldingDao: InMemoryMeldingDao = InMemoryMeldingDao()
    override val metrikkDao = object : MetrikkDao {
        override fun finnUtfallForGodkjenningsbehov(contextId: UUID): GodkjenningsbehovUtfall {
            TODO("Not yet implemented")
        }
    }
    override val opptegnelseDao = object : OpptegnelseDao {
        override fun opprettOpptegnelse(
            fødselsnummer: String,
            payload: String,
            type: OpptegnelseDao.Opptegnelse.Type
        ) {
        }

        override fun finnOpptegnelser(saksbehandlerIdent: UUID): List<OpptegnelseDao.Opptegnelse> {
            TODO("Not yet implemented")
        }

    }
    override val periodehistorikkDao: PeriodehistorikkDao
        get() = TODO("Not yet implemented")

    data class Personinfo(
        val fødselsnummer: String,
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val fødselsdato: LocalDate,
        val kjønn: Kjønn,
        val adressebeskyttelse: Adressebeskyttelse
    )

    override val personDao = InMemoryPersonDao()

    override val påVentDao: PåVentDao
        get() = TODO("Not yet implemented")
    override val reservasjonDao: ReservasjonDao
        get() = TODO("Not yet implemented")
    override val risikovurderingDao: RisikovurderingDao
        get() = TODO("Not yet implemented")
    override val saksbehandlerDao: InMemorySaksbehandlerDao = InMemorySaksbehandlerDao(saksbehandlerRepository)
    override val sykefraværstilfelleDao: SykefraværstilfelleDao
        get() = TODO("Not yet implemented")
    override val tildelingDao: TildelingDao
        get() = TODO("Not yet implemented")
    override val utbetalingDao: UtbetalingDao
        get() = TODO("Not yet implemented")
    override val vedtakDao = InMemoryVedtakDao()
    override val vergemålDao: VergemålDao
        get() = TODO("Not yet implemented")
    override val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao
        get() = TODO("Not yet implemented")
    override val legacyPersonRepository: InMemoryLegacyPersonRepository = InMemoryLegacyPersonRepository()
    override val stansAutomatiskBehandlingSaksbehandlerDao: StansAutomatiskBehandlingSaksbehandlerDao
        get() = TODO("Not yet implemented")
    override val totrinnsvurderingRepository: InMemoryTotrinnsvurderingRepository = InMemoryTotrinnsvurderingRepository()
    override val overstyringRepository: OverstyringRepository
        get() = TODO("Not yet implemented")
    override val dialogRepository: DialogRepository
        get() = TODO("Not yet implemented")
    override val avviksvurderingRepository: InMemoryAvviksvurderingRepository = InMemoryAvviksvurderingRepository()
    override val behandlingRepository: InMemoryBehandlingRepository = InMemoryBehandlingRepository()
    override val tilkommenInntektRepository: TilkommenInntektRepository
        get() = TODO("Not yet implemented")
    override val arbeidsgiverRepository: InMemoryArbeidsgiverRepository = InMemoryArbeidsgiverRepository()
    override val påVentRepository: PåVentRepository
        get() = TODO("Not yet implemented")
    override val personRepository: PersonRepository
        get() = TODO("Not yet implemented")
}
