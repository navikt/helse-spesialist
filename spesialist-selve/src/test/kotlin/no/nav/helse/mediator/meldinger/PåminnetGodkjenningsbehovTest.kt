package no.nav.helse.mediator.meldinger

import AbstractE2ETestV2
import io.mockk.every
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class PåminnetGodkjenningsbehovTest : AbstractE2ETestV2() {

    @Test
    fun `sørger for oppdatert snapshot om det fins oppgave`() {
        fremTilSaksbehandleroppgave()
        assertEquals(1, finnSnapshotVersjon(Testdata.FØDSELSNUMMER))
        sendPåminnetGodkjenningsbehov()
        assertEquals(1, finnSnapshotVersjon(Testdata.FØDSELSNUMMER))

        bumpGlobalVersjon()
        every { snapshotClient.hentSnapshot(Testdata.FØDSELSNUMMER) } returns Testdata.snapshot(
            versjon = 2
        )

        sendPåminnetGodkjenningsbehov()
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

    private fun sendPåminnetGodkjenningsbehov() = sendGodkjenningsbehov()

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
