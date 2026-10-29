package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.db.SpleisBehandlingskoblingDao.SpesialistBehandling
import no.nav.helse.kafka.SpleisBehandlingskoblingRiver.Companion.kobl
import no.nav.helse.kafka.SpleisBehandlingskoblingRiver.Kobling
import no.nav.helse.kafka.SpleisBehandlingskoblingRiver.Kontroll
import no.nav.helse.kafka.SpleisBehandlingskoblingRiver.SpleisBehandling
import no.nav.helse.kafka.SpleisBehandlingskoblingRiver.Utfall
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SpleisBehandlingskoblingRiverTest {
    private val testRapid = TestRapid()
    private val dao = IntegrationTestFixture(testRapid).sessionContext.spleisBehandlingskoblingDao
    private val t0 = LocalDateTime.of(2022, 1, 1, 12, 0)
    private val spleis1 = SpleisBehandling(UUID.randomUUID(), t0, listOf(UUID.randomUUID()))
    private val spleis2 = SpleisBehandling(UUID.randomUUID(), t0.plusDays(10), listOf(UUID.randomUUID(), UUID.randomUUID()))
    private val spleis3 = SpleisBehandling(UUID.randomUUID(), t0.plusDays(20), emptyList())
    private val alleSpleis = listOf(spleis3, spleis1, spleis2)

    @Test
    fun `rad med kjent spleis-id beholder den, og tidsregelen kontrolleres`() {
        assertEquals(
            Kobling(Utfall.KJENT, spleis2.behandlingId, Kontroll.ENIG),
            kobl(rad(t0.plusDays(11), spleisBehandlingId = spleis2.behandlingId), alleSpleis),
        )
        assertEquals(
            Kobling(Utfall.KJENT, spleis2.behandlingId, Kontroll.UENIG),
            kobl(rad(t0.plusDays(21), spleisBehandlingId = spleis2.behandlingId), alleSpleis),
        )
    }

    @Test
    fun `eksakt treff på en av utbetalingene til spleis-behandlingen`() {
        assertEquals(
            Kobling(Utfall.EKSAKT, spleis2.behandlingId, Kontroll.ENIG),
            kobl(rad(t0.plusDays(12), utbetalingId = spleis2.utbetalinger.last()), alleSpleis),
        )
    }

    @Test
    fun `tidsregel gir spleis-behandlingen som var opprettet da raden ble opprettet`() {
        assertEquals(Kobling(Utfall.TIDSREGEL, spleis1.behandlingId, null), kobl(rad(t0.plusDays(9)), alleSpleis))
        assertEquals(Kobling(Utfall.TIDSREGEL, spleis2.behandlingId, null), kobl(rad(t0.plusDays(10)), alleSpleis))
        assertEquals(Kobling(Utfall.TIDSREGEL, spleis3.behandlingId, null), kobl(rad(t0.plusDays(30)), alleSpleis))
    }

    @Test
    fun `rad opprettet før første spleis-behandling får den første`() {
        assertEquals(Kobling(Utfall.TIDSREGEL, spleis1.behandlingId, null), kobl(rad(t0.minusDays(1)), alleSpleis))
    }

    @Test
    fun `utbetaling uten treff faller tilbake på tidsregelen`() {
        assertEquals(
            Kobling(Utfall.TIDSREGEL, spleis1.behandlingId, null),
            kobl(rad(t0.plusDays(1), utbetalingId = UUID.randomUUID()), alleSpleis),
        )
    }

    @Test
    fun `ingen spleis-behandlinger`() {
        assertEquals(Kobling(Utfall.INGEN_SPLEISBEHANDLINGER, null, null), kobl(rad(t0), emptyList()))
    }

    @Test
    fun `setter spleis-id på varslene til raden`() {
        val vedtaksperiodeId = UUID.randomUUID()
        dao.behandlinger[vedtaksperiodeId] = mutableListOf(rad(t0.plusDays(1), id = 1), rad(t0.plusDays(11), id = 2))
        dao.varslerUtenSpleisBehandlingId[1] = 3
        dao.varslerUtenSpleisBehandlingId[2] = 1

        testRapid.sendTestMessage(melding(vedtaksperiodeId, dryrun = false))

        assertEquals(mapOf(1L to spleis1.behandlingId, 2L to spleis2.behandlingId), dao.koblinger)
        assertTrue(dao.varslerUtenSpleisBehandlingId.isEmpty())
    }

    @Test
    fun `endrer ingenting ved dryrun`() {
        val vedtaksperiodeId = UUID.randomUUID()
        dao.behandlinger[vedtaksperiodeId] = mutableListOf(rad(t0.plusDays(1), id = 1))
        dao.varslerUtenSpleisBehandlingId[1] = 3

        testRapid.sendTestMessage(melding(vedtaksperiodeId, dryrun = true))

        assertTrue(dao.koblinger.isEmpty())
        assertEquals(3, dao.varslerUtenSpleisBehandlingId[1])
    }

    private fun rad(
        opprettet: LocalDateTime,
        id: Long = 1,
        utbetalingId: UUID? = null,
        spleisBehandlingId: UUID? = null,
    ) = SpesialistBehandling(id = id, utbetalingId = utbetalingId, spleisBehandlingId = spleisBehandlingId, opprettet = opprettet)

    @Language("JSON")
    private fun melding(
        vedtaksperiodeId: UUID,
        dryrun: Boolean,
    ) = """
        {
          "@event_name": "spleis_behandlingskobling",
          "@id": "${UUID.randomUUID()}",
          "fødselsnummer": "12345678910",
          "dryrun": $dryrun,
          "vedtaksperioder": [
            {
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "behandlinger": [
                ${alleSpleis.joinToString { """{ "behandlingId": "${it.behandlingId}", "opprettet": "${it.opprettet}", "utbetalinger": [${it.utbetalinger.joinToString { u -> "\"$u\"" }}] }""" }}
              ]
            }
          ]
        }
        """.trimIndent()
}
