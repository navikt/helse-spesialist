package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import java.time.LocalDateTime
import java.util.UUID

class NyeVarsler(
    override val id: UUID,
    private val fødselsnummer: String,
    internal val varsler: List<Varsel>,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        varsler = jsonNode["aktiviteter"].varsler(),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) = person.nyeVarsler(varsler)

    private companion object {
        private fun JsonNode.varsler(): List<Varsel> =
            filter { it["nivå"].asText() == "VARSEL" && it["varselkode"]?.asText() != null }
                .filter { it["kontekster"].any { kontekst -> kontekst["konteksttype"].asText() == "Vedtaksperiode" } }
                .map { jsonNode ->
                    val vedtaksperiodeId =
                        jsonNode["kontekster"]
                            .find { it["konteksttype"].asText() == "Vedtaksperiode" }!!["kontekstmap"]["vedtaksperiodeId"]
                            .asUUID()
                    Varsel(
                        jsonNode["id"].asUUID(),
                        jsonNode["varselkode"].asText(),
                        jsonNode["tidsstempel"].asText().let(LocalDateTime::parse),
                        vedtaksperiodeId,
                    )
                }
    }
}
