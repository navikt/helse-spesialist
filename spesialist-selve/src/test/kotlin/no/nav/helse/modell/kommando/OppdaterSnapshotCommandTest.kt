package no.nav.helse.modell.kommando

import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.PersonRepository
import no.nav.helse.db.SnapshotRepository
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spleis.graphql.HentSnapshot
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OppdaterSnapshotCommandTest {

    private companion object {
        private const val FNR = "fnr"
        private const val AKTØR = "9999999999"

        private val PERSON = GraphQLPerson(
            aktorId = AKTØR,
            arbeidsgivere = emptyList(),
            dodsdato = null,
            fodselsnummer = FNR,
            versjon = 1,
            vilkarsgrunnlag = emptyList(),
        )
    }

    private val personRepository = mockk<PersonRepository>(relaxed = false)
    private val snapshotRepository = mockk<SnapshotRepository>(relaxed = true)
    private val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val context = CommandContext(UUID.randomUUID())

    private val command = OppdaterSnapshotCommand(
        snapshotClient = snapshotClient,
        snapshotRepository = snapshotRepository,
        fødselsnummer = FNR,
        personRepository = personRepository,
    )

    @BeforeEach
    fun setup() {
        clearMocks(personRepository, snapshotRepository, snapshotClient)
    }

    @Test
    fun `ignorer meldinger for ukjente personer`() {
        every { personRepository.finnPersonMedFødselsnummer(any()) } returns null
        assertTrue(command.execute(context))
        verify(exactly = 0) { snapshotClient.hentSnapshot(FNR) }
        verify(exactly = 0) { snapshotRepository.lagre(FNR, any()) }
    }

    @Test
    fun `lagrer snapshot`() {
        every { personRepository.finnPersonMedFødselsnummer(any()) } returns 1L
        every { personRepository.finnPersoninfoRef(any()) } returns 1L
        every { snapshotClient.hentSnapshot(FNR) } returns object : GraphQLClientResponse<HentSnapshot.Result> {
            override val data = HentSnapshot.Result(person = PERSON)
        }
        assertTrue(command.execute(context))
        verify(exactly = 1) { snapshotClient.hentSnapshot(FNR) }
        verify(exactly = 1) { snapshotRepository.lagre(FNR, PERSON) }
    }
}
