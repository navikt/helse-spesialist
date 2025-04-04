package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.InntektsendringerEvent
import no.nav.helse.modell.melding.InntektsendringerEvent.Inntektskilde
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrTilkommenInntekt.FjernetInntekt
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrTilkommenInntekt.NyEllerEndretInntekt
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.apr
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.jun
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.mai
import no.nav.helse.spesialist.domain.testfixtures.mar
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OverstyrTilkommenInntektTest {
    @Test
    fun `bygg event`() {
        val fødselsnummer = lagFødselsnummer()
        val aktørId = lagAktørId()
        val arbeidsgiver1 = lagOrganisasjonsnummer()
        val arbeidsgiver2 = lagOrganisasjonsnummer()
        val arbeidsgiver3 = lagOrganisasjonsnummer()
        val overstyring = OverstyrTilkommenInntekt.ny(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = UUID.randomUUID(),
            saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
            nyEllerEndredeInntekter = listOf(
                NyEllerEndretInntekt(
                    arbeidsgiver1,
                    perioder = listOf(
                        NyEllerEndretInntekt.PeriodeMedBeløp(fom = 1 jan 2018, tom = 31 jan 2018, periodeBeløp = 10000.0),
                        NyEllerEndretInntekt.PeriodeMedBeløp(fom = 1 feb 2018, tom = 28 feb 2018, periodeBeløp = 15000.0)
                    )
                ),
                NyEllerEndretInntekt(
                    arbeidsgiver2,
                    perioder = listOf(
                        NyEllerEndretInntekt.PeriodeMedBeløp(fom = 1 mar 2018, tom = 31 mar 2018, periodeBeløp = 10000.0),
                    )
                )
            ),
            fjernedeInntekter = listOf(
                FjernetInntekt(
                    arbeidsgiver1,
                    perioder = listOf(
                        FjernetInntekt.PeriodeUtenBeløp(fom = 1 apr 2018, tom = 30 apr 2018),
                    )
                ),
                FjernetInntekt(
                    arbeidsgiver3,
                    perioder = listOf(
                        FjernetInntekt.PeriodeUtenBeløp(fom = 1 mai 2018, tom = 31 mai 2018),
                        FjernetInntekt.PeriodeUtenBeløp(fom = 1 jun 2018, tom = 30 jun 2018)
                    )
                )
            )
        )
        val event = overstyring.byggEvent()
        assertEquals(
            InntektsendringerEvent(
                inntektskilder = listOf(
                    Inntektskilde(
                        arbeidsgiver1,
                        inntekter = listOf(
                            Inntektskilde.Inntekt(fom = 1 jan 2018, tom = 31 jan 2018, dagsbeløp = 10000.0),
                            Inntektskilde.Inntekt(fom = 1 feb 2018, tom = 28 feb 2018, dagsbeløp = 15000.0),
                        ),
                        nullstill = listOf(
                            Inntektskilde.Nullstilling(fom = 1 apr 2018, tom = 30 apr 2018)
                        )
                    ),
                    Inntektskilde(
                        arbeidsgiver2,
                        inntekter = listOf(
                            Inntektskilde.Inntekt(fom = 1 mar 2018, tom = 31 mar 2018, dagsbeløp = 10000.0),
                        ),
                        nullstill = emptyList(),
                    ),
                    Inntektskilde(
                        arbeidsgiver3,
                        inntekter = emptyList(),
                        nullstill = listOf(
                            Inntektskilde.Nullstilling(fom = 1 mai 2018, tom = 31 mai 2018),
                            Inntektskilde.Nullstilling(fom = 1 jun 2018, tom = 30 jun 2018)
                        )
                    ),
                )
            ),
            event
        )
    }
}
