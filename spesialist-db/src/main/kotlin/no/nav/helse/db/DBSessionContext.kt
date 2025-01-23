package no.nav.helse.db

import kotliquery.Session

class DBSessionContext(session: Session) : SessionContext {
    override val sykefraværstilfelleDao = PgSykefraværstilfelleDao(session)
    override val arbeidsgiverDao = PgArbeidsgiverDao(session)
    override val avviksvurderingDao = PgAvviksvurderingDao(session)
    override val opptegnelseRepository = PgOpptegnelseRepository(session)
    override val inntektskilderRepository = PgInntektskilderRepository(session, arbeidsgiverDao, avviksvurderingDao)
    override val dialogDao = PgDialogDao(session)
    override val notatDao = PgNotatDao(session)
    override val oppgaveDao = PgOppgaveDao(session)
    override val periodehistorikkDao = PgPeriodehistorikkDao(session)
    override val totrinnsvurderingDao = PgTotrinnsvurderingDao(session)
    override val vedtakDao = PgVedtakDao(session)
    override val saksbehandlerDao = PgSaksbehandlerDao(session)
    override val stansAutomatiskBehandlingDao = PgStansAutomatiskBehandlingDao(session)
    override val tildelingDao = PgTildelingDao(session)
    override val generasjonDao = PgGenerasjonDao(session)
    override val reservasjonDao = PgReservasjonDao(session)
    override val commandContextDao = PgCommandContextDao(session)
    override val arbeidsforholdDao = PgArbeidsforholdDao(session)
    override val automatiseringDao = PgAutomatiseringDao(session)
    override val egenAnsattDao = PgEgenAnsattDao(session)
    override val åpneGosysOppgaverDao = PgÅpneGosysOppgaverDao(session)
}
