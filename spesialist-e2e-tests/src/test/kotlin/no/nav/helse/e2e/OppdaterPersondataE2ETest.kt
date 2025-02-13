package no.nav.helse.e2e

import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OppdaterPersondataE2ETest : AbstractE2ETest() {

    @Test
    fun `Oppdaterer Infotrygd-utbetalinger`() {
        val v1 = VEDTAKSPERIODE_ID
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(vedtaksperiodeId = v1)
        )
        håndterSaksbehandlerløsning(vedtaksperiodeId = v1)
        håndterAvsluttetMedVedtak(vedtaksperiodeId = v1)

        håndterOppdaterPersondata()

        assertInfotrygdutbetalingerOppdatert(FØDSELSNUMMER)
        settInfotrygdutbetalingerUtdatert(FØDSELSNUMMER)
        assertInfotrygdutbetalingerOppdatert(FØDSELSNUMMER, forventetDato = LocalDate.now().minusDays(7))

        håndterInfotrygdutbetalingerløsning()
        assertInfotrygdutbetalingerOppdatert(FØDSELSNUMMER)
    }

    private fun settInfotrygdutbetalingerUtdatert(fødselsnummer: String, antallDager: Int = 7) =
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "update person set infotrygdutbetalinger_oppdatert = now() - interval '$antallDager days' where fødselsnummer=:fnr",
                    mapOf("fnr" to fødselsnummer),
                ).asUpdate
            )
        }

    private fun assertInfotrygdutbetalingerOppdatert(
        fødselsnummer: String,
        forventetDato: LocalDate = LocalDate.now()
    ) {
        val dato = sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "select infotrygdutbetalinger_oppdatert from person where fødselsnummer=:fnr",
                    mapOf("fnr" to fødselsnummer),
                ).map { row -> row.localDate(1) }.asSingle
            )
        }
        assertEquals(forventetDato, dato)
    }
}
