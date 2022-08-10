package no.nav.helse.modell.automatisering

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.meldinger.HentEnhetløsning.Companion.erEnhetUtland
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.delvisRefusjon
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetalingTilSykmeldt
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vergemal.VergemålDao
import org.slf4j.LoggerFactory

internal class Automatisering(
    private val warningDao: WarningDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val automatiseringDao: AutomatiseringDao,
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val egenAnsattDao: EgenAnsattDao,
    private val vergemålDao: VergemålDao,
    private val personDao: PersonDao,
    private val vedtakDao: VedtakDao,
    private val snapshotDao: SnapshotDao,
    private val plukkTilManuell: PlukkTilManuell,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(Automatisering::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun settInaktiv(vedtaksperiodeId: UUID, hendelseId: UUID) {
        automatiseringDao.settAutomatiseringInaktiv(vedtaksperiodeId, hendelseId)
        automatiseringDao.settAutomatiseringProblemInaktiv(vedtaksperiodeId, hendelseId)
    }

    internal fun utfør(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
        utbetalingtype: Utbetalingtype,
        onAutomatiserbar: () -> Unit
    ) {
        val problemer = vurder(fødselsnummer, vedtaksperiodeId, utbetalingId)

        val utfallslogger = { tekst: String ->
            sikkerLogg.info(
                tekst,
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("utbetalingId", utbetalingId),
                keyValue("utbetalingtype", utbetalingtype),
                problemer
            )
        }

        when {
            utbetalingtype === Utbetalingtype.REVURDERING || problemer.isNotEmpty() -> {
                if (problemer.isNotEmpty()) utfallslogger("Automatiserer ikke {} ({}, {}) fordi: {}")
                automatiseringDao.manuellSaksbehandling(problemer, vedtaksperiodeId, hendelseId, utbetalingId)
            }
            plukkTilManuell() -> {
                utfallslogger("Automatiserer ikke {}, plukket ut til stikkprøve ({}, {})")
                automatiseringDao.stikkprøve(vedtaksperiodeId, hendelseId, utbetalingId)
                logger.info(
                    "Automatisk godkjenning av {} avbrutt, sendes til manuell behandling",
                    keyValue("vedtaksperiodeId", vedtaksperiodeId)
                )
            }
            else -> {
                utfallslogger("Automatiserer {} ({}, {})")
                onAutomatiserbar()
                automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetalingId)
            }
        }
    }

    private fun vurder(fødselsnummer: String, vedtaksperiodeId: UUID, utbetalingId: UUID): List<String> {
        val risikovurdering =
            risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
                ?: validering("Mangler vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning") { false }
        val warnings = warningDao.finnAktiveWarnings(vedtaksperiodeId)
        val erDigital = digitalKontaktinformasjonDao.erDigital(fødselsnummer)
        val erEgenAnsatt = egenAnsattDao.erEgenAnsatt(fødselsnummer)
        val harVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val tilhørerUtlandsenhet = erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val antallÅpneGosysoppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(fødselsnummer)
        val inntektskilde = vedtakDao.finnInntektskilde(vedtaksperiodeId)
        val vedtaksperiodensUtbetaling = snapshotDao.finnUtbetaling(fødselsnummer, utbetalingId)

        return valider(
            risikovurdering,
            validering("Har varsler") { warnings.isEmpty() },
            validering("Bruker er reservert eller mangler oppdatert samtykke i DKIF") { erDigital ?: false },
            validering("Det finnes åpne oppgaver på sykepenger i Gosys") {
                antallÅpneGosysoppgaver?.let { it == 0 } ?: false
            },
            validering("Bruker er ansatt i Nav") { erEgenAnsatt == false || erEgenAnsatt == null },
            validering("Bruker er under verge") { !harVergemål },
            validering("Bruker tilhører utlandsenhet") { !tilhørerUtlandsenhet },
            validering("Har flere arbeidsgivere") { inntektskilde == Inntektskilde.EN_ARBEIDSGIVER },
            validering("Delvis refusjon") { !vedtaksperiodensUtbetaling.delvisRefusjon() },
            validering("Utbetaling til sykmeldt") { !vedtaksperiodensUtbetaling.utbetalingTilSykmeldt() },
        )
    }

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

