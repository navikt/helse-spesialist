package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.PgDialogDao
import no.nav.helse.db.PgOppgaveDao
import no.nav.helse.db.PgPeriodehistorikkDao
import no.nav.helse.db.PgTotrinnsvurderingDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.toDto
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

internal class GodkjenningService(
    private val dataSource: DataSource,
    private val oppgaveDao: OppgaveDao = PgOppgaveDao(dataSource),
    private val overstyringDao: OverstyringDao = OverstyringDao(dataSource),
    private val rapidsConnection: RapidsConnection,
    private val oppgaveService: OppgaveService,
    private val reservasjonDao: ReservasjonDao = ReservasjonDao(dataSource),
    private val periodehistorikkDao: PeriodehistorikkDao = PgPeriodehistorikkDao(dataSource),
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val totrinnsvurderingService: TotrinnsvurderingService =
        TotrinnsvurderingService(
            PgTotrinnsvurderingDao(dataSource),
            oppgaveDao,
            periodehistorikkDao,
            PgDialogDao(dataSource),
        ),
) : Godkjenninghåndterer {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(GodkjenningService::class.java)
    }

    override fun håndter(
        godkjenningDTO: GodkjenningDto,
        epost: String,
        oid: UUID,
    ) {
        val hendelseId = oppgaveDao.finnHendelseId(godkjenningDTO.oppgavereferanse)
        val fødselsnummer = oppgaveDao.finnFødselsnummer(godkjenningDTO.oppgavereferanse)
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(godkjenningDTO.oppgavereferanse)
        val totrinnsvurdering = totrinnsvurderingService.hentAktiv(vedtaksperiodeId)
        val reserverPersonOid: UUID = totrinnsvurdering?.saksbehandler ?: oid
        val saksbehandleroverstyringer = overstyringDao.finnAktiveOverstyringer(vedtaksperiodeId)
        val saksbehandlerJson = saksbehandler(godkjenningDTO, totrinnsvurdering, oid)
        val beslutterJson = beslutter(godkjenningDTO, totrinnsvurdering)
        val godkjenningMessage =
            JsonMessage
                .newMessage(
                    "saksbehandler_løsning",
                    mutableMapOf(
                        "@forårsaket_av" to
                            mapOf(
                                "event_name" to "behov",
                                "behov" to "Godkjenning",
                                "id" to hendelseId,
                            ),
                        "fødselsnummer" to fødselsnummer,
                        "oppgaveId" to godkjenningDTO.oppgavereferanse,
                        "hendelseId" to hendelseId,
                        "godkjent" to godkjenningDTO.godkjent,
                        "saksbehandlerident" to godkjenningDTO.saksbehandlerIdent,
                        "saksbehandleroid" to oid,
                        "saksbehandlerepost" to epost,
                        "godkjenttidspunkt" to LocalDateTime.now(),
                        "saksbehandleroverstyringer" to saksbehandleroverstyringer,
                        "saksbehandler" to saksbehandlerJson,
                    ).apply {
                        godkjenningDTO.årsak?.let { put("årsak", it) }
                        godkjenningDTO.begrunnelser?.let { put("begrunnelser", it) }
                        godkjenningDTO.kommentar?.let { put("kommentar", it) }
                        compute("beslutter") { _, _ -> beslutterJson }
                    },
                ).also {
                    sikkerlogg.info("Publiserer saksbehandler-løsning: ${it.toJson()}")
                }
        logg.info(
            "Publiserer saksbehandler-løsning for {}, {}",
            StructuredArguments.keyValue("oppgaveId", godkjenningDTO.oppgavereferanse),
            StructuredArguments.keyValue("hendelseId", hendelseId),
        )
        rapidsConnection.publish(fødselsnummer, godkjenningMessage.toJson())

        reserverPerson(reserverPersonOid, fødselsnummer)
        oppgaveService.oppgave(godkjenningDTO.oppgavereferanse) {
            avventerSystem(godkjenningDTO.saksbehandlerIdent, oid)
            overstyringDao.ferdigstillOverstyringerForVedtaksperiode(vedtaksperiodeId)

            if (totrinnsvurdering?.erBeslutteroppgave() == true && godkjenningDTO.godkjent) {
                val beslutter = totrinnsvurdering.beslutter?.let { saksbehandlerRepository.finnSaksbehandler(it)?.toDto() }
                checkNotNull(beslutter) { "Forventer at beslutter er satt" }
                val innslag = HistorikkinnslagDto.totrinnsvurderingFerdigbehandletInnslag(beslutter)
                periodehistorikkDao.lagreMedOppgaveId(innslag, godkjenningDTO.oppgavereferanse)
            }
        }
    }

    private fun saksbehandler(
        godkjenningDTO: GodkjenningDto,
        totrinnsvurdering: TotrinnsvurderingOld?,
        oid: UUID,
    ): Map<String, String> =
        when {
            !godkjenningDTO.godkjent -> saksbehandlerForJson(oid)
            totrinnsvurdering == null -> saksbehandlerForJson(oid)

            else -> {
                checkNotNull(
                    totrinnsvurdering.saksbehandler,
                ) { "Totrinnsvurdering uten saksbehandler gir ikke noen mening ved godkjenning av periode" }
                saksbehandlerForJson(totrinnsvurdering.saksbehandler)
            }
        }

    private fun beslutter(
        godkjenningDTO: GodkjenningDto,
        totrinnsvurdering: TotrinnsvurderingOld?,
    ): Map<String, String>? =
        when {
            !godkjenningDTO.godkjent -> null
            totrinnsvurdering == null -> null

            else -> {
                checkNotNull(
                    totrinnsvurdering.beslutter,
                ) { "Totrinnsvurdering uten beslutter gir ikke noen mening ved godkjenning av periode" }
                saksbehandlerForJson(totrinnsvurdering.beslutter)
            }
        }

    private fun saksbehandlerForJson(oid: UUID): Map<String, String> {
        val saksbehandler =
            requireNotNull(
                saksbehandlerRepository.finnSaksbehandler(oid),
            ) { "Finner ikke saksbehandleren i databasen. Det gir ikke noen mening" }
        return mapOf(
            "ident" to saksbehandler.ident,
            "epostadresse" to saksbehandler.epostadresse,
        )
    }

    private fun reserverPerson(
        oid: UUID,
        fødselsnummer: String,
    ) {
        try {
            reservasjonDao.reserverPerson(oid, fødselsnummer)
        } catch (e: SQLException) {
            logg.warn("Kunne ikke reservere person")
        }
    }
}
