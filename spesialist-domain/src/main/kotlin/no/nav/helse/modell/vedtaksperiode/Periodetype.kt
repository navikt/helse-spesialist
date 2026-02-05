package no.nav.helse.modell.vedtaksperiode

enum class Periodetype {
    FØRSTEGANGSBEHANDLING {
        override val dbVerdi = this.name
    },
    FORLENGELSE {
        override val dbVerdi = this.name
    },
    INFOTRYGDFORLENGELSE {
        override val dbVerdi = this.name
    },
    OVERGANG_FRA_IT {
        override val dbVerdi = this.name
    }, ;

    abstract val dbVerdi: String
}
