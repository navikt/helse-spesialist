package no.nav.helse.modell.utbetaling

import no.nav.helse.modell.person.PersonDao.Utbetalingen
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.bareUtbetalingTilSykmeldt
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.delvisRefusjon
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.somUtbetaling
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.utbetalingTilArbeidsgiver
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.utbetalingTilSykmeldt
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class UtbetalingenTest {

    @Test
    fun `ingen utbetaling`() {
        assertIngenUtbetaling(null)
        assertIngenUtbetaling(utbetalingen(emptyList(), emptyList()))
        assertIngenUtbetaling(objectMapper.readTree("{}").somUtbetaling())
    }

    @Test
    fun `delvis refusjon`() {
        assertDelvisRefusjon(utbetalingen(listOf("ENDR"),listOf("ENDR")))
    }

    @Test
    fun `full refusjon`() {
        assertFullRefusjon(utbetalingen(emptyList(),listOf("ENDR")))
    }

    @Test
    fun `ingen refusjon`() {
        assertIngenRefusjon(utbetalingen(listOf("ENDR"), emptyList()))
    }

    private fun utbetalingen(
        personOppdragLinjer: List<String> = emptyList(),
        arbeidsgiverOppdragLinjer: List<String> = emptyList()
    ) : Utbetalingen {
        @Language("JSON")
        val json = """
        {
            "utbetalingId": "${UUID.randomUUID()}",
            "personOppdrag": {
                "utbetalingslinjer": ${personOppdragLinjer.map { "{}" }}
            },
            "arbeidsgiverOppdrag": {
                "utbetalingslinjer": ${arbeidsgiverOppdragLinjer.map { "{}" }}
            }
        }
        """
        return objectMapper.readTree(json).somUtbetaling()
    }

    private fun assertIngenUtbetaling(utbetalingen: Utbetalingen?) {
        assertFalse(utbetalingen.utbetalingTilArbeidsgiver())
        assertFalse(utbetalingen.utbetalingTilSykmeldt())
        assertFalse(utbetalingen.delvisRefusjon())
        assertFalse(utbetalingen.bareUtbetalingTilSykmeldt())
    }

    private fun assertDelvisRefusjon(utbetalingen: Utbetalingen) {
        assertTrue(utbetalingen.utbetalingTilArbeidsgiver())
        assertTrue(utbetalingen.utbetalingTilSykmeldt())
        assertTrue(utbetalingen.delvisRefusjon())
        assertFalse(utbetalingen.bareUtbetalingTilSykmeldt())
    }

    private fun assertFullRefusjon(utbetalingen: Utbetalingen) {
        assertTrue(utbetalingen.utbetalingTilArbeidsgiver())
        assertFalse(utbetalingen.utbetalingTilSykmeldt())
        assertFalse(utbetalingen.delvisRefusjon())
        assertFalse(utbetalingen.bareUtbetalingTilSykmeldt())
    }

    private fun assertIngenRefusjon(utbetalingen: Utbetalingen) {
        assertFalse(utbetalingen.utbetalingTilArbeidsgiver())
        assertTrue(utbetalingen.utbetalingTilSykmeldt())
        assertFalse(utbetalingen.delvisRefusjon())
        assertTrue(utbetalingen.bareUtbetalingTilSykmeldt())
    }

}
