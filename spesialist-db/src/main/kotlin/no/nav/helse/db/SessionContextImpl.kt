package no.nav.helse.db

import kotliquery.Session

class SessionContextImpl(session: Session) : SessionContext {
    override val sykefraværstilfelleDao = PgSykefraværstilfelleDao(session)
    override val arbeidsgiverDao = PgArbeidsgiverDao(session)
}
