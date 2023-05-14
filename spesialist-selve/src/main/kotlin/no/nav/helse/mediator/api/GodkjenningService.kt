package no.nav.helse.mediator.api

import java.sql.SQLException
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
internal val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

internal class GodkjenningService(
    private val dataSource: DataSource,
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val hendelseDao: HendelseDao = HendelseDao(dataSource),
    private val overstyringDao: OverstyringDao = OverstyringDao(dataSource),
    private val rapidsConnection: RapidsConnection,
    private val reservasjonDao: ReservasjonDao = ReservasjonDao(dataSource),
    private val periodehistorikkDao: PeriodehistorikkDao = PeriodehistorikkDao(dataSource),
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator = TotrinnsvurderingMediator(
        TotrinnsvurderingDao(dataSource), oppgaveDao, periodehistorikkDao,
        NotatMediator(
            NotatDao(dataSource)
        ),
    ),
) {

    internal fun håndter(godkjenningDTO: GodkjenningDto, epost: String, oid: UUID) {
        val hendelseId = oppgaveDao.finnHendelseId(godkjenningDTO.oppgavereferanse)
        val fødselsnummer = hendelseDao.finnFødselsnummer(hendelseId)
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(godkjenningDTO.oppgavereferanse)
        val totrinnsvurdering = totrinnsvurderingMediator.hentAktiv(vedtaksperiodeId)
        val reserverPersonOid: UUID = totrinnsvurdering?.saksbehandler ?: oid
        val saksbehandleroverstyringer = overstyringDao.finnAktiveOverstyringer(vedtaksperiodeId)
        val godkjenningMessage = JsonMessage.newMessage("saksbehandler_løsning", mutableMapOf(
            "@forårsaket_av" to mapOf(
                "event_name" to "behov",
                "behov" to "Godkjenning",
                "id" to hendelseId
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
        ).apply {
            godkjenningDTO.årsak?.let { put("årsak", it) }
            godkjenningDTO.begrunnelser?.let { put("begrunnelser", it) }
            godkjenningDTO.kommentar?.let { put("kommentar", it) }
        }).also {
            sikkerLogg.info("Publiserer saksbehandler-løsning: ${it.toJson()}")
        }
        log.info(
            "Publiserer saksbehandler-løsning for {}, {}",
            StructuredArguments.keyValue("oppgaveId", godkjenningDTO.oppgavereferanse),
            StructuredArguments.keyValue("hendelseId", hendelseId)
        )
        rapidsConnection.publish(fødselsnummer, godkjenningMessage.toJson())

        val oppgave = finnOppgave(godkjenningDTO)
        reserverPerson(reserverPersonOid, fødselsnummer)
        oppgave.lagreAvventerSystem(oppgaveDao, godkjenningDTO.saksbehandlerIdent, oid)
        rapidsConnection.publish(oppgave.lagMelding("oppgave_oppdatert", oppgaveDao = oppgaveDao).toJson())

        overstyringDao.ferdigstillOverstyringerForVedtaksperiode(vedtaksperiodeId)
        totrinnsvurderingMediator.ferdigstill(vedtaksperiodeId)

        if (totrinnsvurdering?.erBeslutteroppgave() == true && godkjenningDTO.godkjent) {
            oppgave.lagrePeriodehistorikk(periodehistorikkDao, oid, PeriodehistorikkType.TOTRINNSVURDERING_ATTESTERT, null)
        }
    }

    private fun finnOppgave(godkjenningDTO: GodkjenningDto) = oppgaveDao.finn(godkjenningDTO.oppgavereferanse)
        ?: throw IllegalArgumentException("oppgaveId ${godkjenningDTO.oppgavereferanse} fins ikke")

    private fun reserverPerson(oid: UUID, fødselsnummer: String) {
        try {
            reservasjonDao.reserverPerson(oid, fødselsnummer)
        } catch (e: SQLException) {
            log.warn("Kunne ikke reservere person")
        }

    }

}
