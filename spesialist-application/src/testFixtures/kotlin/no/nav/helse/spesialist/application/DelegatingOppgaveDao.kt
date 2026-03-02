package no.nav.helse.spesialist.application

import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand
import java.time.LocalDate
import java.util.UUID

class DelegatingOppgaveDao(
    private val oppgaveRepository: InMemoryOppgaveRepository,
    private val behandlingRepository: InMemoryBehandlingRepository,
    private val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository,
    private val personRepository: InMemoryPersonRepository,
    private val totrinnsvurderingRepository: InMemoryTotrinnsvurderingRepository,
    private val saksbehandlerRepository: InMemorySaksbehandlerRepository,
) : OppgaveDao {
    override fun finnOppgaveId(fødselsnummer: String): Long? {
        val vedtaksperiodeIder =
            vedtaksperiodeRepository.alle().filter { it.identitetsnummer.value == fødselsnummer }.map { it.id.value }
        return oppgaveRepository.alle()
            .filter { it.vedtaksperiodeId in vedtaksperiodeIder }
            .firstOrNull { it.tilstand is Oppgave.AvventerSaksbehandler }
            ?.id
    }

    override fun finnSpleisBehandlingId(oppgaveId: Long): UUID =
        behandlingRepository
            .finn(BehandlingUnikId(oppgaveRepository.finn(oppgaveId)!!.behandlingId))!!
            .spleisBehandlingId!!
            .value

    override fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long {
        val vedtaksperiodeIder =
            vedtaksperiodeRepository.alle().filter { it.identitetsnummer.value == fødselsnummer }.map { it.id.value }
        return oppgaveRepository.alle()
            .filter { it.vedtaksperiodeId in vedtaksperiodeIder }
            .maxOf { it.id }
    }

    override fun finnBehandlingId(oppgaveId: Long): UUID =
        behandlingRepository.alle()
            .find { it.spleisBehandlingId?.value == oppgaveRepository.finn(oppgaveId)!!.behandlingId }!!
            .id.value

    override fun finnOppgaveId(utbetalingId: UUID): Long? =
        oppgaveRepository.alle()
            .filter { it.utbetalingId == utbetalingId && it.tilstand !is Oppgave.Invalidert && it.tilstand !is Oppgave.Ferdigstilt }
            .maxByOrNull { it.id }
            ?.id

    override fun finnVedtaksperiodeId(oppgaveId: Long): UUID =
        oppgaveRepository.finn(oppgaveId)!!.vedtaksperiodeId

    override fun invaliderOppgave(oppgaveId: Long) {
        oppgaveRepository.finn(oppgaveId)!!.also { oppgave ->
            oppgave.avbryt()
            oppgaveRepository.lagre(oppgave)
        }
    }

    override fun reserverNesteId(): Long =
        (oppgaveRepository.alle().maxOfOrNull { it.id } ?: 0L) + 1L

    override fun finnAntallOppgaver(saksbehandlerOid: UUID): AntallOppgaverFraDatabase {
        val mineSaker = oppgaveRepository.alle()
            .filter { it.tilstand is Oppgave.AvventerSaksbehandler && it.tildeltTil?.value == saksbehandlerOid }
        return AntallOppgaverFraDatabase(
            antallMineSaker = mineSaker.count { Egenskap.PÅ_VENT !in it.egenskaper },
            antallMineSakerPåVent = mineSaker.count { Egenskap.PÅ_VENT in it.egenskaper },
        )
    }

    override fun finnBehandledeOppgaver(
        behandletAvOid: UUID,
        offset: Int,
        limit: Int,
        fom: LocalDate,
        tom: LocalDate,
    ): List<BehandletOppgaveFraDatabaseForVisning> {
        val fødselsnummerForGodkjentTotrinnsvurdering = totrinnsvurderingRepository.alle()
            .filter { ttv ->
                (ttv.saksbehandler?.value == behandletAvOid || ttv.beslutter?.value == behandletAvOid) &&
                    ttv.oppdatert?.toLocalDate()?.let { it in fom..tom } == true &&
                    ttv.tilstand == TotrinnsvurderingTilstand.GODKJENT
            }
            .map { it.fødselsnummer }
            .toSet()

        val aktuelle = oppgaveRepository.alle()
            .filter { oppgave ->
                oppgave.tilstand is Oppgave.Ferdigstilt || oppgave.tilstand is Oppgave.AvventerSystem
            }
            .filter { oppgave ->
                val oppdatert = oppgaveRepository.hentOppdatertTidspunkt(oppgave.id)?.toLocalDate()
                oppdatert != null && oppdatert >= fom && oppdatert <= tom
            }
            .filter { oppgave ->
                val fødselsnummer = vedtaksperiodeRepository.alle()
                    .find { it.id.value == oppgave.vedtaksperiodeId }?.identitetsnummer?.value
                oppgave.ferdigstiltAvOid == behandletAvOid || fødselsnummer in fødselsnummerForGodkjentTotrinnsvurdering
            }
            .sortedBy { oppgaveRepository.hentOppdatertTidspunkt(it.id) }

        val totaltAntall = aktuelle.size

        return aktuelle.drop(offset).take(limit).map { oppgave ->
            val vedtaksperiode = vedtaksperiodeRepository.alle().find { it.id.value == oppgave.vedtaksperiodeId }
            val person = vedtaksperiode?.let { vp -> personRepository.alle().find { it.id == vp.identitetsnummer } }
            val totrinnsvurdering = vedtaksperiode?.let { vp ->
                totrinnsvurderingRepository.alle().find { it.fødselsnummer == vp.identitetsnummer.value }
            }
            val saksbehandlerIdent = totrinnsvurdering?.saksbehandler?.let { oid ->
                saksbehandlerRepository.alle().find { it.id.value == oid.value }?.ident?.value
            } ?: oppgave.ferdigstiltAvIdent?.value
            val beslutterIdent = totrinnsvurdering?.beslutter?.let { oid ->
                saksbehandlerRepository.alle().find { it.id.value == oid.value }?.ident?.value
            }
            BehandletOppgaveFraDatabaseForVisning(
                id = oppgave.id,
                aktørId = person?.aktørId ?: "",
                fødselsnummer = vedtaksperiode?.identitetsnummer?.value ?: "",
                egenskaper = oppgave.egenskaper.map { EgenskapForDatabase.valueOf(it.name) }.toSet(),
                ferdigstiltTidspunkt = oppgaveRepository.hentOppdatertTidspunkt(oppgave.id) ?: oppgave.opprettet,
                ferdigstiltAv = oppgave.ferdigstiltAvIdent?.value,
                saksbehandler = saksbehandlerIdent,
                beslutter = beslutterIdent,
                navn = PersonnavnFraDatabase(
                    fornavn = person?.info?.fornavn ?: "",
                    mellomnavn = person?.info?.mellomnavn,
                    etternavn = person?.info?.etternavn ?: "",
                ),
                filtrertAntall = totaltAntall,
            )
        }
    }

    override fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<EgenskapForDatabase>? =
        oppgaveRepository.alle()
            .filter { it.vedtaksperiodeId == vedtaksperiodeId && it.utbetalingId == utbetalingId }
            .maxByOrNull { it.opprettet }
            ?.egenskaper?.map { EgenskapForDatabase.valueOf(it.name) }?.toSet()

    override fun finnIdForAktivOppgave(vedtaksperiodeId: UUID): Long? =
        oppgaveRepository.alle()
            .filter { it.vedtaksperiodeId == vedtaksperiodeId && it.tilstand !is Oppgave.Ferdigstilt && it.tilstand !is Oppgave.Invalidert }
            .maxByOrNull { it.opprettet }
            ?.id

    override fun finnFødselsnummer(oppgaveId: Long): String {
        val vedtaksperiodeId = oppgaveRepository.finn(oppgaveId)!!.vedtaksperiodeId
        return vedtaksperiodeRepository.alle().find { it.id.value == vedtaksperiodeId }!!.identitetsnummer.value
    }

    override fun harFerdigstiltOppgave(vedtaksperiodeId: UUID): Boolean =
        oppgaveRepository.alle()
            .any { it.vedtaksperiodeId == vedtaksperiodeId && it.tilstand is Oppgave.Ferdigstilt }

    override fun oppdaterPekerTilGodkjenningsbehov(
        godkjenningsbehovId: UUID,
        utbetalingId: UUID,
    ) {
        oppgaveRepository.oppdaterGodkjenningsbehov(utbetalingId, godkjenningsbehovId)
    }
}
