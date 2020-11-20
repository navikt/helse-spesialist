package no.nav.helse.modell.automatisering

import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import java.util.*

internal class Automatisering(
    private val vedtakDao: VedtakDao,
    private val warningDao: WarningDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val automatiseringDao: AutomatiseringDao,
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val egenAnsattDao: EgenAnsattDao,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle,
    private val personDao: PersonDao,
    private val stikkprøveVelger: StikkprøveVelger
) {
    private val automatiserbareOppgavetyper = listOf(
        Saksbehandleroppgavetype.FORLENGELSE,
        Saksbehandleroppgavetype.INFOTRYGDFORLENGELSE,
        Saksbehandleroppgavetype.OVERGANG_FRA_IT
    )

    internal fun utfør(fødselsnummer: String, vedtaksperiodeId: UUID, hendelseId: UUID, onAutomatiserbar: () -> Unit) {
        val problemer = vurder(fødselsnummer, vedtaksperiodeId)

        problemer.isEmpty().let { skalAutomatiskGodkjennes ->
            if (skalAutomatiskGodkjennes) onAutomatiserbar()
            automatiseringDao.lagre(skalAutomatiskGodkjennes, problemer, vedtaksperiodeId, hendelseId)
        }
    }

    private fun vurder(fødselsnummer: String, vedtaksperiodeId: UUID): List<String> {
        val risikovurdering =
            risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
                ?: validering("Mangler vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning") { false }
        val warnings = warningDao.finnWarnings(vedtaksperiodeId)
        val oppgavetype = vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId)
        val erDigital = digitalKontaktinformasjonDao.erDigital(fødselsnummer)
        val erEgenAnsatt = egenAnsattDao.erEgenAnsatt(fødselsnummer)
        val tilhørerUtlandsenhet = personDao.tilhørerUtlandsenhet(fødselsnummer)
        val antallÅpneGosysoppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(fødselsnummer)

        return valider(
            risikovurdering,
            validering("Har varsler") { warnings.isEmpty() },
            validering("Behandlingen kan ikke automatiseres") { oppgavetype in automatiserbareOppgavetyper },
            validering("Bruker er reservert eller mangler oppdatert samtykke i DKIF") { erDigital ?: false },
            validering("Det finnes åpne oppgaver på sykepenger i Gosys") {
                antallÅpneGosysoppgaver?.let { it == 0 } ?: false
            },
            validering("Vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning er skrudd av") { miljøstyrtFeatureToggle.risikovurdering() },
            validering("Bruker er ansatt i Nav") { erEgenAnsatt == false || erEgenAnsatt == null },
            validering("Bruker tilhører utlandsenhet") { !tilhørerUtlandsenhet },
            validering("Automatisering er skrudd av") { miljøstyrtFeatureToggle.automatisering() }
        ).also {
            return if (it.isEmpty() && stikkprøveVelger()) it + "Saken er plukket ut til manuell saksbehandling"
            else it
        }
    }

    internal fun harBlittAutomatiskBehandlet(vedtaksperiodeId: UUID, hendelseId: UUID) =
        automatiseringDao.hentAutomatisering(vedtaksperiodeId, hendelseId)?.automatisert ?: false

    private fun valider(vararg valideringer: AutomatiseringValidering): List<String> {
        return valideringer.toList()
            .filterNot { it.valider() }
            .map { it.error() }
    }

    private fun validering(error: String, validering: () -> Boolean) =
        object : AutomatiseringValidering {
            override fun valider() = validering()
            override fun error() = error
        }

}

typealias StikkprøveVelger = () -> Boolean

interface AutomatiseringValidering {
    fun valider(): Boolean
    fun error(): String
}

