package no.nav.helse.mediator.builders

import java.util.UUID
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.ActualGenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode

class VedtaksperiodeBuilder(
    private val vedtaksperiodeId: UUID
) {
    internal fun build(
        generasjonRepository: ActualGenerasjonRepository,
        varselRepository: ActualVarselRepository
    ): Vedtaksperiode {
        val generasjonBuilder = GenerasjonBuilder(vedtaksperiodeId)
        generasjonRepository.byggGenerasjon(vedtaksperiodeId, generasjonBuilder)
        val varsler: List<Varsel> = varselRepository.finnVarslerFor(generasjonBuilder.generasjonId())
        generasjonBuilder.varsler(varsler)
        return Vedtaksperiode(vedtaksperiodeId, generasjonBuilder.build()).also {
            it.registrer(generasjonRepository, varselRepository)
        }
    }
}