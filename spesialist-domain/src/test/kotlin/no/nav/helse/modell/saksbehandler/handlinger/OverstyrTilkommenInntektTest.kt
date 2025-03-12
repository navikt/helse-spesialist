package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.apr
import no.nav.helse.modell.feb
import no.nav.helse.modell.jan
import no.nav.helse.modell.jun
import no.nav.helse.modell.lagAktørId
import no.nav.helse.modell.lagFødselsnummer
import no.nav.helse.modell.lagOrganisasjonsnummer
import no.nav.helse.modell.mai
import no.nav.helse.modell.mar
import no.nav.helse.modell.melding.InntektsendringerEvent
import no.nav.helse.modell.melding.InntektsendringerEvent.Inntektskildeendring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrTilkommenInntekt.FjernetInntekt
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrTilkommenInntekt.NyEllerEndretInntekt
import no.nav.helse.spesialist.domain.SaksbehandlerOid
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
                inntektskildeendringer = listOf(
                    Inntektskildeendring(
                        arbeidsgiver1,
                        nyeEllerEndredeInntekter = listOf(
                            Inntektskildeendring.PeriodeMedBeløp(fom = 1 jan 2018, tom = 31 jan 2018, periodebeløp = 10000.0),
                            Inntektskildeendring.PeriodeMedBeløp(fom = 1 feb 2018, tom = 28 feb 2018, periodebeløp = 15000.0),
                        ),
                        fjernedeInntekter = listOf(
                            Inntektskildeendring.PeriodeUtenBeløp(fom = 1 apr 2018, tom = 30 apr 2018)
                        )
                    ),
                    Inntektskildeendring(
                        arbeidsgiver2,
                        nyeEllerEndredeInntekter = listOf(
                            Inntektskildeendring.PeriodeMedBeløp(fom = 1 mar 2018, tom = 31 mar 2018, periodebeløp = 10000.0),
                        ),
                        fjernedeInntekter = emptyList(),
                    ),
                    Inntektskildeendring(
                        arbeidsgiver3,
                        nyeEllerEndredeInntekter = emptyList(),
                        fjernedeInntekter = listOf(
                            Inntektskildeendring.PeriodeUtenBeløp(fom = 1 mai 2018, tom = 31 mai 2018),
                            Inntektskildeendring.PeriodeUtenBeløp(fom = 1 jun 2018, tom = 30 jun 2018)
                        )
                    ),
                )
            ),
            event
        )
    }
}
