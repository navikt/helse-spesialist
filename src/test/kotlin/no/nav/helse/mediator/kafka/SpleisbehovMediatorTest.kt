package no.nav.helse.mediator.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.mediator.kafka.meldinger.ICommandMediator
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeEndretMessage
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.CommandContextTilstand
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.SpleisbehovDao
import no.nav.helse.modell.vedtak.VedtakDto
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.junit.jupiter.api.Test
import java.util.*
import javax.sql.DataSource

internal class SpleisbehovMediatorTest {
    private companion object {
        private val ID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FNR = "fnr"
        private const val SNAPSHOT = "json"
        private val vedtak = VedtakDto(1, 2)
    }

    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    private val dataSource = mockk<DataSource>()
    private val vedtakDao = mockk<VedtakDao>()
    private val snapshotDao = mockk<SnapshotDao>()
    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val spleisbehovDao = mockk<SpleisbehovDao>(relaxed = true)

    private val mediator = SpleisbehovMediator(restClient, dataSource, UUID.randomUUID(), vedtakDao, snapshotDao, commandContextDao, spleisbehovDao)

    @Test
    fun `hendelser blir lagret`() {
        val hendelse = object : Hendelse {
            override val id = ID
            override fun håndter(mediator: ICommandMediator) { }
            override fun toJson(): String { return SNAPSHOT }
        }

        mediator.håndter(hendelse)
        verify(exactly = 1) { commandContextDao.lagre(hendelse, any(), CommandContextTilstand.NY) }
        verify(exactly = 1) { spleisbehovDao.opprett(hendelse) }
    }

    @Test
    fun `vedtaksperiodeEndret oppdaterer snapshot`() {
        every { vedtakDao.findVedtak(VEDTAKSPERIODE_ID) } returns vedtak
        every { restClient.hentSpeilSpapshot(FNR) } returns SNAPSHOT
        every { snapshotDao.oppdaterSnapshotForVedtaksperiode(VEDTAKSPERIODE_ID, SNAPSHOT) } returns 1
        val message = NyVedtaksperiodeEndretMessage(ID, VEDTAKSPERIODE_ID, FNR)
        mediator.håndter(message)
        verify(exactly = 1) { snapshotDao.oppdaterSnapshotForVedtaksperiode(VEDTAKSPERIODE_ID, SNAPSHOT) }
        verify(exactly = 1) { commandContextDao.lagre(any(), any(), CommandContextTilstand.FERDIG) }
    }
}
