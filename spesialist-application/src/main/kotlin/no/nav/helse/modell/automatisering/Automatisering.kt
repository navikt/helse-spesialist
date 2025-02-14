package no.nav.helse.modell.automatisering

import no.nav.helse.AutomatiseringStansetSjekker
import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.GenerasjonDao
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.MeldingDao.OverstyringIgangsattKorrigertSøknad
import no.nav.helse.db.OverstyringDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.VedtakDao
import no.nav.helse.db.VergemålDao
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.modell.automatisering.Automatisering.AutomatiserKorrigertSøknadResultat.SkyldesKorrigertSøknad
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.HentEnhetløsning.Companion.erEnhetUtland
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.utbetaling.Refusjonstype
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class Automatisering(
    private val risikovurderingDao: RisikovurderingDao,
    private val automatiseringStansetSjekker: AutomatiseringStansetSjekker,
    private val automatiseringDao: AutomatiseringDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val vergemålDao: VergemålDao,
    private val personDao: PersonDao,
    private val vedtakDao: VedtakDao,
    private val overstyringDao: OverstyringDao,
    private val stikkprøver: Stikkprøver,
    private val meldingDao: MeldingDao,
    private val generasjonDao: GenerasjonDao,
    private val egenAnsattDao: EgenAnsattDao,
) {
    object Factory {
        fun automatisering(
            sessionContext: SessionContext,
            subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
            stikkprøver: Stikkprøver,
        ): Automatisering {
            return Automatisering(
                risikovurderingDao = sessionContext.risikovurderingDao,
                automatiseringStansetSjekker =
                    StansAutomatiskBehandlingMediator.Factory.stansAutomatiskBehandlingMediator(
                        sessionContext,
                        subsumsjonsmelderProvider,
                    ),
                automatiseringDao = sessionContext.automatiseringDao,
                åpneGosysOppgaverDao = sessionContext.åpneGosysOppgaverDao,
                vergemålDao = sessionContext.vergemålDao,
                personDao = sessionContext.personDao,
                vedtakDao = sessionContext.vedtakDao,
                overstyringDao = sessionContext.overstyringDao,
                stikkprøver = stikkprøver,
                meldingDao = sessionContext.meldingDao,
                generasjonDao = sessionContext.generasjonDao,
                egenAnsattDao = sessionContext.egenAnsattDao,
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Automatisering::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun settInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        automatiseringDao.settAutomatiseringInaktiv(vedtaksperiodeId, hendelseId)
        automatiseringDao.settAutomatiseringProblemInaktiv(vedtaksperiodeId, hendelseId)
    }

    internal fun utfør(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        utbetaling: Utbetaling,
        periodetype: Periodetype,
        sykefraværstilfelle: Sykefraværstilfelle,
        organisasjonsnummer: String,
    ): Automatiseringsresultat {
        val problemer =
            vurder(fødselsnummer, vedtaksperiodeId, utbetaling, periodetype, sykefraværstilfelle, organisasjonsnummer)
        val erUTS = utbetaling.harEndringIUtbetalingTilSykmeldt()
        val flereArbeidsgivere = vedtakDao.finnInntektskilde(vedtaksperiodeId) == Inntektskilde.FLERE_ARBEIDSGIVERE
        val erFørstegangsbehandling = periodetype == FØRSTEGANGSBEHANDLING

        if (problemer.isNotEmpty()) return Automatiseringsresultat.KanIkkeAutomatiseres(problemer)

        when (val resultat = vurderOmBehandlingSkyldesKorrigertSøknad(fødselsnummer, vedtaksperiodeId)) {
            is SkyldesKorrigertSøknad.KanIkkeAutomatiseres,
            -> return Automatiseringsresultat.KanIkkeAutomatiseres(listOf(resultat.årsak))
            is AutomatiserKorrigertSøknadResultat.SkyldesIkkeKorrigertSøknad,
            is SkyldesKorrigertSøknad.KanAutomatiseres,
            -> {}
        }

        if (!erEgenAnsattEllerSkjermet(fødselsnummer)) {
            avgjørStikkprøve(erUTS, flereArbeidsgivere, erFørstegangsbehandling)?.let {
                return Automatiseringsresultat.Stikkprøve(it)
            }
        } else {
            logger.info("Vurderer ikke om det skal tas stikkprøve.")
        }
        return Automatiseringsresultat.KanAutomatiseres
    }

    private fun erEgenAnsattEllerSkjermet(fødselsnummer: String) =
        egenAnsattDao.erEgenAnsatt(fødselsnummer) == true ||
            personDao.finnAdressebeskyttelse(fødselsnummer) != Adressebeskyttelse.Ugradert

    private fun finnSisteOverstyringIgangsattHvisSkyldesKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
    ): OverstyringIgangsattKorrigertSøknad? =
        generasjonDao.førsteGenerasjonVedtakFattetTidspunkt(vedtaksperiodeId)?.let {
            meldingDao.sisteOverstyringIgangsattOmKorrigertSøknad(fødselsnummer, vedtaksperiodeId)
        }

    private sealed interface AutomatiserKorrigertSøknadResultat {
        sealed interface SkyldesKorrigertSøknad : AutomatiserKorrigertSøknadResultat {
            data class KanIkkeAutomatiseres(val årsak: String) : SkyldesKorrigertSøknad

            data object KanAutomatiseres : SkyldesKorrigertSøknad
        }

        data object SkyldesIkkeKorrigertSøknad : AutomatiserKorrigertSøknadResultat
    }

    private fun vurderOmBehandlingSkyldesKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
    ): AutomatiserKorrigertSøknadResultat {
        val overstyringIgangsattKorrigertSøknad =
            finnSisteOverstyringIgangsattHvisSkyldesKorrigertSøknad(fødselsnummer, vedtaksperiodeId)
                ?: return AutomatiserKorrigertSøknadResultat.SkyldesIkkeKorrigertSøknad

        return kanKorrigertSøknadAutomatiseres(vedtaksperiodeId, overstyringIgangsattKorrigertSøknad)
    }

    private fun kanKorrigertSøknadAutomatiseres(
        vedtaksperiodeId: UUID,
        overstyringIgangsattKorrigertSøknad: OverstyringIgangsattKorrigertSøknad,
    ): AutomatiserKorrigertSøknadResultat {
        val hendelseId = UUID.fromString(overstyringIgangsattKorrigertSøknad.meldingId)
        if (meldingDao.erKorrigertSøknadAutomatiskBehandlet(hendelseId)) return SkyldesKorrigertSøknad.KanAutomatiseres

        val orgnummer = vedtakDao.finnOrganisasjonsnummer(vedtaksperiodeId)
        val vedtaksperiodeIdKorrigertSøknad =
            overstyringIgangsattKorrigertSøknad.let { overstyring ->
                overstyring.berørtePerioder.find {
                    it.orgnummer == orgnummer &&
                        overstyringIgangsattKorrigertSøknad.periodeForEndringFom.isEqual(
                            it.periodeFom,
                        )
                }?.vedtaksperiodeId
            }

        vedtaksperiodeIdKorrigertSøknad?.let {
            val merEnn6MånederSidenVedtakPåFørsteMottattSøknad =
                generasjonDao.førsteGenerasjonVedtakFattetTidspunkt(it)
                    ?.isBefore(LocalDateTime.now().minusMonths(6))
                    ?: true
            val antallKorrigeringer = meldingDao.finnAntallAutomatisertKorrigertSøknad(it)
            meldingDao.opprettAutomatiseringKorrigertSøknad(it, hendelseId)

            if (merEnn6MånederSidenVedtakPåFørsteMottattSøknad) {
                return SkyldesKorrigertSøknad.KanIkkeAutomatiseres(
                    "Mer enn 6 måneder siden vedtak på første mottatt søknad",
                )
            }
            if (antallKorrigeringer >= 2) {
                return SkyldesKorrigertSøknad.KanIkkeAutomatiseres(
                    "Antall automatisk godkjente korrigerte søknader er større eller lik 2",
                )
            }

            return SkyldesKorrigertSøknad.KanAutomatiseres
        }

        // Hvis vi ikke finner vedtaksperiodeIdKorrigertSøknad, så er det fordi vi vedtaksperioden som er korrigert er AUU som vi ikke trenger å telle
        return SkyldesKorrigertSøknad.KanAutomatiseres
    }

    private fun avgjørStikkprøve(
        UTS: Boolean,
        flereArbeidsgivere: Boolean,
        førstegangsbehandling: Boolean,
    ): String? {
        when {
            UTS ->
                when {
                    flereArbeidsgivere ->
                        when {
                            førstegangsbehandling && stikkprøver.utsFlereArbeidsgivereFørstegangsbehandling() -> return "UTS, flere arbeidsgivere, førstegangsbehandling"
                            !førstegangsbehandling && stikkprøver.utsFlereArbeidsgivereForlengelse() -> return "UTS, flere arbeidsgivere, forlengelse"
                        }
                    !flereArbeidsgivere ->
                        when {
                            førstegangsbehandling && stikkprøver.utsEnArbeidsgiverFørstegangsbehandling() -> return "UTS, en arbeidsgiver, førstegangsbehandling"
                            !førstegangsbehandling && stikkprøver.utsEnArbeidsgiverForlengelse() -> return "UTS, en arbeidsgiver, forlengelse"
                        }
                }
            flereArbeidsgivere ->
                when {
                    førstegangsbehandling && stikkprøver.fullRefusjonFlereArbeidsgivereFørstegangsbehandling() -> return "Refusjon, flere arbeidsgivere, førstegangsbehandling"
                    !førstegangsbehandling && stikkprøver.fullRefusjonFlereArbeidsgivereForlengelse() -> return "Refusjon, flere arbeidsgivere, forlengelse"
                }
            stikkprøver.fullRefusjonEnArbeidsgiver() -> return "Refusjon, en arbeidsgiver"
        }
        return null
    }

    private fun vurder(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        utbetaling: Utbetaling,
        periodetype: Periodetype,
        sykefraværstilfelle: Sykefraværstilfelle,
        organisasjonsnummer: String,
    ): List<String> {
        val risikovurdering =
            risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
                ?: validering("Mangler risikovurdering") { false }
        val unntattFraAutomatisering =
            automatiseringStansetSjekker.sjekkOmAutomatiseringErStanset(
                fødselsnummer,
                vedtaksperiodeId,
                organisasjonsnummer,
            )
        val forhindrerAutomatisering = sykefraværstilfelle.forhindrerAutomatisering(vedtaksperiodeId)
        val harVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val tilhørerUtlandsenhet = erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val antallÅpneGosysoppgaver = åpneGosysOppgaverDao.antallÅpneOppgaver(fødselsnummer)
        val harPågåendeOverstyring = overstyringDao.harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId)
        val harUtbetalingTilSykmeldt = utbetaling.harEndringIUtbetalingTilSykmeldt()

        val skalStoppesPgaUTS = harUtbetalingTilSykmeldt && periodetype !in listOf(FORLENGELSE, FØRSTEGANGSBEHANDLING)

        return valider(
            risikovurdering,
            validering("Unntatt fra automatisk godkjenning") { !unntattFraAutomatisering },
            validering("Har varsler") { !forhindrerAutomatisering },
            validering("Det finnes åpne oppgaver på sykepenger i Gosys") {
                antallÅpneGosysoppgaver?.let { it == 0 } ?: false
            },
            validering("Bruker er under verge") { !harVergemål },
            validering("Bruker tilhører utlandsenhet") { !tilhørerUtlandsenhet },
            validering("Utbetaling til sykmeldt") { !skalStoppesPgaUTS },
            AutomatiserRevurderinger(utbetaling, fødselsnummer, vedtaksperiodeId),
            validering("Vedtaksperioden har en pågående overstyring") { !harPågåendeOverstyring },
        )
    }

    private fun valider(vararg valideringer: AutomatiseringValidering) =
        valideringer.toList()
            .filterNot(AutomatiseringValidering::erAautomatiserbar)
            .map(AutomatiseringValidering::error)

    private fun validering(
        error: String,
        automatiserbar: () -> Boolean,
    ) = object : AutomatiseringValidering {
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
                (utbetaling.refusjonstype() != Refusjonstype.NEGATIVT_BELØP).also {
                    if (it) {
                        sikkerLogg.info(
                            "Revurdering av $vedtaksperiodeId (person $fødselsnummer) har ikke et negativt beløp, og er godkjent for automatisering",
                        )
                    }
                }

        override fun error() = "Utbetalingen er revurdering med negativt beløp"
    }

    fun erStikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) = automatiseringDao.plukketUtTilStikkprøve(vedtaksperiodeId, hendelseId)
}

typealias PlukkTilManuell<String> = (String?) -> Boolean

interface Stikkprøver {
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
