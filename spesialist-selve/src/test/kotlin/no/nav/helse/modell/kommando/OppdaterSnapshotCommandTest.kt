package no.nav.helse.modell.kommando

import ToggleHelpers.disable
import ToggleHelpers.enable
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.mediator.graphql.HentSnapshot
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class OppdaterSnapshotCommandTest {

    private companion object {
        private const val FNR = "fnr"
        private const val AKTØR = "9999999999"

        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val PERSON = GraphQLPerson(
            aktorId = AKTØR,
            arbeidsgivere = emptyList(),
            dodsdato = null,
            fodselsnummer = FNR,
            inntektsgrunnlag = emptyList(),
            versjon = 1,
            vilkarsgrunnlaghistorikk = emptyList()
        )
    }

    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val snapshotDao = mockk<SnapshotDao>(relaxed = true)
    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val context = CommandContext(UUID.randomUUID())

    private val command = OppdaterSnapshotCommand(
        snapshotClient = snapshotClient,
        snapshotDao = snapshotDao,
        vedtaksperiodeId = VEDTAKSPERIODE,
        fødselsnummer = FNR,
        warningDao = warningDao,
        vedtakDao = vedtakDao
    )

    @BeforeEach
    fun setup() {
        clearMocks(vedtakDao, snapshotDao, snapshotClient)
    }

    @Test
    fun `ignorer vedtaksperioder som ikke finnes`() {
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE) } returns null
        assertTrue(command.execute(context))
        verify(exactly = 0) { snapshotClient.hentSnapshot(FNR) }
        verify(exactly = 0) { snapshotDao.lagre(FNR, any()) }
    }

    @Test
    fun `lagrer snapshot`() {
        Toggle.GraphQLApi.enable()
        test { assertTrue(command.execute(context)) }
        Toggle.GraphQLApi.disable()
    }

    private fun test(block: () -> Unit) {
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE) } returns 1L
        every { snapshotClient.hentSnapshot(FNR) } returns object : GraphQLClientResponse<HentSnapshot.Result> {
            override val data get() = HentSnapshot.Result(person = PERSON)
        }
        every { snapshotDao.lagre(FNR, PERSON) } returns 1
        block()
        verify(exactly = 1) { snapshotClient.hentSnapshot(FNR) }
        verify(exactly = 1) { snapshotDao.lagre(FNR, PERSON) }
    }

}
