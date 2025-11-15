package no.nav.helse.spesialist.application

import no.nav.helse.db.NotatDao
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID

class InMemoryNotatDao(
    private val oppgaveRepository: OppgaveRepository,
    private val notatRepository: NotatRepository
) : NotatDao {
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
