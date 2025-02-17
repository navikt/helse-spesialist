package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.FeatureToggles
import no.nav.helse.db.MeldingDuplikatkontrollDao
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.registrerTidsbrukForDuplikatsjekk
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

class RiverSetup(
    private val rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
    private val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao,
    private val featureToggles: FeatureToggles,
) {
    fun setUp() {
        val delegatedRapid =
            DelegatedRapid(
                rapidsConnection = rapidsConnection,
                beforeRiversAction = mediator::nullstillTilstand,
                skalBehandleMelding = mediator::skalBehandleMelding,
                afterRiversAction = mediator::fortsett,
                errorAction = mediator::errorHandler,
            )
        sequenceOf(
            GodkjenningsbehovRiver(mediator, featureToggles),
            SøknadSendtRiver(mediator),
            SøknadSendtArbeidsledigRiver(mediator),
            PersoninfoløsningRiver(mediator),
            FlerePersoninfoRiver(mediator),
            HentEnhetLøsningRiver(mediator),
            InfotrygdutbetalingerLøsningRiver(mediator),
            SaksbehandlerløsningRiver(mediator),
            ArbeidsgiverinformasjonLøsningRiver(mediator),
            ArbeidsforholdLøsningRiver(mediator),
            VedtaksperiodeForkastetRiver(mediator),
            AdressebeskyttelseEndretRiver(mediator),
            OverstyringIgangsattRiver(mediator),
            EgenAnsattLøsningRiver(mediator),
            VergemålLøsningRiver(mediator),
            FullmaktLøsningRiver(mediator),
            ÅpneGosysOppgaverLøsningRiver(mediator),
            VurderingsmomenterLøsningRiver(mediator),
            InntektLøsningRiver(mediator),
            OppdaterPersondataRiver(mediator),
            KlargjørPersonForVisningRiver(mediator),
            UtbetalingEndretRiver(mediator),
            VedtaksperiodeReberegnetRiver(mediator),
            GosysOppgaveEndretRiver(mediator),
            TilbakedateringBehandletRiver(mediator),
            EndretSkjermetinfoRiver(mediator),
            DokumentRiver(mediator),
            NyeVarslerRiver(mediator),
            AvvikVurdertRiver(mediator),
            VarseldefinisjonRiver(mediator),
            VedtaksperiodeNyUtbetalingRiver(mediator),
            BehovtidsbrukMetrikkRiver(),
            AvsluttetMedVedtakRiver(mediator),
            AvsluttetUtenVedtakRiver(mediator),
            MidnattRiver(mediator),
            BehandlingOpprettetRiver(mediator),
            KommandokjedePåminnelseRiver(mediator),
            StansAutomatiskBehandlingRiver(mediator),
        ).forEach { river ->
            River(delegatedRapid)
                .precondition(river.preconditions())
                .validate(river.validations())
                .register(duplikatsjekkendeRiver(river))
                .onSuccess { packet, _, _, _ ->
                    logg.debug(
                        "${river.name()} leste melding id=${packet.id}, event_name=${packet.eventName()}. En annen/tidligere river har allerede/også behandlet meldingen:${mediator.meldingenHarBlittBehandletAvEnRiver}",
                    )
                    mediator.meldingenHarBlittBehandletAvEnRiver = true
                }
        }
    }

    private fun duplikatsjekkendeRiver(river: River.PacketListener) =
        object : River.PacketListener by river {
            override fun onPacket(
                packet: JsonMessage,
                context: MessageContext,
                metadata: MessageMetadata,
                meterRegistry: MeterRegistry,
            ) {
                val id = packet.id.toUUID()
                if (erDuplikat(id)) {
                    logg.info("Ignorerer melding {} pga duplikatkontroll", id)
                    return
                }
                river.onPacket(packet, context, metadata, meterRegistry)
            }
        }

    private fun erDuplikat(id: UUID): Boolean {
        val (erDuplikat, tid) = measureTimedValue { meldingDuplikatkontrollDao.erBehandlet(id) }
        logg.info("Det tok ${tid.inWholeMilliseconds} ms å gjøre duplikatsjekk mot databasen")
        registrerTidsbrukForDuplikatsjekk(erDuplikat, tid.toDouble(DurationUnit.MILLISECONDS))
        return erDuplikat
    }

    private fun JsonMessage.eventName() =
        run {
            interestedIn("@event_name")
            get("@event_name").textValue() ?: "ukjent"
        }

    private companion object {
        private val logg = LoggerFactory.getLogger(RiverSetup::class.java)
    }
}
