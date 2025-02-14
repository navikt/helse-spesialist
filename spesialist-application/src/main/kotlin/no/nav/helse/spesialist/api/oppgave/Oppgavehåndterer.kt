package no.nav.helse.spesialist.api.oppgave

interface Oppgavehåndterer {
    fun endretEgenAnsattStatus(
        erEgenAnsatt: Boolean,
        fødselsnummer: String,
    )
}
