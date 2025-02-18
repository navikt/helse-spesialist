package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.spesialist.application.DialogRepository
import no.nav.helse.spesialist.application.NotatRepository
import no.nav.helse.spesialist.application.OverstyringRepository
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.db.PgDialogRepository
import no.nav.helse.spesialist.db.PgNotatRepository
import no.nav.helse.spesialist.db.PgOverstyringRepository
import no.nav.helse.spesialist.db.PgSaksbehandlerRepository
import no.nav.helse.spesialist.db.PgTotrinnsvurderingRepository

class DBSessionContext(session: Session, tilgangskontroll: Tilgangskontroll) : SessionContext {
    override val arbeidsforholdDao = PgArbeidsforholdDao(session)
    override val arbeidsgiverDao = PgArbeidsgiverDao(session)
    override val automatiseringDao = PgAutomatiseringDao(session)
    override val commandContextDao = PgCommandContextDao(session)
    override val dialogDao = PgDialogDao(session)
    override val egenAnsattDao = PgEgenAnsattDao(session)
    override val generasjonDao = PgGenerasjonDao(session)
    override val inntektskilderRepository = PgInntektskilderRepository(session, arbeidsgiverDao)
    override val meldingDao = PgMeldingDao(session)
    override val metrikkDao = PgMetrikkDao(session)
    override val notatDao = PgNotatDao(session)
    override val oppgaveDao = PgOppgaveDao(session)
    override val opptegnelseDao = PgOpptegnelseDao(session)
    override val overstyringDao = PgOverstyringDao(session)
    override val periodehistorikkDao = PgPeriodehistorikkDao(session)
    override val personDao = PgPersonDao(session)
    override val påVentDao = PgPåVentDao(session)
    override val reservasjonDao = PgReservasjonDao(session)
    override val risikovurderingDao = PgRisikovurderingDao(session)
    override val saksbehandlerDao = PgSaksbehandlerDao(session, tilgangskontroll)
    override val stansAutomatiskBehandlingDao = PgStansAutomatiskBehandlingDao(session)
    override val sykefraværstilfelleDao = PgSykefraværstilfelleDao(session)
    override val tildelingDao = PgTildelingDao(session)
    override val totrinnsvurderingDao = PgTotrinnsvurderingDao(session)
    override val utbetalingDao = PgUtbetalingDao(session)
    override val vedtakDao = PgVedtakDao(session)
    override val vergemålDao = PgVergemålDao(session)
    override val åpneGosysOppgaverDao = PgÅpneGosysOppgaverDao(session)
    override val vedtaksperiodeRepository = PgVedtaksperiodeRepository(generasjonDao, vedtakDao)
    override val personRepository = PgPersonRepository(session, vedtaksperiodeRepository, sykefraværstilfelleDao, personDao)

    override val overstyringRepository: OverstyringRepository = PgOverstyringRepository(session)
    override val totrinnsvurderingRepository: TotrinnsvurderingRepository =
        PgTotrinnsvurderingRepository(session, tilgangskontroll)
    override val notatRepository: NotatRepository = PgNotatRepository(session)
    override val dialogRepository: DialogRepository = PgDialogRepository(session)
    override val saksbehandlerRepository: SaksbehandlerRepository = PgSaksbehandlerRepository(session)
    override val avviksvurderingRepository: AvviksvurderingRepository = PgAvviksvurderingRepository(session)
}
