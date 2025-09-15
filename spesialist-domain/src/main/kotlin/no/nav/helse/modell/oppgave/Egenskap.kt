package no.nav.helse.modell.oppgave

import no.nav.helse.modell.oppgave.Egenskap.Kategori.Inntektsforhold
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Inntektskilde
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Mottaker
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Oppgavetype
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Periodetype
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Status
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Ukategorisert

enum class Egenskap(
    val kategori: Kategori = Ukategorisert,
) {
    RISK_QA,
    FORTROLIG_ADRESSE,
    STRENGT_FORTROLIG_ADRESSE,
    EGEN_ANSATT,
    BESLUTTER(kategori = Status),
    SPESIALSAK,
    REVURDERING(kategori = Oppgavetype),
    SØKNAD(kategori = Oppgavetype),
    STIKKPRØVE,
    UTBETALING_TIL_SYKMELDT(kategori = Mottaker),
    DELVIS_REFUSJON(kategori = Mottaker),
    UTBETALING_TIL_ARBEIDSGIVER(kategori = Mottaker),
    INGEN_UTBETALING(kategori = Mottaker),
    EN_ARBEIDSGIVER(kategori = Inntektskilde),
    FLERE_ARBEIDSGIVERE(kategori = Inntektskilde),
    FORLENGELSE(kategori = Periodetype),
    FORSTEGANGSBEHANDLING(kategori = Periodetype),
    INFOTRYGDFORLENGELSE(kategori = Periodetype),
    OVERGANG_FRA_IT(kategori = Periodetype),
    UTLAND,
    HASTER,
    RETUR(kategori = Status),
    SKJØNNSFASTSETTELSE,
    PÅ_VENT(kategori = Status),
    TILBAKEDATERT,
    GOSYS,
    MANGLER_IM,
    MEDLEMSKAP,
    VERGEMÅL,
    GRUNNBELØPSREGULERING,
    SELVSTENDIG_NÆRINGSDRIVENDE(kategori = Inntektsforhold),
    ARBEIDSTAKER(kategori = Inntektsforhold),
    ;

    enum class Kategori {
        Mottaker,
        Inntektskilde,
        Inntektsforhold,
        Oppgavetype,
        Ukategorisert,
        Periodetype,
        Status,
    }
}
