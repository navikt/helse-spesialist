package no.nav.helse.db

import kotliquery.Session

class DBSessionContext(session: Session) : SessionContext {
    override val sykefraværstilfelleDao = PgSykefraværstilfelleDao(session)
    override val arbeidsgiverDao = PgArbeidsgiverDao(session)
    override val avviksvurderingDao = PgAvviksvurderingDao(session)
    override val inntektskilderRepository = PgInntektskilderRepository(session, arbeidsgiverDao, avviksvurderingDao)
}
