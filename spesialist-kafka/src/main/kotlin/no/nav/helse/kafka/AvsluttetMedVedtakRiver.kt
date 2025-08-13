package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.withMDC
import no.nav.helse.modell.melding.Sykepengevedtak.Vedtak
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedOpphavIInfotrygd
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedSkjønnsvurdering
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedSkjønnsvurdering.Skjønnsfastsettingopplysninger
import no.nav.helse.modell.melding.SykepengevedtakSelvstendigNæringsdrivendeDto
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode.Companion.finnBehandling
import no.nav.helse.modell.vedtak.Faktatype
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag.Companion.relevanteFor
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering.Companion.finnRiktigAvviksvurdering
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg
import java.math.BigDecimal

class AvsluttetMedVedtakRiver(
    private val sessionFactory: SessionFactory,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "avsluttet_med_vedtak")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId", "organisasjonsnummer")
            it.requireKey("fom", "tom", "skjæringstidspunkt")
            it.requireArray("hendelser")
            it.requireKey("sykepengegrunnlag")
            it.requireKey("vedtakFattetTidspunkt")
            it.requireKey("utbetalingId", "behandlingId")
            it.requireKey("yrkesaktivitetstype")

            it.requireAny(
                "sykepengegrunnlagsfakta.fastsatt",
                listOf("EtterHovedregel", "IInfotrygd", "EtterSkjønn"),
            )
            it.requireKey("sykepengegrunnlagsfakta.omregnetÅrsinntektTotalt")
            it.require("sykepengegrunnlagsfakta.fastsatt") { fastsattNode ->
                when (fastsattNode.asText()) {
                    "EtterHovedregel" -> {
                        it.requireKey(
                            "sykepengegrunnlagsfakta.6G",
                            "sykepengegrunnlagsfakta.arbeidsgivere",
                            "sykepengegrunnlagsfakta.sykepengegrunnlag",
                        )

                        it.requireArray("sykepengegrunnlagsfakta.arbeidsgivere") {
                            requireKey("arbeidsgiver", "omregnetÅrsinntekt", "inntektskilde")
                        }
                    }

                    "EtterSkjønn" -> {
                        it.requireKey(
                            "sykepengegrunnlagsfakta.6G",
                            "sykepengegrunnlagsfakta.arbeidsgivere",
                            "sykepengegrunnlagsfakta.skjønnsfastsatt",
                        )

                        it.requireArray("sykepengegrunnlagsfakta.arbeidsgivere") {
                            requireKey("arbeidsgiver", "omregnetÅrsinntekt", "skjønnsfastsatt", "inntektskilde")
                        }
                    }

                    else -> {}
                }
            }
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val meldingId = packet["@id"].asUUID()
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID()
        val spleisBehandlingId = packet["behandlingId"].asUUID()
        val meldingPubliserer = MessageContextMeldingPubliserer(context)
        val meldingnavn = "AvsluttetMedVedtakMessage"
        withMDC(
            buildMap {
                put("meldingId", meldingId.toString())
                put("meldingnavn", meldingnavn)
                put("vedtaksperiodeId", vedtaksperiodeId.toString())
            },
        ) {
            val outbox = mutableListOf<UtgåendeHendelse>()
            val json = packet.toJson()
            val fødselsnummer = packet["fødselsnummer"].asText()
            val yrkesaktivitetstype = packet["yrkesaktivitetstype"].asText()
            val vedtakFattetTidspunkt = packet["vedtakFattetTidspunkt"].asLocalDateTime()
            val hendelser = packet["hendelser"].map { it.asUUID() }
            val sykepengegrunnlag = packet["sykepengegrunnlag"].asDouble()
            val sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(packet, faktatype(packet))
            logg.info("Melding $meldingnavn mottatt")
            sikkerlogg.info("Melding $meldingnavn mottatt:\n$json")
            try {
                sessionFactory.transactionalSessionScope { sessionContext ->
                    sessionContext.meldingDao.lagre(
                        id = meldingId,
                        json = json,
                        meldingtype = MeldingDao.Meldingtype.AVSLUTTET_MED_VEDTAK,
                        vedtaksperiodeId = vedtaksperiodeId,
                    )
                    sessionContext.personRepository.brukPersonHvisFinnes(fødselsnummer) {
                        logg.info("Personen finnes i databasen, behandler melding $meldingnavn")
                        sikkerlogg.info("Personen finnes i databasen, behandler melding $meldingnavn")
                        val vedtaksperiode =
                            vedtaksperioder().finnBehandling(spleisBehandlingId)
                                ?: throw IllegalStateException("Behandling med spleisBehandlingId=$spleisBehandlingId finnes ikke")
                        val behandling = vedtaksperiode.finnBehandling(spleisBehandlingId)
                        behandling.håndterVedtakFattet()
                        val tag6gBegrenset = "6GBegrenset"
                        val vedtaksperiodeMelding =
                            if (yrkesaktivitetstype.lowercase() == "selvstendig") {
                                SykepengevedtakSelvstendigNæringsdrivendeDto(
                                    fødselsnummer = this.fødselsnummer,
                                    vedtaksperiodeId = behandling.vedtaksperiodeId(),
                                    sykepengegrunnlag = BigDecimal(sykepengegrunnlag.toString()),
                                    sykepengegrunnlagsfakta =
                                        (sykepengegrunnlagsfakta as Sykepengegrunnlagsfakta.Spleis)
                                            .let { fakta ->
                                                SykepengevedtakSelvstendigNæringsdrivendeDto.Sykepengegrunnlagsfakta(
                                                    beregningsgrunnlag =
                                                        BigDecimal(
                                                            fakta.arbeidsgivere
                                                                .single<Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver> { it.organisasjonsnummer.lowercase() == "selvstendig" }
                                                                .omregnetÅrsinntekt
                                                                .toString(),
                                                        ),
                                                    pensjonsgivendeInntekter = emptyList<SykepengevedtakSelvstendigNæringsdrivendeDto.PensjonsgivendeInntekt>(),
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
                                when (sykepengegrunnlagsfakta) {
                                    is Sykepengegrunnlagsfakta.Infotrygd -> {
                                        VedtakMedOpphavIInfotrygd(
                                            fødselsnummer = this.fødselsnummer,
                                            aktørId = aktørId,
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
                                            tags = behandling.tags.toSet<String>(),
                                            vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                                        )
                                    }

                                    is Sykepengegrunnlagsfakta.Spleis -> {
                                        val avviksvurdering =
                                            avviksvurderinger.finnRiktigAvviksvurdering(behandling.skjæringstidspunkt())
                                                ?: error("Fant ikke avviksvurdering for vedtak")

                                        val (tagsForSykepengegrunnlagsfakta, tagsForPeriode) = behandling.tags.partition { it == tag6gBegrenset }
                                        sykepengegrunnlagsfakta.leggTilTags(tagsForSykepengegrunnlagsfakta.toSet<String>())
                                        when (sykepengegrunnlagsfakta) {
                                            is Sykepengegrunnlagsfakta.Spleis.EtterHovedregel -> {
                                                Vedtak(
                                                    fødselsnummer = this.fødselsnummer,
                                                    aktørId = aktørId,
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
                                                    tags = tagsForPeriode.toSet<String>(),
                                                    vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                                                    avviksprosent = avviksvurdering.avviksprosent,
                                                    sammenligningsgrunnlag = avviksvurdering.sammenligningsgrunnlag,
                                                )
                                            }

                                            is Sykepengegrunnlagsfakta.Spleis.EtterSkjønn -> {
                                                VedtakMedSkjønnsvurdering(
                                                    fødselsnummer = this.fødselsnummer,
                                                    aktørId = aktørId,
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
                                                        skjønnsfastsatteSykepengegrunnlag
                                                            .relevanteFor(
                                                                skjæringstidspunkt = behandling.skjæringstidspunkt(),
                                                            ).lastOrNull<SkjønnsfastsattSykepengegrunnlag>()
                                                            ?.let<SkjønnsfastsattSykepengegrunnlag, Skjønnsfastsettingopplysninger> {
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
                                                    tags = tagsForPeriode.toSet<String>(),
                                                    vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                                                    avviksprosent = avviksvurdering.avviksprosent,
                                                    sammenligningsgrunnlag = avviksvurdering.sammenligningsgrunnlag,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        outbox.add(vedtaksperiodeMelding)
                    }
                }
                outbox.forEach { utgåendeHendelse ->
                    meldingPubliserer.publiser(
                        fødselsnummer = fødselsnummer,
                        hendelse = utgåendeHendelse,
                        årsak = meldingnavn,
                    )
                }
            } finally {
                logg.info("Melding $meldingnavn lest")
                sikkerlogg.info("Melding $meldingnavn lest")
            }
        }
    }

    private fun faktatype(packet: JsonMessage): Faktatype =
        when (val fastsattString = packet["sykepengegrunnlagsfakta.fastsatt"].asText()) {
            "EtterSkjønn" -> Faktatype.ETTER_SKJØNN
            "EtterHovedregel" -> Faktatype.ETTER_HOVEDREGEL
            "IInfotrygd" -> Faktatype.I_INFOTRYGD
            else -> throw IllegalArgumentException("FastsattType $fastsattString er ikke støttet")
        }

    private fun sykepengegrunnlagsfakta(
        packet: JsonMessage,
        faktatype: Faktatype,
    ): Sykepengegrunnlagsfakta {
        if (faktatype == Faktatype.I_INFOTRYGD) {
            return Sykepengegrunnlagsfakta.Infotrygd(
                omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntektTotalt"].asDouble(),
            )
        }

        return when (faktatype) {
            Faktatype.ETTER_SKJØNN ->
                Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                    omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntektTotalt"].asDouble(),
                    seksG = packet["sykepengegrunnlagsfakta.6G"].asDouble(),
                    skjønnsfastsatt = packet["sykepengegrunnlagsfakta.skjønnsfastsatt"].asDouble(),
                    arbeidsgivere =
                        packet["sykepengegrunnlagsfakta.arbeidsgivere"].map { arbeidsgiver ->
                            val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                            Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                                organisasjonsnummer = organisasjonsnummer,
                                omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                                skjønnsfastsatt = arbeidsgiver["skjønnsfastsatt"].asDouble(),
                                inntektskilde = inntektskilde(arbeidsgiver["inntektskilde"]),
                            )
                        },
                )

            Faktatype.ETTER_HOVEDREGEL ->
                Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                    omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntektTotalt"].asDouble(),
                    seksG = packet["sykepengegrunnlagsfakta.6G"].asDouble(),
                    sykepengegrunnlag = packet["sykepengegrunnlagsfakta.sykepengegrunnlag"].asDouble(),
                    arbeidsgivere =
                        packet["sykepengegrunnlagsfakta.arbeidsgivere"].map { arbeidsgiver ->
                            val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                            Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                organisasjonsnummer = organisasjonsnummer,
                                omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                                inntektskilde = inntektskilde(arbeidsgiver["inntektskilde"]),
                            )
                        },
                )

            else -> error("Her vet vi ikke hva som har skjedd. Feil i kompilatoren?")
        }
    }

    private fun inntektskilde(inntektskildeNode: JsonNode): Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde =
        when (val inntektskildeString = inntektskildeNode.asText()) {
            "Arbeidsgiver" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver
            "AOrdningen" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.AOrdningen
            "Saksbehandler" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Saksbehandler
            else -> error("$inntektskildeString er ikke en gyldig inntektskilde")
        }
}
