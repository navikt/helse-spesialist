package no.nav.helse.spesialist.api.rest.personer.dokumenter

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknadstype
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class GetSøknadBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val dokumentDao = integrationTestFixture.sessionFactory.sessionContext.dokumentDao
    private val egenAnsattDao = integrationTestFixture.sessionFactory.sessionContext.egenAnsattDao
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository
    private val personPseudoIdDao = integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao

    @Test
    fun `får hentet søknad hvis man har tilgang til person`() {
        // Given:
        val dokumentId = UUID.randomUUID()
        val person = lagPerson().also(personRepository::lagre)
        dokumentDao.lagre(
            fødselsnummer = person.id.value,
            dokumentId = dokumentId,
            dokument = objectMapper.readTree(lagSøknadJson(fnr = person.id.value))
        )

        val pseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        val saksbehandler = lagSaksbehandler()

        // When:
        val response = integrationTestFixture.get(
            url = "/api/personer/${pseudoId.value}/dokumenter/$dokumentId/soknad",
            saksbehandler = saksbehandler
        )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertEquals(ApiSoknadstype.Selvstendig_og_frilanser.name, response.bodyAsJsonNode?.get("type")?.asText())
    }

    @Test
    fun `får ikke hente søknad hvis man ikke har tilgang til person`() {
        // Given:
        val dokumentId = UUID.randomUUID()
        val person =
            lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig).also(personRepository::lagre)
        dokumentDao.lagre(
            fødselsnummer = person.id.value,
            dokumentId = dokumentId,
            dokument = objectMapper.readTree(lagSøknadJson(fnr = person.id.value))
        )

        val pseudoId = personPseudoIdDao.nyPersonPseudoId(Identitetsnummer.fraString(person.id.value))

        val saksbehandler = lagSaksbehandler()

        // When:
        val response = integrationTestFixture.get(
            url = "/api/personer/${pseudoId.value}/dokumenter/$dokumentId/soknad",
            saksbehandler = saksbehandler
        )

        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
    }
}

