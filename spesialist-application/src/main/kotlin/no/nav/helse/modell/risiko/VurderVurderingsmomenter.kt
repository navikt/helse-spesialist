package no.nav.helse.modell.risiko

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.InntektTilRisk
import no.nav.helse.modell.melding.StpPeriodeTilRisk
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDateTime
import java.util.*

internal class VurderVurderingsmomenter(
    private val vedtaksperiodeId: UUID,
    private val periode: Periode,
    private val organisasjonsnummer: String,
    private val yrkesaktivitetstype: Yrkesaktivitetstype,
    private val førstegangsbehandling: Boolean,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val utbetaling: Utbetaling,
    private val sykepengegrunnlagsfakta: Godkjenningsbehov.Sykepengegrunnlagsfakta,
    private val spleisVedtaksperioder: List<SpleisVedtaksperiode>,
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
        if (risikovurderingAlleredeGjort(sessionContext)) return true

        val løsning = commandContext.get<Risikovurderingløsning>()
        if (løsning == null || !løsning.gjelderVedtaksperiode(vedtaksperiodeId)) {
            loggInfo("Trenger risikovurdering av vedtaksperiode $vedtaksperiodeId")
            commandContext.behov(
                Behov.Risikovurdering(
                    vedtaksperiodeId = vedtaksperiodeId,
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
                    skjæringstidspunkt = sykefraværstilfelle.skjæringstidspunkt,
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
        løsning.leggTilVarsler(sessionContext)
        return true
    }

    private fun risikovurderingAlleredeGjort(sessionContext: SessionContext) = sessionContext.risikovurderingDao.hentRisikovurdering(vedtaksperiodeId) != null

    private fun Risikovurderingløsning.leggTilVarsler(sessionContext: SessionContext) {
        if (!kanGodkjennesAutomatisk) {
            logg.info("Oppretter risk-varsel for vedtaksperiode $vedtaksperiodeId")
            val nyesteBehandling =
                sessionContext.behandlingRepository.finnNyesteForVedtaksperiode(VedtaksperiodeId(vedtaksperiodeId))
                    ?: error("Fant ikke behandling")

            val varsel =
                Varsel.nytt(
                    VarselId(UUID.randomUUID()),
                    behandlingUnikId = nyesteBehandling.id,
                    spleisBehandlingId = nyesteBehandling.spleisBehandlingId,
                    kode = Varselkode.SB_RV_1.name,
                    opprettetTidspunkt = LocalDateTime.now(),
                )
            sessionContext.varselRepository.lagre(varsel)
        }
    }
}
