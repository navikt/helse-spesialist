package no.nav.helse.spesialist.api.oppgave

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OppgavetypeTest {

    // Endrer du verdien til Oppgavetype.RISK_QA må du ta stilling til at andre team
    // på Nav bruker denne verdien til å motta meldinger på Kafka og kan i verste fall
    // miste viktige meldinger hvis denne verdien endres.
    @Test
    fun `ta stilling til at det er andre team som bruker verdien av RISK_QA`() {
        assertEquals("RISK_QA", Oppgavetype.RISK_QA.name)
    }

}