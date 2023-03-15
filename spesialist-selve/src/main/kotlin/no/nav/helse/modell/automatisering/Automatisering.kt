package no.nav.helse.modell.automatisering

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.meldinger.løsninger.HentEnhetløsning.Companion.erEnhetUtland
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.delvisRefusjon
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.erRevurdering
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetalingTilSykmeldt
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.gyldigeVarsler
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import org.slf4j.LoggerFactory

internal class Automatisering(
    private val warningDao: WarningDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val automatiseringDao: AutomatiseringDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val egenAnsattDao: EgenAnsattDao,
    private val vergemålDao: VergemålDao,
    private val personDao: PersonDao,
    private val vedtakDao: VedtakDao,
    private val overstyringDao: OverstyringDao,
    private val generasjonRepository: GenerasjonRepository,
    private val snapshotMediator: SnapshotMediator,
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
        onAutomatiserbar: () -> Unit
    ) {
        val problemer = vurder(fødselsnummer, vedtaksperiodeId, utbetalingId)

        val utfallslogger = { tekst: String ->
            sikkerLogg.info(
                tekst,
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("utbetalingId", utbetalingId),
                problemer
            )
        }

        when {
            problemer.isNotEmpty() -> {
                utfallslogger("Automatiserer ikke {} ({}) fordi: {}")
                automatiseringDao.manuellSaksbehandling(problemer, vedtaksperiodeId, hendelseId, utbetalingId)
            }

            plukkTilManuell() -> {
                utfallslogger("Automatiserer ikke {} ({}), plukket ut til stikkprøve")
                automatiseringDao.stikkprøve(vedtaksperiodeId, hendelseId, utbetalingId)
                logger.info(
                    "Automatisk godkjenning av {} avbrutt, sendes til manuell behandling",
                    keyValue("vedtaksperiodeId", vedtaksperiodeId)
                )
            }

            else -> {
                utfallslogger("Automatiserer {} ({})")
                onAutomatiserbar()
                automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetalingId)
            }
        }
    }

    private fun vurder(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID
    ): List<String> {
        val risikovurdering =
            risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
                ?: validering("Mangler vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning") { false }
        val warnings = warningDao.finnAktiveWarnings(vedtaksperiodeId)
        val dedupliserteWarnings = warnings.distinct()
        val generasjoner = generasjonRepository.tilhørendeFor(utbetalingId)
        val varsler = generasjoner.gyldigeVarsler()
        if (dedupliserteWarnings.size != varsler.size) {
            sikkerLogg.info(
                "Nye varsler og Warnings er ikke enige om antall varsler (hhv. ${varsler.size} og ${dedupliserteWarnings.size}) for periode/utbetaling med {}, {}.\n{}\n{}",
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("utbetalingId", utbetalingId),
                kv("nyeVarsler", varsler.map(Varsel::toString)),
                kv("warnings", warnings.map(Warning::toString)),
            )
        } else {
            sikkerLogg.info(
                "Nye varsler og Warnings er enige om antall varsler (hhv. ${varsler.size} og ${dedupliserteWarnings.size}) for periode/utbetaling med {}, {}.",
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("utbetalingId", utbetalingId),
            )
        }
        val erEgenAnsatt = egenAnsattDao.erEgenAnsatt(fødselsnummer)
        val harVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val tilhørerUtlandsenhet = erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val antallÅpneGosysoppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(fødselsnummer)
        val inntektskilde = vedtakDao.finnInntektskilde(vedtaksperiodeId)
        val vedtaksperiodensUtbetaling = snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId)
        val harPågåendeOverstyring = overstyringDao.harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId)

        return valider(
            risikovurdering,
            validering("Har varsler") { warnings.isEmpty() },
            validering("Det finnes åpne oppgaver på sykepenger i Gosys") {
                antallÅpneGosysoppgaver?.let { it == 0 } ?: false
            },
            validering("Bruker er ansatt i Nav") { erEgenAnsatt == false || erEgenAnsatt == null },
            validering("Bruker er under verge") { !harVergemål },
            validering("Bruker tilhører utlandsenhet") { !tilhørerUtlandsenhet },
            validering("Har flere arbeidsgivere") { inntektskilde == Inntektskilde.EN_ARBEIDSGIVER },
            validering("Delvis refusjon") { !vedtaksperiodensUtbetaling.delvisRefusjon() },
            validering("Utbetaling til sykmeldt") { !vedtaksperiodensUtbetaling.utbetalingTilSykmeldt() },
            AutomatiserRevurderinger(vedtaksperiodensUtbetaling),
            validering("Vedtaksperioden har en pågående overstyring") { !harPågåendeOverstyring }
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

    private class AutomatiserRevurderinger(private val utbetaling: GraphQLUtbetaling?): AutomatiseringValidering {
        override fun erAautomatiserbar(): Boolean {
            if (!utbetaling.erRevurdering()) return true
            return Toggle.AutomatiserRevuderinger.enabled
        }
        override fun error() = "Utbetalingen er revurdering"
    }

    fun erStikkprøve(vedtaksperiodeId: UUID, hendelseId: UUID) =
        automatiseringDao.plukketUtTilStikkprøve(vedtaksperiodeId, hendelseId)

}

internal typealias PlukkTilManuell = () -> Boolean

internal interface AutomatiseringValidering {
    fun erAautomatiserbar(): Boolean
    fun error(): String
}

