package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType.FORELDREPENGER
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType.OMSORGSPENGER
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserValidator.Resultat
import no.nav.helse.spesialist.domain.testfixtures.jan
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GraderteAndreYtelserValidatorTest {
    @Test
    fun `gyldige dager`() {
        assertEquals(Resultat.OK, valider(dager = listOf(dag(1 jan 2024), dag(2 jan 2024))))
    }

    @Test
    fun `mangler endringer`() {
        assertEquals(Resultat.ManglerEndringer, valider(dager = emptyList()))
    }

    @Test
    fun `negativ sykdomsgrad`() {
        assertEquals(Resultat.NegativGrad, valider(dager = listOf(dag(1 jan 2024, sykdomsgrad = -1))))
    }

    @Test
    fun `negativ arbeidsgrad`() {
        assertEquals(Resultat.NegativGrad, valider(dager = listOf(dag(1 jan 2024, arbeidsgrad = -1))))
    }

    @Test
    fun `negativ grad for annen ytelse`() {
        assertEquals(
            Resultat.NegativGrad,
            valider(dager = listOf(dag(1 jan 2024, andreYtelserGrader = mapOf(FORELDREPENGER to 50, OMSORGSPENGER to -10)))),
        )
    }

    @Test
    fun `negativ grad avvises selv om totalen er under 100`() {
        assertEquals(
            Resultat.NegativGrad,
            valider(dager = listOf(dag(1 jan 2024, andreYtelserGrader = mapOf(FORELDREPENGER to 80), sykdomsgrad = 50, arbeidsgrad = -30))),
        )
    }

    @Test
    fun `total grad over 100`() {
        assertEquals(
            Resultat.TotalGradOver100,
            valider(dager = listOf(dag(1 jan 2024, andreYtelserGrader = mapOf(FORELDREPENGER to 50, OMSORGSPENGER to 30), sykdomsgrad = 21))),
        )
    }

    @Test
    fun `total grad på nøyaktig 100 er gyldig`() {
        assertEquals(Resultat.OK, valider(dager = listOf(dag(1 jan 2024, sykdomsgrad = 30, arbeidsgrad = 20))))
    }

    private fun valider(dager: List<GraderteAndreYtelserDag> = listOf(dag(1 jan 2024))) = GraderteAndreYtelserValidator.validerDager(dager)

    private fun dag(
        dato: LocalDate,
        andreYtelserGrader: Map<GraderteAndreYtelserType, Grad> = mapOf(FORELDREPENGER to 50),
        sykdomsgrad: Grad = 50,
        arbeidsgrad: Grad = 0,
    ) = GraderteAndreYtelserDag(
        dato = dato,
        andreYtelserGrader = andreYtelserGrader,
        sykdomsgrad = sykdomsgrad,
        arbeidsgrad = arbeidsgrad,
    )
}
