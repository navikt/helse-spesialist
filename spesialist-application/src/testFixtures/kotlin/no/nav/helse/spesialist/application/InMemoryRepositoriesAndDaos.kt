package no.nav.helse.spesialist.application

import no.nav.helse.db.NotatDao
import no.nav.helse.db.PartialOppgaveDao
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID

class InMemoryRepositoriesAndDaos() {
    private val notatRepository = InMemoryNotatRepository()
    private val oppgaveRepository = InMemoryOppgaveRepository()
    private val vedtaksperiodeRepository = InMemoryVedtaksperiodeRepository()
    private val notatDao = object : NotatDao {
        override fun lagreForOppgaveId(
            oppgaveId: Long,
            tekst: String,
            saksbehandlerOid: UUID,
            notatType: NotatType,
            dialogRef: Long
        ) =
            Notat.Factory.ny(
                type = notatType,
                tekst = tekst,
                dialogRef = DialogId(dialogRef),
                vedtaksperiodeId = oppgaveRepository.finn(oppgaveId)!!.vedtaksperiodeId,
                saksbehandlerOid = SaksbehandlerOid(saksbehandlerOid)
            )
                .also(notatRepository::lagre)
                .id().value.toLong()
    }

    private val oppgaveDao = object : PartialOppgaveDao {
        override fun finnOppgaveId(fødselsnummer: String): Long? {
            val vedtaksperiodeIder =
                vedtaksperiodeRepository.finnVedtaksperioder(fødselsnummer).map { it.vedtaksperiodeId }
            return oppgaveRepository.alle()
                .filter { it.vedtaksperiodeId in vedtaksperiodeIder }
                .firstOrNull { it.tilstand is Oppgave.AvventerSaksbehandler }
                ?.id
        }

        override fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long {
            val vedtaksperiodeIder =
                vedtaksperiodeRepository.finnVedtaksperioder(fødselsnummer).map { it.vedtaksperiodeId }
            return oppgaveRepository.alle()
                .filter { it.vedtaksperiodeId in vedtaksperiodeIder }
                .maxOf { it.id }
        }
    }
    private val dialogDao = InMemoryDialogDao()
    private val stansAutomatiskBehandlingDao = InMemoryStansAutomatiskBehandlingDao()
    private val annulleringRepository = InMemoryAnnulleringRepository()

    val daos = InMemoryDaos(
        oppgaveRepository,
        notatDao,
        oppgaveDao,
        dialogDao,
        stansAutomatiskBehandlingDao,
        annulleringRepository,
    )
    val sessionFactory = InMemorySessionFactory(
        notatRepository,
        oppgaveRepository,
        notatDao,
        oppgaveDao,
        vedtaksperiodeRepository,
        dialogDao,
        stansAutomatiskBehandlingDao,
        annulleringRepository,
    )
}
