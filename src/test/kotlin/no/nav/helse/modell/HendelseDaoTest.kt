package no.nav.helse.modell

import DatabaseIntegrationTest
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.VedtaksperiodeForkastet
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
    private lateinit var vedtaksperiodeForkastet: VedtaksperiodeForkastet

    @BeforeAll
    fun setup() {
        hendelsefabrikk = Hendelsefabrikk(
            hendelseDao = hendelseDao,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            warningDao = warningDao,
            commandContextDao = commandContextDao,
            snapshotDao = snapshotDao,
            oppgaveDao = oppgaveDao,
            reservasjonDao = mockk(),
            saksbehandlerDao = mockk(),
            overstyringDao = mockk(),
            risikovurderingDao = mockk(),
            digitalKontaktinformasjonDao = mockk(),
            åpneGosysOppgaverDao = mockk(),
            oppgaveMediator = mockk(),
            speilSnapshotRestClient = restClient,
            miljøstyrtFeatureToggle = mockk(relaxed = true),
            automatisering = mockk(relaxed = true)
        )
    }

    @BeforeEach
    fun setupEach() {
        vedtaksperiodeForkastet = hendelsefabrikk.vedtaksperiodeForkastet(
            testmeldingfabrikk.lagVedtaksperiodeForkastet(
                HENDELSE_ID,
                VEDTAKSPERIODE
            )
        )
    }

    @Test
    fun `lagrer og finner hendelser`() {
        hendelseDao.opprett(vedtaksperiodeForkastet)
        val actual = hendelseDao.finn(HENDELSE_ID, hendelsefabrikk) ?: fail { "Forventet å finne en hendelse med id $HENDELSE_ID" }

        assertNull(finnKobling())

        assertEquals(FNR, actual.fødselsnummer())
        assertEquals(VEDTAKSPERIODE, actual.vedtaksperiodeId())
    }

    @Test
    fun `finner utbetalingsgodkjenningsbehov`() {
        val json = """{"foo": "bar"}"""
        godkjenningsbehov(HENDELSE_ID, FNR, json)
        val actual = hendelseDao.finnUtbetalingsgodkjenningbehov(HENDELSE_ID)
        val expected = objectMapper.readTree(json)
        assertEquals(expected.path("foo"), objectMapper.readTree(actual.toJson()).path("foo"))
    }

    @Test
    fun `lagrer hendelser inkludert kobling til vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        hendelseDao.opprett(vedtaksperiodeForkastet)

        assertEquals(vedtakId, finnKobling())
    }

    @Test
    fun `finner fødselsnummer ved hjelp av hendelseId`() {
        nyPerson()
        godkjenningsbehov(HENDELSE_ID)
        assertEquals(FNR, hendelseDao.finnFødselsnummer(HENDELSE_ID))
    }

    private fun finnKobling(hendelseId: UUID = HENDELSE_ID) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT vedtaksperiode_ref FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId)
                .map { it.long(1)
            }.asSingle
        )
    }

}
