package no.nav.helse.modell.oppgave

import java.util.EnumSet
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Inntektskilde
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Mottaker
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Oppgavetype
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Periodetype
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Ukategorisert

enum class Egenskap(
    val kategori: Kategori = Ukategorisert,
    private val tilgangsstyrt: Boolean = false
) {
    RISK_QA(tilgangsstyrt = true),
    FORTROLIG_ADRESSE(tilgangsstyrt = true),
    EGEN_ANSATT(tilgangsstyrt = true),
    BESLUTTER(tilgangsstyrt = true),
    SPESIALSAK(tilgangsstyrt = true),
    REVURDERING(kategori = Oppgavetype),
    SØKNAD(kategori = Oppgavetype),
    STIKKPRØVE(tilgangsstyrt = true),
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
    RETUR,
    FULLMAKT,
    VERGEMÅL;

    enum class Kategori {
        Mottaker,
        Inntektskilde,
        Oppgavetype,
        Ukategorisert,
        Periodetype
    }

    companion object {
        val alleTilgangsstyrteEgenskaper = EnumSet.allOf(Egenskap::class.java).filter(Egenskap::tilgangsstyrt)
        fun Collection<Egenskap>.tilgangsstyrteEgenskaper() = filter { it in alleTilgangsstyrteEgenskaper }
    }
}