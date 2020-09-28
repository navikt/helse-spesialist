package no.nav.helse

import AbstractEndToEndTest
import io.mockk.mockk
import kotliquery.Session
import kotliquery.sessionOf
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.mediator.kafka.meldinger.BistandSaksbehandlerMessage
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class BistandSaksbehandlerMediatorTest: AbstractEndToEndTest() {
    private lateinit var session: Session
    private val spleisMockClient = SpleisMockClient()
    private val accessTokenClient = accessTokenClient()
    private val speilSnapshotRestClient = SpeilSnapshotRestClient(
        spleisMockClient.client,
        accessTokenClient,
        "spleisClientId"
    )

    private val spesialistOID: UUID = UUID.randomUUID()

    @BeforeAll
    fun setup() {
        session = sessionOf(dataSource, returnGeneratedKey = true)
    }

    @AfterAll
    fun tearDown() {
        session.close()
    }

    @Test
    fun `spleis trenger bistand fra saksbehandler`() {
        val spleisbehovMediator = HendelseMediator(
            rapidsConnection = TestRapid(),
            dataSource = dataSource,
            speilSnapshotRestClient = speilSnapshotRestClient,
            spesialistOID = spesialistOID,
            miljøstyrtFeatureToggle = mockk(relaxed = true)
        )
        val spleisbehovId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        spleisbehovMediator.håndter(
            BistandSaksbehandlerMessage(
                id = eventId,
                vedtaksperiodeId = UUID.randomUUID(),
                fødselsnummer = "1234567890",
                aktørId = "12398761293",
                orgnummer = "98765432",
                periodeFom = LocalDate.of(2020, 1, 1),
                periodeTom = LocalDate.of(2020, 1, 31)
            ), "{}"
        )

        spleisbehovMediator.håndter(
            eventId,
            HentEnhetLøsning("3417"),
            HentPersoninfoLøsning(
                fornavn = "Fornavn",
                mellomnavn = "Mellomnavn",
                etternavn = "Etternavnsen",
                fødselsdato = LocalDate.of(1970, 1, 1),
                kjønn = Kjønn.Ukjent
            ),
            HentInfotrygdutbetalingerLøsning(objectMapper.createObjectNode())
        )
    }
}
