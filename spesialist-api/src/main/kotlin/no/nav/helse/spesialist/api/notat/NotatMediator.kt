package no.nav.helse.spesialist.api.notat

import org.slf4j.LoggerFactory
import java.util.*

class NotatMediator(
    private val notatDao: NotatDao,
) {

    private companion object {
        private val log = LoggerFactory.getLogger(NotatMediator::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    fun lagre(vedtaksperiodeId: UUID, tekst: String, saksbehandler_oid: UUID, notatType: NotatType = NotatType.Generelt) =
        notatDao.opprettNotat(vedtaksperiodeId, tekst, saksbehandler_oid, notatType)

    fun lagreForOppgaveId(oppgaveId: Long, tekst: String, saksbehandler_oid: UUID, notatType: NotatType = NotatType.Generelt): Long? {
        log.info("{} lagrer {} notat for oppgaveId {}", saksbehandler_oid, notatType, oppgaveId)
        return notatDao.opprettNotatForOppgaveId(oppgaveId, tekst, saksbehandler_oid, notatType)
    }

    fun finn(vedtaksperiodeIds: List<UUID>) =
        notatDao.finnNotater(vedtaksperiodeIds)

    fun feilregistrer(notatId: Int, saksbehandler_oid: UUID): Boolean {
        val notat = notatDao.finnNotat(notatId)
        if (notat == null ){
            log.warn("Fant ikke notat til feilregistrering med id=${notatId}")
            return false
        }
        if (notat.saksbehandlerOid != saksbehandler_oid){
            sikkerLogg.warn("saksbehandler med oid=${saksbehandler_oid} kan ikke feilregistrere notat oppført av annen saksbehandler med oid=${notat.saksbehandlerOid}")
            return false
        }
        notatDao.feilregistrer(notatId, saksbehandler_oid)
        sikkerLogg.info("notat med id=${notatId} ble feilregistrert: {}", notat)
        return true
    }
}
