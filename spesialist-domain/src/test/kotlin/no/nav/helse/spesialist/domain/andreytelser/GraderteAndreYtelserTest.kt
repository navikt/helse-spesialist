package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType.FORELDREPENGER
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType.OMSORGSPENGER
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraderteAndreYtelserTest {
    private val id =
        GraderteAndreYtelserId(
            identitetsnummer = lagIdentitetsnummer(),
            yrkesaktivitet = Yrkesaktivitetstype.ARBEIDSTAKER,
            organisasjonsnummer = "123456789",
        )
    private val saksbehandlerIdent = lagSaksbehandler().ident

    @Test
    fun `kan opprette graderte andre ytelser`() {
        val dager = listOf(dag(1 jan 2024), dag(2 jan 2024))

        val graderteAndreYtelser = ny(dager)

        assertEquals(id, graderteAndreYtelser.id)
        assertEquals(dager, graderteAndreYtelser.dager)
        val event = graderteAndreYtelser.events.single()
        assertEquals(dager, event.dager.map { it.tilGraderteAndreYtelserDag() })
        assertEquals("et notat", event.notatTilBeslutter)
        assertEquals(saksbehandlerIdent, event.utførtAvSaksbehandlerIdent)
        assertEquals(event.versjon, graderteAndreYtelser.versjon)
    }

    @Test
    fun `endring erstatter bare dagene som er med i eventet`() {
        val graderteAndreYtelser = ny(listOf(dag(1 jan 2024), dag(2 jan 2024), dag(3 jan 2024)))

        val event = graderteAndreYtelser.endre(listOf(dag(2 jan 2024, sykdomsgrad = 20)), saksbehandlerIdent, "endring")

        assertEquals(listOf(dag(2 jan 2024, sykdomsgrad = 20)), event?.dager?.map { it.tilGraderteAndreYtelserDag() })
        assertEquals(
            listOf(dag(1 jan 2024), dag(2 jan 2024, sykdomsgrad = 20), dag(3 jan 2024)),
            graderteAndreYtelser.dager,
        )
        assertEquals(2, graderteAndreYtelser.events.size)
        assertEquals(event?.versjon, graderteAndreYtelser.versjon)
    }

    @Test
    fun `endring kan legge til nye dager`() {
        val graderteAndreYtelser = ny(listOf(dag(2 jan 2024)))

        graderteAndreYtelser.endre(listOf(dag(1 jan 2024)), saksbehandlerIdent, "endring")

        assertEquals(listOf(dag(1 jan 2024), dag(2 jan 2024)), graderteAndreYtelser.dager)
    }

    @Test
    fun `dager som er like dagens tilstand tas ikke med i eventet`() {
        val graderteAndreYtelser = ny(listOf(dag(1 jan 2024), dag(2 jan 2024)))

        val event =
            graderteAndreYtelser.endre(
                listOf(dag(1 jan 2024), dag(2 jan 2024, andreYtelserGrader = mapOf(OMSORGSPENGER to 50))),
                saksbehandlerIdent,
                "endring",
            )

        assertEquals(
            listOf(dag(2 jan 2024, andreYtelserGrader = mapOf(OMSORGSPENGER to 50))),
            event?.dager?.map { it.tilGraderteAndreYtelserDag() },
        )
    }

    @Test
    fun `eventet tar med verdiene dagen hadde før endringen`() {
        val graderteAndreYtelser = ny(listOf(dag(1 jan 2024)))

        val event =
            graderteAndreYtelser.endre(
                listOf(dag(1 jan 2024, andreYtelserGrader = mapOf(OMSORGSPENGER to 40), sykdomsgrad = 60, arbeidsgrad = 0)),
                saksbehandlerIdent,
                "endring",
            )

        assertEquals(
            listOf(
                GraderteAndreYtelserEndretDag(
                    dato = 1 jan 2024,
                    andreYtelserGrader = mapOf(OMSORGSPENGER to 40),
                    sykdomsgrad = 60,
                    arbeidsgrad = 0,
                    fraAndreYtelserGrader = mapOf(FORELDREPENGER to 50),
                    fraSykdomsgrad = 30,
                    fraArbeidsgrad = 20,
                ),
            ),
            event?.dager,
        )
    }

    @Test
    fun `eventet har ingen fra-verdier for dager som ikke fantes fra før`() {
        val graderteAndreYtelser = ny(listOf(dag(1 jan 2024)))

        val event = graderteAndreYtelser.endre(listOf(dag(2 jan 2024)), saksbehandlerIdent, "endring")

        val endretDag = event?.dager?.single()
        assertEquals(2 jan 2024, endretDag?.dato)
        assertNull(endretDag?.fraAndreYtelserGrader)
        assertNull(endretDag?.fraSykdomsgrad)
        assertNull(endretDag?.fraArbeidsgrad)
    }

    @Test
    fun `fra-verdiene er null når strømmen opprettes`() {
        val event = ny(listOf(dag(1 jan 2024))).events.single()

        assertTrue(event.dager.all { it.fraAndreYtelserGrader == null && it.fraSykdomsgrad == null && it.fraArbeidsgrad == null })
    }

    @Test
    fun `ingen event når alle dagene er like dagens tilstand`() {
        val graderteAndreYtelser = ny(listOf(dag(1 jan 2024)))
        val versjonFør = graderteAndreYtelser.versjon

        val event = graderteAndreYtelser.endre(listOf(dag(1 jan 2024)), saksbehandlerIdent, "endring")

        assertNull(event)
        assertEquals(1, graderteAndreYtelser.events.size)
        assertEquals(versjonFør, graderteAndreYtelser.versjon)
    }

    @Test
    fun `versjoner sorteres etter tidspunkt`() {
        val graderteAndreYtelser = ny(listOf(dag(1 jan 2024)))
        graderteAndreYtelser.endre(listOf(dag(1 jan 2024, sykdomsgrad = 10)), saksbehandlerIdent, "endring 1")
        graderteAndreYtelser.endre(listOf(dag(1 jan 2024, sykdomsgrad = 20)), saksbehandlerIdent, "endring 2")

        val versjoner = graderteAndreYtelser.events.map { it.versjon }
        assertEquals(versjoner.sorted(), versjoner)
        assertTrue(versjoner.all { it.version() == 7 })
    }

    @Test
    fun `senere event overskriver tidligere for samme dato`() {
        val graderteAndreYtelser = ny(listOf(dag(1 jan 2024)))
        graderteAndreYtelser.endre(listOf(dag(1 jan 2024, sykdomsgrad = 10)), saksbehandlerIdent, "endring 1")
        graderteAndreYtelser.endre(listOf(dag(1 jan 2024, sykdomsgrad = 20)), saksbehandlerIdent, "endring 2")

        assertEquals(listOf(dag(1 jan 2024, sykdomsgrad = 20)), graderteAndreYtelser.dager)
    }

    @Test
    fun `fraLagring gir samme tilstand som originalen, uavhengig av rekkefølgen eventene kommer i`() {
        val original = ny(listOf(dag(1 jan 2024), dag(2 jan 2024)))
        original.endre(listOf(dag(2 jan 2024, sykdomsgrad = 10)), saksbehandlerIdent, "endring 1")
        original.endre(listOf(dag(2 jan 2024, sykdomsgrad = 20), dag(3 jan 2024)), saksbehandlerIdent, "endring 2")

        val gjenoppbygd = GraderteAndreYtelser.fraLagring(id, original.events.reversed())

        assertEquals(original.dager, gjenoppbygd.dager)
        assertEquals(original.events, gjenoppbygd.events)
        assertEquals(original.versjon, gjenoppbygd.versjon)
    }

    @Test
    fun `finnEvent finner eventet med gitt versjon`() {
        val graderteAndreYtelser = ny(listOf(dag(1 jan 2024)))
        val event = graderteAndreYtelser.endre(listOf(dag(1 jan 2024, sykdomsgrad = 10)), saksbehandlerIdent, "endring")!!

        assertEquals(event, graderteAndreYtelser.finnEvent(event.versjon))
        assertNull(graderteAndreYtelser.finnEvent(UUID.randomUUID()))
    }

    private fun ny(dager: List<GraderteAndreYtelserDag>) =
        GraderteAndreYtelser.ny(
            id = id,
            dager = dager,
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = "et notat",
        )

    private fun dag(
        dato: LocalDate,
        andreYtelserGrader: Map<GraderteAndreYtelserType, Grad> = mapOf(FORELDREPENGER to 50),
        sykdomsgrad: Grad = 30,
        arbeidsgrad: Grad = 20,
    ) = GraderteAndreYtelserDag(
        dato = dato,
        andreYtelserGrader = andreYtelserGrader,
        sykdomsgrad = sykdomsgrad,
        arbeidsgrad = arbeidsgrad,
    )
}
