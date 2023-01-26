package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Meldingssender.sendVarseldefinisjonerEndret
import no.nav.helse.Testdata.VARSEL_KODE_1
import no.nav.helse.mediator.meldinger.Varseldefinisjon
import no.nav.helse.modell.varsel.DefinisjonDao
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarseldefinisjonE2ETest : AbstractE2ETest() {

    private val definisjonDao = DefinisjonDao(dataSource)

    // Midlertidig - Oppdater opprettet-tidspunkt for eksisterende definisjoner
    @Test
    fun `oppdaterer opprettet-tidspunkt`() {
        val unikId = UUID.randomUUID()
        val opprettet = LocalDateTime.parse("2022-12-12T12:00:00")

        sendVarseldefinisjonerEndret(
            listOf(
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(id = unikId, opprettet = opprettet)
            )
        )
        val definisjon = definisjonDao.definisjonFor(unikId)
        assertEquals(true, definisjon.opprettetIs(opprettet))

        val nyOpprettet = LocalDateTime.parse("2023-01-01T12:00:00")
        sendVarseldefinisjonerEndret(
            listOf(
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(id = unikId, opprettet = nyOpprettet)
            )
        )
        val oppdatertDefinisjon = definisjonDao.definisjonFor(unikId)
        assertEquals(true, oppdatertDefinisjon.opprettetIs(nyOpprettet))
    }

    @Test
    fun `lagrer varseldefinisjoner når vi mottar varseldefinisjoner_endret`() {
        sendVarseldefinisjonerEndret()
        val definisjoner = alleDefinisjoner()

        assertEquals(2, definisjoner.size)
    }

    @Test
    fun `dobbeltlagrer ikke varseldefinisjon når unik_id finnes`() {
        val unikId = UUID.randomUUID()

        sendVarseldefinisjonerEndret(
            listOf(
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(id = unikId)
            )
        )
        sendVarseldefinisjonerEndret(
            listOf(
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(id = unikId, tittel = "NY_TITTEL")
            )
        )

        val definisjoner = alleDefinisjoner()

        assertEquals(
            Varseldefinisjon(
                unikId,
                "EN_KODE",
                "EN_TITTEL",
                "EN_FORKLARING",
                "EN_HANDLING",
                false,
                LocalDateTime.now()
            ), definisjoner.single()
        )
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
        val definisjoner = alleDefinisjoner()

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

        val definisjon = definisjonDao.definisjonFor(unikId)

        assertEquals(
            Varseldefinisjon(
                unikId,
                varselkode = "EN_KODE",
                tittel = "EN_TITTEL",
                forklaring = "EN_FORKLARING",
                handling = "EN_HANDLING",
                avviklet = false,
                LocalDateTime.now()
            ),
            definisjon
        )
    }

    private fun alleDefinisjoner(): List<Varseldefinisjon> {
        @Language("PostgreSQL")
        val query = "SELECT * FROM api_varseldefinisjon;"

        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    query
                ).map {
                    Varseldefinisjon(
                        id = it.uuid("unik_id"),
                        varselkode = it.string("kode"),
                        tittel = it.string("tittel"),
                        forklaring = it.stringOrNull("forklaring"),
                        handling = it.stringOrNull("handling"),
                        avviklet = it.boolean("avviklet"),
                        opprettet = it.localDateTime("opprettet")
                    )
                }.asList
            )
        }
    }
}
