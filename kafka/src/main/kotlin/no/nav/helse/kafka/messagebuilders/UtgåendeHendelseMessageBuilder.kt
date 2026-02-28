package no.nav.helse.kafka.messagebuilders

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.modell.melding.AnnullertUtbetalingEvent
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.HentDokument
import no.nav.helse.modell.melding.InntektsendringerEvent
import no.nav.helse.modell.melding.KlargjørPersonForVisning
import no.nav.helse.modell.melding.LagtPåVentEvent
import no.nav.helse.modell.melding.MinimumSykdomsgradVurdertEvent
import no.nav.helse.modell.melding.OppdaterPersondata
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.melding.OppgaveOpprettet
import no.nav.helse.modell.melding.OverstyrtArbeidsforholdEvent
import no.nav.helse.modell.melding.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.modell.melding.OverstyrtTidslinjeEvent
import no.nav.helse.modell.melding.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.melding.VedtakFattetMelding
import no.nav.helse.modell.melding.VedtaksperiodeAvvistAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeAvvistManuelt
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentManuelt
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.AvventerSaksbehandler
import no.nav.helse.modell.oppgave.Oppgave.AvventerSystem
import no.nav.helse.modell.oppgave.Oppgave.Ferdigstilt
import no.nav.helse.modell.oppgave.Oppgave.Invalidert
import no.nav.helse.spesialist.kafka.objectMapper
import java.time.LocalDateTime
import java.util.UUID

private const val AUTOMATISK_BEHANDLET_IDENT = "Automatisk behandlet"
private const val AUTOMATISK_BEHANDLET_EPOSTADRESSE = "tbd@nav.no"

fun UtgåendeHendelse.somJsonMessage(fødselsnummer: String): JsonMessage = JsonMessage.newMessage(eventName(), mapOf("fødselsnummer" to fødselsnummer) + detaljer())

internal fun UtgåendeHendelse.eventName() =
    when (this) {
        is VedtaksperiodeAvvistAutomatisk,
        is VedtaksperiodeAvvistManuelt,
        -> "vedtaksperiode_avvist"

        is VedtaksperiodeGodkjentAutomatisk,
        is VedtaksperiodeGodkjentManuelt,
        -> "vedtaksperiode_godkjent"

        is Godkjenningsbehovløsning -> "behov"

        is VedtakFattetMelding -> "vedtak_fattet"

        is KlargjørPersonForVisning -> "klargjør_person_for_visning"

        is OppdaterPersondata -> "oppdater_persondata"

        is HentDokument -> "hent-dokument"

        is OppgaveOpprettet -> "oppgave_opprettet"

        is OppgaveOppdatert -> "oppgave_oppdatert"

        is AnnullertUtbetalingEvent -> "annullering"

        is LagtPåVentEvent -> "lagt_på_vent"

        is MinimumSykdomsgradVurdertEvent -> "minimum_sykdomsgrad_vurdert"

        is OverstyrtArbeidsforholdEvent -> "overstyr_arbeidsforhold"

        is OverstyrtInntektOgRefusjonEvent -> "overstyr_inntekt_og_refusjon"

        is OverstyrtTidslinjeEvent -> "overstyr_tidslinje"

        is SkjønnsfastsattSykepengegrunnlagEvent -> "skjønnsmessig_fastsettelse"

        is VarselEndret -> "varsel_endret"

        is InntektsendringerEvent -> "inntektsendringer"
    }

private fun UtgåendeHendelse.detaljer(): Map<String, Any> =
    when (this) {
        is VedtaksperiodeAvvistManuelt -> this.detaljer()
        is VedtaksperiodeAvvistAutomatisk -> this.detaljer()
        is VedtaksperiodeGodkjentManuelt -> this.detaljer()
        is VedtaksperiodeGodkjentAutomatisk -> this.detaljer()
        is Godkjenningsbehovløsning -> this.detaljer()
        is VedtakFattetMelding -> this.detaljer()
        is KlargjørPersonForVisning -> emptyMap()
        is OppdaterPersondata -> emptyMap()
        is HentDokument -> this.detaljer()
        is OppgaveOpprettet -> this.detaljer()
        is OppgaveOppdatert -> this.detaljer()
        is AnnullertUtbetalingEvent -> this.detaljer()
        is LagtPåVentEvent -> this.detaljer()
        is MinimumSykdomsgradVurdertEvent -> this.detaljer()
        is OverstyrtArbeidsforholdEvent -> this.detaljer()
        is OverstyrtInntektOgRefusjonEvent -> this.detaljer()
        is OverstyrtTidslinjeEvent -> this.detaljer()
        is SkjønnsfastsattSykepengegrunnlagEvent -> this.detaljer()
        is VarselEndret -> this.detaljer()
        is InntektsendringerEvent -> this.detaljer()
    }

