package no.nav.helse.modell.oppgave

import no.nav.helse.modell.oppgave.Egenskap.Kategori.Mottaker
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Oppgavetype
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Ukategorisert

sealed interface Egenskap {
    val kategori: Kategori

    enum class Kategori {
        Mottaker,
        Inntektskilde,
        Oppgavetype,
        Ukategorisert
    }
}

sealed interface TilgangsstyrtEgenskap : Egenskap

data object RISK_QA: TilgangsstyrtEgenskap {
    override val kategori: Egenskap.Kategori = Ukategorisert
}

data object FORTROLIG_ADRESSE: TilgangsstyrtEgenskap {
    override val kategori: Egenskap.Kategori = Ukategorisert
}

data object EGEN_ANSATT: TilgangsstyrtEgenskap {
    override val kategori: Egenskap.Kategori = Ukategorisert
}

data object REVURDERING: Egenskap {
    override val kategori: Egenskap.Kategori = Oppgavetype
}

data object SØKNAD: Egenskap {
    override val kategori: Egenskap.Kategori = Oppgavetype
}

data object STIKKPRØVE: Egenskap {
    override val kategori: Egenskap.Kategori = Ukategorisert
}

data object UTBETALING_TIL_SYKMELDT: Egenskap {
    override val kategori: Egenskap.Kategori = Mottaker
}

data object DELVIS_REFUSJON: Egenskap {
    override val kategori: Egenskap.Kategori = Mottaker
}

data object UTBETALING_TIL_ARBEIDSGIVER: Egenskap {
    override val kategori: Egenskap.Kategori = Mottaker
}

data object INGEN_UTBETALING: Egenskap {
    override val kategori: Egenskap.Kategori = Mottaker
}