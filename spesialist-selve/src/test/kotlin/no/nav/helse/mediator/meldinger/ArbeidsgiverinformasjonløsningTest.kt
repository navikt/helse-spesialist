package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.meldinger.løsninger.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ArbeidsgiverinformasjonløsningTest {
    private companion object {
        private const val ORGNR = "123456789"
        private const val NAVN = "Bedrift AS"
        private val BRANSJER = listOf("Spaghettikoding")
        private const val FØDSELSNUMMER = "12345678910"
        private const val AKTØRID = "123456789"
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val rapid = TestRapid()
    private val meldingsfabrikk = Testmeldingfabrikk(FØDSELSNUMMER, AKTØRID)

    init {
        Arbeidsgiverinformasjonløsning.ArbeidsgiverRiver(rapid, mediator)
    }

    @BeforeEach
    internal fun resetTestSetup() {
        rapid.reset()
    }

    @Test
    fun `mottar arbeidsgiverløsning`() {
        rapid.sendTestMessage(
            meldingsfabrikk.lagArbeidsgiverinformasjonløsningOld(
                aktørId = AKTØRID,
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                navn = NAVN,
                bransjer = BRANSJER
            )
        )
        verify(exactly = 1) { mediator.løsning(any(), any(), any(), any<Arbeidsgiverinformasjonløsning>(), any()) }
    }

    @Test
    fun `oppretter løsning`() {
        val arbeidsgiver = Arbeidsgiverinformasjonløsning(
            listOf(
                Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(ORGNR, NAVN, BRANSJER)
            )
        )
        arbeidsgiver.opprett(dao)
        verify(exactly = 1) { dao.insertArbeidsgiver(ORGNR, NAVN, BRANSJER) }
    }

    @Test
    fun `oppdatere navn`() {
        val arbeidsgiver = Arbeidsgiverinformasjonløsning(
            listOf(
                Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(ORGNR, NAVN, BRANSJER)
            )
        )
        arbeidsgiver.oppdater(dao)
        verify(exactly = 1) { dao.upsertNavn(ORGNR, NAVN) }
    }

    @Test
    fun `oppdatere bransje`() {
        val arbeidsgiver = Arbeidsgiverinformasjonløsning(
            listOf(
                Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(ORGNR, NAVN, BRANSJER)
            )
        )
        arbeidsgiver.oppdater(dao)
        verify(exactly = 1) { dao.upsertBransjer(ORGNR, BRANSJER) }
    }
}
