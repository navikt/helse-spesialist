package no.nav.helse.modell.automatisering

import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.Toggle.AutomatiserRevuderinger
import no.nav.helse.mediator.Toggle.AutomatiserUtbetalingTilSykmeldt
import no.nav.helse.mediator.meldinger.løsninger.HentEnhetløsning.Companion.erEnhetUtland
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.erRevurdering
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetalingTilSykmeldt
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
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
    private val snapshotMediator: SnapshotMediator,
    private val stikkprøver: Stikkprøver,
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
        periodetype: Periodetype,
        sykefraværstilfelle: Sykefraværstilfelle,
        periodeTom: LocalDate,
        onAutomatiserbar: () -> Unit
    ) {
        val problemer = vurder(fødselsnummer, vedtaksperiodeId, utbetalingId, periodetype, sykefraværstilfelle, periodeTom)
        val vedtaksperiodensUtbetaling = snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId)
        val erUTS = vedtaksperiodensUtbetaling.utbetalingTilSykmeldt()

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

            (!erUTS && stikkprøver.fullRefusjon()) || (erUTS && stikkprøver.uts()) -> {
                val fullRefujonEllerUTS = if (erUTS) "UTS" else "full refusjon"
                utfallslogger("Automatiserer ikke {} ({}), plukket ut til stikkprøve for $fullRefujonEllerUTS")
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
        utbetalingId: UUID,
        periodetype: Periodetype,
        sykefraværstilfelle: Sykefraværstilfelle,
        periodeTom: LocalDate
    ): List<String> {
        val risikovurdering =
            risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
                ?: validering("Mangler vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning") { false }
        val vedtaksperiodensUtbetaling = snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId)
        val warnings = warningDao.finnAktiveWarnings(vedtaksperiodeId)
        val dedupliserteWarnings = warnings.distinct()
        val forhindrerAutomatisering = sykefraværstilfelle.forhindrerAutomatisering(periodeTom)
        val harWarnings = dedupliserteWarnings.isNotEmpty()
        when {
            !forhindrerAutomatisering && harWarnings -> sikkerLogg.info(
                "Nye varsler mener at perioden kan automatiseres, mens warnings er uenig. Gjelder {}, {}, {}, {}.",
                kv("fødselsnummer", fødselsnummer),
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("utbetalingId", utbetalingId),
                kv("utbetalingstype", vedtaksperiodensUtbetaling?.typeEnum?.name)
            )
            forhindrerAutomatisering && !harWarnings -> sikkerLogg.info(
                "Nye varsler mener at perioden ikke kan automatiseres, mens warnings er uenig. Gjelder {}, {}, {}, {}.",
                kv("fødselsnummer", fødselsnummer),
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("utbetalingId", utbetalingId),
                kv("utbetalingstype", vedtaksperiodensUtbetaling?.typeEnum?.name)
            )
            else -> sikkerLogg.info(
                "Nye varsler og warnings er enige om at perioden ${if(forhindrerAutomatisering) "ikke " else ""}kan automatiseres. Gjelder {}, {}, {}, {}.",
                kv("fødselsnummer", fødselsnummer),
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("utbetalingId", utbetalingId),
                kv("utbetalingstype", vedtaksperiodensUtbetaling?.typeEnum?.name)
            )
        }

        val erEgenAnsatt = egenAnsattDao.erEgenAnsatt(fødselsnummer)
        val harVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val tilhørerUtlandsenhet = erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val antallÅpneGosysoppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(fødselsnummer)
        val inntektskilde = vedtakDao.finnInntektskilde(vedtaksperiodeId)
        val harPågåendeOverstyring = overstyringDao.harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId)

        val skalStoppesPgaUTS =
            if (AutomatiserUtbetalingTilSykmeldt.enabled) {
                (vedtaksperiodensUtbetaling.utbetalingTilSykmeldt() &&
                        periodetype != Periodetype.FORLENGELSE)
            } else vedtaksperiodensUtbetaling.utbetalingTilSykmeldt()

        return valider(
            risikovurdering,
            validering("Har varsler") { !forhindrerAutomatisering },
            validering("Det finnes åpne oppgaver på sykepenger i Gosys") {
                antallÅpneGosysoppgaver?.let { it == 0 } ?: false
            },
            validering("Bruker er ansatt i Nav") { erEgenAnsatt == false || erEgenAnsatt == null },
            validering("Bruker er under verge") { !harVergemål },
            validering("Bruker tilhører utlandsenhet") { !tilhørerUtlandsenhet },
            validering("Har flere arbeidsgivere") { inntektskilde == Inntektskilde.EN_ARBEIDSGIVER },
            validering("Utbetaling til sykmeldt") { !skalStoppesPgaUTS },
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
        override fun erAautomatiserbar() = !utbetaling.erRevurdering() || AutomatiserRevuderinger.enabled
        override fun error() = "Utbetalingen er revurdering"
    }

    fun erStikkprøve(vedtaksperiodeId: UUID, hendelseId: UUID) =
        automatiseringDao.plukketUtTilStikkprøve(vedtaksperiodeId, hendelseId)

}

internal typealias PlukkTilManuell<String> = (String?) -> Boolean

internal interface Stikkprøver {
    fun fullRefusjon(): Boolean
    fun uts(): Boolean
}

internal interface AutomatiseringValidering {
    fun erAautomatiserbar(): Boolean
    fun error(): String
}

