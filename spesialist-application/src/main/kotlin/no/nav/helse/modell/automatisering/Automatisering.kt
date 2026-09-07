package no.nav.helse.modell.automatisering

import no.nav.helse.db.MeldingDao.BehandlingOpprettetKorrigertSøknad
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.modell.automatisering.Automatisering.AutomatiserKorrigertSøknadResultat.SkyldesIkkeKorrigertSøknad
import no.nav.helse.modell.automatisering.Automatisering.AutomatiserKorrigertSøknadResultat.SkyldesKorrigertSøknad
import no.nav.helse.modell.automatisering.sjekker.AutomatiserRevurderinger
import no.nav.helse.modell.automatisering.sjekker.IkkeAutomatiserNåddMaksdatoOgRefusjonAG
import no.nav.helse.modell.automatisering.stikkprøve.Stikkprøver
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.HentEnhetløsning.Companion.erEnhetUtland
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.modell.stoppautomatiskbehandling.VeilederStansSubsumsjonmelder
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.GODKJENT
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Varsel.Companion.forhindrerAutomatisering
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class Automatisering(
    private val veilederStansSubsumsjonmelder: VeilederStansSubsumsjonmelder,
    private val stikkprøver: Stikkprøver,
    private val sessionContext: SessionContext,
) {
    object Factory {
        fun automatisering(
            sessionContext: SessionContext,
            subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
            stikkprøver: Stikkprøver,
        ): Automatisering =
            Automatisering(
                veilederStansSubsumsjonmelder = VeilederStansSubsumsjonmelder(subsumsjonsmelderProvider),
                stikkprøver = stikkprøver,
                sessionContext = sessionContext,
            )
    }

    internal fun settInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        sessionContext.automatiseringDao.settAutomatiseringInaktiv(vedtaksperiodeId, hendelseId)
        sessionContext.automatiseringDao.settAutomatiseringProblemInaktiv(vedtaksperiodeId, hendelseId)
    }

    internal fun utfør(
        fødselsnummer: String,
        vedtaksperiodeId: VedtaksperiodeId,
        utbetaling: Utbetaling,
        periodetype: Periodetype,
        behandlingspakke: Set<Behandling>,
        gjeldendeBehandling: Behandling,
        organisasjonsnummer: String,
        yrkesaktivitetstype: Yrkesaktivitetstype,
        maksdato: LocalDate,
        tags: List<String>,
    ): Automatiseringsresultat {
        if (sessionContext.automatiseringDao.skalTvingeAutomatisering(vedtaksperiodeId.value)) {
            logg.info("Tvinger automatisering for vedtaksperiode ${vedtaksperiodeId.value}")
            return Automatiseringsresultat.KanAutomatiseres
        }

        val sjekkerSomHindrerAutomatisering =
            vurder(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetaling = utbetaling,
                periodetype = periodetype,
                behandlingspakke = behandlingspakke,
                gjeldendeBehandling = gjeldendeBehandling,
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
        val flereArbeidsgivere = sessionContext.vedtakDao.finnInntektskilde(vedtaksperiodeId.value) == Inntektskilde.FLERE_ARBEIDSGIVERE
        val erFørstegangsbehandling = periodetype == FØRSTEGANGSBEHANDLING

        when (
            val resultat =
                vurderOmBehandlingSkyldesKorrigertSøknad(fødselsnummer, vedtaksperiodeId, gjeldendeBehandling)
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
        sessionContext.personRepository
            .finn(Identitetsnummer.fraString(fødselsnummer))
            ?.egenAnsattStatus
            ?.erEgenAnsatt == true ||
            sessionContext.personDao.finnAdressebeskyttelse(fødselsnummer) != Adressebeskyttelse.Ugradert

    private fun finnSisteBehandlingOpprettetSomSkyldesKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: VedtaksperiodeId,
    ): BehandlingOpprettetKorrigertSøknad? =
        sessionContext.legacyBehandlingDao.førsteLegacyBehandlingVedtakFattetTidspunkt(vedtaksperiodeId.value)?.let {
            sessionContext.meldingDao.sisteBehandlingOpprettetOmKorrigertSøknad(fødselsnummer, vedtaksperiodeId.value)
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
        vedtaksperiodeId: VedtaksperiodeId,
        behandling: Behandling,
    ): AutomatiserKorrigertSøknadResultat {
        val behandlingOpprettetKorrigertSøknad =
            finnSisteBehandlingOpprettetSomSkyldesKorrigertSøknad(fødselsnummer, vedtaksperiodeId)
                ?: return SkyldesIkkeKorrigertSøknad

        return kanKorrigertSøknadAutomatiseres(behandlingOpprettetKorrigertSøknad, behandling)
    }

    private fun kanKorrigertSøknadAutomatiseres(
        behandlingOpprettetKorrigertSøknad: BehandlingOpprettetKorrigertSøknad,
        gjeldendeBehandling: Behandling,
    ): AutomatiserKorrigertSøknadResultat {
        val hendelseId = behandlingOpprettetKorrigertSøknad.meldingId
        val vedtaksperiodeId = behandlingOpprettetKorrigertSøknad.vedtaksperiodeId

        if (sessionContext.meldingDao.erKorrigertSøknadTidligereAutomatiskBehandlet(hendelseId)) return SkyldesKorrigertSøknad.KanAutomatiseres

        val merEnn6MånederSidenVedtakPåFørsteMottattSøknad =
            sessionContext.legacyBehandlingDao
                .førsteLegacyBehandlingVedtakFattetTidspunkt(vedtaksperiodeId)
                ?.isBefore(LocalDateTime.now().minusMonths(6))
                ?: true

        if (merEnn6MånederSidenVedtakPåFørsteMottattSøknad) {
            return SkyldesKorrigertSøknad.KanIkkeAutomatiseres(
                "Mer enn 6 måneder siden vedtak på første mottatt søknad",
            )
        }

        val antallTidligereKorrigeringer =
            sessionContext.meldingDao.antallGangerVedtaksperiodeErAutomatisertMedKorrigertSøknad(vedtaksperiodeId)
        if (antallTidligereKorrigeringer >= 2) {
            val varsel = Varsel.nytt(gjeldendeBehandling.id, gjeldendeBehandling.spleisBehandlingId, Varselkode.SB_SØ_1.name)
            sessionContext.varselRepository.lagre(varsel)
            return SkyldesKorrigertSøknad.KanIkkeAutomatiseres(
                "Antall ganger vedtaksperioden er automatisk godkjent med korrigert søknad er to eller mer",
            )
        }

        sessionContext.meldingDao.opprettAutomatiseringMedKorrigertSøknad(vedtaksperiodeId, hendelseId)

        return SkyldesKorrigertSøknad.KanAutomatiseres
    }

    private fun vurder(
        fødselsnummer: String,
        vedtaksperiodeId: VedtaksperiodeId,
        utbetaling: Utbetaling,
        periodetype: Periodetype,
        behandlingspakke: Set<Behandling>,
        gjeldendeBehandling: Behandling,
        organisasjonsnummer: String,
        yrkesaktivitetstype: Yrkesaktivitetstype,
        maksdato: LocalDate,
        tags: List<String>,
    ): List<AutomatiseringValidering> {
        val risikovurdering =
            sessionContext.risikovurderingDao.hentRisikovurdering(vedtaksperiodeId.value)
                ?: validering("Mangler risikovurdering") { false }

        val veilederStans =
            sessionContext.veilederStansRepository.finnAktiv(Identitetsnummer.fraString(fødselsnummer))
        veilederStansSubsumsjonmelder.sendMelding(veilederStans, fødselsnummer, organisasjonsnummer, vedtaksperiodeId.value)

        val automatiseringStansetAvSaksbehandler =
            sessionContext.saksbehandlerStansRepository.finnAktiv(Identitetsnummer.fraString(fødselsnummer))?.erStanset ?: false
        val varsler = sessionContext.varselRepository.finnVarslerFor(behandlingspakke.map { it.id })
        val forhindrerAutomatisering = varsler.forhindrerAutomatisering()
        val harVergemål = sessionContext.vergemålDao.harVergemål(fødselsnummer) ?: false
        val tilhørerUtlandsenhet = erEnhetUtland(sessionContext.personDao.finnEnhetId(fødselsnummer))
        val antallÅpneGosysoppgaver = sessionContext.åpneGosysOppgaverDao.antallÅpneOppgaver(fødselsnummer)
        val harKravOmTotrinnsvurdering =
            sessionContext.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)?.let { it.tilstand != GODKJENT } ?: false
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
            IkkeAutomatiserNåddMaksdatoOgRefusjonAG(maksdato, tags, gjeldendeBehandling, sessionContext.varselRepository),
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
    ) = sessionContext.automatiseringDao.plukketUtTilStikkprøve(vedtaksperiodeId, hendelseId)
}

internal interface AutomatiseringValidering {
    fun erAutomatiserbar(): Boolean

    fun årsakTilIkkeAutomatiserbar(): String
}
