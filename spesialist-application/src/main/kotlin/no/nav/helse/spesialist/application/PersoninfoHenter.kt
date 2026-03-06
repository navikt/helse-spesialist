package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Personinfo

fun interface PersoninfoHenter {
    fun hentPersoninfo(identitetsnummer: Identitetsnummer): Personinfo?
}
