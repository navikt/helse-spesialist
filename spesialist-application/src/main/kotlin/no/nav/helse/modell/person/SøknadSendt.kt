package no.nav.helse.modell.person

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import tools.jackson.databind.JsonNode
import java.util.UUID

class SøknadSendt(
    override val id: UUID,
    private val fødselsnummer: String,
    val aktørId: String,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asString()),
        fødselsnummer = jsonNode["fnr"].asString(),
        aktørId = jsonNode["aktorId"].asString(),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer() = fødselsnummer

    override fun toJson() = json

    override fun behandle(
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {}
}
