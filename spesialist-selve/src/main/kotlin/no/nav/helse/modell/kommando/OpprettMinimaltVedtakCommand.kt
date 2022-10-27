package no.nav.helse.modell.kommando

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import org.slf4j.LoggerFactory

internal class OpprettMinimaltVedtakCommand(
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettMinimaltVedtakCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        return opprett()
    }

    private fun opprett(): Boolean {
        val personRef = requireNotNull(personDao.findPersonByFødselsnummer(fødselsnummer))
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer))
        log.info("Oppretter minimalt vedtak for vedtaksperiode: $vedtaksperiodeId for person=$personRef, arbeidsgiver=$arbeidsgiverRef")
        vedtakDao.opprett(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = periodeFom,
            tom = periodeTom,
            personRef = personRef,
            arbeidsgiverRef = arbeidsgiverRef,
            snapshotRef = null
        )

        return true
    }
}
