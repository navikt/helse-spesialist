package no.nav.helse.modell.oppgave

sealed interface Egenskap

sealed interface TilgangsstyrtEgenskap : Egenskap

data object RISK_QA: TilgangsstyrtEgenskap

data object FORTROLIG_ADRESSE: TilgangsstyrtEgenskap

data object EGEN_ANSATT: TilgangsstyrtEgenskap

data object REVURDERING: Egenskap

data object SØKNAD: Egenskap

data object STIKKPRØVE: Egenskap

data object UTBETALING_TIL_SYKMELDT: Egenskap

data object DELVIS_REFUSJON: Egenskap

data object UTBETALING_TIL_ARBEIDSGIVER: Egenskap

data object INGEN_UTBETALING: Egenskap