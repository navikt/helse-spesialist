package no.nav.helse.spesialist.e2etests

import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.ArbeidsgiverinformasjonJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Fullmakt
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Vergemål
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.util.UUID

class SimulatingTestRapidMeldingssender(private val rapid: SimulatingTestRapid) {
    private val newUUID get() = UUID.randomUUID()

    fun sendPersoninfoløsning(
        aktørId: String,
        fødselsnummer: String,
        adressebeskyttelse: Adressebeskyttelse,
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.last { it.path("@event_name").asText() == "behov" }
        assertEquals("HentPersoninfoV2", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagPersoninfoløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
                adressebeskyttelse = adressebeskyttelse.name
            )
        )
    }

    fun sendEnhetløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        enhet: String,
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("HentEnhet", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagEnhetløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
                enhet = enhet
            )
        )
    }

    fun sendInfotrygdutbetalingerløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("HentInfotrygdutbetalinger", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagInfotrygdutbetalingerløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendArbeidsgiverinformasjonløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        arbeidsgiverinformasjonJson: List<ArbeidsgiverinformasjonJson>? = null,
    ): UUID =
        newUUID.also { id ->
            val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
            assertEquals("Arbeidsgiverinformasjon", behov["@behov"].map { it.asText() }.single())
            val contextId = UUID.fromString(behov["contextId"].asText())
            val hendelseId = UUID.fromString(behov["hendelseId"].asText())

            val arbeidsgivere =
                arbeidsgiverinformasjonJson ?: behov["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map {
                    ArbeidsgiverinformasjonJson(
                        it.asText(),
                        "Navn for ${it.asText()}",
                        listOf("Bransje for ${it.asText()}")
                    )
                }

            rapid.publish(
                Testmeldingfabrikk.lagArbeidsgiverinformasjonløsning(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    ekstraArbeidsgivere = arbeidsgivere,
                    id = id,
                    hendelseId = hendelseId,
                    contextId = contextId
                )
            )
        }

    fun sendArbeidsgiverinformasjonløsningKompositt(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
    ): UUID =
        newUUID.also { id ->
            val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
            assertEquals(
                setOf("Arbeidsgiverinformasjon", "HentPersoninfoV2"),
                behov["@behov"].map { it.asText() }.toSet()
            )
            val contextId = UUID.fromString(behov["contextId"].asText())
            val hendelseId = UUID.fromString(behov["hendelseId"].asText())

            val organisasjoner = behov["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map {
                ArbeidsgiverinformasjonJson(
                    it.asText(),
                    "Navn for ${it.asText()}",
                    listOf("Bransje for ${it.asText()}")
                )
            }

            val personer: List<Map<String, Any>> = behov["HentPersoninfoV2"]["ident"].map {
                mapOf(
                    "ident" to it.asText(),
                    "fornavn" to it.asText(),
                    "etternavn" to it.asText(),
                    "fødselsdato" to LocalDate.now(),
                    "kjønn" to Kjønn.Ukjent.name,
                    "adressebeskyttelse" to Adressebeskyttelse.Ugradert.name,
                )
            }

            rapid.publish(
                Testmeldingfabrikk.lagArbeidsgiverinformasjonKomposittLøsning(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    organisasjoner = organisasjoner,
                    personer = personer,
                    hendelseId = hendelseId,
                    contextId = contextId,
                    id = id,
                )
            )
        }

    fun sendArbeidsforholdløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        løsning: List<Arbeidsforholdløsning.Løsning> = listOf(
            Arbeidsforholdløsning.Løsning(
                stillingstittel = "en-stillingstittel",
                stillingsprosent = 100,
                startdato = LocalDate.now(),
                sluttdato = null
            )
        ),
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("Arbeidsforhold", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagArbeidsforholdløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                løsning = løsning,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendEgenAnsattløsning(
        aktørId: String,
        fødselsnummer: String,
        erEgenAnsatt: Boolean,
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("EgenAnsatt", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagEgenAnsattløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                erEgenAnsatt = erEgenAnsatt,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
            )
        )
    }

    fun sendVergemålOgFullmaktløsning(
        aktørId: String,
        fødselsnummer: String,
        vergemål: List<Vergemål> = emptyList(),
        fremtidsfullmakter: List<Vergemål> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals(listOf("Vergemål", "Fullmakt"), behov["@behov"].map { it.asText() })
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        val payload = Testmeldingfabrikk.VergemålJson(vergemål, fremtidsfullmakter, fullmakter)

        rapid.publish(
            Testmeldingfabrikk.lagVergemålOgFullmaktKomposittLøsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vergemål = payload,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendInntektløsning(
        aktørId: String,
        fødselsnummer: String,
        orgnr: String,
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("InntekterForSykepengegrunnlag", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagInntektløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                orgnummer = orgnr,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
            )
        )
    }

    fun sendÅpneGosysOppgaverløsning(
        aktørId: String,
        fødselsnummer: String,
        antall: Int,
        oppslagFeilet: Boolean,
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("ÅpneOppgaver", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagÅpneGosysOppgaverløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                antall = antall,
                oppslagFeilet = oppslagFeilet,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendRisikovurderingløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean = true,
        funn: List<Risikofunn> = emptyList(),
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("Risikovurdering", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagRisikovurderingløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
                funn = funn,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
            )
        )
    }
}
