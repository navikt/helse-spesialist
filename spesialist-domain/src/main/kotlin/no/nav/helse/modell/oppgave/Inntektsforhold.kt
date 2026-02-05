package no.nav.helse.modell.oppgave

enum class Inntektsforhold {
    SelvstendigNæringsdrivende {
        override val dbVerdi = this.name
    },
    Arbeidstaker {
        override val dbVerdi = this.name
    }, ;

    abstract val dbVerdi: String
}
