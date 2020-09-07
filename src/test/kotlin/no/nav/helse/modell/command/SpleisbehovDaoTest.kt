package no.nav.helse.modell.command

import AbstractEndToEndTest
import io.mockk.mockk
import no.nav.helse.mediator.kafka.Hendelsefabrikk
import no.nav.helse.mediator.kafka.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.*

internal class SpleisbehovDaoTest : AbstractEndToEndTest() {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FNR = "fnr"
        private const val AKTØR = "aktør"
    }

    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, AKTØR)
    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val snapshotDao = mockk<SnapshotDao>(relaxed = true)
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    private val hendelsefabrikk = Hendelsefabrikk(mockk(), mockk(), vedtakDao, mockk(), commandContextDao, snapshotDao, restClient)
    private val vedtaksperiodeForkastetMessage = hendelsefabrikk.nyNyVedtaksperiodeForkastet(testmeldingfabrikk.lagVedtaksperiodeForkastet(
        HENDELSE_ID,
        VEDTAKSPERIODE_ID
    ))

    private lateinit var dao: SpleisbehovDao

    @BeforeAll
    fun setup() {
        dao = SpleisbehovDao(dataSource, hendelsefabrikk)
    }

    @Test
    fun `lagrer og finner hendelser`() {
        dao.opprett(vedtaksperiodeForkastetMessage)
        val actual = dao.finn(HENDELSE_ID) ?: fail { "Forventet å finne en hendelse med id $HENDELSE_ID" }
        assertEquals(FNR, actual.fødselsnummer())
        assertEquals(VEDTAKSPERIODE_ID, actual.vedtaksperiodeId())
    }
}
