package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Enhet
import no.nav.helse.spesialist.domain.Identitetsnummer

fun interface BehandlendeEnhetHenter {
    fun hentFor(identitetsnummer: Identitetsnummer): Enhet?
}
