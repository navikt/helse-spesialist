package no.nav.helse.spesialist.api.modell

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.rapids_rivers.JsonMessage

internal interface SaksbehandlerObserver {
    fun tidslinjeOverstyrt(fødselsnummer: String, event: OverstyrtTidslinjeEvent) {}
}

data class OverstyrtTidslinjeEvent(
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjeEventDag>,
    val begrunnelse: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String
) {
    //TODO: JsonMessage burde ikke være kjent for modell, bør mappes om fra Event til JsonMessage i mediator
    internal fun somJsonMessage(): JsonMessage {
        return JsonMessage.newMessage(
            "saksbehandler_overstyrer_tidslinje", mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer,
                "dager" to dager,
                "begrunnelse" to begrunnelse,
                "saksbehandlerOid" to saksbehandlerOid,
                "saksbehandlerNavn" to saksbehandlerNavn,
                "saksbehandlerIdent" to saksbehandlerIdent,
                "saksbehandlerEpost" to saksbehandlerEpost,
            )
        )
    }

    data class OverstyrtTidslinjeEventDag(
        val dato: LocalDate,
        val type: String,
        val fraType: String,
        val grad: Int?,
        val fraGrad: Int?
    )
}