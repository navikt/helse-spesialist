package no.nav.helse.modell.risiko

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.InntektTilRisk
import no.nav.helse.modell.melding.StpPeriodeTilRisk
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_RV_1
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VedtaksperiodeId

internal class VurderVurderingsmomenter(
    private val periode: Periode,
    private val organisasjonsnummer: String,
    private val yrkesaktivitetstype: Yrkesaktivitetstype,
    private val førstegangsbehandling: Boolean,
    private val utbetaling: Utbetaling,
    private val sykepengegrunnlagsfakta: Godkjenningsbehov.Sykepengegrunnlagsfakta,
    private val spleisVedtaksperioder: List<SpleisVedtaksperiode>,
    private val spleisBehandlingId: SpleisBehandlingId,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ) = behandle(commandContext, sessionContext)

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean = behandle(commandContext, sessionContext)

    private fun behandle(
        commandContext: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        val behandling =
            sessionContext.behandlingRepository.finn(spleisBehandlingId)

        if (risikovurderingAlleredeGjort(sessionContext, behandling.vedtaksperiodeId)) return true

        val løsning = commandContext.get<Risikovurderingløsning>()
        if (løsning == null || !løsning.gjelderVedtaksperiode(behandling.vedtaksperiodeId.value)) {
            loggInfo("Trenger risikovurdering av vedtaksperiode ${behandling.vedtaksperiodeId.value}")
            commandContext.behov(
                Behov.Risikovurdering(
                    vedtaksperiodeId = behandling.vedtaksperiodeId.value,
                    organisasjonsnummer = organisasjonsnummer,
                    yrkesaktivitetstype = yrkesaktivitetstype,
                    førstegangsbehandling = førstegangsbehandling,
                    kunRefusjon = !utbetaling.harEndringIUtbetalingTilSykmeldt(),
                    inntekt =
                        when (sykepengegrunnlagsfakta) {
                            is Godkjenningsbehov.Sykepengegrunnlagsfakta.Infotrygd -> {
                                null
                            }

                            is Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker -> {
                                sykepengegrunnlagsfakta.arbeidsgivere
                                    .find { it.organisasjonsnummer == organisasjonsnummer }
                                    ?.let { sykepengegrunnlagsArbeidsgiver ->
                                        InntektTilRisk(
                                            omregnetÅrsinntekt = sykepengegrunnlagsArbeidsgiver.omregnetÅrsinntekt,
                                            inntektskilde = sykepengegrunnlagsArbeidsgiver.inntektskilde.name,
                                        )
                                    }
                            }

                            is Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.SelvstendigNæringsdrivende -> {
                                InntektTilRisk(
                                    omregnetÅrsinntekt = sykepengegrunnlagsfakta.selvstendig.beregningsgrunnlag.toDouble(),
                                    inntektskilde = "Sigrun", // TODO: Hardkodet, verdi - avklar med Risk og Spleis
                                )
                            }
                        },
                    periode = periode,
                    skjæringstidspunkt = behandling.skjæringstidspunkt,
                    perioderMedSammeSkjæringstidspunkt =
                        spleisVedtaksperioder.map {
                            StpPeriodeTilRisk(
                                fom = it.fom,
                                tom = it.tom,
                                organisasjonsnummer = it.yrkesaktivitet?.organisasjonsnummer,
                                yrkesaktivitetstype = it.yrkesaktivitet?.yrkesaktivitetstype,
                                vedtaksperiodeId = it.vedtaksperiodeId,
                            )
                        },
                ),
            )
            return false
        }

        løsning.lagre(sessionContext.risikovurderingDao)
        if (!løsning.kanGodkjennesAutomatisk) {
            val varsel = Varsel.nytt(behandlingUnikId = behandling.id, behandling.spleisBehandlingId, SB_RV_1.name)
            sessionContext.varselRepository.lagre(varsel)
        }
        return true
    }

    private fun risikovurderingAlleredeGjort(
        sessionContext: SessionContext,
        vedtaksperiodeId: VedtaksperiodeId,
    ) = sessionContext.risikovurderingDao.hentRisikovurdering(vedtaksperiodeId.value) != null
}
