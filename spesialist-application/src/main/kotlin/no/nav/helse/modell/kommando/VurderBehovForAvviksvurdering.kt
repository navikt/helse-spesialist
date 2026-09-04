package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.RV_IV_2
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingBehovLøsning
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class VurderBehovForAvviksvurdering(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val sykepengegrunnlagsfakta: Godkjenningsbehov.Sykepengegrunnlagsfakta,
    private val vilkårsgrunnlagId: UUID,
    private val vedtaksperiodeId: VedtaksperiodeId,
    private val spleisBehandlingId: SpleisBehandlingId,
    private val behandlingUnikId: BehandlingUnikId,
    private val yrkesaktivitetstype: Yrkesaktivitetstype,
    private val organisasjonsnummer: String,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        if (sykepengegrunnlagsfakta !is Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker) return true
        if (yrkesaktivitetstype == Yrkesaktivitetstype.SELVSTENDIG) return true
        return behov(commandContext, sykepengegrunnlagsfakta)
    }

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        if (sykepengegrunnlagsfakta !is Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker) return true
        val løsning =
            commandContext.get<AvviksvurderingBehovLøsning>() ?: return behov(commandContext, sykepengegrunnlagsfakta)
        val eksisterendeAvviksvurdering =
            sessionContext.avviksvurderingRepository.hentAvviksvurderingFor(løsning.avviksvurderingId)

        if (eksisterendeAvviksvurdering != null) {
            sessionContext.avviksvurderingRepository.opprettKobling(
                eksisterendeAvviksvurdering.unikId,
                vilkårsgrunnlagId,
            )
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
        if (!løsning.harAkseptabeltAvvik) {
            val varsel =
                Varsel.nytt(
                    id = VarselId(UUID.randomUUID()),
                    behandlingUnikId = behandlingUnikId,
                    spleisBehandlingId = spleisBehandlingId,
                    kode = RV_IV_2.name,
                    opprettetTidspunkt = LocalDateTime.now(),
                )
            sessionContext.varselRepository.lagre(varsel)
        }
        sessionContext.avviksvurderingRepository.lagre(avviksvurdering)
        return true
    }

    private fun behov(
        commandContext: CommandContext,
        sykepengegrunnlagsfakta: Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker,
    ): Boolean {
        commandContext.behov(
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
                vedtaksperiodeId = vedtaksperiodeId.value,
            ),
        )
        return false
    }
}
