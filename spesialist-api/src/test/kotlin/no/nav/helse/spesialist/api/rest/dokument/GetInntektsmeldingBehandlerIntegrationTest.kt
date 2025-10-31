package no.nav.helse.spesialist.api.rest.dokument

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandler
import no.nav.helse.spesialist.domain.testfixtures.fødselsdato
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagMellomnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.typer.Kjønn
import org.intellij.lang.annotations.Language
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetInntektsmeldingBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val dokumentDao = integrationTestFixture.sessionFactory.sessionContext.dokumentDao
    private val legacyPersonRepository = integrationTestFixture.sessionFactory.sessionContext.legacyPersonRepository
    private val personDao = integrationTestFixture.sessionFactory.sessionContext.personDao

    @Test
    fun `kan hente inntektsmelding hvis man har tilgang til person`() {
        // Given:
        val dokumentId = UUID.randomUUID()
        val fødselsnummer = "29419408008"
        val aktørId = "100000123123"
        val organisasjonsnummer = "99999999"
        dokumentDao.lagre(
            fødselsnummer = fødselsnummer,
            dokumentId = dokumentId,
            dokument = objectMapper.readTree(
                lagInntektsmeldingJson(
                    id = dokumentId,
                    fødselsnummer = fødselsnummer,
                    aktørId = aktørId,
                    organisasjonsnummer = organisasjonsnummer
                )
            )
        )

        val person: LegacyPerson = LegacyPerson.gjenopprett(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            vedtaksperioder = emptyList(),
            skjønnsfastsattSykepengegrunnlag = emptyList(),
            avviksvurderinger = emptyList()
        )

        legacyPersonRepository.leggTilPerson(person)
        personDao.upsertPersoninfo(
            fødselsnummer = fødselsnummer,
            fornavn = lagFornavn(),
            mellomnavn = lagMellomnavn(),
            etternavn = lagEtternavn(),
            fødselsdato = fødselsdato(),
            kjønn = Kjønn.Kvinne,
            adressebeskyttelse = Adressebeskyttelse.Ugradert
        )

        val saksbehandler = lagSaksbehandler()

        // When:
        val response = integrationTestFixture.get(
            url = "/api/personer/$aktørId/dokumenter/$dokumentId/inntektsmelding",
            saksbehandler = saksbehandler
        )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertEquals(organisasjonsnummer, response.bodyAsJsonNode?.get("virksomhetsnummer")?.asText())
    }

    @Test
    fun `har tilgang til å hente inntektsmelding hvis person mangler FNR og har aktørId`() {
        // Given:
        val dokumentId = UUID.randomUUID()
        val fødselsnummer = "29419408008"
        val aktørId = "100000123123"
        val organisasjonsnummer = "99999999"
        dokumentDao.lagre(
            fødselsnummer = fødselsnummer,
            dokumentId = dokumentId,
            dokument = objectMapper.readTree(
                lagInntektsmeldingJson(
                    id = dokumentId,
                    fødselsnummer = "",
                    aktørId = aktørId,
                    organisasjonsnummer = organisasjonsnummer
                )
            )
        )

        val person: LegacyPerson = LegacyPerson.gjenopprett(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            vedtaksperioder = emptyList(),
            skjønnsfastsattSykepengegrunnlag = emptyList(),
            avviksvurderinger = emptyList()
        )

        legacyPersonRepository.leggTilPerson(person)
        personDao.upsertPersoninfo(
            fødselsnummer = fødselsnummer,
            fornavn = lagFornavn(),
            mellomnavn = lagMellomnavn(),
            etternavn = lagEtternavn(),
            fødselsdato = fødselsdato(),
            kjønn = Kjønn.Kvinne,
            adressebeskyttelse = Adressebeskyttelse.Ugradert
        )

        val saksbehandler = lagSaksbehandler()

        // When:
        val response = integrationTestFixture.get(
            url = "/api/personer/$aktørId/dokumenter/$dokumentId/inntektsmelding",
            saksbehandler = saksbehandler
        )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertEquals(organisasjonsnummer, response.bodyAsJsonNode?.get("virksomhetsnummer")?.asText())
    }

    @Test
    fun `har ikke tilgang til å hente inntektsmelding hvis person mangler FNR og aktørId`() {
        // Given:
        val dokumentId = UUID.randomUUID()
        val fødselsnummer = "29419408008"
        val aktørId = "100000123123"
        val organisasjonsnummer = "99999999"
        dokumentDao.lagre(
            fødselsnummer = fødselsnummer,
            dokumentId = dokumentId,
            dokument = objectMapper.readTree(
                lagInntektsmeldingJson(
                    id = dokumentId,
                    fødselsnummer = "",
                    aktørId = "",
                    organisasjonsnummer = organisasjonsnummer
                )
            )
        )

        val person: LegacyPerson = LegacyPerson.gjenopprett(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            vedtaksperioder = emptyList(),
            skjønnsfastsattSykepengegrunnlag = emptyList(),
            avviksvurderinger = emptyList()
        )

        legacyPersonRepository.leggTilPerson(person)
        personDao.upsertPersoninfo(
            fødselsnummer = fødselsnummer,
            fornavn = lagFornavn(),
            mellomnavn = lagMellomnavn(),
            etternavn = lagEtternavn(),
            fødselsdato = fødselsdato(),
            kjønn = Kjønn.Kvinne,
            adressebeskyttelse = Adressebeskyttelse.Ugradert
        )

        val saksbehandler = lagSaksbehandler()

        // When:
        val response = integrationTestFixture.get(
            url = "/api/personer/$aktørId/dokumenter/$dokumentId/inntektsmelding",
            saksbehandler = saksbehandler
        )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
    }

    @Test
    fun `kan ikke hente inntektsmelding hvis man ikke har tilgang til person`() {
        // Given:
        val dokumentId = UUID.randomUUID()
        val fødselsnummer = "29419408008"
        val aktørId = "100000123123"
        val organisasjonsnummer = "99999999"
        dokumentDao.lagre(
            fødselsnummer = fødselsnummer,
            dokumentId = dokumentId,
            dokument = objectMapper.readTree(
                lagInntektsmeldingJson(
                    id = dokumentId,
                    fødselsnummer = fødselsnummer,
                    aktørId = aktørId,
                    organisasjonsnummer = organisasjonsnummer
                )
            )
        )

        val person: LegacyPerson = LegacyPerson.gjenopprett(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            vedtaksperioder = emptyList(),
            skjønnsfastsattSykepengegrunnlag = emptyList(),
            avviksvurderinger = emptyList()
        )

        legacyPersonRepository.leggTilPerson(person)
        personDao.upsertPersoninfo(
            fødselsnummer = fødselsnummer,
            fornavn = lagFornavn(),
            mellomnavn = lagMellomnavn(),
            etternavn = lagEtternavn(),
            fødselsdato = fødselsdato(),
            kjønn = Kjønn.Kvinne,
            adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig
        )

        val saksbehandler = lagSaksbehandler()

        // When:
        val response = integrationTestFixture.get(
            url = "/api/personer/$aktørId/dokumenter/$dokumentId/inntektsmelding",
            saksbehandler = saksbehandler
        )

        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
    }
}

