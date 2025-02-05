package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OverstyringDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.toDto
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.melding.Saksbehandlerløsning
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.UUID

class GodkjenningService(
    private val oppgaveDao: OppgaveDao,
    private val overstyringDao: OverstyringDao,
    private val publiserer: MeldingPubliserer,
    private val oppgaveService: OppgaveService,
    private val reservasjonDao: ReservasjonDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val totrinnsvurderingService: TotrinnsvurderingService,
) : Godkjenninghåndterer {
    private companion object {
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
        val saksbehandlerløsning =
            Saksbehandlerløsning(
                godkjenningsbehovId = hendelseId,
                oppgaveId = godkjenningDTO.oppgavereferanse,
                godkjent = godkjenningDTO.godkjent,
                saksbehandlerIdent = godkjenningDTO.saksbehandlerIdent,
                saksbehandlerOid = oid,
                saksbehandlerEpost = epost,
                godkjenttidspunkt = LocalDateTime.now(),
                saksbehandleroverstyringer = overstyringDao.finnAktiveOverstyringer(vedtaksperiodeId),
                saksbehandler = saksbehandler(godkjenningDTO, totrinnsvurdering, oid),
                årsak = godkjenningDTO.årsak,
                begrunnelser = godkjenningDTO.begrunnelser,
                kommentar = godkjenningDTO.kommentar,
                beslutter = beslutter(godkjenningDTO, totrinnsvurdering),
            )
        logg.info(
            "Publiserer saksbehandler-løsning for {}, {}",
            StructuredArguments.keyValue("oppgaveId", godkjenningDTO.oppgavereferanse),
            StructuredArguments.keyValue("hendelseId", hendelseId),
        )
        publiserer.publiser(fødselsnummer, saksbehandlerløsning, "saksbehandlergodkjenning")

        reserverPerson(reserverPersonOid, fødselsnummer)
        oppgaveService.oppgave(godkjenningDTO.oppgavereferanse) {
            avventerSystem(godkjenningDTO.saksbehandlerIdent, oid)
            overstyringDao.ferdigstillOverstyringerForVedtaksperiode(vedtaksperiodeId)

            if (totrinnsvurdering?.erBeslutteroppgave() == true && godkjenningDTO.godkjent) {
                val beslutter =
                    totrinnsvurdering.beslutter?.let { saksbehandlerDao.finnSaksbehandlerFraDatabase(it)?.toDto() }
                checkNotNull(beslutter) { "Forventer at beslutter er satt" }
                val innslag = Historikkinnslag.totrinnsvurderingFerdigbehandletInnslag(beslutter)
                periodehistorikkDao.lagreMedOppgaveId(innslag, godkjenningDTO.oppgavereferanse)
            }
        }
    }

    private fun saksbehandler(
        godkjenningDTO: GodkjenningDto,
        totrinnsvurdering: TotrinnsvurderingOld?,
        oid: UUID,
    ): Saksbehandler =
        when {
            !godkjenningDTO.godkjent -> saksbehandlerForJson(oid)
            totrinnsvurdering == null -> saksbehandlerForJson(oid)

            else -> {
                val saksbehandler = totrinnsvurdering.saksbehandler
                checkNotNull(
                    saksbehandler,
                ) { "Totrinnsvurdering uten saksbehandler gir ikke noen mening ved godkjenning av periode" }
                saksbehandlerForJson(saksbehandler)
            }
        }

    private fun beslutter(
        godkjenningDTO: GodkjenningDto,
        totrinnsvurdering: TotrinnsvurderingOld?,
    ): Saksbehandler? =
        when {
            !godkjenningDTO.godkjent -> null
            totrinnsvurdering == null -> null

            else -> {
                val beslutter = totrinnsvurdering.beslutter
                checkNotNull(
                    beslutter,
                ) { "Totrinnsvurdering uten beslutter gir ikke noen mening ved godkjenning av periode" }
                saksbehandlerForJson(beslutter)
            }
        }

    private fun saksbehandlerForJson(oid: UUID): Saksbehandler =
        requireNotNull(
            saksbehandlerDao.finnSaksbehandler(oid),
        ) { "Finner ikke saksbehandleren i databasen. Det gir ikke noen mening" }

    private fun reserverPerson(
        oid: UUID,
        fødselsnummer: String,
    ) {
        try {
            reservasjonDao.reserverPerson(oid, fødselsnummer)
        } catch (e: SQLException) {
            logg.warn("Kunne ikke reservere person", e)
        }
    }
}
