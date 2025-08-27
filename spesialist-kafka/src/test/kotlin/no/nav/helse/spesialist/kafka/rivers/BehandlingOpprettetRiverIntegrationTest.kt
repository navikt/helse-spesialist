package no.nav.helse.spesialist.kafka.rivers

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.TestRapidHelpers.publiserteMeldingerUtenGenererteFelter
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BehandlingOpprettetRiverIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    @Test
    fun `oppretter ny vedtaksperiode med behandling`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val fom = LocalDate.parse("2024-10-01")
        val tom = LocalDate.parse("2024-10-31")
        initPerson(fødselsnummer = fødselsnummer, vedtaksperioder = emptyList())

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                yrkesaktivitetstype = "ARBEIDSTAKER"
            )
        )

        // Then:
        personRepository.brukPersonHvisFinnes(fødselsnummer) {
            val behandling = vedtaksperioder().single().råBehandlinger().single()
            assertEquals(vedtaksperiodeId, behandling.vedtaksperiodeId)
            assertEquals(null, behandling.utbetalingId)
            assertEquals(behandlingId, behandling.spleisBehandlingId)
            assertEquals(fom, behandling.skjæringstidspunkt)
            assertEquals(fom, behandling.fom())
            assertEquals(tom, behandling.tom())
            assertEquals("VidereBehandlingAvklares", behandling.tilstand.navn())
            assertEquals(emptyList<String>(), behandling.tags)
            assertEquals(null, behandling.vedtakBegrunnelse)
            assertEquals(emptyList<Varsel>(), behandling.varsler())
            assertEquals(Yrkesaktivitetstype.ARBEIDSTAKER, behandling.yrkesaktivitetstype)
        }

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun `oppretter ny behandling på eksisterende vedtaksperiode`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val fom = LocalDate.parse("2024-10-01")
        val tom = LocalDate.parse("2024-10-31")
        initPerson(
            fødselsnummer = fødselsnummer, vedtaksperioder = listOf(
                VedtaksperiodeDto(
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    forkastet = false,
                    behandlinger = listOf(
                        BehandlingDto(
                            id = UUID.randomUUID(),
                            vedtaksperiodeId = vedtaksperiodeId,
                            utbetalingId = null,
                            spleisBehandlingId = UUID.randomUUID(),
                            skjæringstidspunkt = fom.minusMonths(1),
                            fom = fom.minusMonths(1),
                            tom = tom.minusMonths(1),
                            tilstand = TilstandDto.KlarTilBehandling,
                            tags = emptyList(),
                            vedtakBegrunnelse = null,
                            varsler = emptyList(),
                            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
                        )
                    )
                )
            )
        )

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                yrkesaktivitetstype = "ARBEIDSTAKER"
            )
        )

        // Then:
        personRepository.brukPersonHvisFinnes(fødselsnummer) {
            val vedtaksperiode = vedtaksperioder().single()
            val behandlinger = vedtaksperiode.råBehandlinger()
            assertEquals(2, behandlinger.size)
            val behandling = vedtaksperiode.finnBehandling(behandlingId)
            assertEquals(vedtaksperiodeId, behandling.vedtaksperiodeId)
            assertEquals(null, behandling.utbetalingId)
            assertEquals(behandlingId, behandling.spleisBehandlingId)
            assertEquals(fom.minusMonths(1), behandling.skjæringstidspunkt)
            assertEquals(fom, behandling.fom())
            assertEquals(tom, behandling.tom())
            assertEquals("VidereBehandlingAvklares", behandling.tilstand.navn())
            assertEquals(emptyList<String>(), behandling.tags)
            assertEquals(null, behandling.vedtakBegrunnelse)
            assertEquals(emptyList<Varsel>(), behandling.varsler())
            assertEquals(Yrkesaktivitetstype.ARBEIDSTAKER, behandling.yrkesaktivitetstype)
        }

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun `melding om samme behandling med ny data overses`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val fom = LocalDate.parse("2024-10-01")
        val tom = LocalDate.parse("2024-10-31")
        initPerson(
            fødselsnummer = fødselsnummer, vedtaksperioder = listOf(
                VedtaksperiodeDto(
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    forkastet = false,
                    behandlinger = listOf(
                        BehandlingDto(
                            id = UUID.randomUUID(),
                            vedtaksperiodeId = vedtaksperiodeId,
                            utbetalingId = null,
                            spleisBehandlingId = behandlingId,
                            skjæringstidspunkt = fom,
                            fom = fom,
                            tom = tom,
                            tilstand = TilstandDto.KlarTilBehandling,
                            tags = emptyList(),
                            vedtakBegrunnelse = null,
                            varsler = emptyList(),
                            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
                        )
                    )
                )
            )
        )

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = lagOrganisasjonsnummer(),
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                fom = fom.plusMonths(1),
                tom = tom.plusMonths(1),
                yrkesaktivitetstype = "ARBEIDSTAKER"
            )
        )

        // Then:
        personRepository.brukPersonHvisFinnes(fødselsnummer) {
            val behandling = vedtaksperioder().single().råBehandlinger().single()
            assertEquals(vedtaksperiodeId, behandling.vedtaksperiodeId)
            assertEquals(null, behandling.utbetalingId)
            assertEquals(behandlingId, behandling.spleisBehandlingId)
            assertEquals(fom, behandling.skjæringstidspunkt)
            assertEquals(fom, behandling.fom())
            assertEquals(tom, behandling.tom())
            assertEquals("KlarTilBehandling", behandling.tilstand.navn())
            assertEquals(emptyList<String>(), behandling.tags)
            assertEquals(null, behandling.vedtakBegrunnelse)
            assertEquals(emptyList<Varsel>(), behandling.varsler())
            assertEquals(Yrkesaktivitetstype.ARBEIDSTAKER, behandling.yrkesaktivitetstype)
        }

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun `oppretter ikke ny vedtaksperiode for selvstendig næringsdrivende dersom feature toggle er av`() {
        // Given:
        integrationTestFixture.featureToggles.skalBehandleSelvstendig = false
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val fom = LocalDate.parse("2024-10-01")
        val tom = LocalDate.parse("2024-10-31")
        initPerson(fødselsnummer = fødselsnummer, vedtaksperioder = emptyList())

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = "SELVSTENDIG",
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                yrkesaktivitetstype = "SELVSTENDIG"
            )
        )

        // Then:
        personRepository.brukPersonHvisFinnes(fødselsnummer) {
            assertEquals(emptyList<Vedtaksperiode>(), vedtaksperioder())
        }

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun `oppretter ny vedtaksperiode for selvstendig næringsdrivende dersom feature toggle er på`() {
        // Given:
        integrationTestFixture.featureToggles.skalBehandleSelvstendig = true
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val fom = LocalDate.parse("2024-10-01")
        val tom = LocalDate.parse("2024-10-31")
        initPerson(fødselsnummer = fødselsnummer, vedtaksperioder = emptyList())

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = "SELVSTENDIG",
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                yrkesaktivitetstype = "SELVSTENDIG"
            )
        )

        // Then:
        personRepository.brukPersonHvisFinnes(fødselsnummer) {
            val behandling = vedtaksperioder().single().råBehandlinger().single()
            assertEquals(vedtaksperiodeId, behandling.vedtaksperiodeId)
            assertEquals(null, behandling.utbetalingId)
            assertEquals(behandlingId, behandling.spleisBehandlingId)
            assertEquals(fom, behandling.skjæringstidspunkt)
            assertEquals(fom, behandling.fom())
            assertEquals(tom, behandling.tom())
            assertEquals("VidereBehandlingAvklares", behandling.tilstand.navn())
            assertEquals(emptyList<String>(), behandling.tags)
            assertEquals(null, behandling.vedtakBegrunnelse)
            assertEquals(emptyList<Varsel>(), behandling.varsler())
            assertEquals(Yrkesaktivitetstype.SELVSTENDIG, behandling.yrkesaktivitetstype)
        }

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    private fun initPerson(fødselsnummer: String, vedtaksperioder: List<VedtaksperiodeDto>) {
        personRepository.leggTilPerson(
            Person.gjenopprett(
                aktørId = lagAktørId(),
                fødselsnummer = fødselsnummer,
                vedtaksperioder = vedtaksperioder,
                skjønnsfastsattSykepengegrunnlag = emptyList(),
                avviksvurderinger = emptyList()
            )
        )
    }

    @Language("JSON")
    private fun behandlingOpprettetMelding(
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        yrkesaktivitetstype: String,
    ) = """
        {
          "@event_name": "behandling_opprettet",
          "organisasjonsnummer": "$organisasjonsnummer",
          "yrkesaktivitetstype": "$yrkesaktivitetstype",
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "behandlingId": "$behandlingId",
          "søknadIder": [
            "77cc592f-58e3-48ef-8e25-4196c8299fe8"
          ],
          "type": "Søknad",
          "fom": "$fom",
          "tom": "$tom",
          "kilde": {
            "meldingsreferanseId": "77cc592f-58e3-48ef-8e25-4196c8299fe8",
            "innsendt": "2025-06-01T00:00:00",
            "registrert": "2025-08-27T11:54:41.604072031",
            "avsender": "SYKMELDT"
          },
          "@id": "91235213-ccb7-4636-a078-aae0707a4a66",
          "@opprettet": "2025-08-27T11:54:41.615994353",
          "system_read_count": 1,
          "system_participating_services": [
            {
              "id": "91235213-ccb7-4636-a078-aae0707a4a66",
              "time": "2025-08-27T11:54:41.615994353",
              "service": "helse-spleis",
              "instance": "helse-spleis-78967f9b55-28p6r",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spleis-spleis:2025.08.26-13.14-e2b3e88"
            },
            {
              "id": "91235213-ccb7-4636-a078-aae0707a4a66",
              "time": "2025-08-27T11:54:41.631944467",
              "service": "spesialist",
              "instance": "spesialist-6867b5777f-gs4ht",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.26-14.48-9333d1b"
            }
          ],
          "fødselsnummer": "$fødselsnummer",
          "@forårsaket_av": {
            "id": "77cc592f-58e3-48ef-8e25-4196c8299fe8",
            "opprettet": "2025-06-01T00:00",
            "event_name": "sendt_søknad_nav"
          }
        }
    """.trimIndent()
}
