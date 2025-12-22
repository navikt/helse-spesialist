package no.nav.helse.spesialist.kafka.rivers

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import no.nav.helse.spesialist.kafka.TestRapidHelpers.publiserteMeldingerUtenGenererteFelter
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BehandlingOpprettetRiverIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `oppretter ny vedtaksperiode med behandling`() {
        // Given:
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)

        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val fom = LocalDate.parse("2024-10-01")
        val tom = LocalDate.parse("2024-10-31")

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                person = person,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                yrkesaktivitetstype = "ARBEIDSTAKER",
                organisasjonsnummer = organisasjonsnummer,
                fom = fom,
                tom = tom,
            ),
        )

        // Then:
        val vedtaksperiode = sessionContext.vedtaksperiodeRepository.alle().single()
        assertEquals(vedtaksperiodeId, vedtaksperiode.id)
        assertEquals(person.id.value, vedtaksperiode.fødselsnummer)
        assertEquals(organisasjonsnummer, vedtaksperiode.organisasjonsnummer)
        assertEquals(false, vedtaksperiode.forkastet)

        val behandling = sessionContext.behandlingRepository.alle().single()
        assertEquals(spleisBehandlingId, behandling.spleisBehandlingId)
        assertEquals(vedtaksperiodeId, behandling.vedtaksperiodeId)
        assertEquals(null, behandling.utbetalingId)
        assertEquals(emptySet<String>(), behandling.tags)
        assertEquals(Behandling.Tilstand.VidereBehandlingAvklares, behandling.tilstand)
        assertEquals(fom, behandling.fom)
        assertEquals(tom, behandling.tom)
        assertEquals(fom, behandling.skjæringstidspunkt)
        assertEquals(Yrkesaktivitetstype.ARBEIDSTAKER, behandling.yrkesaktivitetstype)

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun `oppretter ny behandling på eksisterende vedtaksperiode`() {
        // Given:
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)

        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id)
                .also(sessionContext.vedtaksperiodeRepository::lagre)

        val eksisterendeBehandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiode.id,
                tags = setOf("Tag 1", "Tag 2"),
                tilstand = Behandling.Tilstand.KlarTilBehandling,
                utbetalingId = UtbetalingId(UUID.randomUUID()),
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            ).also(sessionContext.behandlingRepository::lagre)

        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val fom = eksisterendeBehandling.fom.plusMonths(1)
        val tom = eksisterendeBehandling.tom.plusMonths(1)

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                person = person,
                vedtaksperiodeId = vedtaksperiode.id,
                spleisBehandlingId = spleisBehandlingId,
                yrkesaktivitetstype = "ARBEIDSTAKER",
                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                fom = fom,
                tom = tom,
            ),
        )

        // Then:
        assertEquals(vedtaksperiode, sessionContext.vedtaksperiodeRepository.alle().single())

        val behandlinger = sessionContext.behandlingRepository.alle()
        assertEquals(2, behandlinger.size)
        val behandling = behandlinger.first { it.spleisBehandlingId == spleisBehandlingId }
        assertEquals(spleisBehandlingId, behandling.spleisBehandlingId)
        assertEquals(vedtaksperiode.id, behandling.vedtaksperiodeId)
        assertEquals(null, behandling.utbetalingId)
        assertEquals(emptySet<String>(), behandling.tags)
        assertEquals(Behandling.Tilstand.VidereBehandlingAvklares, behandling.tilstand)
        assertEquals(fom, behandling.fom)
        assertEquals(tom, behandling.tom)
        assertEquals(eksisterendeBehandling.skjæringstidspunkt, behandling.skjæringstidspunkt)
        assertEquals(Yrkesaktivitetstype.ARBEIDSTAKER, behandling.yrkesaktivitetstype)

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun `flytter aktive varsler fra gammel til ny behandling`() {
        // Given:
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)

        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id)
                .also(sessionContext.vedtaksperiodeRepository::lagre)

        val eksisterendeBehandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiode.id,
                tags = setOf("Tag 1", "Tag 2"),
                tilstand = Behandling.Tilstand.KlarTilBehandling,
                utbetalingId = UtbetalingId(UUID.randomUUID()),
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            ).also(sessionContext.behandlingRepository::lagre)

        val varsel =
            Varsel.fraLagring(
                id = VarselId(UUID.randomUUID()),
                spleisBehandlingId = eksisterendeBehandling.spleisBehandlingId,
                behandlingUnikId = eksisterendeBehandling.id,
                status = Varsel.Status.AKTIV,
                kode = "RV_IV_1",
                opprettetTidspunkt = LocalDateTime.now(),
                vurdering = null,
            )
        sessionContext.varselRepository.lagre(varsel)

        // When:
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                person = person,
                vedtaksperiodeId = vedtaksperiode.id,
                spleisBehandlingId = spleisBehandlingId,
                yrkesaktivitetstype = "ARBEIDSTAKER",
                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                fom = eksisterendeBehandling.fom,
                tom = eksisterendeBehandling.tom,
            ),
        )

        // Then:
        val nyBehandling = sessionContext.behandlingRepository.finn(spleisBehandlingId)
        assertNotNull(nyBehandling)
        val varslerPåGammelBehandling = sessionContext.varselRepository.finnVarslerFor(eksisterendeBehandling.id)
        val varslerPåNyBehandling = sessionContext.varselRepository.finnVarslerFor(nyBehandling.id)
        assertEquals(0, varslerPåGammelBehandling.size)
        assertEquals(1, varslerPåNyBehandling.size)
        val varselet = varslerPåNyBehandling.single()
        assertEquals(nyBehandling.id, varselet.behandlingUnikId)
    }

    @Test
    fun `oppretter ikke ny behandling på eksisterende vedtaksperiode hvis perioden er forkastet`() {
        // Given:
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)

        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id, forkastet = true)
                .also(sessionContext.vedtaksperiodeRepository::lagre)

        val eksisterendeBehandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiode.id,
                tags = setOf("Tag 1", "Tag 2"),
                tilstand = Behandling.Tilstand.KlarTilBehandling,
                utbetalingId = UtbetalingId(UUID.randomUUID()),
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            ).also(sessionContext.behandlingRepository::lagre)

        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val fom = eksisterendeBehandling.fom.plusMonths(1)
        val tom = eksisterendeBehandling.tom.plusMonths(1)

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                person = person,
                vedtaksperiodeId = vedtaksperiode.id,
                spleisBehandlingId = spleisBehandlingId,
                yrkesaktivitetstype = "ARBEIDSTAKER",
                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                fom = fom,
                tom = tom,
            ),
        )

        // Then:
        assertEquals(vedtaksperiode, sessionContext.vedtaksperiodeRepository.alle().single())

        val behandlinger = sessionContext.behandlingRepository.alle()
        assertEquals(1, behandlinger.size)
        val behandling = behandlinger.singleOrNull { it.spleisBehandlingId == spleisBehandlingId }
        assertNull(behandling)
    }

    @Test
    fun `melding om samme behandling med ny data overses`() {
        // Given:
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)

        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id)
                .also(sessionContext.vedtaksperiodeRepository::lagre)

        val fom = LocalDate.now().minusMonths(3).withDayOfMonth(1)
        val tom = fom.withDayOfMonth(YearMonth.from(fom).lengthOfMonth())
        val eksisterendeBehandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiode.id,
                fom = fom,
                tom = tom,
                tilstand = Behandling.Tilstand.KlarTilBehandling,
                tags = setOf("Tag 1", "Tag 2"),
                utbetalingId = UtbetalingId(UUID.randomUUID()),
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            ).also(sessionContext.behandlingRepository::lagre)

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                person = person,
                vedtaksperiodeId = vedtaksperiode.id,
                spleisBehandlingId = eksisterendeBehandling.spleisBehandlingId!!,
                yrkesaktivitetstype = "ARBEIDSTAKER",
                organisasjonsnummer = lagOrganisasjonsnummer(),
                fom = fom.plusMonths(1),
                tom = tom.plusMonths(1),
            ),
        )

        // Then:
        assertEquals(vedtaksperiode, sessionContext.vedtaksperiodeRepository.alle().single())

        val behandling = sessionContext.behandlingRepository.alle().single()
        assertEquals(eksisterendeBehandling, behandling)
        assertEquals(vedtaksperiode.id, behandling.vedtaksperiodeId)
        assertEquals(eksisterendeBehandling.utbetalingId, behandling.utbetalingId)
        assertEquals(eksisterendeBehandling.tags, behandling.tags)
        assertEquals(eksisterendeBehandling.tilstand, behandling.tilstand)
        assertEquals(eksisterendeBehandling.fom, behandling.fom)
        assertEquals(eksisterendeBehandling.tom, behandling.tom)
        assertEquals(eksisterendeBehandling.fom, behandling.skjæringstidspunkt)
        assertEquals(eksisterendeBehandling.yrkesaktivitetstype, behandling.yrkesaktivitetstype)

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun `oppretter ikke ny vedtaksperiode for frilans`() {
        // Given:
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                person = person,
                vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                yrkesaktivitetstype = "FRILANS",
                organisasjonsnummer = "FRILANS",
                fom = LocalDate.parse("2024-10-01"),
                tom = LocalDate.parse("2024-10-31"),
            ),
        )

        // Then:
        assertEquals(emptyList<Vedtaksperiode>(), sessionContext.vedtaksperiodeRepository.alle())
        assertEquals(emptyList<Behandling>(), sessionContext.behandlingRepository.alle())

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun `oppretter ikke ny vedtaksperiode for arbeidsledig`() {
        // Given:
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                person = person,
                vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                yrkesaktivitetstype = "ARBEIDSLEDIG",
                organisasjonsnummer = "ARBEIDSLEDIG",
                fom = LocalDate.parse("2024-10-01"),
                tom = LocalDate.parse("2024-10-31"),
            ),
        )

        // Then:
        assertEquals(emptyList<Vedtaksperiode>(), sessionContext.vedtaksperiodeRepository.alle())
        assertEquals(emptyList<Behandling>(), sessionContext.behandlingRepository.alle())

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun `oppretter ny vedtaksperiode for selvstendig næringsdrivende`() {
        // Given:
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)

        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val fom = LocalDate.parse("2024-10-01")
        val tom = LocalDate.parse("2024-10-31")

        // When:
        testRapid.sendTestMessage(
            behandlingOpprettetMelding(
                person = person,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                yrkesaktivitetstype = "SELVSTENDIG",
                organisasjonsnummer = "SELVSTENDIG",
                fom = fom,
                tom = tom,
            ),
        )

        // Then:
        val vedtaksperiode = sessionContext.vedtaksperiodeRepository.alle().single()
        assertEquals(vedtaksperiodeId, vedtaksperiode.id)
        assertEquals(person.id.value, vedtaksperiode.fødselsnummer)
        assertEquals("SELVSTENDIG", vedtaksperiode.organisasjonsnummer)
        assertEquals(false, vedtaksperiode.forkastet)

        val behandling = sessionContext.behandlingRepository.alle().single()
        assertEquals(spleisBehandlingId, behandling.spleisBehandlingId)
        assertEquals(vedtaksperiodeId, behandling.vedtaksperiodeId)
        assertEquals(null, behandling.utbetalingId)
        assertEquals(emptySet<String>(), behandling.tags)
        assertEquals(Behandling.Tilstand.VidereBehandlingAvklares, behandling.tilstand)
        assertEquals(fom, behandling.fom)
        assertEquals(tom, behandling.tom)
        assertEquals(fom, behandling.skjæringstidspunkt)
        assertEquals(Yrkesaktivitetstype.SELVSTENDIG, behandling.yrkesaktivitetstype)

        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(0, meldinger.size)
    }

    @Language("JSON")
    private fun behandlingOpprettetMelding(
        person: Person,
        vedtaksperiodeId: VedtaksperiodeId,
        spleisBehandlingId: SpleisBehandlingId,
        yrkesaktivitetstype: String,
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
    ) = """
        {
          "@event_name": "behandling_opprettet",
          "organisasjonsnummer": "$organisasjonsnummer",
          "yrkesaktivitetstype": "$yrkesaktivitetstype",
          "vedtaksperiodeId": "${vedtaksperiodeId.value}",
          "behandlingId": "${spleisBehandlingId.value}",
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
          "fødselsnummer": "${person.id.value}",
          "@forårsaket_av": {
            "id": "77cc592f-58e3-48ef-8e25-4196c8299fe8",
            "opprettet": "2025-06-01T00:00",
            "event_name": "sendt_søknad_nav"
          }
        }
        """.trimIndent()
}
