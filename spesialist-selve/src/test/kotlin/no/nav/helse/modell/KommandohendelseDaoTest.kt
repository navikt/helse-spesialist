package no.nav.helse.modell

import DatabaseIntegrationTest
import io.mockk.mockk
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.VedtaksperiodeHendelse
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class KommandohendelseDaoTest : DatabaseIntegrationTest() {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
    }

    private val graphQLClient = mockk<SnapshotClient>(relaxed = true)
    private lateinit var hendelsefabrikk: Hendelsefabrikk
    private lateinit var vedtaksperiodeForkastet: VedtaksperiodeForkastet

    @BeforeAll
    fun setup() {
        hendelsefabrikk = Hendelsefabrikk(
            dataSource = dataSource,
            snapshotClient = graphQLClient,
            oppgaveMediator = { mockk() },
            godkjenningMediator = mockk(relaxed = true),
            automatisering = mockk(relaxed = true),
            overstyringMediator = mockk(),
            versjonAvKode = "versjonAvKode",
        )
    }

    @BeforeEach
    fun setupEach() {
        vedtaksperiodeForkastet = hendelsefabrikk.vedtaksperiodeForkastet(
            Testmeldingfabrikk.lagVedtaksperiodeForkastet(AKTØR, FNR, VEDTAKSPERIODE, id = HENDELSE_ID)
        )
    }

    @Test
    fun `finn siste igangsatte overstyring om den er korrigert søknad`() {
        val overstyringIgangsatt = hendelsefabrikk.overstyringIgangsatt(
            Testmeldingfabrikk.lagOverstyringIgangsatt(
                FNR, berørtePerioder = listOf(
                    mapOf(
                        "vedtaksperiodeId" to "$VEDTAKSPERIODE",
                        "periodeFom" to "2022-01-01",
                        "orgnummer" to "orgnr",
                    )
                )
            )
        )
        val overstyringIgangsattForAnnenVedtaksperiode = hendelsefabrikk.overstyringIgangsatt(
            Testmeldingfabrikk.lagOverstyringIgangsatt(
                FNR, berørtePerioder = listOf(
                    mapOf(
                        "vedtaksperiodeId" to "$VEDTAKSPERIODE",
                        "periodeFom" to "2022-01-01",
                        "orgnummer" to "orgnr",
                    )
                ), årsak = "SYKDOMSTIDSLINJE"
            )
        )
        hendelseDao.opprett(overstyringIgangsatt)
        hendelseDao.opprett(overstyringIgangsattForAnnenVedtaksperiode)
        assertNull(hendelseDao.sisteOverstyringIgangsattOmKorrigertSøknad(FNR, VEDTAKSPERIODE))

        hendelseDao.opprett(
            hendelsefabrikk.overstyringIgangsatt(
                Testmeldingfabrikk.lagOverstyringIgangsatt(
                    FNR,
                    berørtePerioder = listOf(
                        mapOf(
                            "vedtaksperiodeId" to "$VEDTAKSPERIODE",
                            "periodeFom" to "2022-01-01",
                            "orgnummer" to "orgnr",
                        )
                    ),
                )
            )
        )
        assertNotNull(hendelseDao.sisteOverstyringIgangsattOmKorrigertSøknad(FNR, VEDTAKSPERIODE))
    }

    @Test
    fun `finn antall korrigerte søknader`() {
        val overstyringIgangsatt = hendelsefabrikk.overstyringIgangsatt(
            Testmeldingfabrikk.lagOverstyringIgangsatt(FNR)
        )
        val overstyringIgangsattForAnnenVedtaksperiode = hendelsefabrikk.overstyringIgangsatt(
            Testmeldingfabrikk.lagOverstyringIgangsatt(FNR)
        )
        hendelseDao.opprett(overstyringIgangsatt)
        hendelseDao.opprett(overstyringIgangsattForAnnenVedtaksperiode)
        hendelseDao.opprettAutomatiseringKorrigertSøknad(VEDTAKSPERIODE, UUID.randomUUID())
        val actual = hendelseDao.finnAntallAutomatisertKorrigertSøknad(VEDTAKSPERIODE)
        assertEquals(1, actual)
    }

    @Test
    fun `finn ut om automatisering av korrigert søknad allerede er håndtert`() {
        val overstyringIgangsatt = hendelsefabrikk.overstyringIgangsatt(
            Testmeldingfabrikk.lagOverstyringIgangsatt(FNR, id = HENDELSE_ID)
        )
        val overstyringIgangsattForAnnenVedtaksperiode = hendelsefabrikk.overstyringIgangsatt(
            Testmeldingfabrikk.lagOverstyringIgangsatt(FNR)
        )
        hendelseDao.opprett(overstyringIgangsatt)
        hendelseDao.opprett(overstyringIgangsattForAnnenVedtaksperiode)
        hendelseDao.opprettAutomatiseringKorrigertSøknad(VEDTAKSPERIODE, HENDELSE_ID)
        val håndtert = hendelseDao.erAutomatisertKorrigertSøknadHåndtert(HENDELSE_ID)
        assertTrue(håndtert)
    }

    @Test
    fun `lagrer og finner hendelser`() {
        hendelseDao.opprett(vedtaksperiodeForkastet)
        val actual = hendelseDao.finn(HENDELSE_ID, hendelsefabrikk)
            ?: fail { "Forventet å finne en hendelse med id $HENDELSE_ID" }

        assertEquals(VEDTAKSPERIODE, finnKobling())

        assertEquals(FNR, actual.fødselsnummer())
        check(actual is VedtaksperiodeHendelse)
        assertEquals(VEDTAKSPERIODE, actual.vedtaksperiodeId())
    }

    @Test
    fun `finner utbetalingsgodkjenningsbehov`() {
        val json = """{"foo": "bar"}"""
        godkjenningsbehov(HENDELSE_ID, FNR, json)
        val actual = hendelseDao.finnUtbetalingsgodkjenningbehovJson(HENDELSE_ID)
        val expected = objectMapper.readTree(json)
        assertEquals(expected.path("foo"), objectMapper.readTree(actual).path("foo"))
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
            queryOf(
                "SELECT vedtaksperiode_id FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId
            ).map { UUID.fromString(it.string(1)) }.asSingle
        )
    }

}
