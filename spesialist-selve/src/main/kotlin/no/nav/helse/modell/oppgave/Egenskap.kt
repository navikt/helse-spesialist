package no.nav.helse.modell.oppgave

import java.util.EnumSet
import no.nav.helse.modell.oppgave.Egenskap.Kategori.Inntektskilde
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
    SPESIALSAK(tilgangsstyrt = true),
    REVURDERING(kategori = Oppgavetype),
    SØKNAD(kategori = Oppgavetype),
    STIKKPRØVE(kategori = Oppgavetype, tilgangsstyrt = true),
    UTBETALING_TIL_SYKMELDT(kategori = Mottaker),
    DELVIS_REFUSJON(kategori = Mottaker),
    UTBETALING_TIL_ARBEIDSGIVER(kategori = Mottaker),
    INGEN_UTBETALING(kategori = Mottaker),
    HASTER,
    RETUR,
    FULLMAKT,
    VERGEMÅL;

    enum class Kategori {
        Mottaker,
        Inntektskilde,
        Oppgavetype,
        Ukategorisert
    }

    internal companion object {
        internal val alleTilgangsstyrteEgenskaper = EnumSet.allOf(Egenskap::class.java).filter(Egenskap::tilgangsstyrt)
        internal fun Collection<Egenskap>.tilgangsstyrteEgenskaper() = filter { it in alleTilgangsstyrteEgenskaper }

        internal fun Collection<Egenskap>.toMap(): Map<String, String> {
            return associate {
                it.mapToString() to it.kategori.mapToString()
            }
        }

        private fun Egenskap.mapToString(): String = when (this) {
            RISK_QA -> "RISK_QA"
            FORTROLIG_ADRESSE -> "FORTROLIG_ADRESSE"
            EGEN_ANSATT -> "EGEN_ANSATT"
            BESLUTTER -> "BESLUTTER"
            SPESIALSAK -> "SPESIALSAK"
            REVURDERING -> "REVURDERING"
            SØKNAD -> "SØKNAD"
            STIKKPRØVE -> "STIKKPRØVE"
            UTBETALING_TIL_SYKMELDT -> "UTBETALING_TIL_SYKMELDT"
            DELVIS_REFUSJON -> "DELVIS_REFUSJON"
            UTBETALING_TIL_ARBEIDSGIVER -> "UTBETALING_TIL_ARBEIDSGIVER"
            INGEN_UTBETALING -> "INGEN_UTBETALING"
            HASTER -> "HASTER"
            RETUR -> "RETUR"
            FULLMAKT -> "FULLMAKT"
            VERGEMÅL -> "VERGEMÅL"
        }

        private fun Kategori.mapToString(): String {
            return when (this) {
                Mottaker -> "Mottaker"
                Inntektskilde -> "Inntektskilde"
                Oppgavetype -> "Oppgavetype"
                Ukategorisert -> "Ukategorisert"
            }
        }
    }
}