@Language("JSON")
fun lagSøknadJson(fnr: String) = """
    {
          "id": "63b6913a-ce95-30d8-9b6e-6123f5262b05",
          "type": "SELVSTENDIGE_OG_FRILANSERE",
          "status": "SENDT",
          "fnr": "$fnr",
          "sykmeldingId": "6aa9bd4f-b25a-446b-9126-7a513bcd673f",
          "arbeidsgiver": null,
          "arbeidssituasjon": "SELVSTENDIG_NARINGSDRIVENDE",
          "korrigerer": null,
          "korrigertAv": null,
          "soktUtenlandsopphold": false,
          "arbeidsgiverForskutterer": null,
          "fom": "2025-07-01",
          "tom": "2025-07-31",
          "dodsdato": null,
          "startSyketilfelle": "2025-07-01",
          "arbeidGjenopptatt": null,
          "friskmeldt": null,
          "sykmeldingSkrevet": "2025-07-01T02:00:00",
          "opprettet": "2025-09-26T07:36:17.712919",
          "opprinneligSendt": null,
          "sendtNav": "2025-09-26T07:36:49.901699",
          "sendtArbeidsgiver": null,
          "egenmeldinger": null,
          "fravarForSykmeldingen": [],
          "papirsykmeldinger": [],
          "fravar": [],
          "andreInntektskilder": [],
          "soknadsperioder": [
            {
              "fom": "2025-07-01",
              "tom": "2025-07-31",
              "sykmeldingsgrad": 100,
              "faktiskGrad": null,
              "avtaltTimer": null,
              "faktiskTimer": null,
              "sykmeldingstype": "AKTIVITET_IKKE_MULIG",
              "grad": 100
            }
          ],
          "sporsmal": [
            {
              "id": "dee12c89-af8e-35c0-bb25-3986d7d46225",
              "tag": "ANSVARSERKLARING",
              "sporsmalstekst": "Jeg bekrefter at jeg vil svare så riktig som jeg kan.",
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "CHECKBOX_PANEL",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [
                {
                  "verdi": "CHECKED"
                }
              ],
              "undersporsmal": [],
              "metadata": null
            },
            {
              "id": "2897271e-a2a4-3b8b-ab5e-180e41fc7b77",
              "tag": "FRAVAR_FOR_SYKMELDINGEN_V2",
              "sporsmalstekst": "Var du borte fra jobb i fire uker eller mer rett før du ble sykmeldt 1. juli 2025?",
              "undertekst": "Gjelder sammenhengende ferie eller annet fravær gjennom alle fire ukene. Har du jobbet underveis, kan du svare nei. ",
              "min": null,
              "max": null,
              "svartype": "JA_NEI",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [
                {
                  "verdi": "NEI"
                }
              ],
              "undersporsmal": [],
              "metadata": null
            },
            {
              "id": "cd20b9f4-0b6f-38d6-97c4-d907f1dce554",
              "tag": "TILBAKE_I_ARBEID",
              "sporsmalstekst": "Var du tilbake i fullt arbeid som selvstendig næringsdrivende før sykmeldingsperioden utløp 31. juli 2025?",
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "JA_NEI",
              "kriterieForVisningAvUndersporsmal": "JA",
              "svar": [
                {
                  "verdi": "NEI"
                }
              ],
              "undersporsmal": [
                {
                  "id": "c1c5d291-6847-325f-b8b3-27ac6c58b104",
                  "tag": "TILBAKE_NAR",
                  "sporsmalstekst": "Når begynte du å jobbe igjen?",
                  "undertekst": null,
                  "min": "2025-07-01",
                  "max": "2025-07-31",
                  "svartype": "DATO",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": [],
                  "metadata": null
                }
              ],
              "metadata": null
            },
            {
              "id": "a21bd43b-a3ce-3e4c-9627-5fb91c01c6a8",
              "tag": "ARBEID_UNDERVEIS_100_PROSENT_0",
              "sporsmalstekst": "I perioden 1. - 31. juli 2025 var du 100% sykmeldt som selvstendig næringsdrivende. Jobbet du noe i denne perioden?",
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "JA_NEI",
              "kriterieForVisningAvUndersporsmal": "JA",
              "svar": [
                {
                  "verdi": "NEI"
                }
              ],
              "undersporsmal": [
                {
                  "id": "b6ee1d8d-1399-3625-861d-58ee666de631",
                  "tag": "HVOR_MYE_HAR_DU_JOBBET_0",
                  "sporsmalstekst": "Oppgi arbeidsmengde i timer eller prosent:",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "RADIO_GRUPPE_TIMER_PROSENT",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "8bbd0880-b786-34e0-adef-9a3fb95a0414",
                      "tag": "HVOR_MYE_PROSENT_0",
                      "sporsmalstekst": "Prosent",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "RADIO",
                      "kriterieForVisningAvUndersporsmal": "CHECKED",
                      "svar": [],
                      "undersporsmal": [
                        {
                          "id": "5c8cc36d-d098-362b-8ec0-60992dd47a8e",
                          "tag": "HVOR_MYE_PROSENT_VERDI_0",
                          "sporsmalstekst": "Oppgi hvor mange prosent av din normale arbeidstid du jobbet i perioden 1. - 31. juli 2025?",
                          "undertekst": "Oppgi i prosent. Eksempel: 40",
                          "min": "1",
                          "max": "99",
                          "svartype": "PROSENT",
                          "kriterieForVisningAvUndersporsmal": null,
                          "svar": [],
                          "undersporsmal": [],
                          "metadata": null
                        }
                      ],
                      "metadata": null
                    },
                    {
                      "id": "43ac2c7f-c187-3f2f-8381-4ea43118bd49",
                      "tag": "HVOR_MYE_TIMER_0",
                      "sporsmalstekst": "Timer",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "RADIO",
                      "kriterieForVisningAvUndersporsmal": "CHECKED",
                      "svar": [],
                      "undersporsmal": [
                        {
                          "id": "c933eaf5-4be0-3d35-b3ae-73a329b67037",
                          "tag": "HVOR_MYE_TIMER_VERDI_0",
                          "sporsmalstekst": "Oppgi totalt antall timer du jobbet i perioden 1. - 31. juli 2025",
                          "undertekst": "Eksempel: 8,5",
                          "min": "1",
                          "max": "664",
                          "svartype": "TIMER",
                          "kriterieForVisningAvUndersporsmal": null,
                          "svar": [],
                          "undersporsmal": [],
                          "metadata": null
                        }
                      ],
                      "metadata": null
                    }
                  ],
                  "metadata": null
                },
                {
                  "id": "2d491381-2a77-33b3-8452-18feffcd9868",
                  "tag": "JOBBER_DU_NORMAL_ARBEIDSUKE_0",
                  "sporsmalstekst": "Jobber du vanligvis 37,5 timer i uka?",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "JA_NEI",
                  "kriterieForVisningAvUndersporsmal": "NEI",
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "9f169621-1d92-31ab-8a18-4dcbb2d4fd6d",
                      "tag": "HVOR_MANGE_TIMER_PER_UKE_0",
                      "sporsmalstekst": "Oppgi timer per uke",
                      "undertekst": "Eksempel: 8,5",
                      "min": "1",
                      "max": "150",
                      "svartype": "TIMER",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [],
                      "undersporsmal": [],
                      "metadata": null
                    }
                  ],
                  "metadata": null
                }
              ],
              "metadata": null
            },
            {
              "id": "c6cd7b6b-73f0-3542-a2aa-4a199d415bd8",
              "tag": "ARBEID_UTENFOR_NORGE",
              "sporsmalstekst": "Har du arbeidet i utlandet i løpet av de siste 12 månedene?",
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "JA_NEI",
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [
                {
                  "verdi": "NEI"
                }
              ],
              "undersporsmal": [],
              "metadata": null
            },
            {
              "id": "ffabb5e6-cd05-3140-8184-3ef5c97cce9e",
              "tag": "ANDRE_INNTEKTSKILDER",
              "sporsmalstekst": "Har du annen inntekt?",
              "undertekst": null,
              "min": null,
              "max": null,
              "svartype": "JA_NEI",
              "kriterieForVisningAvUndersporsmal": "JA",
              "svar": [
                {
                  "verdi": "NEI"
                }
              ],
              "undersporsmal": [
                {
                  "id": "f1bcb090-a89b-3236-a9bc-7e876ebb4667",
                  "tag": "HVILKE_ANDRE_INNTEKTSKILDER",
                  "sporsmalstekst": "Hvilke inntektskilder har du?",
                  "undertekst": null,
                  "min": null,
                  "max": null,
                  "svartype": "CHECKBOX_GRUPPE",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": [
                    {
                      "id": "c5ff815a-5884-3f8e-a73d-7b4da1cdcc07",
                      "tag": "INNTEKTSKILDE_ARBEIDSFORHOLD",
                      "sporsmalstekst": "arbeidsforhold",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "CHECKBOX",
                      "kriterieForVisningAvUndersporsmal": "CHECKED",
                      "svar": [],
                      "undersporsmal": [
                        {
                          "id": "1a9dde4b-6904-3771-89ef-8af14d21485c",
                          "tag": "INNTEKTSKILDE_ARBEIDSFORHOLD_ER_DU_SYKMELDT",
                          "sporsmalstekst": "Er du sykmeldt fra dette?",
                          "undertekst": null,
                          "min": null,
                          "max": null,
                          "svartype": "JA_NEI",
                          "kriterieForVisningAvUndersporsmal": null,
                          "svar": [],
                          "undersporsmal": [],
                          "metadata": null
                        }
                      ],
                      "metadata": null
                    },
                    {
                      "id": "bcf6e247-8712-30d1-9b46-5c9c01a9464a",
                      "tag": "INNTEKTSKILDE_JORDBRUKER",
                      "sporsmalstekst": "jordbruk / fiske / reindrift",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "CHECKBOX",
                      "kriterieForVisningAvUndersporsmal": "CHECKED",
                      "svar": [],
                      "undersporsmal": [
                        {
                          "id": "79ed1171-1159-3391-b90e-e5ba1f31656d",
                          "tag": "INNTEKTSKILDE_JORDBRUKER_ER_DU_SYKMELDT",
                          "sporsmalstekst": "Er du sykmeldt fra dette?",
                          "undertekst": null,
                          "min": null,
                          "max": null,
                          "svartype": "JA_NEI",
                          "kriterieForVisningAvUndersporsmal": null,
                          "svar": [],
                          "undersporsmal": [],
                          "metadata": null
                        }
                      ],
                      "metadata": null
                    },
                    {
                      "id": "0adf910a-c75d-3efe-a0af-9724e380e309",
                      "tag": "INNTEKTSKILDE_FRILANSER_SELVSTENDIG",
                      "sporsmalstekst": "frilanser",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "CHECKBOX",
                      "kriterieForVisningAvUndersporsmal": "CHECKED",
                      "svar": [],
                      "undersporsmal": [
                        {
                          "id": "830d6af6-7381-3aed-af13-fd4fa4e35e66",
                          "tag": "INNTEKTSKILDE_FRILANSER_SELVSTENDIG_ER_DU_SYKMELDT",
                          "sporsmalstekst": "Er du sykmeldt fra dette?",
                          "undertekst": null,
                          "min": null,
                          "max": null,
                          "svartype": "JA_NEI",
                          "kriterieForVisningAvUndersporsmal": null,
                          "svar": [],
                          "undersporsmal": [],
                          "metadata": null
                        }
                      ],
                      "metadata": null
                    },
                    {
                      "id": "0d1f5791-4fbb-34eb-8245-071b2fa76dcc",
                      "tag": "INNTEKTSKILDE_ANNET",
                      "sporsmalstekst": "annet",
                      "undertekst": null,
                      "min": null,
                      "max": null,
                      "svartype": "CHECKBOX",
                      "kriterieForVisningAvUndersporsmal": null,
                      "svar": [],
                      "undersporsmal": [],
                      "metadata": null
                    }
                  ],
                  "metadata": null
                }],"metadata":null},{"id":"59031543-5352-3d00-9a51-c8a07d0f427c","tag":"OPPHOLD_UTENFOR_EOS","sporsmalstekst":"Var du på reise utenfor EU/EØS mens du var sykmeldt 1. - 31. juli 2025?","undertekst":null,"min":null,"max":null,"svartype":"JA_NEI","kriterieForVisningAvUndersporsmal":"JA","svar":[{"verdi":"NEI"}],"undersporsmal":[{"id":"c38e4726-afe8-369a-a7ff-3906266cdf98","tag":"OPPHOLD_UTENFOR_EOS_NAR","sporsmalstekst":"Når var du utenfor EU/EØS?","undertekst":null,"min":"2025-07-01","max":"2025-07-31","svartype":"PERIODER","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[],"metadata":null}],"metadata":null},{"id":"30f324ce-3366-3f26-bbb8-b3cd5f9c722d","tag":"INNTEKTSOPPLYSNINGER_VIRKSOMHETEN_AVVIKLET","sporsmalstekst":"Har du avviklet virksomheten din før du ble sykmeldt?","undertekst":null,"min":null,"max":null,"svartype":"RADIO_GRUPPE","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[{"id":"8733154a-201a-3b73-adf0-fcf42e0bc2c3","tag":"INNTEKTSOPPLYSNINGER_VIRKSOMHETEN_AVVIKLET_JA","sporsmalstekst":"Ja","undertekst":null,"min":null,"max":null,"svartype":"RADIO","kriterieForVisningAvUndersporsmal":"CHECKED","svar":[],"undersporsmal":[{"id":"79793653-6fbc-3cec-844e-421210745e12","tag":"INNTEKTSOPPLYSNINGER_VIRKSOMHETEN_AVVIKLET_NAR","sporsmalstekst":"Når ble virksomheten avviklet?","undertekst":null,"min":null,"max":"2025-06-30","svartype":"DATO","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[],"metadata":null}],"metadata":null},{"id":"ada55eda-e14f-359e-ac59-b5a17a207d8c","tag":"INNTEKTSOPPLYSNINGER_VIRKSOMHETEN_AVVIKLET_NEI","sporsmalstekst":"Nei","undertekst":null,"min":null,"max":null,"svartype":"RADIO","kriterieForVisningAvUndersporsmal":"CHECKED","svar":[{"verdi":"CHECKED"}],"undersporsmal":[{"id":"5fed7ea5-a3ff-336e-99b1-b800ef8019d5","tag":"INNTEKTSOPPLYSNINGER_NY_I_ARBEIDSLIVET","sporsmalstekst":"Er du ny i arbeidslivet etter 1. januar 2022?","undertekst":null,"min":null,"max":null,"svartype":"RADIO_GRUPPE","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[{"id":"b3fe50b3-1adc-3185-8cc2-7f602ba4a8b1","tag":"INNTEKTSOPPLYSNINGER_NY_I_ARBEIDSLIVET_JA","sporsmalstekst":"Ja","undertekst":null,"min":null,"max":null,"svartype":"RADIO","kriterieForVisningAvUndersporsmal":"CHECKED","svar":[],"undersporsmal":[{"id":"ec6b1169-c956-30e4-be47-31d56b0c6358","tag":"INNTEKTSOPPLYSNINGER_NY_I_ARBEIDSLIVET_DATO","sporsmalstekst":"Når begynte du i arbeidslivet?","undertekst":null,"min":"2022-01-01","max":"2025-06-30","svartype":"DATO","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[],"metadata":null}],"metadata":null},{"id":"4d218996-7b75-3297-99c0-bd45d837938c","tag":"INNTEKTSOPPLYSNINGER_NY_I_ARBEIDSLIVET_NEI","sporsmalstekst":"Nei","undertekst":null,"min":null,"max":null,"svartype":"RADIO","kriterieForVisningAvUndersporsmal":"CHECKED","svar":[{"verdi":"CHECKED"}],"undersporsmal":[{"id":"5f3430d0-80d1-3351-9e55-425f130f6462","tag":"INNTEKTSOPPLYSNINGER_VARIG_ENDRING","sporsmalstekst":"Har det skjedd en varig endring i arbeidssituasjonen eller virksomheten din i mellom 1. januar 2022 og frem tilsykmeldingstidspunktet?","undertekst":null,"min":null,"max":null,"svartype":"JA_NEI","kriterieForVisningAvUndersporsmal":"JA","svar":[{"verdi":"NEI"}],"undersporsmal":[{"id":"588e0293-4082-381f-a761-f769e9d6d6e5","tag":"INNTEKTSOPPLYSNINGER_VARIG_ENDRING_BEGRUNNELSE","sporsmalstekst":"Hvilken endring har skjedd i din arbeidssituasjon eller virksomhet?","undertekst":"Du kan velge ett eller flere alternativer","min":null,"max":null,"svartype":"CHECKBOX_GRUPPE","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[{"id":"4d73578d-81dd-3e32-9bd1-70cc1536de7f","tag":"INNTEKTSOPPLYSNINGER_VARIG_ENDRING_BEGRUNNELSE_OPPRETTELSE_NEDLEGGELSE","sporsmalstekst":"Opprettelse eller nedleggelse av næringsvirksomhet","undertekst":null,"min":null,"max":null,"svartype":"CHECKBOX","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[],"metadata":null},{"id":"a5f864f8-9003-3048-a6d7-ab06c512b10e","tag":"INNTEKTSOPPLYSNINGER_VARIG_ENDRING_BEGRUNNELSE_ENDRET_INNSATS","sporsmalstekst":"Økt eller redusert innsats","undertekst":null,"min":null,"max":null,"svartype":"CHECKBOX","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[],"metadata":null},{"id":"2932c514-5690-328b-9fd7-305c920078ee","tag":"INNTEKTSOPPLYSNINGER_VARIG_ENDRING_BEGRUNNELSE_OMLEGGING_AV_VIRKSOMHETEN","sporsmalstekst":"Omlegging av virksomheten","undertekst":null,"min":null,"max":null,"svartype":"CHECKBOX","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[],"metadata":null},{"id":"700f13e4-c332-37b8-b507-1a3739d6fdb2","tag":"INNTEKTSOPPLYSNINGER_VARIG_ENDRING_BEGRUNNELSE_ENDRET_MARKEDSSITUASJON","sporsmalstekst":"Endret markedssituasjon","undertekst":null,"min":null,"max":null,"svartype":"CHECKBOX","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[],"metadata":null},{"id":"c95aca0e-6730-3ca8-968f-4ea0dd34b22e","tag":"INNTEKTSOPPLYSNINGER_VARIG_ENDRING_BEGRUNNELSE_ANNET","sporsmalstekst":"Annet","undertekst":null,"min":null,"max":null,"svartype":"CHECKBOX","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[],"metadata":null}],"metadata":null},{"id":"5621c659-ff15-3b25-a73e-248966470fd0","tag":"INNTEKTSOPPLYSNINGER_VARIG_ENDRING_25_PROSENT","sporsmalstekst":"Har du hatt mer enn 25 prosent endring i årsinntekten din som følge av den varige endringen?","undertekst":null,"min":null,"max":null,"svartype":"JA_NEI","kriterieForVisningAvUndersporsmal":"JA","svar":[],"undersporsmal":[{"id":"551d8684-7f24-3965-b37c-92fbb7951d33","tag":"INNTEKTSOPPLYSNINGER_VARIG_ENDRING_DATO","sporsmalstekst":"Når skjedde den siste varige endringen?","undertekst":null,"min":"2022-01-01","max":"2025-07-01","svartype":"DATO","kriterieForVisningAvUndersporsmal":null,"svar":[],"undersporsmal":[],"metadata":null}],"metadata":{"sigrunInntekt":{"inntekter":[{"aar":"2024","verdi":709238},{"aar":"2023","verdi":772630},{"aar":"2022","verdi":787400}],"g-verdier":[{"aar":"2024","verdi":122225},{"aar":"2023","verdi":116239},{"aar":"2022","verdi":109784}],"g-sykmelding":130160,"beregnet":{"snitt":756422,"p25":945528,"m25":567317},"original-inntekt":[{"inntektsaar":"2024","pensjonsgivendeInntekt":[{"datoForFastsetting":"2025-09-26T05:32:49.946Z","skatteordning":"FASTLAND","loenn":0,"loenn-bare-pensjon":0,"naering":666000,"fiske-fangst-familiebarnehage":0}],"totalInntekt":666000},{"inntektsaar":"2023","pensjonsgivendeInntekt":[{"datoForFastsetting":"2025-09-26T05:34:01.182Z","skatteordning":"FASTLAND","loenn":0,"loenn-bare-pensjon":0,"naering":689995,"fiske-fangst-familiebarnehage":0}],"totalInntekt":689995},{"inntektsaar":"2022","pensjonsgivendeInntekt":[{"datoForFastsetting":"2025-09-26T05:34:16.259Z","skatteordning":"FASTLAND","loenn":0,"loenn-bare-pensjon":0,"naering":674999,"fiske-fangst-familiebarnehage":0}],"totalInntekt":674999}]}}}],"metadata":null}],"metadata":null}],"metadata":null}],"metadata":null}],"metadata":null},{"id":"351d2a33-a434-3467-8032-584c6bb33e1e","tag":"TIL_SLUTT","sporsmalstekst":null,"undertekst":null,"min":null,"max":null,"svartype":"OPPSUMMERING","kriterieForVisningAvUndersporsmal":null,"svar":[{"verdi":"true"}],"undersporsmal":[],"metadata":null}],"avsendertype":"BRUKER","ettersending":false,"mottaker":null,"egenmeldtSykmelding":false,"yrkesskade":null,"arbeidUtenforNorge":false,"harRedusertVenteperiode":false,"behandlingsdager":[],"permitteringer":[],"merknaderFraSykmelding":null,"egenmeldingsdagerFraSykmelding":null,"merknader":null,"sendTilGosys":null,"utenlandskSykmelding":false,"medlemskapVurdering":null,"forstegangssoknad":true,"tidligereArbeidsgiverOrgnummer":null,"fiskerBlad":null,"inntektFraNyttArbeidsforhold":[],"selvstendigNaringsdrivende":{"roller":[],"inntekt":{"norskPersonidentifikator":"29419408008","inntektsAar":[{"aar":"2024","pensjonsgivendeInntekt":{"pensjonsgivendeInntektAvLoennsinntekt":0,"pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel":0,"pensjonsgivendeInntektAvNaeringsinntekt":666000,"pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage":0},"erFerdigLignet":true},{"aar":"2023","pensjonsgivendeInntekt":{"pensjonsgivendeInntektAvLoennsinntekt":0,"pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel":0,"pensjonsgivendeInntektAvNaeringsinntekt":689995,"pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage":0},"erFerdigLignet":true},{"aar":"2022","pensjonsgivendeInntekt":{"pensjonsgivendeInntektAvLoennsinntekt":0,"pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel":0,"pensjonsgivendeInntektAvNaeringsinntekt":674999,"pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage":0},"erFerdigLignet":true}]},"ventetid":{"fom":"2025-07-01","tom":"2025-07-16"},"syketilfelleHistorikk":null,"hovedSporsmalSvar":{"FRAVAR_FOR_SYKMELDINGEN_V2":false,"TILBAKE_I_ARBEID":false,"ARBEID_UNDERVEIS_100_PROSENT_0":false,"ARBEID_UTENFOR_NORGE":false,"ANDRE_INNTEKTSKILDER":false,"OPPHOLD_UTENFOR_EOS":false,"INNTEKTSOPPLYSNINGER_VIRKSOMHETEN_AVVIKLET":false,"INNTEKTSOPPLYSNINGER_NY_I_ARBEIDSLIVET":false,"INNTEKTSOPPLYSNINGER_VARIG_ENDRING":false},"harForsikring":false},"friskTilArbeidVedtakId":null,"friskTilArbeidVedtakPeriode":null,"fortsattArbeidssoker":null,"inntektUnderveis":null,"ignorerArbeidssokerregister":null}
""".trimIndent()
