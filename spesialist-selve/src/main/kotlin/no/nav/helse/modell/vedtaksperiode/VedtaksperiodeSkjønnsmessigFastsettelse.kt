package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.mediator.meldinger.VedtaksperiodeHendelse
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.command.KlargjørArbeidsforholdCommand
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.egenansatt.KontrollerEgenAnsattstatus
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.KlargjørArbeidsgiverCommand
import no.nav.helse.modell.kommando.KlargjørPersonCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class VedtaksperiodeSkjønnsmessigFastsettelse(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    aktørId: String,
    private val json: String,
    organisasjonsnummer: String,
    snapshotDao: SnapshotDao,
    snapshotClient: SnapshotClient,
    personDao: PersonDao,
    generasjonRepository: ActualGenerasjonRepository,
    arbeidsgiverDao: ArbeidsgiverDao,
    arbeidsforholdDao: ArbeidsforholdDao,
    egenAnsattDao: EgenAnsattDao,
) : Kommandohendelse, VedtaksperiodeHendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        KlargjørPersonCommand(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            førsteKjenteDagFinner = { generasjonRepository.førsteKjenteDag(fødselsnummer)},
            personDao = personDao
        ),
        KlargjørArbeidsgiverCommand(
            orgnummere = listOf(organisasjonsnummer),
            arbeidsgiverDao = arbeidsgiverDao,
        ),
        KlargjørArbeidsforholdCommand(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            førstegangsbehandling = true
        ),
        KontrollerEgenAnsattstatus(
            fødselsnummer = fødselsnummer,
            egenAnsattDao = egenAnsattDao,
        ),
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            personDao = personDao,
        ),
        ikkesuspenderendeCommand("markerPersonSomKlarForVisning") {
            personDao.markerPersonSomKlarForVisning(fødselsnummer)
        }
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json
}
