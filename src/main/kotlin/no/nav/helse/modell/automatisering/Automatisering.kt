package no.nav.helse.modell.automatisering

import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import java.util.*

internal class Automatisering(
    private val vedtakDao: VedtakDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val automatiseringDao: AutomatiseringDao,
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) {
    fun vurder(fødselsnummer: String, vedtaksperiodeId: UUID): Automatiseringsvurdering {
        val risikovurdering =
            risikovurderingDao.hentRisikovurdering(vedtaksperiodeId) ?: validering("Mangler vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning") { false }
        val warnings = vedtakDao.finnWarnings(vedtaksperiodeId)
        val oppgavetype = vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId)
        val erDigital = digitalKontaktinformasjonDao.erDigital(fødselsnummer)
        val antallÅpneGosysoppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(fødselsnummer)

        return valider(
            risikovurdering,
            validering("Har varsler") { warnings.isEmpty() },
            validering("Behandlingen kan ikke automatiseres") { oppgavetype == Saksbehandleroppgavetype.FORLENGELSE },
            validering("Bruker er reservert eller mangler oppdatert samtykke i DKIF") { erDigital ?: false },
            validering("Det finnes åpne oppgaver på sykepenger i Gosys") { antallÅpneGosysoppgaver?.let { it == 0 } ?: false },
            validering("Vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning er skrudd av") { miljøstyrtFeatureToggle.risikovurdering() },
            validering("Automatisering er skrudd av") { miljøstyrtFeatureToggle.automatisering() }
        )
    }

    fun lagre(vurdering: Automatiseringsvurdering, vedtaksperiodeId: UUID, hendelseId: UUID) {
        vurdering.lagre(vedtaksperiodeId, hendelseId, automatiseringDao)
    }

    fun harBlittAutomatiskBehandlet(vedtaksperiodeId: UUID, hendelseId: UUID) =
        automatiseringDao.hentAutomatisering(vedtaksperiodeId, hendelseId)?.automatisert ?: false

    private fun valider(vararg valideringer: AutomatiseringValidering): Automatiseringsvurdering {
        val validations = valideringer.toList()
        val problems = mutableListOf<String>()

        validations
            .forEach { if (!it.valider()) problems.add(it.error()) }
        return Automatiseringsvurdering(problems)
    }

    private fun validering(error: String, validering: () -> Boolean) =
        object : AutomatiseringValidering {
            override fun valider() = validering()
            override fun error() = error
        }

    class Automatiseringsvurdering(private val problems: MutableList<String>) {
        fun erAutomatiserbar() = problems.isEmpty()
        fun lagre(vedtaksperiodeId: UUID, hendelseId: UUID, automatiseringDao: AutomatiseringDao) {
            automatiseringDao.lagre(erAutomatiserbar(), problems.toList(), vedtaksperiodeId, hendelseId)
        }
    }
}

interface AutomatiseringValidering {
    fun valider(): Boolean
    fun error(): String
}