@Language("JSON")
fun lagInntektsmeldingJson(
    id: UUID = UUID.randomUUID(),
    fødselsnummer: String = lagFødselsnummer(),
    aktørId: String = lagAktørId(),
    organisasjonsnummer: String = lagOrganisasjonsnummer(),
) = """
    {
      "inntektsmeldingId": "$id",
      "vedtaksperiodeId": null,
      "arbeidstakerFnr": "$fødselsnummer",
      "arbeidstakerAktorId": "$aktørId",
      "virksomhetsnummer": "$organisasjonsnummer",
      "arbeidsgiverFnr": null,
      "arbeidsgiverAktorId": null,
      "innsenderFulltNavn": "Navn Navnesen",
      "innsenderTelefon": "81549300",
      "begrunnelseForReduksjonEllerIkkeUtbetalt": "FerieEllerAvspasering",
      "bruttoUtbetalt": "20000.00",
      "arbeidsgivertype": "VIRKSOMHET",
      "arbeidsforholdId": null,
      "beregnetInntekt": "40000.00",
      "inntektsdato": null,
      "refusjon": {
        "beloepPrMnd": "0.00",
        "opphoersdato": null
      },
      "endringIRefusjoner": [],
      "opphoerAvNaturalytelser": [],
      "gjenopptakelseNaturalytelser": [],
      "arbeidsgiverperioder": [
        {
          "fom": "2025-01-01",
          "tom": "2025-02-01"
        }
      ],
      "status": "GYLDIG",
      "arkivreferanse": "AR151515151",
      "ferieperioder": [],
      "foersteFravaersdag": "2025-01-01",
      "mottattDato": "2025-01-01T09:00:00",
      "naerRelasjon": false,
      "avsenderSystem": {
        "navn": "SUP",
        "versjon": "1337"
      },
      "inntektEndringAarsak": null,
      "inntektEndringAarsaker": [],
      "arsakTilInnsending": "Ny",
      "mottaksKanal": "ALTINN",
      "format": "Inntektsmelding",
      "forespurt": false
    }
""".trimIndent()
