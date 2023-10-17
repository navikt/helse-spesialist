package no.nav.helse.modell.automatisering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.HendelseDao.OverstyringIgangsattKorrigertSøknad
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.HentEnhetløsning.Companion.erEnhetUtland
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Refusjonstype
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vergemal.VergemålDao
import org.slf4j.LoggerFactory

internal class Automatisering(
    private val risikovurderingDao: RisikovurderingDao,
    private val automatiseringDao: AutomatiseringDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val vergemålDao: VergemålDao,
    private val personDao: PersonDao,
    private val vedtakDao: VedtakDao,
    private val overstyringDao: OverstyringDao,
    private val stikkprøver: Stikkprøver,
    private val hendelseDao: HendelseDao,
    private val generasjonDao: GenerasjonDao,
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
        val problemer =
            vurder(fødselsnummer, vedtaksperiodeId, utbetaling, periodetype, sykefraværstilfelle, periodeTom)
        val erUTS = utbetaling.harUtbetalingTilSykmeldt()
        val flereArbeidsgivere = vedtakDao.finnInntektskilde(vedtaksperiodeId) == Inntektskilde.FLERE_ARBEIDSGIVERE
        val erFørstegangsbehandling = periodetype == FØRSTEGANGSBEHANDLING

        val utfallslogger = { tekst: String ->
            sikkerLogg.info(
                tekst,
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("utbetalingId", utbetaling.utbetalingId),
                problemer
            )
        }

        if (Toggle.AutomatiserSpesialsak.enabled && erSpesialsakSomKanAutomatiseres(sykefraværstilfelle, utbetaling, vedtaksperiodeId)) {
            utfallslogger("Automatiserer spesialsak med {} ({})")
            onAutomatiserbar()
            sykefraværstilfelle.automatiskGodkjennSpesialsakvarsler(vedtaksperiodeId)
            automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetaling.utbetalingId)
            return
        }

        if (problemer.isNotEmpty()) {
            utfallslogger("Automatiserer ikke {} ({}) fordi: {}")
            automatiseringDao.manuellSaksbehandling(problemer, vedtaksperiodeId, hendelseId, utbetaling.utbetalingId)
            return
        }

        overstyringIgangsattKorrigertSøknad(fødselsnummer, vedtaksperiodeId)?.let {
            val kanKorrigertSøknadAutomatiseres = kanKorrigertSøknadAutomatiseres(vedtaksperiodeId, it)
            if (!kanKorrigertSøknadAutomatiseres.first) {
                utfallslogger("Automatiserer ikke {} ({}) fordi: ${kanKorrigertSøknadAutomatiseres.second}")
                automatiseringDao.manuellSaksbehandling(
                    listOf("${kanKorrigertSøknadAutomatiseres.second}"),
                    vedtaksperiodeId,
                    hendelseId,
                    utbetaling.utbetalingId
                )
                return
            }
        }

        avgjørStikkprøve(erUTS, flereArbeidsgivere, erFørstegangsbehandling)?.let {
            tilStikkprøve(it, utfallslogger, vedtaksperiodeId, hendelseId, utbetaling.utbetalingId)
        } ?: run {
            utfallslogger("Automatiserer {} ({})")
            onAutomatiserbar()
            automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetaling.utbetalingId)
        }
    }

    private fun overstyringIgangsattKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID
    ): OverstyringIgangsattKorrigertSøknad? = generasjonDao.førsteGenerasjonLåstTidspunkt(vedtaksperiodeId)?.let {
        hendelseDao.sisteOverstyringIgangsattOmKorrigertSøknad(fødselsnummer, vedtaksperiodeId)
    }

    private fun kanKorrigertSøknadAutomatiseres(
        vedtaksperiodeId: UUID,
        overstyringIgangsattKorrigertSøknad: OverstyringIgangsattKorrigertSøknad
    ): Pair<Boolean, String?> {
        val hendelseId = UUID.fromString(overstyringIgangsattKorrigertSøknad.hendelseId)
        if (hendelseDao.erAutomatisertKorrigertSøknadHåndtert(hendelseId)) return Pair(true, null)

        val orgnummer = vedtakDao.finnOrgnummer(vedtaksperiodeId)
        val vedtaksperiodeIdKorrigertSøknad =
            overstyringIgangsattKorrigertSøknad.let { overstyring ->
                overstyring.berørtePerioder.find {
                    it.orgnummer == orgnummer && overstyringIgangsattKorrigertSøknad.periodeForEndringFom.isEqual(
                        it.periodeFom
                    )
                }?.vedtaksperiodeId
            }

        vedtaksperiodeIdKorrigertSøknad?.let {
            val merEnn6MånederSidenVedtakPåFørsteMottattSøknad = generasjonDao.førsteGenerasjonLåstTidspunkt(it)
                ?.isBefore(LocalDateTime.now().minusMonths(6))
                ?: true
            val antallKorrigeringer = hendelseDao.finnAntallAutomatisertKorrigertSøknad(it)
            hendelseDao.opprettAutomatiseringKorrigertSøknad(it, hendelseId)

            if (merEnn6MånederSidenVedtakPåFørsteMottattSøknad) return Pair(false, "Mer enn 6 måneder siden vedtak på første mottatt søknad")
            if (antallKorrigeringer >= 2) return Pair(false, "Antall automatisk godkjente korrigerte søknader er større eller lik 2")

            return Pair(true, null)
        }

        // Hvis vi ikke finner vedtaksperiodeIdKorrigertSøknad, så er det fordi vi vedtaksperioden som er korrigert er AUU som vi ikke trenger å telle
        return Pair(true, null)
    }

    private fun avgjørStikkprøve(
        UTS: Boolean,
        flereArbeidsgivere: Boolean,
        førstegangsbehandling: Boolean,
    ): String? {
        when {
            UTS -> when {
                flereArbeidsgivere -> when {
                    førstegangsbehandling && stikkprøver.utsFlereArbeidsgivereFørstegangsbehandling() -> return "UTS, flere arbeidsgivere, førstegangsbehandling"
                    !førstegangsbehandling && stikkprøver.utsFlereArbeidsgivereForlengelse() -> return "UTS, flere arbeidsgivere, forlengelse"
                }
                !flereArbeidsgivere -> when {
                    førstegangsbehandling && stikkprøver.utsEnArbeidsgiverFørstegangsbehandling() -> return "UTS, en arbeidsgiver, førstegangsbehandling"
                    !førstegangsbehandling && stikkprøver.utsEnArbeidsgiverForlengelse() -> return "UTS, en arbeidsgiver, forlengelse"
                }
            }
            flereArbeidsgivere -> when {
                førstegangsbehandling && stikkprøver.fullRefusjonFlereArbeidsgivereFørstegangsbehandling() -> return "Refusjon, flere arbeidsgivere, førstegangsbehandling"
                !førstegangsbehandling && stikkprøver.fullRefusjonFlereArbeidsgivereForlengelse() -> return "Refusjon, flere arbeidsgivere, forlengelse"
            }
            stikkprøver.fullRefusjonEnArbeidsgiver() -> return "Refusjon, en arbeidsgiver"
        }
        return null
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
        val forhindrerAutomatisering = sykefraværstilfelle.forhindrerAutomatisering(periodeTom)
        val harVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val tilhørerUtlandsenhet = erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val antallÅpneGosysoppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(fødselsnummer)
        val harPågåendeOverstyring = overstyringDao.harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId)
        val harUtbetalingTilSykmeldt = utbetaling.harUtbetalingTilSykmeldt()

        val skalStoppesPgaUTS = harUtbetalingTilSykmeldt && periodetype !in listOf(FORLENGELSE, FØRSTEGANGSBEHANDLING)

        return valider(
            risikovurdering,
            validering("Har varsler") { !forhindrerAutomatisering },
            validering("Det finnes åpne oppgaver på sykepenger i Gosys") {
                antallÅpneGosysoppgaver?.let { it == 0 } ?: false
            },
            validering("Bruker er under verge") { !harVergemål },
            validering("Bruker tilhører utlandsenhet") { !tilhørerUtlandsenhet },
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

    private fun erSpesialsakSomKanAutomatiseres(sykefraværstilfelle: Sykefraværstilfelle, utbetaling: Utbetaling, vedtaksperiodeId: UUID): Boolean {
        val erSpesialsak = vedtakDao.erSpesialsak(vedtaksperiodeId)
        val kanAutomatiseres = sykefraværstilfelle.spesialsakSomKanAutomatiseres(vedtaksperiodeId)
        val ingenUtbetaling = utbetaling.ingenUtbetaling()

        if (erSpesialsak) {
            sikkerLogg.info(
                "vedtaksperiode med {} er spesialsak, {}, {}, {}",
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("kanAutomatiseres", kanAutomatiseres),
                kv("ingenUtbetaling", ingenUtbetaling),
                kv("blirAutomatiskGodkjent", kanAutomatiseres && ingenUtbetaling),
            )
        }

        return erSpesialsak && kanAutomatiseres && ingenUtbetaling
    }

    private class AutomatiserRevurderinger(
        private val utbetaling: Utbetaling,
        private val fødselsnummer: String,
        private val vedtaksperiodeId: UUID,
    ) : AutomatiseringValidering {
        override fun erAautomatiserbar() =
            !utbetaling.erRevurdering() ||
                    (utbetaling.refusjonstype() != Refusjonstype.NEGATIVT_BELØP).also {
                        if (it) sikkerLogg.info("Revurdering av $vedtaksperiodeId (person $fødselsnummer) har ikke et negativt beløp, og er godkjent for automatisering")
                    }

        override fun error() = "Utbetalingen er revurdering med negativt beløp"
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

