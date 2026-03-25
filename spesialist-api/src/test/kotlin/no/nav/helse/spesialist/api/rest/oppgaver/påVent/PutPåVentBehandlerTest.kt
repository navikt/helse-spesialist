package no.nav.helse.spesialist.api.rest.oppgaver.påVent

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.LagtPåVentEvent
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.periodehistorikk.EndrePåVent
import no.nav.helse.modell.periodehistorikk.LagtPåVent
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.mai
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PutPåVentBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository
    private val oppgaveRepository = integrationTestFixture.sessionFactory.sessionContext.oppgaveRepository
    private val vedtaksperiodeRepository = integrationTestFixture.sessionFactory.sessionContext.vedtaksperiodeRepository
    private val behandlingRepository = integrationTestFixture.sessionFactory.sessionContext.behandlingRepository
    private val saksbehandlerRepository = integrationTestFixture.sessionFactory.sessionContext.saksbehandlerRepository
    private val periodehistorikkDao = integrationTestFixture.sessionFactory.sessionContext.periodehistorikkDao

    @Test
    fun `happy path`() {
        // given
        val person = lagPerson().also(personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(behandlingRepository::lagre)
        val spleisBehandlingId = behandling.spleisBehandlingId!!
        val oppgave = lagOppgave(spleisBehandlingId, UUID.randomUUID(), vedtaksperiode.id).also(oppgaveRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(saksbehandlerRepository::lagre)

        // when
        val response =
            integrationTestFixture.put(
                url = "/api/oppgaver/${oppgave.id.value}/pa-vent",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "frist": "2026-05-05",
                      "skalTildeles": true,
                      "notattekst": "En notattekst",
                      "årsaker": [
                        {
                          "key": "f3bfeb69-0929-425d-868c-c1517654848a",
                          "årsak": "en årsak"
                        },
                        {
                          "key": "9162a99f-5db2-42ea-b495-9b564088b29d",
                          "årsak": "en annen årsak"
                        }
                      ]
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        val funnedeHistorikkinnslag = periodehistorikkDao.behandlingData[behandling.id.value] ?: emptyList()
        assertEquals(1, funnedeHistorikkinnslag.size)
        assertIs<LagtPåVent>(funnedeHistorikkinnslag.single())
        assertEquals(saksbehandler, funnedeHistorikkinnslag.single().saksbehandler)
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse = OppgaveOppdatert(oppgave),
                årsak = "Oppgave lagt på vent",
            ),
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse = OppgaveOppdatert(oppgave),
                årsak = "Oppgave lagt på vent",
            ),
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse =
                    LagtPåVentEvent(
                        oppgaveId = oppgave.id.value,
                        skalTildeles = true,
                        frist = 5.mai(2026),
                        notatTekst = "En notattekst",
                        årsaker =
                            listOf(
                                LagtPåVentEvent.PåVentÅrsak(
                                    key = "f3bfeb69-0929-425d-868c-c1517654848a",
                                    årsak = "en årsak",
                                ),
                                LagtPåVentEvent.PåVentÅrsak(
                                    key = "9162a99f-5db2-42ea-b495-9b564088b29d",
                                    årsak = "en annen årsak",
                                ),
                            ),
                        fødselsnummer = person.id.value,
                        behandlingId = spleisBehandlingId.value,
                        saksbehandlerOid = saksbehandler.id.value,
                        saksbehandlerIdent = saksbehandler.ident.value,
                    ),
                årsak = "Lagt på vent",
            ),
        )
    }

    @Test
    fun `oppdater på vent`() {
        // given
        val person = lagPerson().also(personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(behandlingRepository::lagre)
        val spleisBehandlingId = behandling.spleisBehandlingId!!
        val oppgave = lagOppgave(spleisBehandlingId, UUID.randomUUID(), vedtaksperiode.id).also(oppgaveRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(saksbehandlerRepository::lagre)
        integrationTestFixture.put(
            url = "/api/oppgaver/${oppgave.id.value}/pa-vent",
            saksbehandler = saksbehandler,
            body =
                """
                {
                  "frist": "2026-05-05",
                  "skalTildeles": true,
                  "notattekst": "En notattekst",
                  "årsaker": [
                    {
                      "key": "f3bfeb69-0929-425d-868c-c1517654848a",
                      "årsak": "en årsak"
                    },
                    {
                      "key": "9162a99f-5db2-42ea-b495-9b564088b29d",
                      "årsak": "en annen årsak"
                    }
                  ]
                }
                """.trimIndent(),
        )

        // when
        val response =
            integrationTestFixture.put(
                url = "/api/oppgaver/${oppgave.id.value}/pa-vent",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "frist": "2026-05-06",
                      "skalTildeles": false,
                      "notattekst": "En annen notattekst",
                      "årsaker": [
                        {
                          "key": "8d7550d8-b7b3-49f6-abe8-21e904399451",
                          "årsak": "en tredje årsak"
                        },
                        {
                          "key": "b73be817-7ad3-495d-8070-accc56fbbbc8",
                          "årsak": "en fjerde årsak"
                        }
                      ]
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        val funnedeHistorikkinnslag = periodehistorikkDao.behandlingData[behandling.id.value] ?: emptyList()
        assertEquals(2, funnedeHistorikkinnslag.size)
        assertIs<EndrePåVent>(funnedeHistorikkinnslag.last())
        assertEquals(saksbehandler, funnedeHistorikkinnslag.last().saksbehandler)
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse = OppgaveOppdatert(oppgave),
                årsak = "Oppgave lagt på vent",
            ),
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse = OppgaveOppdatert(oppgave),
                årsak = "Oppgave lagt på vent",
            ),
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse =
                    LagtPåVentEvent(
                        oppgaveId = oppgave.id.value,
                        skalTildeles = true,
                        frist = 5.mai(2026),
                        notatTekst = "En notattekst",
                        årsaker =
                            listOf(
                                LagtPåVentEvent.PåVentÅrsak(
                                    key = "f3bfeb69-0929-425d-868c-c1517654848a",
                                    årsak = "en årsak",
                                ),
                                LagtPåVentEvent.PåVentÅrsak(
                                    key = "9162a99f-5db2-42ea-b495-9b564088b29d",
                                    årsak = "en annen årsak",
                                ),
                            ),
                        fødselsnummer = person.id.value,
                        behandlingId = spleisBehandlingId.value,
                        saksbehandlerOid = saksbehandler.id.value,
                        saksbehandlerIdent = saksbehandler.ident.value,
                    ),
                årsak = "Lagt på vent",
            ),
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse = OppgaveOppdatert(oppgave),
                årsak = "Oppgave lagt på vent",
            ),
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse =
                    LagtPåVentEvent(
                        oppgaveId = oppgave.id.value,
                        skalTildeles = false,
                        frist = 6.mai(2026),
                        notatTekst = "En annen notattekst",
                        årsaker =
                            listOf(
                                LagtPåVentEvent.PåVentÅrsak(
                                    key = "8d7550d8-b7b3-49f6-abe8-21e904399451",
                                    årsak = "en tredje årsak",
                                ),
                                LagtPåVentEvent.PåVentÅrsak(
                                    key = "b73be817-7ad3-495d-8070-accc56fbbbc8",
                                    årsak = "en fjerde årsak",
                                ),
                            ),
                        fødselsnummer = person.id.value,
                        behandlingId = spleisBehandlingId.value,
                        saksbehandlerOid = saksbehandler.id.value,
                        saksbehandlerIdent = saksbehandler.ident.value,
                    ),
                årsak = "Lagt på vent",
            ),
        )
    }

    @Test
    fun `error hvis oppgave ikke finnes`() {
        val response =
            integrationTestFixture.put(
                url = "/api/oppgaver/1/pa-vent",
                body =
                    """
                    {
                      "frist": "2026-05-05",
                      "skalTildeles": true,
                      "notattekst": "En notattekst",
                      "årsaker": [
                        {
                          "key": "f3bfeb69-0929-425d-868c-c1517654848a",
                          "årsak": "en årsak"
                        },
                        {
                          "key": "9162a99f-5db2-42ea-b495-9b564088b29d",
                          "årsak": "en annen årsak"
                        }
                      ]
                    }
                    """.trimIndent(),
            )
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """{
                "type": "about:blank",
                "status": 404,
                "title": "Oppgave ikke funnet",
                "code": "OPPGAVE_IKKE_FUNNET"
            }""",
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `error hvis saksbehandler ikke har tilgang til personen`() {
        // given
        val person = lagPerson(erEgenAnsatt = true).also(personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(behandlingRepository::lagre)
        val spleisBehandlingId = behandling.spleisBehandlingId!!
        val oppgave = lagOppgave(spleisBehandlingId, UUID.randomUUID(), vedtaksperiode.id).also(oppgaveRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(saksbehandlerRepository::lagre)

        // when
        val response =
            integrationTestFixture.put(
                url = "/api/oppgaver/${oppgave.id.value}/pa-vent",
                body =
                    """
                    {
                      "frist": "2026-05-05",
                      "skalTildeles": true,
                      "notattekst": "En notattekst",
                      "årsaker": [
                        {
                          "key": "f3bfeb69-0929-425d-868c-c1517654848a",
                          "årsak": "en årsak"
                        },
                        {
                          "key": "9162a99f-5db2-42ea-b495-9b564088b29d",
                          "årsak": "en annen årsak"
                        }
                      ]
                    }
                    """.trimIndent(),
                saksbehandler = saksbehandler,
                brukerroller = emptySet(),
            )
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        assertJsonEquals(
            """{
                "type": "about:blank",
                "status": 403,
                "title": "Mangler tilgang til person",
                "code": "MANGLER_TILGANG_TIL_PERSON"
            }""",
            response.bodyAsJsonNode!!,
        )
    }
}
