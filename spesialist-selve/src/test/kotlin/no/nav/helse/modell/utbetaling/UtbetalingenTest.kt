package no.nav.helse.modell.utbetaling

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.person.PersonDao.Utbetalingen
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.bareArbeidsgiverutbetaling
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.delvisRefusjon
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.somUtbetaling
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.utbetalingTilSykmeldt
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingenTest {

    @Test
    fun `ingen utbetaling`() {
        assertIngenUtbetaling(null, 1.januar, 31.januar)
        assertIngenUtbetaling(utbetalingen(emptyList(), emptyList()), 1.januar, 31.januar)
        assertIngenUtbetaling(objectMapper.readTree("{}").somUtbetaling(), 1.januar, 31.januar)
        assertIngenUtbetaling(utbetalingen(listOf(
            Triple(1.januar, 500, null),
            Triple(2.januar, 500, null),
            Triple(3.januar, 500, 500),
        ), listOf(1.januar..3.januar), listOf(3.januar..3.januar)), 1.februar, 28.februar)
    }

    @Test
    fun `delvis refusjon 1`() {
        assertDelvisRefusjon(utbetalingen(listOf(
            Triple(1.januar, 500, null),
            Triple(2.januar, 500, null),
            Triple(3.januar, 500, 500),
        ), listOf(1.januar..3.januar), listOf(3.januar..3.januar)), 1.januar, 3.januar)
    }

    @Test
    fun `delvis refusjon 2`() {
        assertIngenRefusjon(utbetalingen(listOf(
            Triple(1.januar, 500, null),
            Triple(2.januar, 500, null),
            Triple(3.januar, 500, 500),
        ), listOf(1.januar..2.januar), emptyList()), 1.januar, 2.januar)
    }

    @Test
    fun `delvis refusjon 3`() {
        assertDelvisRefusjon(utbetalingen(listOf(
            Triple(1.januar, 500, 0),
            Triple(2.januar, 500, 0),
            Triple(3.januar, 500, 500),
        ), listOf(1.januar..3.januar), listOf(3.januar..3.januar)), 1.januar, 3.januar)
    }

    @Test
    fun `full refusjon 1`() {
        assertFullRefusjon(utbetalingen(listOf(
            Triple(1.januar, 0, 500),
            Triple(2.januar, 0, 500),
            Triple(3.januar, 0, 500),
        ), emptyList(), listOf(1.januar..3.januar)), 1.januar, 3.januar)
    }

    @Test
    fun `full refusjon 2`() {
        assertFullRefusjon(utbetalingen(listOf(
            Triple(1.januar, null, 500),
            Triple(2.januar, null, 500),
            Triple(3.januar, null, 500),
        ), emptyList(), listOf(1.januar..3.januar)), 1.januar, 3.januar)
    }

    @Test
    fun `ingen refusjon 1`() {
        assertIngenRefusjon(utbetalingen(listOf(
            Triple(1.januar, 500, null),
            Triple(2.januar, 500, null),
            Triple(3.januar, 500, null),
        ), listOf(1.januar..3.januar), emptyList()), 1.januar, 3.januar)
    }

    @Test
    fun `ingen refusjon 2`() {
        assertIngenRefusjon(utbetalingen(listOf(
            Triple(1.januar, 500, 0),
            Triple(2.januar, 500, 0),
            Triple(3.januar, 500, 0),
        ), listOf(1.januar..3.januar), emptyList()), 1.januar, 3.januar)
    }

    @Test
    fun `ingen refusjon 3`() {
        assertIngenRefusjon(utbetalingen(listOf(
            Triple(1.januar, 0, 500),
            Triple(2.januar, 0, 0),
            Triple(3.januar, 500, 0),
        ), listOf(3.januar..3.januar), emptyList()), 3.januar, 3.januar)
    }

    private fun utbetalingen(
        utbetalingstidslinje: List<Triple<LocalDate, Int?, Int?>>,
        personOppdragLinjer: List<ClosedRange<LocalDate>> = emptyList(),
        arbeidsgiverOppdragLinjer: List<ClosedRange<LocalDate>> = emptyList()
    ) : Utbetalingen {
        @Language("JSON")
        val json = """
        {
            "utbetalingId": "${UUID.randomUUID()}",
            "utbetalingstidslinje": ${utbetalingstidslinje.map { (dato, personbeløp, arbeidsgiverbeløp) ->
                """
               {
                    "dato": "$dato",
                    "arbeidsgiverbeløp": $arbeidsgiverbeløp,
                    "personbeløp": $personbeløp
                }
                """
        } },
            "personOppdrag": {
                "utbetalingslinjer": ${personOppdragLinjer.map { 
                    """
                    {
                        "fom": "${it.start}",
                        "tom": "${it.endInclusive}"
                     }
                    """
        }}
            },
            "arbeidsgiverOppdrag": {
                "utbetalingslinjer": ${arbeidsgiverOppdragLinjer.map {
                """
                    {
                        "fom": "${it.start}",
                        "tom": "${it.endInclusive}"
                     }
                    """
                }}
            }
        }
        """
        return objectMapper.readTree(json).somUtbetaling()
    }

    private fun assertIngenUtbetaling(utbetalingen: Utbetalingen?, periodeFom: LocalDate, periodeTom: LocalDate) {
        assertFalse(utbetalingen.delvisRefusjon(periodeFom, periodeTom))
        assertFalse(utbetalingen.utbetalingTilSykmeldt(periodeFom, periodeTom))
        assertFalse(utbetalingen.bareArbeidsgiverutbetaling(periodeFom, periodeTom))
        assertFalse(utbetalingen.utbetalingTilSykmeldt(periodeFom, periodeTom))
    }

    private fun assertDelvisRefusjon(utbetalingen: Utbetalingen, periodeFom: LocalDate, periodeTom: LocalDate) {
        assertTrue(utbetalingen.delvisRefusjon(periodeFom, periodeTom))
        assertTrue(utbetalingen.utbetalingTilSykmeldt(periodeFom, periodeTom))
        assertFalse(utbetalingen.bareArbeidsgiverutbetaling(periodeFom, periodeTom))
        assertFalse(utbetalingen.barePersonutbetaling(periodeFom, periodeTom))
    }

    private fun assertFullRefusjon(utbetalingen: Utbetalingen, periodeFom: LocalDate, periodeTom: LocalDate) {
        assertFalse(utbetalingen.delvisRefusjon(periodeFom, periodeTom))
        assertFalse(utbetalingen.utbetalingTilSykmeldt(periodeFom, periodeTom))
        assertTrue(utbetalingen.bareArbeidsgiverutbetaling(periodeFom, periodeTom))
        assertFalse(utbetalingen.barePersonutbetaling(periodeFom, periodeTom))
    }

    private fun assertIngenRefusjon(utbetalingen: Utbetalingen, periodeFom: LocalDate, periodeTom: LocalDate) {
        assertFalse(utbetalingen.delvisRefusjon(periodeFom, periodeTom))
        assertTrue(utbetalingen.utbetalingTilSykmeldt(periodeFom, periodeTom))
        assertFalse(utbetalingen.bareArbeidsgiverutbetaling(periodeFom, periodeTom))
        assertTrue(utbetalingen.barePersonutbetaling(periodeFom, periodeTom))
    }

}
