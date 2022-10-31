package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.Meldingssender.sendAktivitetsloggNyAktivitet
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class NyeVarslerE2ETest : AbstractE2ETest() {

    @Test
    fun `lagrer varsler når vi mottar ny aktivitet i aktivitetsloggen`() {
        TODO("Fikse testen")
//        settOppBruker()
//        sendAktivitetsloggNyAktivitet(ORGNR, VEDTAKSPERIODE_ID)
//        val varsler = nyVarselDao.alleVarslerForVedtaksperiode(VEDTAKSPERIODE_ID)
//
//        assertEquals(1, varsler.size)
    }
}
