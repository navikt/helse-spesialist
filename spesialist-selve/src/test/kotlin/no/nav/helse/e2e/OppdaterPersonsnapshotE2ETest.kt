package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDate
import java.util.UUID.randomUUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.snapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OppdaterPersonsnapshotE2ETest : AbstractE2ETest() {

    @Test
    fun `Oppdater personsnapshot oppdaterer alle snapshots på personen`() {
        val v1 = randomUUID()
        val v2 = randomUUID()
        fremTilSaksbehandleroppgave(vedtaksperiodeId = v1, snapshotversjon = 1)
        håndterSaksbehandlerløsning(vedtaksperiodeId = v1)
        håndterVedtakFattet(vedtaksperiodeId = v1)

        fremTilSaksbehandleroppgave(vedtaksperiodeId = v2, snapshotversjon = 1, harOppdatertMetadata = true)
        håndterSaksbehandlerløsning(vedtaksperiodeId = v2)
        håndterVedtakFattet(vedtaksperiodeId = v2)

        val snapshotFinal = snapshot(3)
        håndterOppdaterPersonsnapshot(snapshotSomSkalHentes = snapshotFinal)

        assertSnapshot(snapshotFinal, v1)
        assertSnapshotversjon(v1, 3)
        assertSnapshot(snapshotFinal, v2)
        assertSnapshotversjon(v2, 3)
    }

    @Test
    fun `Oppdaterer også Infotrygd-utbetalinger`() {
        val v1 = randomUUID()
        val snapshot = snapshot(2)
        fremTilSaksbehandleroppgave(vedtaksperiodeId = v1, snapshotversjon = 1)
        håndterSaksbehandlerløsning(vedtaksperiodeId = v1)
        håndterVedtakFattet(vedtaksperiodeId = v1)

        håndterOppdaterPersonsnapshot(snapshotSomSkalHentes = snapshot)

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
                    "update person set infotrygdutbetalinger_oppdatert = now() - interval '$antallDager days' where fodselsnummer=:fnr",
                    mapOf("fnr" to fødselsnummer.toLong()),
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
                    "select infotrygdutbetalinger_oppdatert from person where fodselsnummer=:fnr",
                    mapOf("fnr" to fødselsnummer.toLong()),
                ).map { row -> row.localDate(1) }.asSingle
            )
        }
        assertEquals(forventetDato, dato)
    }
}
