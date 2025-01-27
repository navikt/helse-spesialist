package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.InntektskilderRepository
import no.nav.helse.db.PersonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettMinimalArbeidsgiverCommand
import no.nav.helse.modell.kommando.OpprettMinimalPersonCommand
import java.util.UUID

class SøknadSendt(
    override val id: UUID,
    private val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fnr"].asText(),
        aktørId = jsonNode["aktorId"].asText(),
        organisasjonsnummer = jsonNode.path("arbeidsgiver")["orgnummer"]?.asText() ?: jsonNode["tidligereArbeidsgiverOrgnummer"].asText(),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer() = fødselsnummer

    override fun toJson() = json

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        // Ikke i bruk, SøknadSendt har egen sti inn da det muligens ikke finnes noen person enda
        // På sikt ønsker vi kanskje å opprette personen dersom den ikke finnes enda, og da kan denne tas i bruk
    }
}

class SøknadSendtCommand(
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    personDao: PersonDao,
    inntektskilderRepository: InntektskilderRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OpprettMinimalPersonCommand(fødselsnummer, aktørId, personDao),
            OpprettMinimalArbeidsgiverCommand(organisasjonsnummer, inntektskilderRepository),
        )
}
