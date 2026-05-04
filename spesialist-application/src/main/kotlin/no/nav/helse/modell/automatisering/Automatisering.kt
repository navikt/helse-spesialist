package no.nav.helse.modell.automatisering

import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.LegacyBehandlingDao
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.MeldingDao.BehandlingOpprettetKorrigertSøknad
import no.nav.helse.db.PersonDao
import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.VedtakDao
import no.nav.helse.db.VergemålDao
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.modell.automatisering.Automatisering.AutomatiserKorrigertSøknadResultat.SkyldesIkkeKorrigertSøknad
import no.nav.helse.modell.automatisering.Automatisering.AutomatiserKorrigertSøknadResultat.SkyldesKorrigertSøknad
import no.nav.helse.modell.automatisering.sjekker.AutomatiserRevurderinger
import no.nav.helse.modell.automatisering.sjekker.IkkeAutomatiserNåddMaksdatoOgRefusjonAG
import no.nav.helse.modell.automatisering.stikkprøve.Stikkprøver
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.HentEnhetløsning.Companion.erEnhetUtland
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.modell.stoppautomatiskbehandling.VeilederStansSubsumsjonmelder
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.application.SaksbehandlerStansRepository
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.VeilederStansRepository
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.GODKJENT
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class Automatisering(
    private val risikovurderingDao: RisikovurderingDao,
    private val veilederStansSubsumsjonmelder: VeilederStansSubsumsjonmelder,
    private val automatiseringDao: AutomatiseringDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val vergemålDao: VergemålDao,
    private val personDao: PersonDao,
    private val vedtakDao: VedtakDao,
    private val stikkprøver: Stikkprøver,
    private val meldingDao: MeldingDao,
    private val legacyBehandlingDao: LegacyBehandlingDao,
    private val personRepository: PersonRepository,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
    private val veilederStansRepository: VeilederStansRepository,
    private val saksbehandlerStansRepository: SaksbehandlerStansRepository,
) {
    object Factory {
        fun automatisering(
            sessionContext: SessionContext,
            subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
            stikkprøver: Stikkprøver,
        ): Automatisering =
            Automatisering(
                risikovurderingDao = sessionContext.risikovurderingDao,
                veilederStansSubsumsjonmelder = VeilederStansSubsumsjonmelder(subsumsjonsmelderProvider),
                automatiseringDao = sessionContext.automatiseringDao,
                åpneGosysOppgaverDao = sessionContext.åpneGosysOppgaverDao,
                vergemålDao = sessionContext.vergemålDao,
                personDao = sessionContext.personDao,
                vedtakDao = sessionContext.vedtakDao,
                stikkprøver = stikkprøver,
                meldingDao = sessionContext.meldingDao,
                legacyBehandlingDao = sessionContext.legacyBehandlingDao,
                personRepository = sessionContext.personRepository,
                totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository,
                veilederStansRepository = sessionContext.veilederStansRepository,
                saksbehandlerStansRepository = sessionContext.saksbehandlerStansRepository,
            )
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
        yrkesaktivitetstype: Yrkesaktivitetstype,
        maksdato: LocalDate,
        tags: List<String>,
    ): Automatiseringsresultat {
        if (automatiseringDao.skalTvingeAutomatisering(vedtaksperiodeId)) {
            logg.info("Tvinger automatisering for vedtaksperiode $vedtaksperiodeId")
            return Automatiseringsresultat.KanAutomatiseres
        }

        val sjekkerSomHindrerAutomatisering =
            vurder(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetaling = utbetaling,
                periodetype = periodetype,
                sykefraværstilfelle = sykefraværstilfelle,
                organisasjonsnummer = organisasjonsnummer,
                yrkesaktivitetstype = yrkesaktivitetstype,
                maksdato = maksdato,
                tags = tags,
            )
        if (sjekkerSomHindrerAutomatisering.isNotEmpty()) {
            return Automatiseringsresultat.KanIkkeAutomatiseres(
                sjekkerSomHindrerAutomatisering.map(AutomatiseringValidering::årsakTilIkkeAutomatiserbar),
            )
        }

        val erUTS = utbetaling.harEndringIUtbetalingTilSykmeldt()
        val flereArbeidsgivere = vedtakDao.finnInntektskilde(vedtaksperiodeId) == Inntektskilde.FLERE_ARBEIDSGIVERE
        val erFørstegangsbehandling = periodetype == FØRSTEGANGSBEHANDLING

        when (
            val resultat =
                vurderOmBehandlingSkyldesKorrigertSøknad(fødselsnummer, vedtaksperiodeId, sykefraværstilfelle)
        ) {
            is SkyldesKorrigertSøknad.KanIkkeAutomatiseres,
            -> return Automatiseringsresultat.KanIkkeAutomatiseres(listOf(resultat.årsak))

            is SkyldesIkkeKorrigertSøknad,
            is SkyldesKorrigertSøknad.KanAutomatiseres,
            -> Unit
        }

        if (!erEgenAnsattEllerSkjermet(fødselsnummer)) {
            stikkprøver.avgjørStikkprøve(erUTS, flereArbeidsgivere, erFørstegangsbehandling, yrkesaktivitetstype)?.let {
                return Automatiseringsresultat.Stikkprøve(it)
            }
        } else {
            logg.info("Vurderer ikke om det skal tas stikkprøve.")
        }
        return Automatiseringsresultat.KanAutomatiseres
    }

    private fun erEgenAnsattEllerSkjermet(fødselsnummer: String) =
        personRepository.finn(Identitetsnummer.fraString(fødselsnummer))?.egenAnsattStatus?.erEgenAnsatt == true ||
            personDao.finnAdressebeskyttelse(fødselsnummer) != Adressebeskyttelse.Ugradert

    private fun finnSisteBehandlingOpprettetSomSkyldesKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
    ): BehandlingOpprettetKorrigertSøknad? =
        legacyBehandlingDao.førsteLegacyBehandlingVedtakFattetTidspunkt(vedtaksperiodeId)?.let {
            meldingDao.sisteBehandlingOpprettetOmKorrigertSøknad(fødselsnummer, vedtaksperiodeId)
        }

    private sealed interface AutomatiserKorrigertSøknadResultat {
        sealed interface SkyldesKorrigertSøknad : AutomatiserKorrigertSøknadResultat {
            data class KanIkkeAutomatiseres(
                val årsak: String,
            ) : SkyldesKorrigertSøknad

            data object KanAutomatiseres : SkyldesKorrigertSøknad
        }

        data object SkyldesIkkeKorrigertSøknad : AutomatiserKorrigertSøknadResultat
    }

    private fun vurderOmBehandlingSkyldesKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        sykefraværstilfelle: Sykefraværstilfelle,
    ): AutomatiserKorrigertSøknadResultat {
        val behandlingOpprettetKorrigertSøknad =
            finnSisteBehandlingOpprettetSomSkyldesKorrigertSøknad(fødselsnummer, vedtaksperiodeId)
                ?: return SkyldesIkkeKorrigertSøknad

        return kanKorrigertSøknadAutomatiseres(behandlingOpprettetKorrigertSøknad, sykefraværstilfelle)
    }

    private fun kanKorrigertSøknadAutomatiseres(
        behandlingOpprettetKorrigertSøknad: BehandlingOpprettetKorrigertSøknad,
        sykefraværstilfelle: Sykefraværstilfelle,
    ): AutomatiserKorrigertSøknadResultat {
        val hendelseId = behandlingOpprettetKorrigertSøknad.meldingId
        val vedtaksperiodeId = behandlingOpprettetKorrigertSøknad.vedtaksperiodeId

        if (meldingDao.erKorrigertSøknadAutomatiskBehandlet(hendelseId)) return SkyldesKorrigertSøknad.KanAutomatiseres

        val merEnn6MånederSidenVedtakPåFørsteMottattSøknad =
            legacyBehandlingDao
                .førsteLegacyBehandlingVedtakFattetTidspunkt(vedtaksperiodeId)
                ?.isBefore(LocalDateTime.now().minusMonths(6))
                ?: true

        if (merEnn6MånederSidenVedtakPåFørsteMottattSøknad) {
            return SkyldesKorrigertSøknad.KanIkkeAutomatiseres(
                "Mer enn 6 måneder siden vedtak på første mottatt søknad",
            )
        }

        val antallTidligereKorrigeringer = meldingDao.finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId)
        if (antallTidligereKorrigeringer >= 2) {
            sykefraværstilfelle.håndter(Varselkode.SB_SØ_1.nyttVarsel(vedtaksperiodeId))
            return SkyldesKorrigertSøknad.KanIkkeAutomatiseres(
                "Antall automatisk godkjente korrigerte søknader er større eller lik 2",
            )
        }

        meldingDao.opprettAutomatiseringKorrigertSøknad(vedtaksperiodeId, hendelseId)

        return SkyldesKorrigertSøknad.KanAutomatiseres
    }

    private fun vurder(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        utbetaling: Utbetaling,
        periodetype: Periodetype,
        sykefraværstilfelle: Sykefraværstilfelle,
        organisasjonsnummer: String,
        yrkesaktivitetstype: Yrkesaktivitetstype,
        maksdato: LocalDate,
        tags: List<String>,
    ): List<AutomatiseringValidering> {
        val risikovurdering =
            risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
                ?: validering("Mangler risikovurdering") { false }

        val veilederStans =
            veilederStansRepository.finnAktiv(Identitetsnummer.fraString(fødselsnummer))
        veilederStansSubsumsjonmelder.sendMelding(veilederStans, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)

        val automatiseringStansetAvSaksbehandler =
            saksbehandlerStansRepository.finnAktiv(Identitetsnummer.fraString(fødselsnummer))?.erStanset ?: false
        val forhindrerAutomatisering = sykefraværstilfelle.forhindrerAutomatisering(vedtaksperiodeId)
        val harVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val tilhørerUtlandsenhet = erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val antallÅpneGosysoppgaver = åpneGosysOppgaverDao.antallÅpneOppgaver(fødselsnummer)
        val harKravOmTotrinnsvurdering =
            totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)?.let { it.tilstand != GODKJENT } ?: false
        val harUtbetalingTilSykmeldt = utbetaling.harEndringIUtbetalingTilSykmeldt()
        val selvstendigNæringsdrivendeFGB =
            yrkesaktivitetstype == Yrkesaktivitetstype.SELVSTENDIG && periodetype == FØRSTEGANGSBEHANDLING

        val skalStoppesPgaUTS = harUtbetalingTilSykmeldt && periodetype !in listOf(FORLENGELSE, FØRSTEGANGSBEHANDLING)

        return valider(
            validering("Gjelder selvstendig næring") { !selvstendigNæringsdrivendeFGB },
            risikovurdering,
            validering("Automatisering stanset av saksbehandler") { !automatiseringStansetAvSaksbehandler },
            validering("Unntatt fra automatisk godkjenning") { veilederStans == null },
            validering("Har varsler") { !forhindrerAutomatisering },
            validering("Det finnes åpne oppgaver på sykepenger i Gosys") {
                antallÅpneGosysoppgaver?.let { it == 0 } ?: false
            },
            validering("Bruker er under verge") { !harVergemål },
            validering("Bruker tilhører utlandsenhet") { !tilhørerUtlandsenhet },
            validering("Utbetaling til sykmeldt") { !skalStoppesPgaUTS },
            AutomatiserRevurderinger(utbetaling, fødselsnummer, vedtaksperiodeId),
            validering("Perioden skal til totrinnskontroll") { !harKravOmTotrinnsvurdering },
            IkkeAutomatiserNåddMaksdatoOgRefusjonAG(maksdato, tags, sykefraværstilfelle, vedtaksperiodeId),
        )
    }

    private fun valider(vararg valideringer: AutomatiseringValidering) =
        valideringer
            .toList()
            .filterNot(AutomatiseringValidering::erAutomatiserbar)

    private fun validering(
        årsakHvisIkkeAutomatiserbar: String,
        automatiserbar: () -> Boolean,
    ) = object : AutomatiseringValidering {
        override fun erAutomatiserbar() = automatiserbar()

        override fun årsakTilIkkeAutomatiserbar() = årsakHvisIkkeAutomatiserbar
    }

    fun erStikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) = automatiseringDao.plukketUtTilStikkprøve(vedtaksperiodeId, hendelseId)
}

internal interface AutomatiseringValidering {
    fun erAutomatiserbar(): Boolean

    fun årsakTilIkkeAutomatiserbar(): String
}
