package no.nav.helse.db

interface SessionContext {
    val sykefraværstilfelleDao: SykefraværstilfelleDao
    val arbeidsgiverDao: ArbeidsgiverDao
    val avviksvurderingDao: AvviksvurderingDao
    val inntektskilderRepository: InntektskilderRepository
    val opptegnelseRepository: OpptegnelseRepository
}
