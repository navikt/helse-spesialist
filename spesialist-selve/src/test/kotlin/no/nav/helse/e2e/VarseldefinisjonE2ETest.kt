package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import no.nav.helse.Meldingssender.sendVarseldefinisjonerEndret
import no.nav.helse.Testdata.VARSEL_KODE_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class VarseldefinisjonE2ETest : AbstractE2ETest() {

    @Test
    fun `lagrer varseldefinisjoner når vi mottar varseldefinisjoner_endret`() {
        sendVarseldefinisjonerEndret()
        val definisjoner = nyVarselDao.alleDefinisjoner()

        assertEquals(2, definisjoner.size)
    }

    @Test
    fun `dobbeltlagrer ikke varseldefinisjon når unik_id finnes`() {
        val unikId = UUID.randomUUID()
        val tittel = "En tittel"

        sendVarseldefinisjonerEndret(
            listOf(
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(id = unikId, tittel = tittel)
            )
        )
        sendVarseldefinisjonerEndret(
            listOf(
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(id = unikId, tittel = "Ny tittel")
            )
        )

        val definisjoner = nyVarselDao.alleDefinisjoner()

        assertEquals(tittel, definisjoner.single().tittel)
    }

    @Test
    fun `Kan lagre flere varseldefinisjoner for samme varselkode`() {
        sendVarseldefinisjonerEndret(
            listOf(
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(kode = VARSEL_KODE_1)
            )
        )
        sendVarseldefinisjonerEndret(
            listOf(
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(kode = VARSEL_KODE_1, tittel = "Ny tittel")
            )
        )
        val definisjoner = nyVarselDao.alleDefinisjoner()

        assertEquals(2, definisjoner.size)
    }

    @Test
    fun `Lagrer og henter definisjon for gitt unik_id`() {
        val unikId = UUID.randomUUID()
        sendVarseldefinisjonerEndret(
            listOf(
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(id = unikId)
            )
        )

        val definisjonForId = nyVarselDao.definisjonForId(unikId)

        assertNotNull(definisjonForId)
    }
}
