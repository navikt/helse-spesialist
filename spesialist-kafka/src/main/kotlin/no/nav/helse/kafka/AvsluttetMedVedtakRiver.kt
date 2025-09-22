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
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.VedtakFattetMelding
import no.nav.helse.modell.melding.VedtakFattetMelding.SelvstendigNæringsdrivendeSykepengegrunnlagsfakta.PensjonsgivendeInntekt
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode.Companion.finnBehandling
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.Skjønnsfastsettingstype
import no.nav.helse.modell.vedtak.Skjønnsfastsettingsårsak
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import java.math.BigDecimal
import java.util.UUID

class AvsluttetMedVedtakRiver(
    private val sessionFactory: SessionFactory,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "avsluttet_med_vedtak")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey(
                "@id",
                "fødselsnummer",
                "yrkesaktivitetstype",
                "vedtaksperiodeId",
                "behandlingId",
                "vedtakFattetTidspunkt",
                "hendelser",
                "sykepengegrunnlag",
                "sykepengegrunnlagsfakta",
            )
            it.requireArray("hendelser")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        withMDC(
            buildMap {
                put("meldingId", packet["@id"].asText())
                put("meldingnavn", MELDINGNAVN)
                put("vedtaksperiodeId", packet["vedtaksperiodeId"].asText())
            },
        ) {
            logg.info("Melding $MELDINGNAVN mottatt")
            val meldingJson = packet.toJson()
            sikkerlogg.info("Melding $MELDINGNAVN mottatt:\n$meldingJson")

            try {
                val meldingPubliserer = MessageContextMeldingPubliserer(context)
                val outbox = mutableListOf<UtgåendeHendelse>()

                sessionFactory.transactionalSessionScope { sessionContext ->
                    sessionContext.meldingDao.lagre(
                        id = packet["@id"].asUUID(),
                        json = meldingJson,
                        meldingtype = MeldingDao.Meldingtype.AVSLUTTET_MED_VEDTAK,
                        vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
                    )
                }
                sessionFactory.transactionalSessionScope { sessionContext ->
                    sessionContext.personRepository.brukPersonHvisFinnes(packet["fødselsnummer"].asText()) {
                        logg.info("Personen finnes i databasen, behandler melding $MELDINGNAVN")
                        sikkerlogg.info("Personen finnes i databasen, behandler melding $MELDINGNAVN")

                        val spleisBehandlingId = packet["behandlingId"].asUUID()
                        val vedtaksperiode =
                            vedtaksperioder().finnBehandling(spleisBehandlingId)
                                ?: error("Behandling med spleisBehandlingId=$spleisBehandlingId finnes ikke")
                        val behandling = vedtaksperiode.finnBehandling(spleisBehandlingId)

                        if (behandling.tags.isEmpty()) {
                            sikkerlogg.error(
                                "Ingen tags funnet for spleisBehandlingId: ${behandling.spleisBehandlingId} på vedtaksperiodeId: ${behandling.vedtaksperiodeId}",
                            )
                        }

                        val sisteGodkjenningsbehov: Godkjenningsbehov? =
                            sessionContext.meldingDao.finnSisteGodkjenningsbehov(spleisBehandlingId)
                        if (sisteGodkjenningsbehov == null) {
                            logg.warn("Finner ikke godkjenningsbehov for spleis behandlingid $spleisBehandlingId")
                        }

                        behandling.håndterVedtakFattet()

                        outbox.add(
                            byggVedtaksmelding(
                                packet = packet,
                                behandling = behandling,
                                vedtaksperiode = vedtaksperiode,
                                godkjenningsbehov = sisteGodkjenningsbehov,
                            ),
                        )
                    }
                }
                outbox.forEach { utgåendeHendelse ->
                    meldingPubliserer.publiser(
                        fødselsnummer = packet["fødselsnummer"].asText(),
                        hendelse = utgåendeHendelse,
                        årsak = MELDINGNAVN,
                    )
                }
            } finally {
                logg.info("Melding $MELDINGNAVN lest")
                sikkerlogg.info("Melding $MELDINGNAVN lest")
            }
        }
    }

    private fun Person.byggVedtaksmelding(
        packet: JsonMessage,
        behandling: LegacyBehandling,
        vedtaksperiode: Vedtaksperiode,
        godkjenningsbehov: Godkjenningsbehov?,
    ): UtgåendeHendelse {
        val fastsatt = packet["sykepengegrunnlagsfakta"]["fastsatt"].asText()

        return VedtakFattetMelding(
            fødselsnummer = packet["fødselsnummer"].asText(),
            aktørId = aktørId,
            organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
            yrkesaktivitetstype = packet["yrkesaktivitetstype"].asText(),
            vedtaksperiodeId = behandling.vedtaksperiodeId,
            behandlingId = behandling.behandlingId(),
            utbetalingId = behandling.utbetalingId(),
            fom = behandling.fom(),
            tom = behandling.tom(),
            skjæringstidspunkt = behandling.skjæringstidspunkt,
            hendelser = packet["hendelser"].map<JsonNode, UUID> { it.asUUID() },
            sykepengegrunnlag = packet["sykepengegrunnlag"].asBigDecimal(),
            vedtakFattetTidspunkt = packet["vedtakFattetTidspunkt"].asLocalDateTime(),
            tags =
                (
                    if (fastsatt == FASTSATT_I_INFOTRYGD) {
                        behandling.tags
                    } else {
                        behandling.tags.filterNot { it == TAG_6G_BEGRENSET }
                    }
                ).toSet(),
            begrunnelser =
                if (fastsatt == FASTSATT_ETTER_SKJØNN) {
                    val skjønnsfastsattSykepengegrunnlag =
                        skjønnsfastsatteSykepengegrunnlag.lastOrNull { it.skjæringstidspunkt == behandling.skjæringstidspunkt() }
                            ?: error("Forventer å finne opplysninger fra saksbehandler ved bygging av vedtak når sykepengegrunnlaget er fastsatt etter skjønn")
                    listOf(
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagMal,
                            begrunnelse = skjønnsfastsattSykepengegrunnlag.begrunnelseFraMal,
                        ),
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagFritekst,
                            begrunnelse = skjønnsfastsattSykepengegrunnlag.begrunnelseFraFritekst,
                        ),
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagKonklusjon,
                            begrunnelse = skjønnsfastsattSykepengegrunnlag.begrunnelseFraKonklusjon,
                        ),
                    )
                } else {
                    emptyList()
                } + listOfNotNull(behandling.vedtakBegrunnelse?.toBegrunnelse()),
            sykepengegrunnlagsfakta =
                if (packet["yrkesaktivitetstype"].asText() == YRKESAKTIVITETSTYPE_SELVSTENDIG_NÆRINGSDRIVENDE) {
                    if (fastsatt != FASTSATT_ETTER_HOVEDREGEL) {
                        error(
                            "Ustøttet verdi sykepengegrunnlagsfakta.fastsatt for selvstendig næringsdrivende: \"$fastsatt\"." +
                                " Kun \"${FASTSATT_ETTER_HOVEDREGEL}\" støttes.",
                        )
                    }
                    byggSelvstendigNæringsdrivendeSykepengegrunnlagsfakta(
                        packet = packet,
                        tags = behandling.tags,
                        sykepengegrunnlagsfakta = godkjenningsbehov!!.sykepengegrunnlagsfakta as Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.SelvstendigNæringsdrivende,
                    )
                } else {
                    when (fastsatt) {
                        FASTSATT_I_INFOTRYGD ->
                            byggFastsattIInfotrygdSykepengegrunnlagsfakta(packet = packet)

                        FASTSATT_ETTER_HOVEDREGEL ->
                            byggFastsattEtterHovedregelSykepengegrunnlagsfakta(
                                packet = packet,
                                tags = behandling.tags,
                                avviksvurdering = finnAvviksvurdering(behandling),
                            )

                        FASTSATT_ETTER_SKJØNN -> {
                            val skjønnsfastsattSykepengegrunnlag =
                                skjønnsfastsatteSykepengegrunnlag.lastOrNull { it.skjæringstidspunkt == behandling.skjæringstidspunkt() }
                                    ?: error("Forventer å finne opplysninger fra saksbehandler ved bygging av vedtak når sykepengegrunnlaget er fastsatt etter skjønn")
                            byggFastsattEtterSkjønnSykepengegrunnlagsfakta(
                                packet = packet,
                                tags = behandling.tags,
                                skjønnsfastsattSykepengegrunnlag = skjønnsfastsattSykepengegrunnlag,
                                avviksvurdering = finnAvviksvurdering(behandling),
                            )
                        }

                        else -> error("Ukjent verdi for sykepengegrunnlagsfakta.fastsatt: \"$fastsatt\"")
                    }
                },
        )
    }

    private fun byggSelvstendigNæringsdrivendeSykepengegrunnlagsfakta(
        packet: JsonMessage,
        tags: List<String>,
        sykepengegrunnlagsfakta: Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.SelvstendigNæringsdrivende,
    ) = VedtakFattetMelding.SelvstendigNæringsdrivendeSykepengegrunnlagsfakta(
        beregningsgrunnlag =
            BigDecimal(
                packet["sykepengegrunnlagsfakta"]["arbeidsgivere"]
                    .single { it["arbeidsgiver"].asText() == YRKESAKTIVITETSTYPE_SELVSTENDIG_NÆRINGSDRIVENDE }
                    ["omregnetÅrsinntekt"]
                    .asText(),
            ),
        tags = tags.filter { it == TAG_6G_BEGRENSET }.toSet(),
        seksG = BigDecimal(packet["sykepengegrunnlagsfakta"]["6G"].asText()),
        pensjonsgivendeInntekter =
            sykepengegrunnlagsfakta.selvstendig.pensjonsgivendeInntekter.map { inntekt ->
                PensjonsgivendeInntekt(
                    årstall = inntekt.årstall,
                    beløp = inntekt.beløp,
                )
            },
    )

    private fun byggFastsattEtterHovedregelSykepengegrunnlagsfakta(
        packet: JsonMessage,
        avviksvurdering: Avviksvurdering,
        tags: List<String>,
    ) = VedtakFattetMelding.FastsattEtterHovedregelSykepengegrunnlagsfakta(
        omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta"]["omregnetÅrsinntektTotalt"].asBigDecimal(),
        seksG = packet["sykepengegrunnlagsfakta"]["6G"].asBigDecimal(),
        tags = tags.filter { it == TAG_6G_BEGRENSET }.toSet(),
        avviksprosent = avviksvurdering.avviksprosent.toBigDecimal(),
        innrapportertÅrsinntekt = avviksvurdering.sammenligningsgrunnlag.totalbeløp.toBigDecimal(),
        arbeidsgivere =
            packet["sykepengegrunnlagsfakta"]["arbeidsgivere"].map { arbeidsgiver ->
                val arbeidsgiverreferanse = arbeidsgiver["arbeidsgiver"].asText()
                VedtakFattetMelding.FastsattEtterHovedregelSykepengegrunnlagsfakta.Arbeidsgiver(
                    organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText(),
                    omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asBigDecimal(),
                    innrapportertÅrsinntekt =
                        avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter
                            .filter { it.arbeidsgiverreferanse == arbeidsgiverreferanse }
                            .flatMap(InnrapportertInntekt::inntekter)
                            .sumOf(Inntekt::beløp)
                            .toBigDecimal(),
                )
            },
    )

    private fun byggFastsattEtterSkjønnSykepengegrunnlagsfakta(
        packet: JsonMessage,
        skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlag,
        avviksvurdering: Avviksvurdering,
        tags: List<String>,
    ) = VedtakFattetMelding.FastsattEtterSkjønnSykepengegrunnlagsfakta(
        omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta"]["omregnetÅrsinntektTotalt"].asBigDecimal(),
        seksG = packet["sykepengegrunnlagsfakta"]["6G"].asBigDecimal(),
        avviksprosent = avviksvurdering.avviksprosent.toBigDecimal(),
        innrapportertÅrsinntekt = avviksvurdering.sammenligningsgrunnlag.totalbeløp.toBigDecimal(),
        tags = tags.filter { it == TAG_6G_BEGRENSET }.toSet(),
        skjønnsfastsatt = packet["sykepengegrunnlagsfakta"]["skjønnsfastsatt"].asBigDecimal(),
        skjønnsfastsettingtype =
            when (skjønnsfastsattSykepengegrunnlag.type) {
                Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT -> VedtakFattetMelding.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
                Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT -> VedtakFattetMelding.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
                Skjønnsfastsettingstype.ANNET -> VedtakFattetMelding.Skjønnsfastsettingstype.ANNET
            },
        skjønnsfastsettingsårsak =
            when (skjønnsfastsattSykepengegrunnlag.årsak) {
                Skjønnsfastsettingsårsak.ANDRE_AVSNITT -> VedtakFattetMelding.Skjønnsfastsettingsårsak.ANDRE_AVSNITT
                Skjønnsfastsettingsårsak.TREDJE_AVSNITT -> VedtakFattetMelding.Skjønnsfastsettingsårsak.TREDJE_AVSNITT
            },
        arbeidsgivere =
            packet["sykepengegrunnlagsfakta"]["arbeidsgivere"].map { arbeidsgiver ->
                val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                VedtakFattetMelding.FastsattEtterSkjønnSykepengegrunnlagsfakta.Arbeidsgiver(
                    organisasjonsnummer = organisasjonsnummer,
                    omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asBigDecimal(),
                    innrapportertÅrsinntekt =
                        avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter
                            .filter { it.arbeidsgiverreferanse == organisasjonsnummer }
                            .flatMap(InnrapportertInntekt::inntekter)
                            .sumOf(Inntekt::beløp)
                            .toBigDecimal(),
                    skjønnsfastsatt = arbeidsgiver["skjønnsfastsatt"].asBigDecimal(),
                )
            },
    )

    private fun byggFastsattIInfotrygdSykepengegrunnlagsfakta(packet: JsonMessage) =
        VedtakFattetMelding.FastsattIInfotrygdSykepengegrunnlagsfakta(
            omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta"]["omregnetÅrsinntektTotalt"].asBigDecimal(),
        )

    private fun Person.finnAvviksvurdering(behandling: LegacyBehandling): Avviksvurdering =
        avviksvurderinger
            .filter { it.skjæringstidspunkt == behandling.skjæringstidspunkt() }
            .maxByOrNull { it.opprettet }
            ?: error("Fant ikke avviksvurdering for vedtak")

    private fun VedtakBegrunnelse.toBegrunnelse(): VedtakFattetMelding.Begrunnelse =
        VedtakFattetMelding.Begrunnelse(
            type =
                when (utfall) {
                    Utfall.AVSLAG -> VedtakFattetMelding.BegrunnelseType.Avslag
                    Utfall.DELVIS_INNVILGELSE -> VedtakFattetMelding.BegrunnelseType.DelvisInnvilgelse
                    Utfall.INNVILGELSE -> VedtakFattetMelding.BegrunnelseType.Innvilgelse
                },
            begrunnelse = begrunnelse.orEmpty(),
        )

    companion object {
        private const val MELDINGNAVN = "AvsluttetMedVedtakMessage"

        private const val FASTSATT_ETTER_HOVEDREGEL = "EtterHovedregel"
        private const val FASTSATT_ETTER_SKJØNN = "EtterSkjønn"
        private const val FASTSATT_I_INFOTRYGD = "IInfotrygd"

        private const val YRKESAKTIVITETSTYPE_SELVSTENDIG_NÆRINGSDRIVENDE = "SELVSTENDIG"
        private const val TAG_6G_BEGRENSET = "6GBegrenset"
    }
}

private fun JsonNode.asBigDecimal(): BigDecimal = BigDecimal(asText())
