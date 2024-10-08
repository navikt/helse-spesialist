package no.nav.helse.db

interface TotrinnsvurderingRepository {
    fun hentAktivTotrinnsvurdering(oppgaveId: Long): TotrinnsvurderingFraDatabase?

    fun oppdater(totrinnsvurderingFraDatabase: TotrinnsvurderingFraDatabase)
}
