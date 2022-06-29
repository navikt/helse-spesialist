package no.nav.helse.spesialist.api.behandlingsstatistikk

class BehandlingsstatistikkMediator(private val behandlingsstatistikkDao: BehandlingsstatistikkDao) {
    fun hentSaksbehandlingsstatistikk() = behandlingsstatistikkDao.oppgavestatistikk().let(BehandlingstatistikkForSpeilDto::toSpeilMap)
}
