package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import no.nav.helse.Meldingssender.sendAktivitetsloggNyAktivitet
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class NyeVarslerE2ETest : AbstractE2ETest() {

    @Test
    fun `lagrer varsler når vi mottar ny aktivitet i aktivitetsloggen`() {
        sendAktivitetsloggNyAktivitet(orgnr = ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID)
        val varsler = nyVarselDao.alleVarslerForVedtaksperiode(VEDTAKSPERIODE_ID)

        assertEquals(1, varsler.size)
    }

    @Test
    fun `lagrer varsler når vi mottar flere ny aktivitet i aktivitetsloggen`() {
        sendAktivitetsloggNyAktivitet(orgnr = ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID)
        sendAktivitetsloggNyAktivitet(orgnr = ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID)
        val varsler = nyVarselDao.alleVarslerForVedtaksperiode(VEDTAKSPERIODE_ID)

        assertEquals(2, varsler.size)
    }

    @Test
    fun `lagrer flere varsler når vi mottar flere nye aktiviteter i samme aktivitetslogg`() {
        sendAktivitetsloggNyAktivitet(ORGNR, VEDTAKSPERIODE_ID, listOf("Kode 1", "Kode 2"))
        val varsler = nyVarselDao.alleVarslerForVedtaksperiode(VEDTAKSPERIODE_ID)

        assertEquals(2, varsler.size)
    }

    @Test
    fun `varsler for ulike vedtaksperioder går ikke i beina på hverandre`() {
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        sendAktivitetsloggNyAktivitet(orgnr = ORGNR, vedtaksperiodeId = v1)
        sendAktivitetsloggNyAktivitet(orgnr = ORGNR, vedtaksperiodeId = v2)
        val v1varsler = nyVarselDao.alleVarslerForVedtaksperiode(v1)
        val v2varsler = nyVarselDao.alleVarslerForVedtaksperiode(v2)

        assertEquals(1, v1varsler.size)
        assertEquals(1, v2varsler.size)
    }
}
