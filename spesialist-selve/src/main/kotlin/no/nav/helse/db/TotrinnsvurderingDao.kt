package no.nav.helse.db

interface TotrinnsvurderingDao {
    fun hentAktivTotrinnsvurdering(oppgaveId: Long): Pair<Long, TotrinnsvurderingFraDatabase>?

    fun oppdater(totrinnsvurderingFraDatabase: TotrinnsvurderingFraDatabase)
}
