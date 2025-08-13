package no.nav.helse.mediator.meldinger.hendelser

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.melding.Sykepengevedtak.Vedtak
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedOpphavIInfotrygd
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedSkjønnsvurdering
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedSkjønnsvurdering.Skjønnsfastsettingopplysninger
import no.nav.helse.modell.melding.SykepengevedtakSelvstendigNæringsdrivendeDto
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode.Companion.finnBehandling
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag.Companion.relevanteFor
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering.Companion.finnRiktigAvviksvurdering
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class AvsluttetMedVedtakMessage(
    override val id: UUID,
    private val yrkesaktivitetstype: String,
    private val fødselsnummer: String,
    private val vedtakFattetTidspunkt: LocalDateTime,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID,
    private val hendelser: List<UUID>,
    private val sykepengegrunnlag: Double,
    private val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
    private val json: String,
) : Vedtaksperiodemelding {
    override fun fødselsnummer(): String = fødselsnummer

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        val vedtaksperiode =
            person.vedtaksperioder().finnBehandling(spleisBehandlingId)
                ?: throw IllegalStateException("Behandling med spleisBehandlingId=$spleisBehandlingId finnes ikke")
        val behandling = vedtaksperiode.finnBehandling(spleisBehandlingId)
        behandling.håndterVedtakFattet()
        val vedtaksperiodeMelding = lagMelding(person, behandling, vedtaksperiode)
        person.meldingslogg.nyMelding(vedtaksperiodeMelding)
    }

    private fun lagMelding(
        person: Person,
        behandling: LegacyBehandling,
        vedtaksperiode: Vedtaksperiode,
    ): UtgåendeHendelse {
        val tag6gBegrenset = "6GBegrenset"
        return if (yrkesaktivitetstype.lowercase() == "selvstendig") {
            SykepengevedtakSelvstendigNæringsdrivendeDto(
                fødselsnummer = person.fødselsnummer,
                vedtaksperiodeId = behandling.vedtaksperiodeId(),
                sykepengegrunnlag = BigDecimal(sykepengegrunnlag.toString()),
                sykepengegrunnlagsfakta =
                    (sykepengegrunnlagsfakta as Sykepengegrunnlagsfakta.Spleis)
                        .let { fakta ->
                            SykepengevedtakSelvstendigNæringsdrivendeDto.Sykepengegrunnlagsfakta(
                                beregningsgrunnlag =
                                    BigDecimal(
                                        fakta.arbeidsgivere
                                            .single { it.organisasjonsnummer.lowercase() == "selvstendig" }
                                            .omregnetÅrsinntekt
                                            .toString(),
                                    ),
                                pensjonsgivendeInntekter = emptyList(),
                                erBegrensetTil6G = tag6gBegrenset in behandling.tags,
                                `6G` = BigDecimal(fakta.seksG.toString()),
                            )
                        },
                fom = behandling.fom(),
                tom = behandling.tom(),
                skjæringstidspunkt = behandling.skjæringstidspunkt(),
                hendelser = hendelser,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                utbetalingId = behandling.utbetalingId(),
                vedtakBegrunnelse = behandling.vedtakBegrunnelse,
            )
        } else {
            if (behandling.tags.isEmpty()) {
                sikkerlogg.error(
                    "Ingen tags funnet for spleisBehandlingId: ${behandling.spleisBehandlingId} på vedtaksperiodeId: ${behandling.vedtaksperiodeId}",
                )
            }
            when (val sykepengegrunnlagsfakta = sykepengegrunnlagsfakta) {
                is Sykepengegrunnlagsfakta.Infotrygd -> {
                    VedtakMedOpphavIInfotrygd(
                        fødselsnummer = person.fødselsnummer,
                        aktørId = person.aktørId,
                        organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                        vedtaksperiodeId = behandling.vedtaksperiodeId,
                        spleisBehandlingId = behandling.behandlingId(),
                        utbetalingId = behandling.utbetalingId(),
                        fom = behandling.fom(),
                        tom = behandling.tom(),
                        skjæringstidspunkt = behandling.skjæringstidspunkt,
                        hendelser = hendelser,
                        sykepengegrunnlag = sykepengegrunnlag,
                        sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                        vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                        tags = behandling.tags.toSet(),
                        vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                    )
                }

                is Sykepengegrunnlagsfakta.Spleis -> {
                    val avviksvurdering =
                        person.avviksvurderinger.finnRiktigAvviksvurdering(behandling.skjæringstidspunkt())
                            ?: error("Fant ikke avviksvurdering for vedtak")

                    val (tagsForSykepengegrunnlagsfakta, tagsForPeriode) = behandling.tags.partition { it == tag6gBegrenset }
                    sykepengegrunnlagsfakta.leggTilTags(tagsForSykepengegrunnlagsfakta.toSet())
                    when (sykepengegrunnlagsfakta) {
                        is Sykepengegrunnlagsfakta.Spleis.EtterHovedregel -> {
                            Vedtak(
                                fødselsnummer = person.fødselsnummer,
                                aktørId = person.aktørId,
                                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                                vedtaksperiodeId = behandling.vedtaksperiodeId,
                                spleisBehandlingId = behandling.behandlingId(),
                                utbetalingId = behandling.utbetalingId(),
                                fom = behandling.fom(),
                                tom = behandling.tom(),
                                skjæringstidspunkt = behandling.skjæringstidspunkt,
                                hendelser = hendelser,
                                sykepengegrunnlag = sykepengegrunnlag,
                                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                                tags = tagsForPeriode.toSet(),
                                vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                                avviksprosent = avviksvurdering.avviksprosent,
                                sammenligningsgrunnlag = avviksvurdering.sammenligningsgrunnlag,
                            )
                        }

                        is Sykepengegrunnlagsfakta.Spleis.EtterSkjønn -> {
                            VedtakMedSkjønnsvurdering(
                                fødselsnummer = person.fødselsnummer,
                                aktørId = person.aktørId,
                                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                                vedtaksperiodeId = behandling.vedtaksperiodeId,
                                spleisBehandlingId = behandling.behandlingId(),
                                utbetalingId = behandling.utbetalingId(),
                                fom = behandling.fom(),
                                tom = behandling.tom(),
                                skjæringstidspunkt = behandling.skjæringstidspunkt,
                                hendelser = hendelser,
                                sykepengegrunnlag = sykepengegrunnlag,
                                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                                skjønnsfastsettingopplysninger =
                                    person.skjønnsfastsatteSykepengegrunnlag
                                        .relevanteFor(
                                            skjæringstidspunkt = behandling.skjæringstidspunkt(),
                                        ).lastOrNull()
                                        ?.let {
                                            Skjønnsfastsettingopplysninger(
                                                begrunnelseFraMal = it.begrunnelseFraMal,
                                                begrunnelseFraFritekst = it.begrunnelseFraFritekst,
                                                begrunnelseFraKonklusjon = it.begrunnelseFraKonklusjon,
                                                skjønnsfastsettingtype = it.type,
                                                skjønnsfastsettingsårsak = it.årsak,
                                            )
                                        }
                                        ?: error("Forventer å finne opplysninger fra saksbehandler ved bygging av vedtak når sykepengegrunnlaget er fastsatt etter skjønn"),
                                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                                tags = tagsForPeriode.toSet(),
                                vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                                avviksprosent = avviksvurdering.avviksprosent,
                                sammenligningsgrunnlag = avviksvurdering.sammenligningsgrunnlag,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun toJson(): String = json
}
