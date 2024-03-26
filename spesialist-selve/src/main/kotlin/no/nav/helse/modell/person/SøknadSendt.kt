package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.mediator.meldinger.PersonmeldingOld
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettMinimalArbeidsgiverCommand
import no.nav.helse.modell.kommando.OpprettMinimalPersonCommand
import no.nav.helse.rapids_rivers.JsonMessage

internal class SøknadSendt private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    private val json: String,
) : PersonmeldingOld {
    internal constructor(jsonNode: JsonNode): this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fnr"].asText(),
        aktørId = jsonNode["aktorId"].asText(),
        organisasjonsnummer = jsonNode["arbeidsgiver.orgnummer"]?.asText() ?: jsonNode["tidligereArbeidsgiverOrgnummer"].asText(),
        json = jsonNode.toString(),
    )
    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json

    internal companion object {
        fun søknadSendt(packet: JsonMessage) = SøknadSendt(
            id = UUID.fromString(packet["@id"].asText()),
            fødselsnummer = packet["fnr"].asText(),
            aktørId = packet["aktorId"].asText(),
            organisasjonsnummer = packet["arbeidsgiver.orgnummer"].asText(),
            json = packet.toJson()
        )

        fun søknadSendtArbeidsledig(packet: JsonMessage) = SøknadSendt(
            id = UUID.fromString(packet["@id"].asText()),
            fødselsnummer = packet["fnr"].asText(),
            aktørId = packet["aktorId"].asText(),
            organisasjonsnummer = packet["tidligereArbeidsgiverOrgnummer"].asText(),
            json = packet.toJson()
        )
    }
}

internal class SøknadSendtCommand(
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao
): MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettMinimalPersonCommand(fødselsnummer, aktørId, personDao),
        OpprettMinimalArbeidsgiverCommand(organisasjonsnummer, arbeidsgiverDao),
    )
}