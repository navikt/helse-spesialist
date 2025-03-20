package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.FeatureToggles
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.melding.Saksbehandlerløsning
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.GODKJENT
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.UUID

class GodkjenningService(
    private val oppgaveDao: OppgaveDao,
    private val publiserer: MeldingPubliserer,
    private val oppgaveService: OppgaveService,
    private val reservasjonDao: ReservasjonDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val sessionFactory: SessionFactory,
    private val featureToggles: FeatureToggles,
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

        sessionFactory.transactionalSessionScope { session ->
            val totrinnsvurdering =
                if (featureToggles.skalBenytteNyTotrinnsvurderingsløsning()) {
                    session.totrinnsvurderingRepository.finn(fødselsnummer)
                } else {
                    session.totrinnsvurderingRepository.finn(vedtaksperiodeId)
                }

            val reserverPersonOid: UUID = totrinnsvurdering?.saksbehandler?.value ?: oid
            val saksbehandlerløsning =
                Saksbehandlerløsning(
                    godkjenningsbehovId = hendelseId,
                    oppgaveId = godkjenningDTO.oppgavereferanse,
                    godkjent = godkjenningDTO.godkjent,
                    saksbehandlerIdent = godkjenningDTO.saksbehandlerIdent,
                    saksbehandlerOid = oid,
                    saksbehandlerEpost = epost,
                    godkjenttidspunkt = LocalDateTime.now(),
                    saksbehandleroverstyringer =
                        totrinnsvurdering?.overstyringer?.filter {
                            it.vedtaksperiodeId == vedtaksperiodeId ||
                                it.kobledeVedtaksperioder()
                                    .contains(vedtaksperiodeId)
                        }?.map { it.eksternHendelseId } ?: emptyList(),
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
                totrinnsvurdering?.ferdigstill(
                    this.utbetalingId,
                    featureToggles.skalBenytteNyTotrinnsvurderingsløsning(),
                )

                if (totrinnsvurdering?.tilstand == GODKJENT && godkjenningDTO.godkjent) {
                    val beslutter =
                        totrinnsvurdering.beslutter?.let { saksbehandlerId ->
                            session.saksbehandlerRepository.finn(saksbehandlerId)?.let { saksbehandler ->
                                SaksbehandlerDto(
                                    epostadresse = saksbehandler.epost,
                                    oid = saksbehandler.id().value,
                                    navn = saksbehandler.navn,
                                    ident = saksbehandler.ident,
                                )
                            }
                        }
                    checkNotNull(beslutter) { "Forventer at beslutter er satt" }
                    val innslag = Historikkinnslag.totrinnsvurderingFerdigbehandletInnslag(beslutter)
                    periodehistorikkDao.lagreMedOppgaveId(innslag, godkjenningDTO.oppgavereferanse)
                }
            }
            if (totrinnsvurdering != null) session.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        }
    }

    private fun saksbehandler(
        godkjenningDTO: GodkjenningDto,
        totrinnsvurdering: Totrinnsvurdering?,
        oid: UUID,
    ): Saksbehandler =
        when {
            !godkjenningDTO.godkjent -> saksbehandlerForJson(oid)
            totrinnsvurdering == null -> saksbehandlerForJson(oid)

            else -> {
                val saksbehandler = totrinnsvurdering.saksbehandler?.value
                checkNotNull(
                    saksbehandler,
                ) { "Totrinnsvurdering uten saksbehandler gir ikke noen mening ved godkjenning av periode" }
                saksbehandlerForJson(saksbehandler)
            }
        }

    private fun beslutter(
        godkjenningDTO: GodkjenningDto,
        totrinnsvurdering: Totrinnsvurdering?,
    ): Saksbehandler? =
        when {
            !godkjenningDTO.godkjent -> null
            totrinnsvurdering == null -> null

            else -> {
                val beslutter = totrinnsvurdering.beslutter?.value
                checkNotNull(
                    beslutter,
                ) { "Totrinnsvurdering uten beslutter gir ikke noen mening ved godkjenning av periode" }
                saksbehandlerForJson(beslutter)
            }
        }

    private fun saksbehandlerForJson(oid: UUID): Saksbehandler =
        requireNotNull(
            sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.finn(SaksbehandlerOid(oid)) },
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
