package no.nav.helse.modell.automatisering

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.Inntektskilde
import org.slf4j.LoggerFactory
import java.util.*

internal class Automatisering(
    private val warningDao: WarningDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val automatiseringDao: AutomatiseringDao,
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val egenAnsattDao: EgenAnsattDao,
    private val personDao: PersonDao,
    private val vedtakDao: VedtakDao,
    private val plukkTilManuell: PlukkTilManuell
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(Automatisering::class.java)
    }

    internal fun utfør(fødselsnummer: String, vedtaksperiodeId: UUID, hendelseId: UUID, onAutomatiserbar: () -> Unit) {
        val problemer = vurder(fødselsnummer, vedtaksperiodeId)

        when {
            problemer.isNotEmpty() ->
                automatiseringDao.manuellSaksbehandling(problemer, vedtaksperiodeId, hendelseId)
            plukkTilManuell() -> {
                automatiseringDao.stikkprøve(vedtaksperiodeId, hendelseId)
                logger.info("Automatisk godkjenning av {} avbrutt, sendes til manuell behandling", keyValue("vedtaksperiodeId", vedtaksperiodeId))
            }
            else -> {
                onAutomatiserbar()
                automatiseringDao.automatisert(vedtaksperiodeId, hendelseId)
            }
        }
    }

    private fun vurder(fødselsnummer: String, vedtaksperiodeId: UUID): List<String> {
        val risikovurdering =
            risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
                ?: validering("Mangler vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning") { false }
        val warnings = warningDao.finnWarnings(vedtaksperiodeId)
        val erDigital = digitalKontaktinformasjonDao.erDigital(fødselsnummer)
        val erEgenAnsatt = egenAnsattDao.erEgenAnsatt(fødselsnummer)
        val tilhørerUtlandsenhet = personDao.tilhørerUtlandsenhet(fødselsnummer)
        val antallÅpneGosysoppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(fødselsnummer)
        val inntektskilde = vedtakDao.finnInntektskilde(vedtaksperiodeId)

        return valider(
            risikovurdering,
            validering("Har varsler") { warnings.isEmpty() },
            validering("Bruker er reservert eller mangler oppdatert samtykke i DKIF") { erDigital ?: false },
            validering("Det finnes åpne oppgaver på sykepenger i Gosys") {
                antallÅpneGosysoppgaver?.let { it == 0 } ?: false
            },
            validering("Bruker er ansatt i Nav") { erEgenAnsatt == false || erEgenAnsatt == null },
            validering("Bruker tilhører utlandsenhet") { !tilhørerUtlandsenhet },
            validering("Har flere arbeidsgivere") { inntektskilde == Inntektskilde.EN_ARBEIDSGIVER },
        )
    }

    internal fun harBlittAutomatiskBehandlet(vedtaksperiodeId: UUID, hendelseId: UUID) =
        automatiseringDao.hentAutomatisering(vedtaksperiodeId, hendelseId)?.automatisert ?: false

    private fun valider(vararg valideringer: AutomatiseringValidering) =
        valideringer.toList()
            .filterNot(AutomatiseringValidering::erAautomatiserbar)
            .map(AutomatiseringValidering::error)

    private fun validering(error: String, automatiserbar: () -> Boolean) =
        object : AutomatiseringValidering {
            override fun erAautomatiserbar() = automatiserbar()
            override fun error() = error
        }

    fun erStikkprøve(vedtaksperiodeId: UUID, hendelseId: UUID) =
        automatiseringDao.plukketUtTilStikkprøve(vedtaksperiodeId, hendelseId)

}

internal typealias PlukkTilManuell = () -> Boolean

internal interface AutomatiseringValidering {
    fun erAautomatiserbar(): Boolean
    fun error(): String
}

