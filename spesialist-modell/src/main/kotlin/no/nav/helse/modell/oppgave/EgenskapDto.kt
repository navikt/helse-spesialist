package no.nav.helse.modell.oppgave

enum class EgenskapDto {
    RISK_QA,
    FORTROLIG_ADRESSE,
    STRENGT_FORTROLIG_ADRESSE,
    EGEN_ANSATT,
    BESLUTTER,
    SPESIALSAK,
    REVURDERING,
    SØKNAD,
    STIKKPRØVE,
    UTBETALING_TIL_SYKMELDT,
    DELVIS_REFUSJON,
    UTBETALING_TIL_ARBEIDSGIVER,
    INGEN_UTBETALING,
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE,
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
    UTLAND,
    HASTER,
    RETUR,
    SKJØNNSFASTSETTELSE,
    PÅ_VENT,
    TILBAKEDATERT,
    GOSYS,
    MEDLEMSKAP,
    VERGEMÅL,
    ;

    internal companion object {
        fun Egenskap.toDto(): EgenskapDto {
            return when (this) {
                Egenskap.RISK_QA -> RISK_QA
                Egenskap.FORTROLIG_ADRESSE -> FORTROLIG_ADRESSE
                Egenskap.STRENGT_FORTROLIG_ADRESSE -> STRENGT_FORTROLIG_ADRESSE
                Egenskap.EGEN_ANSATT -> EGEN_ANSATT
                Egenskap.BESLUTTER -> BESLUTTER
                Egenskap.SPESIALSAK -> SPESIALSAK
                Egenskap.REVURDERING -> REVURDERING
                Egenskap.SØKNAD -> SØKNAD
                Egenskap.STIKKPRØVE -> STIKKPRØVE
                Egenskap.UTBETALING_TIL_SYKMELDT -> UTBETALING_TIL_SYKMELDT
                Egenskap.DELVIS_REFUSJON -> DELVIS_REFUSJON
                Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> UTBETALING_TIL_ARBEIDSGIVER
                Egenskap.INGEN_UTBETALING -> INGEN_UTBETALING
                Egenskap.EN_ARBEIDSGIVER -> EN_ARBEIDSGIVER
                Egenskap.FLERE_ARBEIDSGIVERE -> FLERE_ARBEIDSGIVERE
                Egenskap.FORLENGELSE -> FORLENGELSE
                Egenskap.FORSTEGANGSBEHANDLING -> FORSTEGANGSBEHANDLING
                Egenskap.INFOTRYGDFORLENGELSE -> INFOTRYGDFORLENGELSE
                Egenskap.OVERGANG_FRA_IT -> OVERGANG_FRA_IT
                Egenskap.UTLAND -> UTLAND
                Egenskap.HASTER -> HASTER
                Egenskap.RETUR -> RETUR
                Egenskap.SKJØNNSFASTSETTELSE -> SKJØNNSFASTSETTELSE
                Egenskap.PÅ_VENT -> PÅ_VENT
                Egenskap.TILBAKEDATERT -> TILBAKEDATERT
                Egenskap.GOSYS -> GOSYS
                Egenskap.MEDLEMSKAP -> MEDLEMSKAP
                Egenskap.VERGEMÅL -> VERGEMÅL
            }
        }

        fun EgenskapDto.gjenopprett() =
            when (this) {
                RISK_QA -> Egenskap.RISK_QA
                FORTROLIG_ADRESSE -> Egenskap.FORTROLIG_ADRESSE
                STRENGT_FORTROLIG_ADRESSE -> Egenskap.STRENGT_FORTROLIG_ADRESSE
                EGEN_ANSATT -> Egenskap.EGEN_ANSATT
                BESLUTTER -> Egenskap.BESLUTTER
                SPESIALSAK -> Egenskap.SPESIALSAK
                REVURDERING -> Egenskap.REVURDERING
                SØKNAD -> Egenskap.SØKNAD
                STIKKPRØVE -> Egenskap.STIKKPRØVE
                UTBETALING_TIL_SYKMELDT -> Egenskap.UTBETALING_TIL_SYKMELDT
                DELVIS_REFUSJON -> Egenskap.DELVIS_REFUSJON
                UTBETALING_TIL_ARBEIDSGIVER -> Egenskap.UTBETALING_TIL_ARBEIDSGIVER
                INGEN_UTBETALING -> Egenskap.INGEN_UTBETALING
                EN_ARBEIDSGIVER -> Egenskap.EN_ARBEIDSGIVER
                FLERE_ARBEIDSGIVERE -> Egenskap.FLERE_ARBEIDSGIVERE
                FORLENGELSE -> Egenskap.FORLENGELSE
                FORSTEGANGSBEHANDLING -> Egenskap.FORSTEGANGSBEHANDLING
                INFOTRYGDFORLENGELSE -> Egenskap.INFOTRYGDFORLENGELSE
                OVERGANG_FRA_IT -> Egenskap.OVERGANG_FRA_IT
                UTLAND -> Egenskap.UTLAND
                HASTER -> Egenskap.HASTER
                RETUR -> Egenskap.RETUR
                SKJØNNSFASTSETTELSE -> Egenskap.SKJØNNSFASTSETTELSE
                PÅ_VENT -> Egenskap.PÅ_VENT
                TILBAKEDATERT -> Egenskap.TILBAKEDATERT
                GOSYS -> Egenskap.GOSYS
                MEDLEMSKAP -> Egenskap.MEDLEMSKAP
                VERGEMÅL -> Egenskap.VERGEMÅL
            }
    }
}
