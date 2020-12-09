package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.junit.jupiter.api.Test

internal class ArbeidsgiverinformasjonløsningTest {
    private companion object {
        private const val ORGNR = "123456789"
        private const val NAVN = "Bedrift AS"
        private const val BRANSJER = "Spaghettikoding"
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)

    @Test
    fun `oppdatere navn`() {
        val arbeidsgiver = Arbeidsgiverinformasjonløsning(NAVN, BRANSJER)
        arbeidsgiver.oppdater(dao, ORGNR)
        verify(exactly = 1) { dao.updateNavn(ORGNR, NAVN) }
    }

    @Test
    fun `oppdatere bransje`() {
        val arbeidsgiver = Arbeidsgiverinformasjonløsning(NAVN, BRANSJER)
        arbeidsgiver.oppdater(dao, ORGNR)
        verify(exactly = 1) { dao.updateBransjer(ORGNR, BRANSJER) }
    }
}
