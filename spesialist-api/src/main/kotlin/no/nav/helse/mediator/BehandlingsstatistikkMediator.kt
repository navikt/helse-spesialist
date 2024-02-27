package no.nav.helse.mediator

import java.time.LocalDate
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse

interface IBehandlingsstatistikkMediator {
    fun getBehandlingsstatistikk(fom: LocalDate = LocalDate.now()): BehandlingsstatistikkResponse
}