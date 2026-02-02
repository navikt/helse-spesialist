package no.nav.helse.mediator.oppgave

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.ny
import no.nav.helse.modell.saksbehandler.handlinger.EndrePåVent
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.modell.saksbehandler.handlinger.Oppgavehandling
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgruppehenter
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgruppehenter.Feil
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
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
    private val oppgaveRepository: OppgaveRepository,
    private val tilgangsgruppehenter: Tilgangsgruppehenter,
) : Oppgavehåndterer,
    Oppgavefinner {
    fun nyOppgaveService(sessionContext: SessionContext): OppgaveService =
        OppgaveService(
            oppgaveDao = sessionContext.oppgaveDao,
            reservasjonDao = sessionContext.reservasjonDao,
            meldingPubliserer = meldingPubliserer,
            oppgaveRepository = sessionContext.oppgaveRepository,
            tilgangsgruppehenter = tilgangsgruppehenter,
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
        teamLogs.info("Oppretter saksbehandleroppgave for {}", kv("fødselsnummer", fødselsnummer))
        val nesteId = oppgaveDao.reserverNesteId()
        val oppgavemelder = Oppgavemelder(fødselsnummer, meldingPubliserer)
        val oppgave =
            ny(
                id = nesteId,
                førsteOpprettet = oppgaveRepository.førsteOpprettetForBehandlingId(behandlingId),
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
        oppgaveRepository.lagre(oppgave)
    }

    fun <T> oppgave(
        id: Long,
        oppgaveBlock: Oppgave.() -> T,
    ): T {
        val oppgave = oppgaveRepository.finn(id) ?: error("Forventer å finne oppgave med oppgaveId=$id")
        val fødselsnummer = oppgaveDao.finnFødselsnummer(id)
        oppgave.register(Oppgavemelder(fødselsnummer, meldingPubliserer))
        val returverdi = oppgaveBlock(oppgave)
        oppgaveRepository.lagre(oppgave)
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
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        oppgave(handling.oppgaveId()) {
            handling.oppgave(this)
            handling.utførAv(saksbehandlerWrapper)
        }
    }

    // Sørger for at vi alltid bruker den aktive oppgaven, i tilfelle saksbehandler har utdaterte data når de forsøker å utføre handlingen
    private fun finnAktivOppgaveId(oppgaveId: Long): Long? = oppgaveDao.finnVedtaksperiodeId(oppgaveId).let { oppgaveDao.finnIdForAktivOppgave(it) }

    fun leggPåVent(
        handling: LeggPåVent,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        finnAktivOppgaveId(handling.oppgaveId)?.let { oppgaveId ->
            oppgave(oppgaveId) {
                this.leggPåVent(handling.skalTildeles, saksbehandlerWrapper)
            }
        }
    }

    fun endrePåVent(
        handling: EndrePåVent,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        finnAktivOppgaveId(handling.oppgaveId)?.let { oppgaveId ->
            oppgave(oppgaveId) {
                this.endrePåVent(handling.skalTildeles, saksbehandlerWrapper)
            }
        }
    }

    fun fjernFraPåVent(oppgaveId: Long) {
        finnAktivOppgaveId(oppgaveId)?.let { aktivOppgaveId ->
            oppgave(aktivOppgaveId) {
                this.fjernFraPåVent()
            }
        }
    }

    override fun endretEgenAnsattStatus(
        erEgenAnsatt: Boolean,
        fødselsnummer: String,
    ) {
        val oppgaveId =
            oppgaveDao.finnOppgaveId(fødselsnummer) ?: run {
                teamLogs.info("Ingen aktiv oppgave for {}", kv("fødselsnummer", fødselsnummer))
                return
            }
        oppgave(oppgaveId) {
            if (erEgenAnsatt) {
                logg.info("Legger til egenskap EGEN_ANSATT på {}", kv("oppgaveId", oppgaveId))
                teamLogs.info("Legger til egenskap EGEN_ANSATT for {}", kv("fødselsnummer", fødselsnummer))
                leggTilEgenAnsatt()
            } else {
                logg.info("Fjerner egenskap EGEN_ANSATT på {}", kv("oppgaveId", oppgaveId))
                teamLogs.info("Fjerner egenskap EGEN_ANSATT for {}", kv("fødselsnummer", fødselsnummer))
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
            teamLogs.warn("Kunne ikke reservere person", e)
        }
    }

    fun finnFødselsnummer(oppgavereferanse: Long): String = oppgaveDao.finnFødselsnummer(oppgavereferanse)

    private fun tildelVedReservasjon(
        fødselsnummer: String,
        oppgave: Oppgave,
    ) {
        val (saksbehandler) =
            reservasjonDao.hentReservasjonFor(fødselsnummer) ?: run {
                logg.info("Finner ingen reservasjon for $oppgave, blir ikke tildelt.")
                return
            }

        when (val result = tilgangsgruppehenter.hentTilgangsgrupper(saksbehandler.id)) {
            is Either.Failure<Set<Brukerrolle>, Feil> -> {
                when (result.error) {
                    Feil.SaksbehandlerFinnesIkke -> {
                        logg.info("Saksbehandler personen er reservert til eksisterer ikke (lenger). Tildeler ikke oppgaven.")
                    }
                }
            }

            is Either.Success<Set<Brukerrolle>, Feil> -> {
                try {
                    oppgave.forsøkTildelingVedReservasjon(SaksbehandlerWrapper(saksbehandler = saksbehandler), result.result)
                } catch (manglerTilgang: ManglerTilgang) {
                    logg.info("Saksbehandler har ikke (lenger) tilgang til egenskapene i denne oppgaven, tildeler ikke tross reservasjon")
                    teamLogs.info(
                        "Saksbehandler har ikke (lenger) tilgang til egenskapene i denne oppgaven, tildeler ikke tross reservasjon",
                        manglerTilgang,
                    )
                }
            }
        }
    }

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) = oppgaveDao.harFerdigstiltOppgave(vedtaksperiodeId)
}
