package no.nav.helse.mediator.meldinger

import AbstractE2ETest
import io.mockk.every
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.Testdata
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PåminnetGodkjenningsbehovTest : AbstractE2ETest() {

    @Test
    fun `sørger for oppdatert snapshot om det fins oppgave`() {
        val utbetalingId = UUID.randomUUID()
        fremTilSaksbehandleroppgave(utbetalingId = utbetalingId)
        assertEquals(1, finnSnapshotVersjon(Testdata.FØDSELSNUMMER))
        sendPåminnetGodkjenningsbehov(utbetalingId)
        assertEquals(1, finnSnapshotVersjon(Testdata.FØDSELSNUMMER))

        bumpGlobalVersjon()
        every { snapshotClient.hentSnapshot(Testdata.FØDSELSNUMMER) } returns Testdata.snapshot(
            versjon = 2
        )

        sendPåminnetGodkjenningsbehov(utbetalingId)
        assertEquals(2, finnSnapshotVersjon(Testdata.FØDSELSNUMMER))
    }

    private fun bumpGlobalVersjon() {
        @Language("postgresql")
        val query = """
            update global_snapshot_versjon ny
            set versjon = forrige.versjon + 1, sist_endret = now()
            from global_snapshot_versjon forrige
            where ny.versjon = 1
        """
        sessionOf(dataSource).use { session -> session.run(queryOf(query).asUpdate)
        }
    }

    private fun sendPåminnetGodkjenningsbehov(utbetalingId: UUID) = sendGodkjenningsbehov(GodkjenningsbehovTestdata(utbetalingId = utbetalingId))

    private fun finnSnapshotVersjon(fnr: String): Int {
        @Language("postgresql")
        val query = "select versjon from snapshot where person_ref = (select id from person where fodselsnummer = :fnr)"
        return sessionOf(dataSource, strict = true).use {
            it.run(queryOf(
                query, mapOf("fnr" to fnr.toLong())
            ).map { it.int(1) }.asSingle)
        }!!
    }
}