private fun Godkjenningsbehovløsning.detaljer(): Map<String, Any> {
    val orginaltBehov = objectMapper.readValue<Map<String, Any>>(this.json)
    val løsning =
        mapOf(
            "@løsning" to
                mapOf(
                    "Godkjenning" to
                        buildMap {
                            put("godkjent", godkjent)
                            put("saksbehandlerIdent", saksbehandlerIdent)
                            put("saksbehandlerEpost", saksbehandlerEpost)
                            put("godkjenttidspunkt", godkjenttidspunkt)
                            put("automatiskBehandling", automatiskBehandling)
                            årsak?.let { put("årsak", it) }
                            begrunnelser?.let { put("begrunnelser", it) }
                            kommentar?.let { put("kommentar", it) }
                            put("saksbehandleroverstyringer", saksbehandleroverstyringer)
                        },
                ),
        )
    return orginaltBehov +
        løsning +
        mapOf("@id" to UUID.randomUUID(), "@opprettet" to LocalDateTime.now())
}

private fun VedtaksperiodeAvvistAutomatisk.detaljer(): Map<String, Any> =
    buildMap {
        put("fødselsnummer", fødselsnummer)
        put("vedtaksperiodeId", vedtaksperiodeId)
        put("saksbehandlerIdent", AUTOMATISK_BEHANDLET_IDENT)
        put("saksbehandlerEpost", AUTOMATISK_BEHANDLET_EPOSTADRESSE)
        put(
            "saksbehandler",
            mapOf(
                "ident" to AUTOMATISK_BEHANDLET_IDENT,
                "epostadresse" to AUTOMATISK_BEHANDLET_EPOSTADRESSE,
            ),
        )
        put("automatiskBehandling", true)
        årsak?.let { put("årsak", it) }
        begrunnelser?.let { put("begrunnelser", it) }
        put("periodetype", periodetype)
        put("behandlingId", behandlingId)
        put("yrkesaktivitetstype", yrkesaktivitetstype)
    }

private fun VedtaksperiodeAvvistManuelt.detaljer(): Map<String, Any> =
    buildMap {
        put("fødselsnummer", fødselsnummer)
        put("vedtaksperiodeId", vedtaksperiodeId)
        put("saksbehandlerIdent", saksbehandlerIdent)
        put("saksbehandlerEpost", saksbehandlerEpost)
        put(
            "saksbehandler",
            mapOf(
                "ident" to saksbehandlerIdent,
                "epostadresse" to saksbehandlerEpost,
            ),
        )
        put("automatiskBehandling", false)
        årsak?.let { put("årsak", it) }
        begrunnelser?.let { put("begrunnelser", it) }
        kommentar?.let { put("kommentar", it) }
        put("periodetype", periodetype)
        put("behandlingId", behandlingId)
        put("yrkesaktivitetstype", yrkesaktivitetstype)
    }

private fun VedtaksperiodeGodkjentAutomatisk.detaljer(): Map<String, Any> =
    mapOf(
        "fødselsnummer" to fødselsnummer,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "periodetype" to periodetype,
        "saksbehandlerIdent" to AUTOMATISK_BEHANDLET_IDENT,
        "saksbehandlerEpost" to AUTOMATISK_BEHANDLET_EPOSTADRESSE,
        "automatiskBehandling" to true,
        "saksbehandler" to
            mapOf(
                "ident" to AUTOMATISK_BEHANDLET_IDENT,
                "epostadresse" to AUTOMATISK_BEHANDLET_EPOSTADRESSE,
            ),
        "behandlingId" to behandlingId,
        "yrkesaktivitetstype" to yrkesaktivitetstype,
    )

private fun VedtaksperiodeGodkjentManuelt.detaljer(): Map<String, Any> =
    buildMap {
        put("fødselsnummer", fødselsnummer)
        put("vedtaksperiodeId", vedtaksperiodeId)
        put("periodetype", periodetype)
        put("saksbehandlerIdent", saksbehandlerIdent)
        put("saksbehandlerEpost", saksbehandlerEpost)
        put("automatiskBehandling", false)
        put(
            "saksbehandler",
            mapOf(
                "ident" to saksbehandlerIdent,
                "epostadresse" to saksbehandlerEpost,
            ),
        )
        if (beslutterEpost != null && beslutterIdent != null) {
            put(
                "beslutter",
                mapOf(
                    "ident" to beslutterIdent,
                    "epostadresse" to beslutterEpost,
                ),
            )
        }
        put("behandlingId", behandlingId)
        put("yrkesaktivitetstype", yrkesaktivitetstype)
    }

