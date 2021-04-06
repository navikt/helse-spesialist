package no.nav.helse.mediator

import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingsstatistikkDao
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingstatistikkForSpeilDto

internal class BehandlingsstatistikkMediator(private val behandlingsstatistikkDao: BehandlingsstatistikkDao) {
    internal fun hentSaksbehandlingsstatistikk() = behandlingsstatistikkDao.oppgavestatistikk().let(BehandlingstatistikkForSpeilDto::toSpeilMap)
}
