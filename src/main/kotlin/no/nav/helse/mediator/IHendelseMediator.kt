package no.nav.helse.mediator

import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal interface IHendelseMediator {
    fun vedtaksperiodeEndret(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    )

    fun vedtaksperiodeForkastet(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    )

    fun saksbehandlerløsning(
        message: JsonMessage,
        id: UUID,
        godkjenningsbehovhendelseId: UUID,
        contextId: UUID,
        fødselsnummer: String,
        godkjent: Boolean,
        saksbehandlerident: String,
        saksbehandleroid: UUID,
        saksbehandlerepost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        oppgaveId: Long,
        context: RapidsConnection.MessageContext
    )

    fun løsning(hendelseId: UUID, contextId: UUID, behovId: UUID, løsning: Any, context: RapidsConnection.MessageContext)

    fun godkjenningsbehov(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        vedtaksperiodeId: UUID,
        warnings: List<String>,
        periodetype: Saksbehandleroppgavetype,
        context: RapidsConnection.MessageContext
    )

    fun overstyring(message: JsonMessage, id: UUID, fødselsnummer: String, context: RapidsConnection.MessageContext)
    fun tilbakerulling(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        vedtaksperiodeIder: List<UUID>,
        context: RapidsConnection.MessageContext
    )

    fun utbetalingAnnullert(
        message: JsonMessage,
        context: RapidsConnection.MessageContext
    )
}
