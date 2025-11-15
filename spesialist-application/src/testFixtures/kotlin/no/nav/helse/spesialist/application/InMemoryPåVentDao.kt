package no.nav.helse.spesialist.application

import no.nav.helse.db.PåVentDao
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.time.LocalDate
import java.util.UUID

class InMemoryPåVentDao(
    private val påVentRepository: InMemoryPåVentRepository,
    private val oppgaveRepository: InMemoryOppgaveRepository,
) : PåVentDao {
    override fun erPåVent(vedtaksperiodeId: UUID): Boolean =
        påVentRepository.alle().any { it.vedtaksperiodeId == vedtaksperiodeId }

    override fun lagrePåVent(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
        frist: LocalDate?,
        årsaker: List<PåVentÅrsak>,
        notatTekst: String?,
        dialogRef: Long
    ) {
        påVentRepository.lagre(
            PåVent.Factory.ny(
                vedtaksperiodeId = oppgaveRepository.finn(oppgaveId)!!.vedtaksperiodeId,
                saksbehandlerOid = SaksbehandlerOid(saksbehandlerOid),
                frist = frist!!,
                dialogRef = DialogId(dialogRef),
                årsaker = årsaker.map { it.årsak },
                notattekst = notatTekst
            )
        )
    }

    override fun slettPåVent(oppgaveId: Long): Int {
        val vedtaksperiodeId = oppgaveRepository.finn(oppgaveId)!!.vedtaksperiodeId
        val påVenter = påVentRepository.alle()
            .filter { it.vedtaksperiodeId == vedtaksperiodeId }
            .onEach { påVentRepository.slett(it.id()) }
        return påVenter.size
    }

    override fun erPåVent(oppgaveId: Long): Boolean =
        erPåVent(oppgaveRepository.finn(oppgaveId)!!.vedtaksperiodeId)

    override fun oppdaterPåVent(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
        frist: LocalDate?,
        årsaker: List<PåVentÅrsak>,
        notatTekst: String?,
        dialogRef: Long
    ) {
        slettPåVent(oppgaveId)
        lagrePåVent(
            oppgaveId = oppgaveId,
            saksbehandlerOid = saksbehandlerOid,
            frist = frist,
            årsaker = årsaker,
            notatTekst = notatTekst,
            dialogRef = dialogRef
        )
    }
}