private fun HentDokument.detaljer(): Map<String, Any> =
    mapOf(
        "dokumentId" to dokumentId,
        "dokumentType" to dokumentType,
    )

private fun OppgaveOpprettet.detaljer(): Map<String, Any> = oppgave.toDetaljer()

private fun OppgaveOppdatert.detaljer(): Map<String, Any> = oppgave.toDetaljer()

private fun Oppgave.toDetaljer(): Map<String, Any> =
    listOfNotNull(
        "@forårsaket_av" to mapOf("id" to godkjenningsbehovId),
        "hendelseId" to godkjenningsbehovId,
        "oppgaveId" to id,
        "tilstand" to
            when (tilstand) {
                AvventerSaksbehandler -> "AvventerSaksbehandler"
                AvventerSystem -> "AvventerSystem"
                Ferdigstilt -> "Ferdigstilt"
                Invalidert -> "Invalidert"
            },
        "egenskaper" to
            egenskaper.map {
                when (it) {
                    Egenskap.SØKNAD -> "SØKNAD"
                    Egenskap.STIKKPRØVE -> "STIKKPRØVE"
                    Egenskap.RISK_QA -> "RISK_QA"
                    Egenskap.REVURDERING -> "REVURDERING"
                    Egenskap.FORTROLIG_ADRESSE -> "FORTROLIG_ADRESSE"
                    Egenskap.STRENGT_FORTROLIG_ADRESSE -> "STRENGT_FORTROLIG_ADRESSE"
                    Egenskap.UTBETALING_TIL_SYKMELDT -> "UTBETALING_TIL_SYKMELDT"
                    Egenskap.DELVIS_REFUSJON -> "DELVIS_REFUSJON"
                    Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> "UTBETALING_TIL_ARBEIDSGIVER"
                    Egenskap.INGEN_UTBETALING -> "INGEN_UTBETALING"
                    Egenskap.EGEN_ANSATT -> "EGEN_ANSATT"
                    Egenskap.EN_ARBEIDSGIVER -> "EN_ARBEIDSGIVER"
                    Egenskap.FLERE_ARBEIDSGIVERE -> "FLERE_ARBEIDSGIVERE"
                    Egenskap.UTLAND -> "UTLAND"
                    Egenskap.HASTER -> "HASTER"
                    Egenskap.BESLUTTER -> "BESLUTTER"
                    Egenskap.RETUR -> "RETUR"
                    Egenskap.VERGEMÅL -> "VERGEMÅL"
                    Egenskap.SPESIALSAK -> "SPESIALSAK"
                    Egenskap.FORLENGELSE -> "FORLENGELSE"
                    Egenskap.FORSTEGANGSBEHANDLING -> "FORSTEGANGSBEHANDLING"
                    Egenskap.INFOTRYGDFORLENGELSE -> "INFOTRYGDFORLENGELSE"
                    Egenskap.OVERGANG_FRA_IT -> "OVERGANG_FRA_IT"
                    Egenskap.SKJØNNSFASTSETTELSE -> "SKJØNNSFASTSETTELSE"
                    Egenskap.PÅ_VENT -> "PÅ_VENT"
                    Egenskap.TILBAKEDATERT -> "TILBAKEDATERT"
                    Egenskap.GOSYS -> "GOSYS"
                    Egenskap.MANGLER_IM -> "MANGLER_IM"
                    Egenskap.MEDLEMSKAP -> "MEDLEMSKAP"
                    Egenskap.GRUNNBELØPSREGULERING -> "GRUNNBELØPSREGULERING"
                    Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE -> "SELVSTENDIG_NÆRINGSDRIVENDE"
                    Egenskap.ARBEIDSTAKER -> "ARBEIDSTAKER"
                    Egenskap.JORDBRUKER_REINDRIFT -> "JORDBRUKER_REINDRIFT"
                }
            },
        "behandlingId" to behandlingId,
        tildeltTil?.let {
            "saksbehandler" to it.value
        },
    ).toMap()

private fun AnnullertUtbetalingEvent.detaljer(): Map<String, Any> =
    listOfNotNull(
        "organisasjonsnummer" to organisasjonsnummer,
        ("yrkesaktivitetstype" to "SELVSTENDIG").takeIf { organisasjonsnummer == "SELVSTENDIG" },
        "saksbehandler" to
            mapOf(
                "epostaddresse" to saksbehandlerEpost,
                "oid" to saksbehandlerOid,
                "ident" to saksbehandlerIdent,
            ),
        "begrunnelser" to begrunnelser,
        "vedtaksperiodeId" to vedtaksperiodeId,
        kommentar?.let { "kommentar" to it },
        arsaker?.let { "arsaker" to it.map { arsak -> mapOf("arsak" to arsak.arsak, "key" to arsak.key) } },
    ).toMap()

