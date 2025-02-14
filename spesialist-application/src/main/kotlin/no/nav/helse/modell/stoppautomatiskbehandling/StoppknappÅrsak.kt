package no.nav.helse.modell.stoppautomatiskbehandling

enum class StoppknappÅrsak {
    MEDISINSK_VILKAR,
    AKTIVITETSKRAV,
    MANGLENDE_MEDVIRKING,

    // BESTRIDELSE_SYKMELDING er ikke lenger i bruk hos iSyfo, men spesialist har historiske meldinger med denne årsaken
    BESTRIDELSE_SYKMELDING,
}
