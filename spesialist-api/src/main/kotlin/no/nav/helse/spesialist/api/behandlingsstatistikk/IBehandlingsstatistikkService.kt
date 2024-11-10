package no.nav.helse.spesialist.api.behandlingsstatistikk

import java.time.LocalDate

interface IBehandlingsstatistikkService {
    fun getBehandlingsstatistikk(fom: LocalDate = LocalDate.now()): BehandlingsstatistikkResponse
}
