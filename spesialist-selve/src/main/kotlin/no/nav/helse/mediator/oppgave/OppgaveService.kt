package no.nav.helse.mediator.oppgave

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.Repositories
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.nyOppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.toDto
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.saksbehandler.handlinger.EndrePåVent
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.modell.saksbehandler.handlinger.Oppgavehandling
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDto
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.UUID

interface Oppgavefinner {
    fun oppgave(
        utbetalingId: UUID,
        oppgaveBlock: Oppgave.() -> Unit,
    )
}

class OppgaveService(
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val reservasjonDao: ReservasjonDao,
    private val opptegnelseDao: OpptegnelseDao,
    private val totrinnsvurderingDao: TotrinnsvurderingDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val meldingPubliserer: MeldingPubliserer,
    private val tilgangskontroll: Tilgangskontroll,
    private val tilgangsgrupper: Tilgangsgrupper,
    private val repositories: Repositories,
) : Oppgavehåndterer, Oppgavefinner {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    internal fun nyOppgaveService(sessionContext: SessionContext): OppgaveService =
        OppgaveService(
            oppgaveDao = sessionContext.oppgaveDao,
            tildelingDao = sessionContext.tildelingDao,
            reservasjonDao = sessionContext.reservasjonDao,
            opptegnelseDao = sessionContext.opptegnelseDao,
            totrinnsvurderingDao = sessionContext.totrinnsvurderingDao,
            saksbehandlerDao = sessionContext.saksbehandlerDao,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = tilgangskontroll,
            tilgangsgrupper = tilgangsgrupper,
            repositories = repositories,
        )

    fun nyOppgave(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        kanAvvises: Boolean,
        egenskaper: Set<Egenskap>,
    ) {
        logg.info("Oppretter saksbehandleroppgave")
        sikkerlogg.info("Oppretter saksbehandleroppgave for {}", kv("fødselsnummer", fødselsnummer))
        val nesteId = oppgaveDao.reserverNesteId()
        val oppgavemelder = Oppgavemelder(fødselsnummer, meldingPubliserer)
        val oppgave =
            nyOppgave(
                id = nesteId,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                utbetalingId = utbetalingId,
                hendelseId = hendelseId,
                kanAvvises = kanAvvises,
                egenskaper = egenskaper,
            )
        oppgave.register(oppgavemelder)
        oppgavemelder.oppgaveOpprettet(oppgave)
        tildelVedReservasjon(fødselsnummer, oppgave)
        Oppgavelagrer(tildelingDao).lagre(this, oppgave.toDto())
    }

    fun <T> oppgave(
        id: Long,
        oppgaveBlock: Oppgave.() -> T,
    ): T {
        val oppgave =
            Oppgavehenter(
                oppgaveDao,
                totrinnsvurderingDao,
                saksbehandlerDao,
                tilgangskontroll,
            ).oppgave(id)
        val fødselsnummer = oppgaveDao.finnFødselsnummer(id)
        oppgave.register(Oppgavemelder(fødselsnummer, meldingPubliserer))
        val returverdi = oppgaveBlock(oppgave)
        Oppgavelagrer(tildelingDao).oppdater(this@OppgaveService, oppgave.toDto())
        return returverdi
    }

    override fun oppgave(
        utbetalingId: UUID,
        oppgaveBlock: Oppgave.() -> Unit,
    ) {
        val oppgaveId = oppgaveDao.finnOppgaveId(utbetalingId)
        oppgaveId?.let {
            oppgave(it, oppgaveBlock)
        }
    }

    fun lagreTotrinnsvurdering(totrinnsvurderingDto: TotrinnsvurderingDto) {
        val totrinnsvurderingFraDatabase =
            TotrinnsvurderingFraDatabase(
                vedtaksperiodeId = totrinnsvurderingDto.vedtaksperiodeId,
                erRetur = totrinnsvurderingDto.erRetur,
                saksbehandler = totrinnsvurderingDto.saksbehandler?.oid,
                beslutter = totrinnsvurderingDto.beslutter?.oid,
                utbetalingId = totrinnsvurderingDto.utbetalingId,
                opprettet = totrinnsvurderingDto.opprettet,
                oppdatert = totrinnsvurderingDto.oppdatert,
            )
        totrinnsvurderingDao.oppdater(totrinnsvurderingFraDatabase)
    }

    fun avbrytOppgave(
        handling: Oppgavehandling,
        saksbehandler: Saksbehandler,
    ) {
        oppgave(handling.oppgaveId()) {
            handling.oppgave(this)
            handling.utførAv(saksbehandler)
        }
    }

    fun avbrytOppgave(handling: Overstyring) {
        oppgaveDao.finnOppgaveId(handling.gjelderFødselsnummer())?.let {
            oppgave(it) {
                this.avbryt()
            }
        }
    }

    fun leggPåVent(
        handling: LeggPåVent,
        saksbehandler: Saksbehandler,
    ) {
        oppgave(handling.oppgaveId) {
            this.leggPåVent(handling.skalTildeles, saksbehandler)
        }
    }

    fun endrePåVent(
        handling: EndrePåVent,
        saksbehandler: Saksbehandler,
    ) {
        oppgave(handling.oppgaveId) {
            this.endrePåVent(handling.skalTildeles, saksbehandler)
        }
    }

    fun fjernFraPåVent(oppgaveId: Long) {
        oppgave(oppgaveId) {
            this.fjernFraPåVent()
        }
    }

    override fun endretEgenAnsattStatus(
        erEgenAnsatt: Boolean,
        fødselsnummer: String,
    ) {
        val oppgaveId =
            oppgaveDao.finnOppgaveId(fødselsnummer) ?: run {
                sikkerlogg.info("Ingen aktiv oppgave for {}", kv("fødselsnummer", fødselsnummer))
                return
            }
        oppgave(oppgaveId) {
            if (erEgenAnsatt) {
                logg.info("Legger til egenskap EGEN_ANSATT på {}", kv("oppgaveId", oppgaveId))
                sikkerlogg.info("Legger til egenskap EGEN_ANSATT for {}", kv("fødselsnummer", fødselsnummer))
                leggTilEgenAnsatt()
            } else {
                logg.info("Fjerner egenskap EGEN_ANSATT på {}", kv("oppgaveId", oppgaveId))
                sikkerlogg.info("Fjerner egenskap EGEN_ANSATT for {}", kv("fødselsnummer", fødselsnummer))
                fjernEgenAnsatt()
            }
        }
    }

    fun avbrytOppgaveFor(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.also {
            oppgave(it) {
                this.avbryt()
            }
        }
    }

    fun fjernTilbakedatert(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.also { oppgaveId ->
            oppgave(oppgaveId) {
                logg.info("Fjerner egenskap TILBAKEDATERT på {}", kv("oppgaveId", oppgaveId))
                fjernTilbakedatert()
            }
        }
    }

    fun fjernGosysEgenskap(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.also { oppgaveId ->
            oppgave(oppgaveId) {
                logg.info("Fjerner egenskap GOSYS på {}", kv("oppgaveId", oppgaveId))
                fjernGosys()
            }
        }
    }

    fun leggTilGosysEgenskap(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.also { oppgaveId ->
            oppgave(oppgaveId) {
                logg.info("Legger til egenskap GOSYS på {}", kv("oppgaveId", oppgaveId))
                leggTilGosys()
            }
        }
    }

    fun opprett(
        id: Long,
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
        utbetalingId: UUID,
        egenskaper: List<EgenskapForDatabase>,
        godkjenningsbehovId: UUID,
        kanAvvises: Boolean,
    ) {
        oppgaveDao.opprettOppgave(
            id,
            godkjenningsbehovId,
            egenskaper,
            vedtaksperiodeId,
            behandlingId,
            utbetalingId,
            kanAvvises,
        )
        opptegnelseDao.opprettOpptegnelse(
            oppgaveDao.finnFødselsnummer(id),
            GodkjenningsbehovPayload(godkjenningsbehovId).toJson(),
            OpptegnelseDao.Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE,
        )
    }

    fun oppdater(
        oppgaveId: Long,
        status: String,
        ferdigstiltAvIdent: String?,
        ferdigstiltAvOid: UUID?,
        egenskaper: List<EgenskapForDatabase>,
    ) {
        oppgaveDao.updateOppgave(oppgaveId, status, ferdigstiltAvIdent, ferdigstiltAvOid, egenskaper)
    }

    fun reserverOppgave(
        saksbehandleroid: UUID,
        fødselsnummer: String,
    ) {
        try {
            reservasjonDao.reserverPerson(saksbehandleroid, fødselsnummer)
        } catch (e: SQLException) {
            logg.warn("Kunne ikke reservere person")
        }
    }

    fun finnIdForAktivOppgave(vedtaksperiodeId: UUID): Long? = oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)

    private fun tildelVedReservasjon(
        fødselsnummer: String,
        oppgave: Oppgave,
    ) {
        val (saksbehandlerFraDatabase) =
            reservasjonDao.hentReservasjonFor(fødselsnummer) ?: run {
                logg.info("Finner ingen reservasjon for $oppgave, blir ikke tildelt.")
                return
            }
        val saksbehandler =
            Saksbehandler(
                epostadresse = saksbehandlerFraDatabase.epostadresse,
                oid = saksbehandlerFraDatabase.oid,
                navn = saksbehandlerFraDatabase.navn,
                ident = saksbehandlerFraDatabase.ident,
                tilgangskontroll = tilgangskontroll,
            )
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
    }

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) = oppgaveDao.harFerdigstiltOppgave(vedtaksperiodeId)
}
