package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import org.intellij.lang.annotations.Language
import kotlin.test.Test

class PersonAvstemmingRiverTest {
    val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid =
        TestRapid().medRivers(PersonAvstemmingRiver(mediator))

    @Test
    fun `Leser inn person_avstemt event`() {
        testRapid.sendTestMessage(personAvstemt())
        verify(exactly = 1) { mediator.finnBehandlingerFor("12345678910") }
    }

    @Language("JSON")
    private fun personAvstemt(): String = """
        {
            "@event_name": "person_avstemt",
            "fødselsnummer": "12345678910",
            "arbeidsgivere": [
                {
                    "organisasjonsnummer": "999999999",
                    "vedtaksperioder": [
                        {
                            "id": "84eb578a-a959-44b7-a424-ca01042ccfdf",
                            "tilstand": "AVVENTER_GODKJENNING",
                            "opprettet": "2025-03-14T14:22:05.788122774",
                            "oppdatert": "2025-05-07T10:42:42.843098859",
                            "fom": "2024-11-30",
                            "tom": "2024-12-31",
                            "skjæringstidspunkt": "2024-11-30",
                            "utbetalinger": [
                                "2c533d1f-1ffd-4b27-89f4-6024d95236a2"
                            ],
                            "behandlinger": [
                                {
                                    "behandlingId": "2c888d1f-1ffd-4b27-89f4-6024d95236a2",
                                    "behandlingOpprettet": "2025-05-07T10:42:42.8308062"
                                },
                                {
                                    "behandlingId": "2c888d1f-1ffd-4b27-89f4-6024d95236b3",
                                    "behandlingOpprettet": "2025-06-07T10:42:42.8308062"
                                }
                            ]
                        }
                    ],
                    "forkastedeVedtaksperioder": [
                        {
                            "id": "84eb578a-a959-44b7-a424-123456789012",
                            "tilstand": "AVVENTER_GODKJENNING",
                            "opprettet": "2025-03-14T14:22:05.788122774",
                            "oppdatert": "2025-05-07T10:42:42.843098859",
                            "fom": "2024-11-30",
                            "tom": "2024-12-31",
                            "skjæringstidspunkt": "2024-11-30",
                            "utbetalinger": [
                                "2c533d1f-1ffd-4b27-89f4-6024d95236a2"
                            ],
                            "behandlinger": [
                                {
                                    "behandlingId": "2c888d1f-1ffd-4b27-89f4-123456789012",
                                    "behandlingOpprettet": "2025-06-07T10:42:42.8308062"
                                }
                            ]
                        }],
                    "utbetalinger": [
                        {
                            "id": "a157dc31-0561-45c2-af47-acbee6755417",
                            "type": "UTBETALING",
                            "status": "FORKASTET",
                            "opprettet": "2025-03-14T14:22:08.7054424",
                            "oppdatert": "2025-03-27T10:35:58.302409169",
                            "avsluttet": null,
                            "vurdering": null
                        },
                        {
                            "id": "bcb85698-5c96-4dcc-881a-07993fc56382",
                            "type": "UTBETALING",
                            "status": "FORKASTET",
                            "opprettet": "2025-03-27T11:36:06.638937254",
                            "oppdatert": "2025-05-07T10:42:37.144750828",
                            "avsluttet": null,
                            "vurdering": null
                        },
                        {
                            "id": "2c533d1f-1ffd-4b27-89f4-6024d95236a2",
                            "type": "UTBETALING",
                            "status": "IKKE_UTBETALT",
                            "opprettet": "2025-05-07T10:42:42.8308062",
                            "oppdatert": "2025-05-07T10:42:42.83135389",
                            "avsluttet": null,
                            "vurdering": null
                        }
                    ]
                }
            ],
            "@id": "08d680d2-114e-42c9-bde4-b40f5807f425",
            "@opprettet": "2025-06-23T01:00:28.980594659",
            "system_read_count": 0,
            "system_participating_services": [
                {
                    "id": "08d680d2-114e-42c9-bde4-b40f5807f425",
                    "time": "2025-06-23T01:00:28.980594659",
                    "service": "helse-spleis",
                    "instance": "helse-spleis-67c4c6f964-85bx6",
                    "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spleis-spleis:2025.06.18-09.25-b26ad1b"
                }
            ]
        }
    """.trimIndent()
}