package no.nav.helse.modell.kommando

import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import org.slf4j.LoggerFactory

internal class OpprettMinimaltVedtakCommand(
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
) : Command {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val personRef = requireNotNull(personDao.findPersonByFødselsnummer(fødselsnummer))
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(organisasjonsnummer))
        if (vedtakDao.finnVedtakId(vedtaksperiodeId) != null) {
            sikkerlogg.info(
                "Oppretter ikke vedtaksperiode for {}, {}, {}, da denne finnes i databasen allerede",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("orgnummer", organisasjonsnummer),
            )
            return true
        }
        return opprett(personRef, arbeidsgiverRef)
    }

    private fun opprett(personRef: Long, arbeidsgiverRef: Long): Boolean {
        sikkerlogg.info(
            "Oppretter minimal vedtaksperiode for {}, {}, {}",
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("fødselsnummer", fødselsnummer),
            keyValue("orgnummer", organisasjonsnummer),
        )
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
