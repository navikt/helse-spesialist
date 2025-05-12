package no.nav.helse.spesialist.domain.tilkommeninntekt

import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.random.Random
import kotlin.test.Test

class TilkommenInntektPeriodeValidatorTest {
    @Test
    fun `kan ikke legge til periode som overlapper med annen periode`() {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val tilkommenInntekt = TilkommenInntekt.ny(
            periode = (1 jan 2018) tilOgMed (31 jan 2018),
            ekskluderteUkedager = setOf(1 jan 2018, 31 jan 2018),
            periodebeløp = BigDecimal("10000.0"),
            fødselsnummer = fødselsnummer,
            saksbehandlerIdent = lagSaksbehandlerident(),
            notatTilBeslutter = "et notat til beslutter",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
            organisasjonsnummer = organisasjonsnummer
        )

        assertThrows<IllegalStateException> {
            TilkommenInntektPeriodeValidator.validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
                periode = (15 jan 2018) tilOgMed (31 jan 2018),
                organisasjonsnummer = organisasjonsnummer,
                andreTilkomneInntekter = listOf(tilkommenInntekt)
            )
        }
    }
}
