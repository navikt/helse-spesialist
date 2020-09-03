package no.nav.helse.modell.person

import kotliquery.Session
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import java.time.Duration
import java.util.*

internal class OpprettPersonCommand(
    private val fødselsnummer: String,
    private val aktørId: String,
    eventId: UUID,
    parent: Command
) : Command(
    eventId = eventId,
    parent = parent,
    timeout = Duration.ofHours(1)
) {
    override fun execute(session: Session): Resultat =
        if (session.findPersonByFødselsnummer(fødselsnummer) != null) {
            Resultat.Ok.System
        } else {
            Resultat.HarBehov(
                Behovtype.HentPersoninfo,
                Behovtype.HentEnhet,
                Behovtype.HentInfotrygdutbetalinger()
            )
        }

    override fun resume(session: Session, løsninger: Løsninger) {
        val hentEnhetLøsning = løsninger.løsning<HentEnhetLøsning>()
        val hentPersoninfoLøsning = løsninger.løsning<HentPersoninfoLøsning>()
        val hentInfotrygdutbetalingerLøsning = løsninger.løsning<HentInfotrygdutbetalingerLøsning>()

        val enhetId = hentEnhetLøsning.enhetNr.toInt()
        val navnId = session.insertPersoninfo(
            hentPersoninfoLøsning.fornavn,
            hentPersoninfoLøsning.mellomnavn,
            hentPersoninfoLøsning.etternavn,
            hentPersoninfoLøsning.fødselsdato,
            hentPersoninfoLøsning.kjønn
        )
        val infotrygdutbetalingerId =
            session.insertInfotrygdutbetalinger(hentInfotrygdutbetalingerLøsning.utbetalinger)

        session.insertPerson(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId.toLong(),
            navnId = navnId,
            enhetId = enhetId,
            infotrygdutbetalingerId = infotrygdutbetalingerId
        )
    }
}
