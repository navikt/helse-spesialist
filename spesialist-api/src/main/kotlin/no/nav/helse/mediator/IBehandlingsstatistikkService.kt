package no.nav.helse.mediator

import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse
import java.time.LocalDate

interface IBehandlingsstatistikkService {
    fun getBehandlingsstatistikk(fom: LocalDate = LocalDate.now()): BehandlingsstatistikkResponse
}
