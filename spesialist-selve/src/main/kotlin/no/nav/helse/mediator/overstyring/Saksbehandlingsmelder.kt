package no.nav.helse.mediator.overstyring

import no.nav.helse.modell.saksbehandler.AnnullertUtbetalingEvent
import no.nav.helse.modell.saksbehandler.MinimumSykdomsgradVurdertEvent
import no.nav.helse.modell.saksbehandler.OverstyrtArbeidsforholdEvent
import no.nav.helse.modell.saksbehandler.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.modell.saksbehandler.OverstyrtTidslinjeEvent
import no.nav.helse.modell.saksbehandler.SaksbehandlerObserver
import no.nav.helse.modell.saksbehandler.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

internal class Saksbehandlingsmelder(
    private val rapidsConnection: RapidsConnection,
) : SaksbehandlerObserver {
    override fun tidslinjeOverstyrt(
        fødselsnummer: String,
        event: OverstyrtTidslinjeEvent,
    ) {
        val jsonMessage =
            JsonMessage.newMessage(
                "overstyr_tidslinje",
                mutableMapOf(
                    "@id" to event.id,
                    "fødselsnummer" to event.fødselsnummer,
                    "aktørId" to event.aktørId,
                    "organisasjonsnummer" to event.organisasjonsnummer,
                    "dager" to event.dager,
                ),
            )
        rapidsConnection.publish(fødselsnummer, jsonMessage.toJson())
    }

    override fun arbeidsforholdOverstyrt(
        fødselsnummer: String,
        event: OverstyrtArbeidsforholdEvent,
    ) {
        val jsonMessage =
            JsonMessage.newMessage(
                "overstyr_arbeidsforhold",
                mapOf(
                    "@id" to event.id,
                    "fødselsnummer" to event.fødselsnummer,
                    "aktørId" to event.aktørId,
                    "saksbehandlerOid" to event.saksbehandlerOid,
                    "saksbehandlerNavn" to event.saksbehandlerNavn,
                    "saksbehandlerIdent" to event.saksbehandlerIdent,
                    "saksbehandlerEpost" to event.saksbehandlerEpost,
                    "skjæringstidspunkt" to event.skjæringstidspunkt,
                    "overstyrteArbeidsforhold" to event.overstyrteArbeidsforhold,
                ),
            )
        rapidsConnection.publish(fødselsnummer, jsonMessage.toJson())
    }

    override fun inntektOgRefusjonOverstyrt(
        fødselsnummer: String,
        event: OverstyrtInntektOgRefusjonEvent,
    ) {
        val jsonMessage =
            JsonMessage.newMessage(
                "overstyr_inntekt_og_refusjon",
                listOfNotNull(
                    "@id" to event.id,
                    "aktørId" to event.aktørId,
                    "fødselsnummer" to event.fødselsnummer,
                    "skjæringstidspunkt" to event.skjæringstidspunkt,
                    "arbeidsgivere" to event.arbeidsgivere,
                    "saksbehandlerOid" to event.saksbehandlerOid,
                    "saksbehandlerNavn" to event.saksbehandlerNavn,
                    "saksbehandlerIdent" to event.saksbehandlerIdent,
                    "saksbehandlerEpost" to event.saksbehandlerEpost,
                ).toMap(),
            )
        rapidsConnection.publish(fødselsnummer, jsonMessage.toJson())
    }

    override fun sykepengegrunnlagSkjønnsfastsatt(
        fødselsnummer: String,
        event: SkjønnsfastsattSykepengegrunnlagEvent,
    ) {
        val jsonMessage =
            JsonMessage.newMessage(
                "skjønnsmessig_fastsettelse",
                listOfNotNull(
                    "@id" to event.id,
                    "aktørId" to event.aktørId,
                    "fødselsnummer" to event.fødselsnummer,
                    "skjæringstidspunkt" to event.skjæringstidspunkt,
                    "arbeidsgivere" to event.arbeidsgivere,
                    "saksbehandlerOid" to event.saksbehandlerOid,
                    "saksbehandlerNavn" to event.saksbehandlerNavn,
                    "saksbehandlerIdent" to event.saksbehandlerIdent,
                    "saksbehandlerEpost" to event.saksbehandlerEpost,
                ).toMap(),
            )
        rapidsConnection.publish(fødselsnummer, jsonMessage.toJson())
    }

    override fun minimumSykdomsgradVurdert(
        fødselsnummer: String,
        event: MinimumSykdomsgradVurdertEvent,
    ) {
        val jsonMessage =
            JsonMessage.newMessage(
                "minimum_sykdomsgrad_vurdert",
                listOfNotNull(
                    "@id" to event.id,
                    "aktørId" to event.aktørId,
                    "fødselsnummer" to event.fødselsnummer,
                    "perioderMedMinimumSykdomsgradVurdertOk" to event.perioderMedMinimumSykdomsgradVurdertOk,
                    "perioderMedMinimumSykdomsgradVurdertIkkeOk" to event.perioderMedMinimumSykdomsgradVurdertIkkeOk,
                    "saksbehandlerOid" to event.saksbehandlerOid,
                    "saksbehandlerNavn" to event.saksbehandlerNavn,
                    "saksbehandlerIdent" to event.saksbehandlerIdent,
                    "saksbehandlerEpost" to event.saksbehandlerEpost,
                ).toMap(),
            )
        rapidsConnection.publish(fødselsnummer, jsonMessage.toJson())
    }

    override fun utbetalingAnnullert(
        fødselsnummer: String,
        event: AnnullertUtbetalingEvent,
    ) {
        val jsonMessage =
            JsonMessage.newMessage(
                "annullering",
                mutableMapOf(
                    "fødselsnummer" to fødselsnummer,
                    "organisasjonsnummer" to event.organisasjonsnummer,
                    "aktørId" to event.aktørId,
                    "saksbehandler" to
                        mapOf(
                            "epostaddresse" to event.saksbehandlerEpost,
                            "oid" to event.saksbehandlerOid,
                            "navn" to event.saksbehandlerNavn,
                            "ident" to event.saksbehandlerIdent,
                        ),
                    "begrunnelser" to event.begrunnelser,
                    "utbetalingId" to event.utbetalingId,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "arbeidsgiverFagsystemId" to event.arbeidsgiverFagsystemId,
                    "personFagsystemId" to event.personFagsystemId,
                ).apply {
                    compute("kommentar") { _, _ -> event.kommentar }
                    compute("arsaker") { _, _ ->
                        event.arsaker?.map { mapOf("arsak" to it.arsak, "key" to it.key) }
                    }
                },
            )
        rapidsConnection.publish(fødselsnummer, jsonMessage.toJson())
    }
}
