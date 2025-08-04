package no.nav.helse.modell.risiko

import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.InntektTilRisk
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_RV_1
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VurderVurderingsmomenter(
    private val vedtaksperiodeId: UUID,
    private val risikovurderingDao: RisikovurderingDao,
    private val organisasjonsnummer: String,
    private val førstegangsbehandling: Boolean,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val utbetaling: Utbetaling,
    private val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
) : Command {
    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        if (risikovurderingAlleredeGjort()) return true

        val løsning = context.get<Risikovurderingløsning>()
        if (løsning == null || !løsning.gjelderVedtaksperiode(vedtaksperiodeId)) {
            logg.info("Trenger risikovurdering av vedtaksperiode $vedtaksperiodeId")
            context.behov(
                Behov.Risikovurdering(
                    vedtaksperiodeId = vedtaksperiodeId,
                    organisasjonsnummer = organisasjonsnummer,
                    førstegangsbehandling = førstegangsbehandling,
                    kunRefusjon = !utbetaling.harEndringIUtbetalingTilSykmeldt(),
                    inntekt =
                        when (sykepengegrunnlagsfakta) {
                            is Sykepengegrunnlagsfakta.Infotrygd -> null
                            is Sykepengegrunnlagsfakta.Spleis ->
                                sykepengegrunnlagsfakta.arbeidsgivere.find { it.organisasjonsnummer == organisasjonsnummer }?.let { sykepengegrunnlagsArbeidsgiver ->
                                    InntektTilRisk(
                                        omregnetÅrsinntekt = sykepengegrunnlagsArbeidsgiver.omregnetÅrsinntekt,
                                        inntektskilde = sykepengegrunnlagsArbeidsgiver.inntektskilde.name,
                                    )
                                }
                        },
                ),
            )
            return false
        }

        løsning.lagre(risikovurderingDao)
        løsning.leggTilVarsler()
        return true
    }

    private fun risikovurderingAlleredeGjort() = risikovurderingDao.hentRisikovurdering(vedtaksperiodeId) != null

    private fun Risikovurderingløsning.leggTilVarsler() {
        if (!kanGodkjennesAutomatisk) {
            sykefraværstilfelle.håndter(SB_RV_1.nyttVarsel(vedtaksperiodeId))
        }
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(VurderVurderingsmomenter::class.java)
    }
}
