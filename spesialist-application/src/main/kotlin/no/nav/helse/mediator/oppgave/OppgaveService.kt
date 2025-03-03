package no.nav.helse.mediator.oppgave

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.Daos
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.ny
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.saksbehandler.handlinger.EndrePåVent
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.modell.saksbehandler.handlinger.Oppgavehandling
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
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
    private val reservasjonDao: ReservasjonDao,
    private val meldingPubliserer: MeldingPubliserer,
    private val tilgangskontroll: Tilgangskontroll,
    private val tilgangsgrupper: Tilgangsgrupper,
    private val oppgavelagrer: Oppgavelagrer,
    private val daos: Daos,
) : Oppgavehåndterer, Oppgavefinner {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    internal fun nyOppgaveService(sessionContext: SessionContext): OppgaveService =
        OppgaveService(
            oppgaveDao = sessionContext.oppgaveDao,
            reservasjonDao = sessionContext.reservasjonDao,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = tilgangskontroll,
            tilgangsgrupper = tilgangsgrupper,
            oppgavelagrer = sessionContext.oppgavelagrer,
            daos = daos,
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
            ny(
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
        oppgavelagrer.lagre(oppgave)
    }

    fun <T> oppgave(
        id: Long,
        oppgaveBlock: Oppgave.() -> T,
    ): T {
        val oppgave =
            Oppgavehenter(
                oppgaveDao,
                tilgangskontroll,
            ).oppgave(id)
        val fødselsnummer = oppgaveDao.finnFødselsnummer(id)
        oppgave.register(Oppgavemelder(fødselsnummer, meldingPubliserer))
        val returverdi = oppgaveBlock(oppgave)
        oppgavelagrer.oppdater(oppgave)
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

    fun avbrytOppgave(
        handling: Oppgavehandling,
        legacySaksbehandler: LegacySaksbehandler,
    ) {
        oppgave(handling.oppgaveId()) {
            handling.oppgave(this)
            handling.utførAv(legacySaksbehandler)
        }
    }

    fun leggPåVent(
        handling: LeggPåVent,
        legacySaksbehandler: LegacySaksbehandler,
    ) {
        oppgaveDao.finnVedtaksperiodeId(handling.oppgaveId).also {
            oppgaveDao.finnIdForAktivOppgave(it)?.also { oppgaveId ->
                oppgave(oppgaveId) {
                    this.leggPåVent(handling.skalTildeles, legacySaksbehandler)
                }
            }
        }
    }

    fun endrePåVent(
        handling: EndrePåVent,
        legacySaksbehandler: LegacySaksbehandler,
    ) {
        oppgaveDao.finnVedtaksperiodeId(handling.oppgaveId).also {
            oppgaveDao.finnIdForAktivOppgave(it)?.also { oppgaveId ->
                oppgave(oppgaveId) {
                    this.endrePåVent(handling.skalTildeles, legacySaksbehandler)
                }
            }
        }
    }

    fun fjernFraPåVent(oppgaveId: Long) {
        oppgaveDao.finnVedtaksperiodeId(oppgaveId).also {
            oppgaveDao.finnIdForAktivOppgave(it)?.also { aktivOppgaveId ->
                oppgave(aktivOppgaveId) {
                    this.fjernFraPåVent()
                }
            }
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
            oppgave(oppgaveId) { fjernGosys() }
        }
    }

    fun leggTilGosysEgenskap(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.also { oppgaveId ->
            oppgave(oppgaveId) { leggTilGosys() }
        }
    }

    fun reserverOppgave(
        saksbehandleroid: UUID,
        fødselsnummer: String,
    ) {
        try {
            reservasjonDao.reserverPerson(saksbehandleroid, fødselsnummer)
        } catch (e: SQLException) {
            logg.warn("Kunne ikke reservere person. Se sikker logg for mer informasjon")
            sikkerlogg.warn("Kunne ikke reservere person", e)
        }
    }

    fun finnVedtaksperiodeId(oppgavereferanse: Long): UUID = oppgaveDao.finnVedtaksperiodeId(oppgavereferanse)

    fun finnFødselsnummer(oppgavereferanse: Long): String = oppgaveDao.finnFødselsnummer(oppgavereferanse)

    private fun tildelVedReservasjon(
        fødselsnummer: String,
        oppgave: Oppgave,
    ) {
        val (saksbehandlerFraDatabase) =
            reservasjonDao.hentReservasjonFor(fødselsnummer) ?: run {
                logg.info("Finner ingen reservasjon for $oppgave, blir ikke tildelt.")
                return
            }
        val legacySaksbehandler =
            LegacySaksbehandler(
                epostadresse = saksbehandlerFraDatabase.epostadresse,
                oid = saksbehandlerFraDatabase.oid,
                navn = saksbehandlerFraDatabase.navn,
                ident = saksbehandlerFraDatabase.ident,
                tilgangskontroll = tilgangskontroll,
            )
        oppgave.forsøkTildelingVedReservasjon(legacySaksbehandler)
    }

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) = oppgaveDao.harFerdigstiltOppgave(vedtaksperiodeId)
}
