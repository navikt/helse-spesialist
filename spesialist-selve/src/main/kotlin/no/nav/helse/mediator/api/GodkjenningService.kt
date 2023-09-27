package no.nav.helse.mediator.api

import java.sql.SQLException
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType.TOTRINNSVURDERING_ATTESTERT
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.slf4j.LoggerFactory


internal class GodkjenningService(
    private val dataSource: DataSource,
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val hendelseDao: HendelseDao = HendelseDao(dataSource),
    private val overstyringDao: OverstyringDao = OverstyringDao(dataSource),
    private val rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
    private val reservasjonDao: ReservasjonDao = ReservasjonDao(dataSource),
    private val periodehistorikkDao: PeriodehistorikkDao = PeriodehistorikkDao(dataSource),
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator = TotrinnsvurderingMediator(
        TotrinnsvurderingDao(dataSource), oppgaveDao, periodehistorikkDao,
        NotatMediator(
            NotatDao(dataSource)
        ),
    ),
) : Godkjenninghåndterer {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(GodkjenningService::class.java)
    }

    override fun håndter(godkjenningDTO: GodkjenningDto, epost: String, oid: UUID, behandlingId: UUID) {
        val hendelseId = oppgaveDao.finnHendelseId(godkjenningDTO.oppgavereferanse)
        val fødselsnummer = hendelseDao.finnFødselsnummer(hendelseId)
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(godkjenningDTO.oppgavereferanse)
        val utbetalingId = requireNotNull(oppgaveDao.finnUtbetalingId(godkjenningDTO.oppgavereferanse))
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
            "behandlingId" to behandlingId,
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
            sikkerlogg.info("Publiserer saksbehandler-løsning: ${it.toJson()}")
        }
        logg.info(
            "Publiserer saksbehandler-løsning for {}, {}",
            StructuredArguments.keyValue("oppgaveId", godkjenningDTO.oppgavereferanse),
            StructuredArguments.keyValue("hendelseId", hendelseId)
        )
        rapidsConnection.publish(fødselsnummer, godkjenningMessage.toJson())

        reserverPerson(reserverPersonOid, fødselsnummer)
        oppgaveMediator.oppgave(godkjenningDTO.oppgavereferanse) {
            avventerSystem(godkjenningDTO.saksbehandlerIdent, oid)
            overstyringDao.ferdigstillOverstyringerForVedtaksperiode(vedtaksperiodeId)

            if (totrinnsvurdering?.erBeslutteroppgave() == true && godkjenningDTO.godkjent) {
                periodehistorikkDao.lagre(TOTRINNSVURDERING_ATTESTERT, oid, utbetalingId, null)
            }
        }
    }

    private fun reserverPerson(oid: UUID, fødselsnummer: String) {
        try {
            reservasjonDao.reserverPerson(oid, fødselsnummer, false)
        } catch (e: SQLException) {
            logg.warn("Kunne ikke reservere person")
        }
    }
}
