package no.nav.helse.spesialist.api.varsel

enum class Varselstatus {
    INAKTIV,
    AKTIV,

    // Varsler er 'VURDERT' når saksbehandler har trykket på avkrysningsboksen i Speil
    VURDERT,

    // Varsler er 'GODKJENT' når behandlingen de tilhører er godkjent/ferdigbehandlet
    GODKJENT,
    AVVIST,
}
