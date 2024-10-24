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
                buildMap {
                    put("fødselsnummer", fødselsnummer)
                    put("organisasjonsnummer", event.organisasjonsnummer)
                    put("aktørId", event.aktørId)
                    put(
                        "saksbehandler",
                        mapOf(
                            "epostaddresse" to event.saksbehandlerEpost,
                            "oid" to event.saksbehandlerOid,
                            "navn" to event.saksbehandlerNavn,
                            "ident" to event.saksbehandlerIdent,
                        ),
                    )
                    put("begrunnelser", event.begrunnelser)
                    put("utbetalingId", event.utbetalingId)
                    put("vedtaksperiodeId", event.vedtaksperiodeId)
                    put("arbeidsgiverFagsystemId", event.arbeidsgiverFagsystemId)
                    put("personFagsystemId", event.personFagsystemId)
                    event.kommentar?.let { put("kommentar", it) }
                    event.arsaker?.let { arsaker ->
                        put("arsaker", arsaker.map { mapOf("arsak" to it.arsak, "key" to it.key) })
                    }
                },
            )
        rapidsConnection.publish(fødselsnummer, jsonMessage.toJson())
    }
}
