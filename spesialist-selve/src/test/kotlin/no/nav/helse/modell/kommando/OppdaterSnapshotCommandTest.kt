package no.nav.helse.modell.kommando

import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.graphql.SpeilSnapshotGraphQLClient
import no.nav.helse.mediator.graphql.HentSnapshot
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
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
    private val speilSnapshotGraphQLClient = mockk<SpeilSnapshotGraphQLClient>(relaxed = true)
    private val context = CommandContext(UUID.randomUUID())

    private val command =
        OppdaterSnapshotCommand(speilSnapshotGraphQLClient, vedtakDao, snapshotDao, VEDTAKSPERIODE, FNR)

    @BeforeEach
    fun setup() {
        clearMocks(vedtakDao, snapshotDao, speilSnapshotGraphQLClient)
        Toggle.GraphQLApi.enable()
    }

    @AfterEach
    fun cleanup() {
        Toggle.GraphQLApi.disable()
    }

    @Test
    fun `ignorer vedtaksperioder som ikke finnes`() {
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE) } returns null
        assertTrue(command.execute(context))
        verify(exactly = 0) { speilSnapshotGraphQLClient.hentSnapshot(FNR) }
        verify(exactly = 0) { snapshotDao.lagre(FNR, any()) }
    }

    @Test
    fun `lagrer snapshot`() {
        test { assertTrue(command.execute(context)) }
    }

    private fun test(block: () -> Unit) {
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE) } returns 1L
        every { speilSnapshotGraphQLClient.hentSnapshot(FNR) } returns object : GraphQLClientResponse<HentSnapshot.Result> {
            override val data get() = HentSnapshot.Result(person = PERSON)
        }
        every { snapshotDao.lagre(FNR, PERSON) } returns 1
        block()
        verify(exactly = 1) { speilSnapshotGraphQLClient.hentSnapshot(FNR) }
        verify(exactly = 1) { snapshotDao.lagre(FNR, PERSON) }
    }

}
