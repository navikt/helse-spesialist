package no.nav.helse.spesialist.domain.testfixtures.testdata

import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Inntektsforhold
import no.nav.helse.modell.oppgave.Mottaker
import no.nav.helse.modell.oppgave.Oppgavetype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype

fun Set<Egenskap>.finnMottaker() = when {
    contains(Egenskap.INGEN_UTBETALING) -> Mottaker.IngenUtbetaling
    contains(Egenskap.DELVIS_REFUSJON) -> Mottaker.DelvisRefusjon
    contains(Egenskap.UTBETALING_TIL_SYKMELDT) -> Mottaker.UtbetalingTilSykmeldt
    else -> Mottaker.UtbetalingTilArbeidsgiver
}

fun Set<Egenskap>.finnOppgavetype() = when {
    contains(Egenskap.REVURDERING) -> Oppgavetype.Revurdering
    else -> Oppgavetype.Søknad
}

fun Set<Egenskap>.finnInntektskilde() = when {
    contains(Egenskap.FLERE_ARBEIDSGIVERE) -> Inntektskilde.FLERE_ARBEIDSGIVERE
    else -> Inntektskilde.EN_ARBEIDSGIVER
}

fun Set<Egenskap>.finnInntektsforhold() = when {
    contains(Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE) -> Inntektsforhold.SelvstendigNæringsdrivende
    else -> Inntektsforhold.Arbeidstaker
}

fun Set<Egenskap>.finnPeriodetype() = when {
    contains(Egenskap.FORLENGELSE) -> Periodetype.FORLENGELSE
    contains(Egenskap.OVERGANG_FRA_IT) -> Periodetype.OVERGANG_FRA_IT
    contains(Egenskap.INFOTRYGDFORLENGELSE) -> Periodetype.INFOTRYGDFORLENGELSE
    else -> Periodetype.FØRSTEGANGSBEHANDLING
}

fun Array<out Egenskap>.finnMottaker(): Mottaker = toSet().finnMottaker()
fun Array<out Egenskap>.finnOppgavetype(): Oppgavetype = toSet().finnOppgavetype()
fun Array<out Egenskap>.finnInntektskilde(): Inntektskilde = toSet().finnInntektskilde()
fun Array<out Egenskap>.finnInntektsforhold(): Inntektsforhold = toSet().finnInntektsforhold()
fun Array<out Egenskap>.finnPeriodetype(): Periodetype = toSet().finnPeriodetype()
