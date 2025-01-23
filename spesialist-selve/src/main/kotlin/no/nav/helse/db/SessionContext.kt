package no.nav.helse.db

interface SessionContext {
    val sykefraværstilfelleDao: SykefraværstilfelleDao
    val arbeidsgiverDao: ArbeidsgiverDao
}
