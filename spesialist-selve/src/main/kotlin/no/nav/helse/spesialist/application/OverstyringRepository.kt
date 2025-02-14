package no.nav.helse.spesialist.application

import no.nav.helse.modell.saksbehandler.handlinger.Overstyring

interface OverstyringRepository {
    fun lagre(overstyringer: List<Overstyring>)

    fun finnAktive(fødselsnummer: String): List<Overstyring>
}
