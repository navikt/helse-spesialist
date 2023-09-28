package no.nav.helse.modell.oppgave

import java.util.EnumSet
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Mottaker
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Oppgavetype
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Ukategorisert

enum class Egenskap(
    private val kategori: Kategori = Ukategorisert,
    private val tilgangsstyrt: Boolean = false
) {
    RISK_QA(tilgangsstyrt = true),
    FORTROLIG_ADRESSE(tilgangsstyrt = true),
    EGEN_ANSATT(tilgangsstyrt = true),
    BESLUTTER(tilgangsstyrt = true),
    REVURDERING(kategori = Oppgavetype),
    SØKNAD(kategori = Oppgavetype),
    STIKKPRØVE(kategori = Oppgavetype),
    UTBETALING_TIL_SYKMELDT(kategori = Mottaker),
    DELVIS_REFUSJON(kategori = Mottaker),
    UTBETALING_TIL_ARBEIDSGIVER(kategori = Mottaker),
    INGEN_UTBETALING(kategori = Mottaker),
    HASTER,
    RETUR,
    FULLMAKT,
    VERGEMÅL,
    SPESIALSAK;

    enum class Kategori {
        Mottaker,
        Inntektskilde,
        Oppgavetype,
        Ukategorisert
    }

    internal companion object {
        internal val alleTilgangsstyrteEgenskaper = EnumSet.allOf(Egenskap::class.java).filter(Egenskap::tilgangsstyrt)
        internal fun Collection<Egenskap>.tilgangsstyrteEgenskaper() = filter { it in alleTilgangsstyrteEgenskaper }
    }
}