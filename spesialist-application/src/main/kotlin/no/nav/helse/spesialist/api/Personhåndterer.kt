package no.nav.helse.spesialist.api

interface Personhåndterer {
    fun oppdaterPersondata(fødselsnummer: String)

    fun klargjørPersonForVisning(fødselsnummer: String)
}
