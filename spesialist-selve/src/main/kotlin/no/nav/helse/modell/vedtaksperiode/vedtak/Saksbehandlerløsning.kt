package no.nav.helse.modell.vedtaksperiode.vedtak

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.person.Person
import java.time.LocalDateTime
import java.util.UUID

/**
 * Behandler input til godkjenningsbehov fra saksbehandler som har blitt lagt på rapid-en av API-biten av spesialist.
 */

class Saksbehandlerløsning(
    override val id: UUID,
    val oppgaveId: Long,
    val godkjenningsbehovhendelseId: UUID,
    private val fødselsnummer: String,
    val godkjent: Boolean,
    val ident: String,
    val epostadresse: String,
    val godkjenttidspunkt: LocalDateTime,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?,
    val saksbehandleroverstyringer: List<UUID>,
    val saksbehandler: Saksbehandler,
    val beslutter: Saksbehandler?,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        oppgaveId = jsonNode["oppgaveId"].asLong(),
        godkjenningsbehovhendelseId = UUID.fromString(jsonNode["hendelseId"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        godkjent = jsonNode["godkjent"].asBoolean(),
        ident = jsonNode["saksbehandlerident"].asText(),
        epostadresse = jsonNode["saksbehandlerepost"].asText(),
        godkjenttidspunkt = jsonNode["godkjenttidspunkt"].asText().let(LocalDateTime::parse),
        årsak = jsonNode.path("årsak").takeUnless(JsonNode::isMissingOrNull)?.asText(),
        begrunnelser = jsonNode.path("begrunnelser").takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
        kommentar = jsonNode.path("kommentar").takeUnless(JsonNode::isMissingOrNull)?.asText(),
        saksbehandler =
            Saksbehandler(
                jsonNode["saksbehandler"]["ident"].asText(),
                jsonNode["saksbehandler"]["epostadresse"].asText(),
            ),
        beslutter =
            jsonNode.path("beslutter").takeUnless(JsonNode::isMissingOrNull)?.let {
                Saksbehandler(
                    jsonNode["beslutter"]["ident"].asText(),
                    jsonNode["beslutter"]["epostadresse"].asText(),
                )
            },
        saksbehandleroverstyringer =
            jsonNode.path("saksbehandleroverstyringer").takeUnless(JsonNode::isMissingOrNull)?.map {
                UUID.fromString(it.asText())
            } ?: emptyList(),
        json = jsonNode.toString(),
    )

    data class Saksbehandler(
        val ident: String,
        val epostadresse: String,
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) = kommandostarter { løsGodkjenningsbehov(this@Saksbehandlerløsning, person, transactionalSession) }

    override fun fødselsnummer() = fødselsnummer

    override fun toJson() = json
}

private fun JsonNode.isMissingOrNull() = isMissingNode || isNull
