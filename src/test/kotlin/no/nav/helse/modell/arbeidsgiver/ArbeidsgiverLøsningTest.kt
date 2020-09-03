package no.nav.helse.modell.arbeidsgiver

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class ArbeidsgiverLøsningTest {
    private companion object {
        private const val ORGNR = "123456789"
        private const val NAVN = "Bedrift AS"
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)

    @Test
    fun `oppdatere navn`() {
        val arbeidsgiver = ArbeidsgiverLøsning(NAVN)
        arbeidsgiver.oppdater(dao, ORGNR)
        verify(exactly = 1) { dao.updateNavn(ORGNR.toLong(), NAVN) }
    }
}
