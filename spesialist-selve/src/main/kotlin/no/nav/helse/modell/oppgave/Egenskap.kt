package no.nav.helse.modell.oppgave

import no.nav.helse.modell.oppgave.Egenskap.Kategori.Mottaker
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Oppgavetype
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Ukategorisert

sealed interface Egenskap {
    fun kategori() = Ukategorisert

    enum class Kategori {
        Mottaker,
        Inntektskilde,
        Oppgavetype,
        Ukategorisert
    }
}

sealed interface TilgangsstyrtEgenskap : Egenskap

data object RISK_QA: TilgangsstyrtEgenskap

data object FORTROLIG_ADRESSE: TilgangsstyrtEgenskap

data object EGEN_ANSATT: TilgangsstyrtEgenskap

data object REVURDERING: Egenskap {
    override fun kategori(): Egenskap.Kategori = Oppgavetype
}

data object SØKNAD: Egenskap {
    override fun kategori(): Egenskap.Kategori = Oppgavetype
}

data object STIKKPRØVE: Egenskap

data object UTBETALING_TIL_SYKMELDT: Egenskap {
    override fun kategori(): Egenskap.Kategori = Mottaker
}

data object DELVIS_REFUSJON: Egenskap {
    override fun kategori(): Egenskap.Kategori = Mottaker
}

data object UTBETALING_TIL_ARBEIDSGIVER: Egenskap {
    override fun kategori(): Egenskap.Kategori = Mottaker
}

data object INGEN_UTBETALING: Egenskap {
    override fun kategori(): Egenskap.Kategori = Mottaker
}

data object HASTER: Egenskap

data object RETUR: Egenskap

data object BESLUTTER: Egenskap