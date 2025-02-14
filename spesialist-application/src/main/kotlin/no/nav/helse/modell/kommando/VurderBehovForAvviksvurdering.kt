package no.nav.helse.modell.kommando

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.vedtaksperiode.Behandling
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.RV_IV_2
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingBehovLøsning
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import java.time.LocalDate
import java.util.UUID

class VurderBehovForAvviksvurdering(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val avviksvurderingRepository: AvviksvurderingRepository,
    private val beregningsgrunnlag: Beregningsgrunnlag,
    private val vilkårsgrunnlagId: UUID,
    private val behandling: Behandling,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        // TODO: Toggle her
        // TODO: sende ut behov
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<AvviksvurderingBehovLøsning>() ?: context.behov(Behov.Avviksvurdering(beregningsgrunnlag)).let { return false }
        when (løsning) {
            is AvviksvurderingBehovLøsning.NyVurderingForetatt -> {
                val avviksvurdering =
                    Avviksvurdering.ny(
                        id = løsning.avviksvurderingId,
                        vilkårsgrunnlagId = vilkårsgrunnlagId,
                        fødselsnummer = fødselsnummer,
                        skjæringstidspunkt = skjæringstidspunkt,
                        opprettet = løsning.opprettet,
                        avviksprosent = løsning.avviksprosent,
                        sammenligningsgrunnlag = løsning.sammenligningsgrunnlag,
                        beregningsgrunnlag = løsning.beregningsgrunnlag,
                    )
                avviksvurderingRepository.lagre(avviksvurdering)

                if (!løsning.harAkseptabeltAvvik) behandling.håndterNyttVarsel(RV_IV_2.nyttVarsel(behandling.vedtaksperiodeId()))
                // TODO: lage varsel dersom over akseptabelt avvik
            }
            is AvviksvurderingBehovLøsning.TrengerIkkeNyVurdering -> {
                avviksvurderingRepository.opprettKobling(løsning.avviksvurderingId, vilkårsgrunnlagId)
            }
        }
        return true
    }
}