private fun LagtPåVentEvent.detaljer(): Map<String, Any> =
    listOfNotNull(
        "oppgaveId" to oppgaveId,
        "behandlingId" to behandlingId,
        "skalTildeles" to skalTildeles,
        "frist" to frist,
        "saksbehandlerOid" to saksbehandlerOid,
        "saksbehandlerIdent" to saksbehandlerIdent,
        "årsaker" to årsaker.map { mapOf("årsak" to it.årsak, "key" to it.key) },
        notatTekst?.let { "notatTekst" to it },
    ).toMap()

private fun MinimumSykdomsgradVurdertEvent.detaljer(): Map<String, Any> =
    mapOf(
        "@id" to eksternHendelseId,
        "aktørId" to aktørId,
        "perioderMedMinimumSykdomsgradVurdertOk" to perioderMedMinimumSykdomsgradVurdertOk,
        "perioderMedMinimumSykdomsgradVurdertIkkeOk" to perioderMedMinimumSykdomsgradVurdertIkkeOk,
        "saksbehandlerOid" to saksbehandlerOid,
        "saksbehandlerNavn" to saksbehandlerNavn,
        "saksbehandlerIdent" to saksbehandlerIdent,
        "saksbehandlerEpost" to saksbehandlerEpost,
    )

private fun OverstyrtArbeidsforholdEvent.detaljer(): Map<String, Any> =
    mapOf(
        "@id" to eksternHendelseId,
        "aktørId" to aktørId,
        "saksbehandlerOid" to saksbehandlerOid,
        "saksbehandlerNavn" to saksbehandlerNavn,
        "saksbehandlerIdent" to saksbehandlerIdent,
        "saksbehandlerEpost" to saksbehandlerEpost,
        "skjæringstidspunkt" to skjæringstidspunkt,
        "overstyrteArbeidsforhold" to overstyrteArbeidsforhold,
    )

private fun OverstyrtInntektOgRefusjonEvent.detaljer(): Map<String, Any> =
    mapOf(
        "@id" to eksternHendelseId,
        "aktørId" to aktørId,
        "skjæringstidspunkt" to skjæringstidspunkt,
        "arbeidsgivere" to arbeidsgivere,
        "saksbehandlerOid" to saksbehandlerOid,
        "saksbehandlerNavn" to saksbehandlerNavn,
        "saksbehandlerIdent" to saksbehandlerIdent,
        "saksbehandlerEpost" to saksbehandlerEpost,
    )

private fun OverstyrtTidslinjeEvent.detaljer(): Map<String, Any> =
    mapOf(
        "@id" to eksternHendelseId,
        "aktørId" to aktørId,
        "organisasjonsnummer" to organisasjonsnummer,
        "dager" to dager,
    ).let { if (organisasjonsnummer.uppercase() == "SELVSTENDIG") it + mapOf("yrkesaktivitetstype" to "SELVSTENDIG") else it }

private fun SkjønnsfastsattSykepengegrunnlagEvent.detaljer(): Map<String, Any> =
    mapOf(
        "@id" to eksternHendelseId,
        "aktørId" to aktørId,
        "skjæringstidspunkt" to skjæringstidspunkt,
        "arbeidsgivere" to arbeidsgivere,
        "saksbehandlerOid" to saksbehandlerOid,
        "saksbehandlerNavn" to saksbehandlerNavn,
        "saksbehandlerIdent" to saksbehandlerIdent,
        "saksbehandlerEpost" to saksbehandlerEpost,
    )

private fun VarselEndret.detaljer(): Map<String, Any> =
    mapOf(
        "vedtaksperiode_id" to vedtaksperiodeId,
        "behandling_id" to behandlingIdForBehandlingSomBleGodkjent,
        "varsel_id" to varselId,
        "varseltittel" to varseltittel,
        "varselkode" to varselkode,
        "forrige_status" to forrigeStatus,
        "gjeldende_status" to gjeldendeStatus,
    )

private fun InntektsendringerEvent.detaljer(): Map<String, Any> =
    buildMap {
        put(
            "inntektsendringer",
            this@detaljer.inntektskilder.map { inntektskilde ->
                mapOf(
                    "inntektskilde" to inntektskilde.inntektskilde,
                    "inntekter" to
                        inntektskilde.inntekter.map { inntekt ->
                            mapOf(
                                "fom" to inntekt.fom,
                                "tom" to inntekt.tom,
                                "dagsbeløp" to inntekt.dagsbeløp,
                            )
                        },
                    "nullstill" to
                        inntektskilde.nullstill.map { nullstill ->
                            mapOf(
                                "fom" to nullstill.fom,
                                "tom" to nullstill.tom,
                            )
                        },
                )
            },
        )
    }
