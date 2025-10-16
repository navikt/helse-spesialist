package no.nav.helse.spesialist.db

import kotliquery.Session
import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.spesialist.application.ArbeidsgiverRepository
import no.nav.helse.spesialist.application.DialogRepository
import no.nav.helse.spesialist.application.NotatRepository
import no.nav.helse.spesialist.application.OverstyringRepository
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.application.PåVentRepository
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.application.TilkommenInntektRepository
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.db.dao.PgAnnulleringRepository
import no.nav.helse.spesialist.db.dao.PgArbeidsforholdDao
import no.nav.helse.spesialist.db.dao.PgAutomatiseringDao
import no.nav.helse.spesialist.db.dao.PgAvviksvurderingRepository
import no.nav.helse.spesialist.db.dao.PgCommandContextDao
import no.nav.helse.spesialist.db.dao.PgDialogDao
import no.nav.helse.spesialist.db.dao.PgDokumentDao
import no.nav.helse.spesialist.db.dao.PgEgenAnsattDao
import no.nav.helse.spesialist.db.dao.PgGenerasjonDao
import no.nav.helse.spesialist.db.dao.PgLegacyPersonRepository
import no.nav.helse.spesialist.db.dao.PgMeldingDao
import no.nav.helse.spesialist.db.dao.PgMetrikkDao
import no.nav.helse.spesialist.db.dao.PgNotatDao
import no.nav.helse.spesialist.db.dao.PgOppgaveDao
import no.nav.helse.spesialist.db.dao.PgOpptegnelseDao
import no.nav.helse.spesialist.db.dao.PgPeriodehistorikkDao
import no.nav.helse.spesialist.db.dao.PgPersonDao
import no.nav.helse.spesialist.db.dao.PgPåVentDao
import no.nav.helse.spesialist.db.dao.PgReservasjonDao
import no.nav.helse.spesialist.db.dao.PgRisikovurderingDao
import no.nav.helse.spesialist.db.dao.PgSaksbehandlerDao
import no.nav.helse.spesialist.db.dao.PgStansAutomatiskBehandlingDao
import no.nav.helse.spesialist.db.dao.PgStansAutomatiskBehandlingSaksbehandlerDao
import no.nav.helse.spesialist.db.dao.PgSykefraværstilfelleDao
import no.nav.helse.spesialist.db.dao.PgTildelingDao
import no.nav.helse.spesialist.db.dao.PgUtbetalingDao
import no.nav.helse.spesialist.db.dao.PgVedtakDao
import no.nav.helse.spesialist.db.dao.PgVedtaksperiodeRepository
import no.nav.helse.spesialist.db.dao.PgVergemålDao
import no.nav.helse.spesialist.db.dao.PgÅpneGosysOppgaverDao
import no.nav.helse.spesialist.db.repository.PgArbeidsgiverRepository
import no.nav.helse.spesialist.db.repository.PgBehandlingRepository
import no.nav.helse.spesialist.db.repository.PgDialogRepository
import no.nav.helse.spesialist.db.repository.PgNotatRepository
import no.nav.helse.spesialist.db.repository.PgOppgaveRepository
import no.nav.helse.spesialist.db.repository.PgOverstyringRepository
import no.nav.helse.spesialist.db.repository.PgPersonRepository
import no.nav.helse.spesialist.db.repository.PgPåVentRepository
import no.nav.helse.spesialist.db.repository.PgSaksbehandlerRepository
import no.nav.helse.spesialist.db.repository.PgTilkommenInntektRepository
import no.nav.helse.spesialist.db.repository.PgTotrinnsvurderingRepository

class DBSessionContext(
    session: Session,
) : SessionContext {
    override val arbeidsforholdDao = PgArbeidsforholdDao(session)
    override val automatiseringDao = PgAutomatiseringDao(session)
    override val commandContextDao = PgCommandContextDao(session)
    override val dialogDao = PgDialogDao(session)
    override val egenAnsattDao = PgEgenAnsattDao(session)
    override val generasjonDao = PgGenerasjonDao(session)
    override val meldingDao = PgMeldingDao(session)
    override val metrikkDao = PgMetrikkDao(session)
    override val notatDao = PgNotatDao(session)
    override val oppgaveDao = PgOppgaveDao(session)
    override val opptegnelseDao = PgOpptegnelseDao(session)
    override val periodehistorikkDao = PgPeriodehistorikkDao(session)
    override val personDao = PgPersonDao(session)
    override val påVentDao = PgPåVentDao(session)
    override val reservasjonDao = PgReservasjonDao(session)
    override val risikovurderingDao = PgRisikovurderingDao(session)
    override val saksbehandlerDao = PgSaksbehandlerDao(session)
    override val stansAutomatiskBehandlingDao = PgStansAutomatiskBehandlingDao(session)
    override val sykefraværstilfelleDao = PgSykefraværstilfelleDao(session)
    override val tildelingDao = PgTildelingDao(session)
    override val utbetalingDao = PgUtbetalingDao(session)
    override val vedtakDao = PgVedtakDao(session)
    override val vergemålDao = PgVergemålDao(session)
    override val dokumentDao = PgDokumentDao(session)
    override val åpneGosysOppgaverDao = PgÅpneGosysOppgaverDao(session)
    override val vedtaksperiodeRepository = PgVedtaksperiodeRepository(generasjonDao, vedtakDao)
    override val legacyPersonRepository =
        PgLegacyPersonRepository(session, vedtaksperiodeRepository, sykefraværstilfelleDao, personDao)
    override val stansAutomatiskBehandlingSaksbehandlerDao = PgStansAutomatiskBehandlingSaksbehandlerDao(session)

    override val overstyringRepository: OverstyringRepository = PgOverstyringRepository(session)
    override val totrinnsvurderingRepository: TotrinnsvurderingRepository = PgTotrinnsvurderingRepository(session)
    override val notatRepository: NotatRepository = PgNotatRepository(session)
    override val dialogRepository: DialogRepository = PgDialogRepository(session)
    override val saksbehandlerRepository: SaksbehandlerRepository = PgSaksbehandlerRepository(session)
    override val avviksvurderingRepository: AvviksvurderingRepository = PgAvviksvurderingRepository(session)
    override val oppgaveRepository: OppgaveRepository = PgOppgaveRepository(session)
    override val behandlingRepository: BehandlingRepository = PgBehandlingRepository(session)
    override val tilkommenInntektRepository: TilkommenInntektRepository = PgTilkommenInntektRepository(session)
    override val arbeidsgiverRepository: ArbeidsgiverRepository = PgArbeidsgiverRepository(session)
    override val annulleringRepository: PgAnnulleringRepository = PgAnnulleringRepository(session)
    override val påVentRepository: PåVentRepository = PgPåVentRepository(session)
    override val personRepository: PersonRepository = PgPersonRepository(session)
}
