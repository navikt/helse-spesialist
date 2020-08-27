package no.nav.helse.mediator.kafka.meldinger

class AnnulleringMessage(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val fagsystemId: String,
    val saksbehandler: String,
    val saksbehandlerEpost: String
)
