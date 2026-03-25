package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Infotrygdperiode
import java.time.LocalDate

fun interface InfotrygdperiodeHenter {
    fun hentFor(
        identitetsnummer: Identitetsnummer,
        fom: LocalDate,
    ): List<Infotrygdperiode>
}
