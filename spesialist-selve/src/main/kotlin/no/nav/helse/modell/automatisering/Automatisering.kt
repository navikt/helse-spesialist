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
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Refusjonstype
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålDao
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
        utbetaling: Utbetaling,
        periodetype: Periodetype,
        sykefraværstilfelle: Sykefraværstilfelle,
        periodeTom: LocalDate,
        onAutomatiserbar: () -> Unit
    ) {
        val problemer = vurder(fødselsnummer, vedtaksperiodeId, utbetaling, periodetype, sykefraværstilfelle, periodeTom)
        val erUTS = utbetaling.harUtbetalingTilSykmeldt()
        val erFullRefusjon = !erUTS
        val flereArbeidsgivere = vedtakDao.finnInntektskilde(vedtaksperiodeId) == Inntektskilde.FLERE_ARBEIDSGIVERE
        val enArbeidsgiver = !flereArbeidsgivere
        val erFørstegangsbehandling = periodetype == Periodetype.FØRSTEGANGSBEHANDLING
        val erForlengelse = periodetype == Periodetype.FORLENGELSE

        val utfallslogger = { tekst: String ->
            sikkerLogg.info(
                tekst,
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("utbetalingId", utbetaling.utbetalingId),
                problemer
            )
        }

        var årsakTilStikkprøve: String? = null
        when {
            problemer.isNotEmpty() -> {
                utfallslogger("Automatiserer ikke {} ({}) fordi: {}")
                automatiseringDao.manuellSaksbehandling(problemer, vedtaksperiodeId, hendelseId, utbetaling.utbetalingId)
            }

            erUTS && flereArbeidsgivere && erFørstegangsbehandling && stikkprøver.utsFlereArbeidsgivereFørstegangsbehandling() ->
                årsakTilStikkprøve = "UTS, flere arbeidsgivere, førstegangsbehandling"

            erUTS && flereArbeidsgivere && erForlengelse && stikkprøver.utsFlereArbeidsgivereForlengelse() ->
                årsakTilStikkprøve = "UTS, flere arbeidsgivere, forlengelse"

            erUTS && enArbeidsgiver && erFørstegangsbehandling && stikkprøver.utsEnArbeidsgiverFørstegangsbehandling() ->
                årsakTilStikkprøve = "UTS, en arbeidsgiver, førstegangsbehandling"

            erUTS && enArbeidsgiver && erForlengelse && stikkprøver.utsEnArbeidsgiverForlengelse() ->
                årsakTilStikkprøve = "UTS, en arbeidsgiver, forlengelse"

            erFullRefusjon && flereArbeidsgivere && erFørstegangsbehandling && stikkprøver.fullRefusjonFlereArbeidsgivereFørstegangsbehandling() ->
                årsakTilStikkprøve = "Refusjon, flere arbeidsgivere, førstegangsbehandling"

            erFullRefusjon && flereArbeidsgivere && erForlengelse && stikkprøver.fullRefusjonFlereArbeidsgivereForlengelse() ->
                årsakTilStikkprøve = "Refusjon, flere arbeidsgivere, forlengelse"

            erFullRefusjon && enArbeidsgiver && stikkprøver.fullRefusjonEnArbeidsgiver() ->
                årsakTilStikkprøve = "Refusjon, en arbeidsgiver"

            else -> {
                utfallslogger("Automatiserer {} ({})")
                onAutomatiserbar()
                automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetaling.utbetalingId)
            }
        }
        årsakTilStikkprøve?.also { tilStikkprøve(it, utfallslogger, vedtaksperiodeId, hendelseId, utbetaling.utbetalingId) }
    }

    private fun tilStikkprøve(
        årsak: String,
        utfallslogger: (String) -> Unit,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    ) {
        utfallslogger("Automatiserer ikke {} ({}), plukket ut til stikkprøve for $årsak")
        automatiseringDao.stikkprøve(vedtaksperiodeId, hendelseId, utbetalingId)
        logger.info(
            "Automatisk godkjenning av {} avbrutt, sendes til manuell behandling",
            keyValue("vedtaksperiodeId", vedtaksperiodeId)
        )
    }

    private fun vurder(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        utbetaling: Utbetaling,
        periodetype: Periodetype,
        sykefraværstilfelle: Sykefraværstilfelle,
        periodeTom: LocalDate
    ): List<String> {
        val risikovurdering =
            risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
                ?: validering("Mangler vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning") { false }
        val warnings = warningDao.finnAktiveWarnings(vedtaksperiodeId)
        val dedupliserteWarnings = warnings.distinct()
        val forhindrerAutomatisering = sykefraværstilfelle.forhindrerAutomatisering(periodeTom)
        val harWarnings = dedupliserteWarnings.isNotEmpty()
        when {
            !forhindrerAutomatisering && harWarnings -> sikkerLogg.info(
                "Nye varsler mener at perioden kan automatiseres, mens warnings er uenig. Gjelder {}, {}, {}.",
                kv("fødselsnummer", fødselsnummer),
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("utbetaling", utbetaling),
            )
            forhindrerAutomatisering && !harWarnings -> sikkerLogg.info(
                "Nye varsler mener at perioden ikke kan automatiseres, mens warnings er uenig. Gjelder {}, {}, {}.",
                kv("fødselsnummer", fødselsnummer),
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("utbetaling", utbetaling),
            )
            else -> sikkerLogg.info(
                "Nye varsler og warnings er enige om at perioden ${if(forhindrerAutomatisering) "ikke " else ""}kan automatiseres. Gjelder {}, {}, {}.",
                kv("fødselsnummer", fødselsnummer),
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("utbetaling", utbetaling),
            )
        }

        val erEgenAnsatt = egenAnsattDao.erEgenAnsatt(fødselsnummer)
        val harVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val tilhørerUtlandsenhet = erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val antallÅpneGosysoppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(fødselsnummer)
        val inntektskilde = vedtakDao.finnInntektskilde(vedtaksperiodeId)
        val harPågåendeOverstyring = overstyringDao.harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId)
        val harUtbetalingTilSykmeldt = utbetaling.harUtbetalingTilSykmeldt()

        val skalStoppesPgaUTS =
            if (AutomatiserUtbetalingTilSykmeldt.enabled) {
                (harUtbetalingTilSykmeldt &&
                        !arrayOf(Periodetype.FORLENGELSE, Periodetype.FØRSTEGANGSBEHANDLING).contains(periodetype))
            } else harUtbetalingTilSykmeldt

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
            AutomatiserRevurderinger(utbetaling, fødselsnummer, vedtaksperiodeId),
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

    private class AutomatiserRevurderinger(
        private val utbetaling: Utbetaling,
        private val fødselsnummer: String,
        private val vedtaksperiodeId: UUID,
    ) : AutomatiseringValidering {
        override fun erAautomatiserbar() =
            !utbetaling.erRevurdering() ||
                    AutomatiserRevuderinger.enabled ||
                    (utbetaling.refusjonstype() == Refusjonstype.INGEN_UTBETALING).also {
                        if (it) sikkerLogg.info("Revurdering av $vedtaksperiodeId (person $fødselsnummer) har ingen endring, og er godkjent for automatisering")
                    }

        override fun error() = "Utbetalingen er revurdering"
    }

    fun erStikkprøve(vedtaksperiodeId: UUID, hendelseId: UUID) =
        automatiseringDao.plukketUtTilStikkprøve(vedtaksperiodeId, hendelseId)

}

internal typealias PlukkTilManuell<String> = (String?) -> Boolean

internal interface Stikkprøver {
    fun utsFlereArbeidsgivereFørstegangsbehandling(): Boolean
    fun utsFlereArbeidsgivereForlengelse(): Boolean
    fun utsEnArbeidsgiverFørstegangsbehandling(): Boolean
    fun utsEnArbeidsgiverForlengelse(): Boolean
    fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling(): Boolean
    fun fullRefusjonFlereArbeidsgivereForlengelse(): Boolean
    fun fullRefusjonEnArbeidsgiver(): Boolean
}

internal interface AutomatiseringValidering {
    fun erAautomatiserbar(): Boolean
    fun error(): String
}

