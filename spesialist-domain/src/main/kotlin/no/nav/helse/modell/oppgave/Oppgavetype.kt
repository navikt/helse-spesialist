package no.nav.helse.modell.oppgave

enum class Oppgavetype {
    Revurdering {
        override val dbVerdi = this.name
    },
    Søknad {
        override val dbVerdi = this.name
    }, ;

    abstract val dbVerdi: String
}
