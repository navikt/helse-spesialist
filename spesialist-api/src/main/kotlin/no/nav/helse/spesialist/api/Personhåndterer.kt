package no.nav.helse.spesialist.api

interface Personhåndterer {
    fun oppdaterSnapshot(fødselsnummer: String)

    fun klargjørPersonForVisning(fødselsnummer: String)
}
