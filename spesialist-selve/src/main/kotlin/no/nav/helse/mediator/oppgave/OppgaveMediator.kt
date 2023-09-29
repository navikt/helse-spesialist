package no.nav.helse.mediator.oppgave

import java.sql.SQLException
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.mediator.TilgangskontrollørForApi
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.Companion.tilgangsstyrteEgenskaper
import no.nav.helse.modell.oppgave.Egenskap.Companion.toMap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload.Companion.lagre
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Periodetype
import no.nav.helse.spesialist.api.graphql.schema.Personnavn
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.tildeling.Oppgavehåndterer
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import org.slf4j.LoggerFactory

interface Oppgavefinner {
    fun oppgave(utbetalingId: UUID, oppgaveBlock: Oppgave?.() -> Unit)
}

internal class OppgaveMediator(
    private val hendelseDao: HendelseDao,
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val reservasjonDao: ReservasjonDao,
    private val opptegnelseDao: OpptegnelseDao,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val rapidsConnection: RapidsConnection,
    private val tilgangskontroll: Tilgangskontroll,
    private val tilgangsgrupper: Tilgangsgrupper
): Oppgavehåndterer, Oppgavefinner {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    internal fun nyOppgave(fødselsnummer: String, contextId: UUID, opprettOppgaveBlock: (reservertId: Long) -> Oppgave) {
        val nesteId = oppgaveDao.reserverNesteId()
        val oppgave = opprettOppgaveBlock(nesteId)
        val oppgavemelder = Oppgavemelder(hendelseDao, oppgaveDao, rapidsConnection)
        oppgavemelder.oppgaveOpprettet(oppgave)
        oppgave.register(oppgavemelder)
        tildelVedReservasjon(fødselsnummer, oppgave)
        Oppgavelagrer(tildelingDao).apply {
            oppgave.accept(this)
            this.lagre(this@OppgaveMediator, contextId)
        }
    }

    fun <T> oppgave(id: Long, oppgaveBlock: Oppgave.() -> T): T {
        val oppgave = Oppgavehenter(oppgaveDao, totrinnsvurderingRepository, saksbehandlerRepository, tilgangskontroll).oppgave(id)
        oppgave.register(Oppgavemelder(hendelseDao, oppgaveDao, rapidsConnection))
        val returverdi = oppgaveBlock(oppgave)
        Oppgavelagrer(tildelingDao).apply {
            oppgave.accept(this)
            oppdater(this@OppgaveMediator)
        }
        return returverdi
    }

    override fun oppgave(utbetalingId: UUID, oppgaveBlock: Oppgave?.() -> Unit) {
        val oppgaveId = oppgaveDao.finnOppgaveId(utbetalingId)
        oppgaveId?.let {
            oppgave(it, oppgaveBlock)
        }
    }

    internal fun lagreTotrinnsvurdering(totrinnsvurderingFraDatabase: TotrinnsvurderingFraDatabase) {
        totrinnsvurderingRepository.oppdater(totrinnsvurderingFraDatabase)
    }

    override fun sendTilBeslutter(oppgaveId: Long, behandlendeSaksbehandler: SaksbehandlerFraApi) {
        val saksbehandler = behandlendeSaksbehandler.tilSaksbehandler()
        oppgave(oppgaveId) {
            sendTilBeslutter(saksbehandler)
        }
    }

    override fun sendIRetur(oppgaveId: Long, besluttendeSaksbehandler: SaksbehandlerFraApi) {
        val saksbehandler = besluttendeSaksbehandler.tilSaksbehandler()
        oppgave(oppgaveId) {
            sendIRetur(saksbehandler)
        }
    }

    override fun leggPåVent(oppgaveId: Long): TildelingApiDto {
        return oppgave(oppgaveId) {
            val tildeling = this.leggPåVent()
            tildeling.let {
                TildelingApiDto(it.navn, it.epost, it.oid, it.påVent)
            }
        }
    }

    override fun fjernPåVent(oppgaveId: Long): TildelingApiDto {
        return oppgave(oppgaveId) {
            val tildeling = this.fjernPåVent()
            tildeling.let {
                TildelingApiDto(it.navn, it.epost, it.oid, it.påVent)
            }
        }
    }

    override fun venterPåSaksbehandler(oppgaveId: Long): Boolean = oppgaveDao.venterPåSaksbehandler(oppgaveId)
    override fun erRiskoppgave(oppgaveId: Long): Boolean = oppgaveDao.erRiskoppgave(oppgaveId)

    override fun oppgaver(saksbehandlerFraApi: SaksbehandlerFraApi): List<OppgaveTilBehandling> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val oppgaver = oppgaveDao.finnOppgaverForVisning().groupBy { it.egenskaper.tilEgenskaper() }
        val oppgaverSaksbehandlerHarTilgangTil = oppgaver.filterKeys { saksbehandler.harTilgangTil(it.tilgangsstyrteEgenskaper()) }
        return oppgaverSaksbehandlerHarTilgangTil.flatMap { it.value }.tilOppgaveTilBehandling()
    }

    fun avbrytOppgaver(vedtaksperiodeId: UUID) {
        oppgaveDao.finnNyesteOppgaveId(vedtaksperiodeId)?.also {
            oppgave(it) {
                this.avbryt()
            }
        }
    }

    internal fun opprett(
        id: Long,
        contextId: UUID,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        egenskap: String,
        egenskaper: List<String>,
        hendelseId: UUID
    ) {
        if (oppgaveDao.harGyldigOppgave(utbetalingId)) {
            sikkerlogg.info("Utbetaling med {} har gyldig oppgave.", kv("utbetalingId", utbetalingId))
            return
        }
        oppgaveDao.opprettOppgave(id, contextId, egenskap, egenskaper, vedtaksperiodeId, utbetalingId)
        GodkjenningsbehovPayload(hendelseId).lagre(opptegnelseDao, oppgaveDao.finnFødselsnummer(id))
    }

    fun oppdater(
        oppgaveId: Long,
        status: String,
        ferdigstiltAvIdent: String?,
        ferdigstiltAvOid: UUID?,
        egenskaper: List<String>,
    ) {
        oppgaveDao.updateOppgave(oppgaveId, status, ferdigstiltAvIdent, ferdigstiltAvOid, egenskaper)
    }

    fun reserverOppgave(saksbehandleroid: UUID, fødselsnummer: String) {
        try {
            reservasjonDao.reserverPerson(saksbehandleroid, fødselsnummer, false)
        } catch (e: SQLException) {
            logg.warn("Kunne ikke reservere person")
        }
    }

    private fun tildelVedReservasjon(fødselsnummer: String, oppgave: Oppgave) {
        val (saksbehandlerFraDatabase, settPåVent) = reservasjonDao.hentReservasjonFor(fødselsnummer) ?: return
        val saksbehandler = Saksbehandler(
            epostadresse = saksbehandlerFraDatabase.epostadresse,
            oid = saksbehandlerFraDatabase.oid,
            navn = saksbehandlerFraDatabase.navn,
            ident = saksbehandlerFraDatabase.ident,
            tilgangskontroll = tilgangskontroll
        )
        oppgave.forsøkTildelingVedReservasjon(saksbehandler, settPåVent)
    }

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) = oppgaveDao.harFerdigstiltOppgave(vedtaksperiodeId)

    private fun SaksbehandlerFraApi.tilSaksbehandler() = Saksbehandler(
        epostadresse = epost,
        oid = oid,
        navn = navn,
        ident = ident,
        tilgangskontroll = TilgangskontrollørForApi(grupper, tilgangsgrupper),
    )

    private fun List<String>.tilEgenskaper(): List<Egenskap> = this.map { enumValueOf<Egenskap>(it) }

    private fun List<OppgaveFraDatabaseForVisning>.tilOppgaveTilBehandling() = map {
        OppgaveTilBehandling(
            id = it.id.toString(),
            opprettet = it.opprettet.toString(),
            opprinneligSoknadsdato = it.opprinneligSøknadsdato.toString(),
            vedtaksperiodeId = it.vedtaksperiodeId.toString(),
            navn = Personnavn(
                fornavn = it.navn.fornavn,
                etternavn = it.navn.etternavn,
                mellomnavn = it.navn.mellomnavn,
            ),
            aktorId = it.aktørId,
            inntektskilde = inntektskilde(it.inntektskilde),
            tildeling = it.tildelt?.let { tildelt ->
                Tildeling(
                    tildelt.navn,
                    tildelt.epostadresse,
                    tildelt.oid.toString(),
                    it.påVent
                )
            },
            periodetype = periodetype(it.periodetype),
            egenskaper = it.egenskaper.tilEgenskaper().toMap()
        )
    }

    private fun periodetype(periodetype: String): Periodetype {
        return when (periodetype) {
            "INFOTRYGDFORLENGELSE" -> Periodetype.INFOTRYGDFORLENGELSE
            "FØRSTEGANGSBEHANDLING" -> Periodetype.FORSTEGANGSBEHANDLING
            "OVERGANG_FRA_IT" -> Periodetype.OVERGANG_FRA_IT
            "FORLENGELSE" -> Periodetype.FORLENGELSE
            else -> throw IllegalArgumentException("$periodetype er ikke en gyldig periodetype")
        }
    }

    private fun inntektskilde(inntektskilde: String): String {
        return when (inntektskilde) {
            "EN_ARBEIDSGIVER" -> "EN_ARBEIDSGIVER"
            "FLERE_ARBEIDSGIVERE" -> "FLERE_ARBEIDSGIVERE"
            else -> throw IllegalArgumentException("$inntektskilde er ikke en gyldig inntektskilde")
        }
    }
}
