package no.nav.helse.modell.automatisering.sjekker

import no.nav.helse.modell.automatisering.AutomatiseringValidering
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.spesialist.application.VarselRepository
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Varsel
import java.time.LocalDate

internal class IkkeAutomatiserNåddMaksdatoOgRefusjonAG(
    maksdato: LocalDate,
    tags: List<String>,
    private val behandling: Behandling,
    private val varselRepository: VarselRepository,
) : AutomatiseringValidering {
    private val harNåddMaksdato = maksdato < behandling.skjæringstidspunkt
    private val arbeidsgiverØnskerRefusjon = tags.contains("ArbeidsgiverØnskerRefusjon")

    override fun erAutomatiserbar(): Boolean {
        val stopperAutomatisering = harNåddMaksdato && arbeidsgiverØnskerRefusjon
        if (stopperAutomatisering) {
            val varsel = Varsel.nytt(behandling.id, behandling.spleisBehandlingId, Varselkode.RV_OV_5.name)
            varselRepository.lagre(varsel)
        }
        return !stopperAutomatisering
    }

    override fun årsakTilIkkeAutomatiserbar() = "Nådd maksdato og har refusjon til arbeidsgiver"
}
