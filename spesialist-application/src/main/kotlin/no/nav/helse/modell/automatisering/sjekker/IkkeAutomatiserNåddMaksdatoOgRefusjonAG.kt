package no.nav.helse.modell.automatisering.sjekker

import no.nav.helse.modell.automatisering.AutomatiseringValidering
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import java.time.LocalDate
import java.util.UUID

internal class IkkeAutomatiserNåddMaksdatoOgRefusjonAG(
    maksdato: LocalDate,
    tags: List<String>,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val vedtaksperiodeId: UUID,
) : AutomatiseringValidering {
    private val harNåddMaksdato = maksdato < sykefraværstilfelle.skjæringstidspunkt
    private val arbeidsgiverØnskerRefusjon = tags.contains("ArbeidsgiverØnskerRefusjon")

    override fun erAutomatiserbar(): Boolean {
        val stopperAutomatisering = harNåddMaksdato && arbeidsgiverØnskerRefusjon
        if (stopperAutomatisering) {
            sykefraværstilfelle.håndter(Varselkode.RV_OV_5.nyttVarsel(vedtaksperiodeId))
        }
        return !stopperAutomatisering
    }

    override fun årsakTilIkkeAutomatiserbar() = "Nådd maksdato og har refusjon til arbeidsgiver"
}
