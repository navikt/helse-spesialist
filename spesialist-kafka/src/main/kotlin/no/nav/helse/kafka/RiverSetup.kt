package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.MeldingDuplikatkontrollDao
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.registrerTidsbrukForDuplikatsjekk
import no.nav.helse.spesialist.application.ForsikringHenter
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggInfo
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

class RiverSetup(
    private val mediator: MeldingMediator,
    private val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao,
    sessionFactory: SessionFactory,
    versjonAvKode: String,
    forsikringHenter: ForsikringHenter,
    environmentToggles: EnvironmentToggles,
) {
    private val rivers =
        listOf(
            GodkjenningsbehovRiver(mediator),
            SøknadSendtRiver(),
            PersoninfoløsningRiver(mediator),
            FlerePersoninfoRiver(mediator),
            HentEnhetLøsningRiver(mediator),
            InfotrygdutbetalingerLøsningRiver(mediator),
            ArbeidsgiverinformasjonLøsningRiver(mediator),
            ArbeidsforholdLøsningRiver(mediator),
            VedtaksperiodeForkastetRiver(mediator),
            AdressebeskyttelseEndretRiver(mediator),
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
            NyeVarslerRiver(),
            VarseldefinisjonRiver(mediator),
            VedtaksperiodeNyUtbetalingRiver(mediator),
            BehovtidsbrukMetrikkRiver(),
            AvsluttetMedVedtakRiver(
                forsikringHenter,
                environmentToggles,
            ),
            AvsluttetUtenVedtakRiver(),
            MidnattRiver(sessionFactory),
            BehandlingOpprettetRiver(),
            BehandlingLukketRiver(),
            KommandokjedePåminnelseRiver(mediator),
            StansAutomatiskBehandlingRiver(mediator),
            AvviksvurderingLøsningRiver(mediator),
        ).map { DuplikatsjekkendeRiver(it, meldingDuplikatkontrollDao, sessionFactory, versjonAvKode) }

    fun registrerRivers(rapidsConnection: RapidsConnection) {
        val delegatedRapid =
            DelegatedRapid(
                rapidsConnection = rapidsConnection,
                beforeRiversAction = mediator::nullstillTilstand,
                skalBehandleMelding = mediator::skalBehandleMelding,
                afterRiversAction = mediator::fortsett,
                errorAction = mediator::errorHandler,
            )
        rivers
            .forEach { river ->
                River(delegatedRapid)
                    .precondition(river.preconditions())
                    .validate(river.validations())
                    .register(river)
                    .onSuccess { packet, _, _, _ ->
                        val eventName =
                            packet.run {
                                interestedIn("@event_name")
                                get("@event_name").textValue() ?: "ukjent"
                            }
                        logg.debug(
                            "{} leste melding id={}, event_name={}. En annen/tidligere river har allerede/også behandlet meldingen:{}",
                            river.name(),
                            packet.id,
                            eventName,
                            mediator.meldingenHarBlittBehandletAvEnRiver.get(),
                        )
                        mediator.meldingenHarBlittBehandletAvEnRiver.set(true)
                    }
            }
    }
}

class DuplikatsjekkendeRiver(
    private val river: SpesialistRiver,
    private val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao,
    private val sessionFactory: SessionFactory,
    private val versjonAvKode: String,
) : SpesialistRiver by river {
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        loggInfo("Melding mottatt", "json" to packet.toJson())
        val id = packet.id.toUUID()
        if (erDuplikat(id)) {
            loggInfo("Ignorerer melding pga duplikatkontroll")
            return
        }

        if (river is TransaksjonellRiver) {
            val outbox = Outbox(versjonAvKode)
            sessionFactory.transactionalSessionScope { transaksjon ->
                packet.interestedIn("@id")
                river.transaksjonellOnPacket(
                    packet = packet,
                    outbox = outbox,
                    transaksjon = transaksjon,
                    eventMetadata = EventMetadata(`@id` = packet["@id"].asUUID()),
                )
            }
            outbox.sendAlle(MessageContextMeldingPubliserer(context))
        } else {
            river.onPacket(packet, context, metadata, meterRegistry)
        }
        loggInfo("Melding ferdig håndtert i river")
    }

    private fun erDuplikat(id: UUID): Boolean {
        val (erDuplikat, tid) = measureTimedValue { meldingDuplikatkontrollDao.erBehandlet(id) }
        loggDebug("Det tok ${tid.inWholeMilliseconds} ms å gjøre duplikatsjekk mot databasen")
        registrerTidsbrukForDuplikatsjekk(erDuplikat, tid.toDouble(DurationUnit.MILLISECONDS))
        return erDuplikat
    }
}
