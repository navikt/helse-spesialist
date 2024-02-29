package no.nav.helse.modell

import DatabaseIntegrationTest
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class HendelseDaoTest : DatabaseIntegrationTest() {
    private val godkjenningsbehov: Godkjenningsbehov = mockGodkjenningsbehov()

    @Test
    fun `finn siste igangsatte overstyring om den er korrigert søknad`() {
        val fødselsnummer = FNR
        val overstyringIgangsatt = mockOverstyringIgangsatt(fødselsnummer, listOf(VEDTAKSPERIODE), "KORRIGERT_SØKNAD")

        val overstyringIgangsattForAnnenVedtaksperiode = mockOverstyringIgangsatt(fødselsnummer, listOf(VEDTAKSPERIODE), "SYKDOMSTIDSLINJE")

        hendelseDao.opprett(overstyringIgangsatt)
        hendelseDao.opprett(overstyringIgangsattForAnnenVedtaksperiode)
        assertNull(hendelseDao.sisteOverstyringIgangsattOmKorrigertSøknad(fødselsnummer, VEDTAKSPERIODE))

        hendelseDao.opprett(mockOverstyringIgangsatt(fødselsnummer, listOf(VEDTAKSPERIODE), "KORRIGERT_SØKNAD"))
        assertNotNull(hendelseDao.sisteOverstyringIgangsattOmKorrigertSøknad(fødselsnummer, VEDTAKSPERIODE))
    }

    @Test
    fun `finn antall korrigerte søknader`() {
        hendelseDao.opprettAutomatiseringKorrigertSøknad(VEDTAKSPERIODE, UUID.randomUUID())
        val actual = hendelseDao.finnAntallAutomatisertKorrigertSøknad(VEDTAKSPERIODE)
        assertEquals(1, actual)
    }

    @Test
    fun `finn ut om automatisering av korrigert søknad allerede er håndtert`() {
        hendelseDao.opprettAutomatiseringKorrigertSøknad(VEDTAKSPERIODE, HENDELSE_ID)
        val håndtert = hendelseDao.erAutomatisertKorrigertSøknadHåndtert(HENDELSE_ID)
        assertTrue(håndtert)
    }

    @Test
    fun `lagrer og finner hendelser`() {
        hendelseDao.opprett(godkjenningsbehov)
        val actual = hendelseDao.finn(HENDELSE_ID)
            ?: fail { "Forventet å finne en hendelse med id $HENDELSE_ID" }
        assertEquals(FNR, actual.fødselsnummer())
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

        hendelseDao.opprett(godkjenningsbehov)
        assertEquals(VEDTAKSPERIODE, finnKobling())
    }

    @Test
    fun `finner fødselsnummer ved hjelp av hendelseId`() {
        nyPerson()
        godkjenningsbehov(HENDELSE_ID)
        assertEquals(FNR, hendelseDao.finnFødselsnummer(HENDELSE_ID))
    }

    private fun mockOverstyringIgangsatt(fødselsnummer: String, berørtePeriodeIder: List<UUID>, årsak: String): OverstyringIgangsatt {
        return mockk<OverstyringIgangsatt>(relaxed = true) {
            every { id } returns UUID.randomUUID()
            every { fødselsnummer() } returns fødselsnummer
            every { berørteVedtaksperiodeIder } returns berørtePeriodeIder
            every { toJson() } returns Testmeldingfabrikk.lagOverstyringIgangsatt(
                fødselsnummer = fødselsnummer,
                berørtePerioder = berørtePeriodeIder.map {
                    mapOf(
                        "vedtaksperiodeId" to "$it",
                        "periodeFom" to "2022-01-01",
                        "orgnummer" to "orgnr",
                    )
                },
                årsak = årsak,
            )
        }
    }

    private fun mockGodkjenningsbehov(): Godkjenningsbehov {
        return mockk<Godkjenningsbehov>(relaxed = true) {
            every { id } returns HENDELSE_ID
            every { fødselsnummer() } returns FNR
            every { vedtaksperiodeId() } returns VEDTAKSPERIODE
            every { toJson() } returns Testmeldingfabrikk.lagGodkjenningsbehov(AKTØR, FNR, VEDTAKSPERIODE)
        }
    }

    private fun finnKobling(hendelseId: UUID = HENDELSE_ID) = sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                "SELECT vedtaksperiode_id FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId
            ).map { UUID.fromString(it.string(1)) }.asSingle
        )
    }

}
