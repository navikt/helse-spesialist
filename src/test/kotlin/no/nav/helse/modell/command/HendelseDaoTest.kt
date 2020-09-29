package no.nav.helse.modell.command

import DatabaseIntegrationTest
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.Hendelsefabrikk
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeForkastetMessage
import no.nav.helse.mediator.kafka.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.IHendelsefabrikk
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.*

internal class HendelseDaoTest : DatabaseIntegrationTest() {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
    }

    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, AKTØR)
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    private lateinit var hendelsefabrikk: IHendelsefabrikk
    private lateinit var vedtaksperiodeForkastetMessage: NyVedtaksperiodeForkastetMessage

    private lateinit var dao: HendelseDao

    @BeforeAll
    fun setup() {
        hendelsefabrikk = Hendelsefabrikk(
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            commandContextDao = commandContextDao,
            snapshotDao = snapshotDao,
            oppgaveDao = oppgaveDao,
            speilSnapshotRestClient = restClient,
            reservasjonDao = mockk(),
            saksbehandlerDao = mockk(),
            overstyringDao = mockk(),
            oppgaveMediator = mockk(),
            risikovurderingDao = mockk(),
            miljøstyrtFeatureToggle = mockk(relaxed = true),
            automatisering = mockk(relaxed = true)
        )
        dao = HendelseDao(dataSource, hendelsefabrikk)
    }

    @BeforeEach
    fun setupEach() {
        vedtaksperiodeForkastetMessage = hendelsefabrikk.nyNyVedtaksperiodeForkastet(
            testmeldingfabrikk.lagVedtaksperiodeForkastet(
                HENDELSE_ID,
                VEDTAKSPERIODE
            )
        )
    }

    @Test
    fun `lagrer og finner hendelser`() {
        dao.opprett(vedtaksperiodeForkastetMessage)
        val actual = dao.finn(HENDELSE_ID) ?: fail { "Forventet å finne en hendelse med id $HENDELSE_ID" }

        assertNull(finnKobling())

        assertEquals(FNR, actual.fødselsnummer())
        assertEquals(VEDTAKSPERIODE, actual.vedtaksperiodeId())
    }

    @Test
    fun `lagrer hendelser inkludert kobling til vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        dao.opprett(vedtaksperiodeForkastetMessage)

        assertEquals(vedtakId, finnKobling())
    }

    @Test
    fun `finner fødselsnummer ved hjelp av hendelseId`() {
        nyPerson()
        godkjenningsbehov(HENDELSE_ID)
        assertEquals(FNR, dao.finnFødselsnummer(HENDELSE_ID))
    }

    private fun finnKobling(hendelseId: UUID = HENDELSE_ID) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT vedtaksperiode_ref FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId)
                .map { it.long(1)
            }.asSingle
        )
    }

}
