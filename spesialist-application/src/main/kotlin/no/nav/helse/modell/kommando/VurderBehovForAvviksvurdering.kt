package no.nav.helse.modell.kommando

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.vedtaksperiode.Behandling
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.RV_IV_2
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingBehovLøsning
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import java.time.LocalDate
import java.util.UUID

class VurderBehovForAvviksvurdering(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val avviksvurderingRepository: AvviksvurderingRepository,
    private val omregnedeÅrsinntekter: List<OmregnetÅrsinntekt>,
    private val vilkårsgrunnlagId: UUID,
    private val behandling: Behandling,
    private val erInngangsvilkårVurdertISpleis: Boolean,
    private val organisasjonsnummer: String,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (!erInngangsvilkårVurdertISpleis) return true
        return behov(context)
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<AvviksvurderingBehovLøsning>() ?: return behov(context)
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
            }

            is AvviksvurderingBehovLøsning.TrengerIkkeNyVurdering -> {
                avviksvurderingRepository.opprettKobling(løsning.avviksvurderingId, vilkårsgrunnlagId)
            }
        }
        return true
    }

    private fun behov(context: CommandContext): Boolean {
        context.behov(
            Behov.Avviksvurdering(
                omregnedeÅrsinntekter,
                vilkårsgrunnlagId,
                skjæringstidspunkt,
                organisasjonsnummer,
                behandling.vedtaksperiodeId(),
            ),
        )
        return false
    }
}
