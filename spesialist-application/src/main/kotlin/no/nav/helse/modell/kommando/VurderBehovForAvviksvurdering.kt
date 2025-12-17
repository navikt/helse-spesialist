package no.nav.helse.modell.kommando

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.RV_IV_2
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingBehovLøsning
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import java.time.LocalDate
import java.util.UUID

class VurderBehovForAvviksvurdering(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val avviksvurderingRepository: AvviksvurderingRepository,
    private val sykepengegrunnlagsfakta: Godkjenningsbehov.Sykepengegrunnlagsfakta,
    private val vilkårsgrunnlagId: UUID,
    private val legacyBehandling: LegacyBehandling,
    private val yrkesaktivitetstype: Yrkesaktivitetstype,
    private val organisasjonsnummer: String,
) : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        if (sykepengegrunnlagsfakta !is Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker) return true
        if (yrkesaktivitetstype == Yrkesaktivitetstype.SELVSTENDIG) return true
        return behov(context, sykepengegrunnlagsfakta)
    }

    override fun resume(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        if (sykepengegrunnlagsfakta !is Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker) return true
        val løsning = context.get<AvviksvurderingBehovLøsning>() ?: return behov(context, sykepengegrunnlagsfakta)
        val eksisterendeAvviksvurdering = avviksvurderingRepository.hentAvviksvurderingFor(løsning.avviksvurderingId)

        if (eksisterendeAvviksvurdering != null) {
            avviksvurderingRepository.opprettKobling(eksisterendeAvviksvurdering.unikId, vilkårsgrunnlagId)
            return true
        }
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
        if (!løsning.harAkseptabeltAvvik) legacyBehandling.håndterNyttVarsel(RV_IV_2.nyttVarsel(legacyBehandling.vedtaksperiodeId()))
        return true
    }

    private fun behov(
        context: CommandContext,
        sykepengegrunnlagsfakta: Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker,
    ): Boolean {
        context.behov(
            Behov.Avviksvurdering(
                omregnedeÅrsinntekter =
                    sykepengegrunnlagsfakta.arbeidsgivere.map {
                        OmregnetÅrsinntekt(
                            arbeidsgiverreferanse = it.organisasjonsnummer,
                            beløp = it.omregnetÅrsinntekt,
                        )
                    },
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                skjæringstidspunkt = skjæringstidspunkt,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = legacyBehandling.vedtaksperiodeId(),
            ),
        )
        return false
    }
}
