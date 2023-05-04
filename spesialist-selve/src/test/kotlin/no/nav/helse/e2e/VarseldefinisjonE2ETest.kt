package no.nav.helse.e2e

import AbstractE2ETestV2
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VarseldefinisjonE2ETest : AbstractE2ETestV2() {

    @BeforeEach
    fun beforeEach() {
        nullstillVarseldefinisjoner()
    }

    @Test
    fun `lagrer varseldefinisjoner når vi mottar varseldefinisjoner_endret`() {
        håndterVarseldefinisjonerEndret(Triple(UUID.randomUUID(), "SB_EX_1", "ny tittel"))
        assertDefinisjonerFor("SB_EX_1", "ny tittel")
    }

    @Test
    fun `dobbeltlagrer ikke varseldefinisjon når unik_id finnes`() {
        val unikId = UUID.randomUUID()
        håndterVarseldefinisjonerEndret(Triple(unikId, "SB_EX_1", "ny tittel"))
        håndterVarseldefinisjonerEndret(Triple(unikId, "SB_EX_1", "ny tittel"))
        assertDefinisjonerFor("SB_EX_1", "ny tittel")
    }

    @Test
    fun `Kan lagre flere varseldefinisjoner for samme varselkode`() {
        håndterVarseldefinisjonerEndret(Triple(UUID.randomUUID(), "SB_EX_1", "tittel"))
        håndterVarseldefinisjonerEndret(Triple(UUID.randomUUID(), "SB_EX_1", "ny tittel"))
        assertDefinisjonerFor("SB_EX_1", "tittel", "ny tittel")
    }
}
