package no.nav.helse.spesialist.db

import kotliquery.Session
import no.nav.helse.db.*
import no.nav.helse.db.overstyring.venting.VenterPåKvitteringForOverstyringRepository
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.spesialist.application.*
import no.nav.helse.spesialist.db.dao.*
import no.nav.helse.spesialist.db.repository.*

class DBSessionContext(
    session: Session,
) : SessionContext {
    override val arbeidsforholdDao = PgArbeidsforholdDao(session)
    override val automatiseringDao = PgAutomatiseringDao(session)
    override val commandContextDao = PgCommandContextDao(session)
    override val dialogDao = PgDialogDao(session)
    override val legacyBehandlingDao = PgLegacyBehandlingDao(session)
    override val meldingDao = PgMeldingDao(session)
    override val metrikkDao = PgMetrikkDao(session)
    override val notatDao = PgNotatDao(session)
    override val oppgaveDao = PgOppgaveDao(session)
    override val periodehistorikkDao = PgPeriodehistorikkDao(session)
    override val personDao = PgPersonDao(session)
    override val påVentDao = PgPåVentDao(session)
    override val reservasjonDao = PgReservasjonDao(session)
    override val risikovurderingDao = PgRisikovurderingDao(session)
    override val saksbehandlerDao = PgSaksbehandlerDao(session)
    override val sykefraværstilfelleDao = PgSykefraværstilfelleDao(session)
    override val tildelingDao = PgTildelingDao(session)
    override val utbetalingDao = PgUtbetalingDao(session)
    override val vedtakDao = PgVedtakDao(session)
    override val vergemålDao = PgVergemålDao(session)
    override val dokumentDao = PgDokumentDao(session)
    override val åpneGosysOppgaverDao = PgÅpneGosysOppgaverDao(session)
    override val legacyVedtaksperiodeRepository = PgLegacyVedtaksperiodeRepository(legacyBehandlingDao, vedtakDao)
    override val legacyPersonRepository =
        PgLegacyPersonRepository(session, legacyVedtaksperiodeRepository, sykefraværstilfelleDao, personDao)
    override val saksbehandlerStansRepository = PgSaksbehandlerStansRepository(session)
    override val veilederStansRepository = PgVeilederStansRepository(session)

    override val overstyringRepository: OverstyringRepository = PgOverstyringRepository(session)
    override val totrinnsvurderingRepository: TotrinnsvurderingRepository = PgTotrinnsvurderingRepository(session)
    override val notatRepository: NotatRepository = PgNotatRepository(session)
    override val dialogRepository: DialogRepository = PgDialogRepository(session)
    override val saksbehandlerRepository: SaksbehandlerRepository = PgSaksbehandlerRepository(session)
    override val avviksvurderingRepository: AvviksvurderingRepository = PgAvviksvurderingRepository(session)
    override val oppgaveRepository: OppgaveRepository = PgOppgaveRepository(session)
    override val behandlingRepository: BehandlingRepository = PgBehandlingRepository(session)
    override val graderteAndreYtelserRepository: GraderteAndreYtelserRepository = PgGraderteAndreYtelserRepository(session)
    override val tilkommenInntektRepository: TilkommenInntektRepository = PgTilkommenInntektRepository(session)
    override val arbeidsgiverRepository: ArbeidsgiverRepository = PgArbeidsgiverRepository(session)
    override val annulleringRepository: PgAnnulleringRepository = PgAnnulleringRepository(session)
    override val påVentRepository: PåVentRepository = PgPåVentRepository(session)
    override val personRepository: PersonRepository = PgPersonRepository(session)
    override val vedtaksperiodeRepository: VedtaksperiodeRepository = PgVedtaksperiodeRepository(session)
    override val varselRepository: VarselRepository = PgVarselRepository(session)
    override val varseldefinisjonRepository: VarseldefinisjonRepository = PgVarseldefinisjonRepository(session)
    override val individuellBegrunnelseRepository: IndividuellBegrunnelseRepository = PgIndividuellBegrunnelseRepository(session)
    override val midlertidigBehandlingVedtakFattetDao: MidlertidigBehandlingVedtakFattetDao =
        PgMidlertidigBehandlingVedtakFattetDao(session)
    override val vedtakRepository: VedtakRepository = PgVedtakRepository(session)
    override val vedtakBegrunnelseDao: VedtakBegrunnelseDao = PgVedtakBegrunnelseDao(session)
    override val opptegnelseRepository: OpptegnelseRepository = PgOpptegnelseRepository(session)
    override val venterPåKvitteringForOverstyringRepository: VenterPåKvitteringForOverstyringRepository = PgVenterPåKvitteringForOverstyringRepository(session)
}
