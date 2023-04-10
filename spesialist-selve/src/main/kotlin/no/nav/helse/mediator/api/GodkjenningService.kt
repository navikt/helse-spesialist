package no.nav.helse.mediator.api

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
internal val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

internal class GodkjenningService(
    private val dataSource: DataSource,
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val hendelseDao: HendelseDao = HendelseDao(dataSource),
    private val overstyringDao: OverstyringDao = OverstyringDao(dataSource),
    private val rapidsConnection: RapidsConnection,
    private val tildelingDao: TildelingDao = TildelingDao(dataSource),
    private val reservasjonDao: ReservasjonDao = ReservasjonDao(dataSource),
    private val opptegnelseDao: OpptegnelseDao = OpptegnelseDao(dataSource),
    private val periodehistorikkDao: PeriodehistorikkDao = PeriodehistorikkDao(dataSource),
    private val oppgaveMediator: OppgaveMediator,
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator = TotrinnsvurderingMediator(TotrinnsvurderingDao(dataSource), oppgaveMediator, NotatMediator(
        NotatDao(dataSource)
    )),
) {

    internal fun håndter(godkjenningDTO: GodkjenningDTO, epost: String, oid: UUID) {
        val contextId = oppgaveDao.finnContextId(godkjenningDTO.oppgavereferanse)
        val hendelseId = oppgaveDao.finnHendelseId(godkjenningDTO.oppgavereferanse)
        val fødselsnummer = hendelseDao.finnFødselsnummer(hendelseId)
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(godkjenningDTO.oppgavereferanse)
        val totrinnsvurdering = totrinnsvurderingMediator.hentAktiv(vedtaksperiodeId)
        val erBeslutteroppgave = oppgaveMediator.erBeslutteroppgave(godkjenningDTO.oppgavereferanse)
        val tidligereSaksbehandler = oppgaveMediator.finnTidligereSaksbehandler(godkjenningDTO.oppgavereferanse)
        val reserverPersonOid: UUID =
            if (erBeslutteroppgave && tidligereSaksbehandler != null) tidligereSaksbehandler
            else if (totrinnsvurdering?.erBeslutteroppgave() == true) totrinnsvurdering.saksbehandler!!
            else oid
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

        val internOppgaveMediator =
            OppgaveMediator(
                oppgaveDao = oppgaveDao,
                tildelingDao = tildelingDao,
                reservasjonDao = reservasjonDao,
                opptegnelseDao = opptegnelseDao,
                periodehistorikkDao = periodehistorikkDao
            )
        internOppgaveMediator.reserverOppgave(reserverPersonOid, fødselsnummer)
        internOppgaveMediator.avventerSystem(godkjenningDTO.oppgavereferanse, godkjenningDTO.saksbehandlerIdent, oid)
        internOppgaveMediator.lagreOppgaver(rapidsConnection, hendelseId, contextId)

        overstyringDao.ferdigstillOverstyringerForVedtaksperiode(vedtaksperiodeId)
        totrinnsvurderingMediator.ferdigstill(vedtaksperiodeId)

        if ((erBeslutteroppgave || totrinnsvurdering?.erBeslutteroppgave() == true) && godkjenningDTO.godkjent) {
            internOppgaveMediator.lagrePeriodehistorikk(
                oppgaveId = godkjenningDTO.oppgavereferanse,
                saksbehandleroid = oid,
                type = PeriodehistorikkType.TOTRINNSVURDERING_ATTESTERT
            )
        }
    }

}
