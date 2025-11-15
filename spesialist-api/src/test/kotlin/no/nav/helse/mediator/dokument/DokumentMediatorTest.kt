package no.nav.helse.mediator.dokument

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.DokumentDao
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class DokumentMediatorTest {
    private val FNR = lagFødselsnummer()
    private val DOKUMENTID = UUID.randomUUID()
    private val DOKUMENTTYPE = DokumentMediator.DokumentType.SØKNAD
    private val RETRIES = 2

    private val dokumentDao = mockk<DokumentDao>(relaxed = true)

    private val meldingPubliserer = object : MeldingPubliserer {
        var antallMeldinger: Int = 0
            private set

        override fun publiser(fødselsnummer: String, hendelse: UtgåendeHendelse, årsak: String) {
            antallMeldinger++
        }

        override fun publiser(fødselsnummer: String, subsumsjonEvent: SubsumsjonEvent, versjonAvKode: String) =
            error("Not implemented in test")

        override fun publiser(hendelseId: UUID, commandContextId: UUID, fødselsnummer: String, behov: List<Behov>) =
            error("Not implemented in test")

        override fun publiser(fødselsnummer: String, event: KommandokjedeEndretEvent, hendelseNavn: String) =
            error("Not implemented in test")
    }

    private val mediator = DokumentMediator(
        publiserer = meldingPubliserer,
        retries = RETRIES,
    )

    @Test
    fun `Prøver å hente dokumentet {retries + 1} ganger`() {
        every { dokumentDao.hent(any(), any()) } returns null
        mediator.hentDokument(dokumentDao, FNR, DOKUMENTID, DOKUMENTTYPE)
        verify(exactly = RETRIES + 1) {
            dokumentDao.hent(any(), any())
        }
    }

    @Test
    fun `Sender behov dersom dokumentet ikke finnes i databasen`() {
        every { dokumentDao.hent(any(), any()) } returns null
        mediator.hentDokument(dokumentDao, FNR, DOKUMENTID, DOKUMENTTYPE)
        assertEquals(1, meldingPubliserer.antallMeldinger)
    }

    @Test
    fun `Sender nytt behov dersom dokumentet i databasen er tomt`() {
        every { dokumentDao.hent(any(), any()) } returns objectMapper.createObjectNode()
        mediator.hentDokument(dokumentDao, FNR, DOKUMENTID, DOKUMENTTYPE)
        assertEquals(1, meldingPubliserer.antallMeldinger)
    }

    @Test
    fun `Sender ikke behov dersom dokumentet finnes i databasen`() {
        every { dokumentDao.hent(any(), any()) } returns objectMapper.readTree("""{"ikkeTom":"harVerdi"}""")

        mediator.hentDokument(dokumentDao, FNR, DOKUMENTID, DOKUMENTTYPE)
        verify(exactly = 1) {
            dokumentDao.hent(any(), any())
        }
        assertEquals(0, meldingPubliserer.antallMeldinger)
    }
}
