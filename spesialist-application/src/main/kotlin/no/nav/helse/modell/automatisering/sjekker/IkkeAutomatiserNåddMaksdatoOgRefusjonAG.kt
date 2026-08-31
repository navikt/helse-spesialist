package no.nav.helse.modell.automatisering.sjekker

import no.nav.helse.db.BehandlingRepository
import no.nav.helse.modell.automatisering.AutomatiseringValidering
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.spesialist.application.VarselRepository
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class IkkeAutomatiserNåddMaksdatoOgRefusjonAG(
    private val maksdato: LocalDate,
    tags: List<String>,
    private val vedtaksperiodeId: UUID,
    private val behandlingRepository: BehandlingRepository,
    private val varselRepository: VarselRepository,
) : AutomatiseringValidering {
    private val arbeidsgiverØnskerRefusjon = tags.contains("ArbeidsgiverØnskerRefusjon")

    override fun erAutomatiserbar(): Boolean {
        val nyesteBehandling =
            behandlingRepository.finnNyesteForVedtaksperiode(VedtaksperiodeId(vedtaksperiodeId))
                ?: error("Fant ikke behandling")
        val harNåddMaksdato = maksdato < nyesteBehandling.skjæringstidspunkt

        /**  TODO flytt dette til egen kommando tidligere i kommandokjeden
         * Oppretting av varsler bør skje i en egen kommando som kjøres før automatisering.
         * Da kan hele denne klassen droppes, fordi automatisering sjekker om det finnes varsler.
         * */
        val stopperAutomatisering = harNåddMaksdato && arbeidsgiverØnskerRefusjon
        if (stopperAutomatisering) {
            logg.info("Håndterer varsel om nådd maksdato og refusjon til arbeidsgiver på vedtaksperiode $vedtaksperiodeId")

            val varsel =
                Varsel.nytt(
                    VarselId(UUID.randomUUID()),
                    behandlingUnikId = nyesteBehandling.id,
                    spleisBehandlingId = nyesteBehandling.spleisBehandlingId,
                    kode = Varselkode.RV_OV_5.name,
                    opprettetTidspunkt = LocalDateTime.now(),
                )
            varselRepository.lagre(varsel)
        }
        return !stopperAutomatisering
    }

    override fun årsakTilIkkeAutomatiserbar() = "Nådd maksdato og har refusjon til arbeidsgiver"
}
