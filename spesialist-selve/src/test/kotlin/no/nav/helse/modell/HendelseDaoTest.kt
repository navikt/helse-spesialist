package no.nav.helse.modell

import DatabaseIntegrationTest
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.VedtaksperiodeForkastet
import org.junit.jupiter.api.Assertions.assertEquals
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
    private val graphQLClient = mockk<SnapshotClient>(relaxed = true)
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
            oppgaveDao = oppgaveDao,
            commandContextDao = commandContextDao,
            snapshotDao = snapshotDao,
            reservasjonDao = reservasjonDao,
            tildelingDao = tildelingDao,
            saksbehandlerDao = saksbehandlerDao,
            overstyringDao = overstyringDao,
            risikovurderingDao = risikovurderingDao,
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            egenAnsattDao = egenAnsattDao,
            snapshotClient = graphQLClient,
            oppgaveMediator = mockk(),
            godkjenningMediator = mockk(relaxed = true),
            automatisering = mockk(relaxed = true),
            arbeidsforholdDao = arbeidsforholdDao,
            utbetalingDao = utbetalingDao,
            opptegnelseDao = opptegnelseDao,
            vergemålDao = vergemålDao,
            overstyrtVedtaksperiodeDao = overstyrtVedtaksperiodeDao,
            periodehistorikkDao = periodehistorikkDao,
            automatiseringDao = automatiseringDao,
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

        assertEquals(VEDTAKSPERIODE, finnKobling())

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

        assertEquals(VEDTAKSPERIODE, finnKobling())
    }

    @Test
    fun `finner fødselsnummer ved hjelp av hendelseId`() {
        nyPerson()
        godkjenningsbehov(HENDELSE_ID)
        assertEquals(FNR, hendelseDao.finnFødselsnummer(HENDELSE_ID))
    }

    private fun finnKobling(hendelseId: UUID = HENDELSE_ID) = sessionOf(dataSource).use { session ->
        session.run(
            queryOf("SELECT vedtaksperiode_id FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId)
                .map { UUID.fromString(it.string(1)) }.asSingle
        )
    }

}
