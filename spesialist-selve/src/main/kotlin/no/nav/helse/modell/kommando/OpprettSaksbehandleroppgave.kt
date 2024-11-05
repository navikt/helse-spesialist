package no.nav.helse.modell.kommando

import no.nav.helse.db.EgenAnsattRepository
import no.nav.helse.db.PersonRepository
import no.nav.helse.db.PåVentRepository
import no.nav.helse.db.RisikovurderingRepository
import no.nav.helse.db.VedtakDao
import no.nav.helse.db.VergemålRepository
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.EN_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.FLERE_ARBEIDSGIVERE
import no.nav.helse.modell.oppgave.Egenskap.FORLENGELSE
import no.nav.helse.modell.oppgave.Egenskap.FORSTEGANGSBEHANDLING
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.GOSYS
import no.nav.helse.modell.oppgave.Egenskap.HASTER
import no.nav.helse.modell.oppgave.Egenskap.INFOTRYGDFORLENGELSE
import no.nav.helse.modell.oppgave.Egenskap.INGEN_UTBETALING
import no.nav.helse.modell.oppgave.Egenskap.MEDLEMSKAP
import no.nav.helse.modell.oppgave.Egenskap.OVERGANG_FRA_IT
import no.nav.helse.modell.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.modell.oppgave.Egenskap.REVURDERING
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.SKJØNNSFASTSETTELSE
import no.nav.helse.modell.oppgave.Egenskap.SPESIALSAK
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Egenskap.TILBAKEDATERT
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_SYKMELDT
import no.nav.helse.modell.oppgave.Egenskap.UTLAND
import no.nav.helse.modell.oppgave.Egenskap.VERGEMÅL
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.slf4j.LoggerFactory
import java.util.UUID

internal class OpprettSaksbehandleroppgave(
    private val behovData: GodkjenningsbehovData,
    private val oppgaveService: OppgaveService,
    private val automatisering: Automatisering,
    private val personRepository: PersonRepository,
    private val risikovurderingRepository: RisikovurderingRepository,
    private val egenAnsattRepository: EgenAnsattRepository,
    private val utbetalingtype: Utbetalingtype,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val utbetaling: Utbetaling,
    private val vergemålRepository: VergemålRepository,
    private val vedtakDao: VedtakDao,
    private val påVentRepository: PåVentRepository,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettSaksbehandleroppgave::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val fødselsnummer = behovData.fødselsnummer
        val vedtaksperiodeId = behovData.vedtaksperiodeId
        val hendelseId = behovData.id
        val utbetalingId = utbetaling.utbetalingId
        val periodetype = behovData.periodetype
        val inntektskilde = behovData.inntektskilde
        val kanAvvises = behovData.kanAvvises

        val egenskaper =
            emptyList<Egenskap>()
                .vurderEgenAnsatt(fødselsnummer)
                .vurderAdressebeskyttelse(fødselsnummer)
                .vurderOppgavetype(utbetalingtype)
                .vurderStikkprøve(vedtaksperiodeId, hendelseId)
                .vurderVurderingsmomenter(vedtaksperiodeId)
                .vurderVergemål(fødselsnummer)
                .vurderEnhetUtland(fødselsnummer)
                .vurderMottaker()
                .vurderInntektskilde(inntektskilde)
                .vurderPeriodetype(periodetype)
                .vurderSpesialsak(vedtaksperiodeId)
                .vurderPåVent(vedtaksperiodeId)
                .vurderSkjønnsfastsettelse(vedtaksperiodeId)
                .vurderTilbakedatert(vedtaksperiodeId)
                .vurderKunÅpenGosysOppgave(vedtaksperiodeId)
                .vurderMedlemskap(vedtaksperiodeId)
                .vurderHaster(vedtaksperiodeId)

        oppgaveService.nyOppgave(fødselsnummer, context.id()) { reservertId ->
            val oppgave = Oppgave.nyOppgave(reservertId, vedtaksperiodeId, utbetalingId, hendelseId, kanAvvises, egenskaper)

            logg.info("Saksbehandleroppgave opprettet, avventer lagring: $oppgave")
            sikkerLogg.info("Saksbehandleroppgave opprettet, avventer lagring: $oppgave")
            oppgave
        }

        return true
    }

    private fun List<Egenskap>.vurderEgenAnsatt(fødselsnummer: String): List<Egenskap> {
        if (egenAnsattRepository.erEgenAnsatt(fødselsnummer) == true) return this + EGEN_ANSATT
        return this
    }

    private fun List<Egenskap>.vurderAdressebeskyttelse(fødselsnummer: String): List<Egenskap> {
        val adressebeskyttelse = personRepository.finnAdressebeskyttelse(fødselsnummer) ?: return this
        return when (adressebeskyttelse) {
            Adressebeskyttelse.StrengtFortrolig,
            Adressebeskyttelse.StrengtFortroligUtland,
            -> this + STRENGT_FORTROLIG_ADRESSE
            Adressebeskyttelse.Fortrolig -> this + FORTROLIG_ADRESSE
            else -> this
        }
    }

    private fun List<Egenskap>.vurderOppgavetype(utbetalingtype: Utbetalingtype): List<Egenskap> {
        if (utbetalingtype == Utbetalingtype.REVURDERING) return this + REVURDERING
        return this + SØKNAD
    }

    private fun List<Egenskap>.vurderStikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ): List<Egenskap> {
        if (automatisering.erStikkprøve(vedtaksperiodeId, hendelseId)) return this + STIKKPRØVE
        return this
    }

    private fun List<Egenskap>.vurderVurderingsmomenter(vedtaksperiodeId: UUID): List<Egenskap> {
        if (!this.contains(REVURDERING) && risikovurderingRepository.kreverSupersaksbehandler(vedtaksperiodeId)) {
            return this + RISK_QA
        }
        return this
    }

    private fun List<Egenskap>.vurderVergemål(fødselsnummer: String): List<Egenskap> {
        if (vergemålRepository.harVergemål(fødselsnummer) == true) return this + VERGEMÅL
        return this
    }

    private fun List<Egenskap>.vurderEnhetUtland(fødselsnummer: String): List<Egenskap> {
        if (HentEnhetløsning.erEnhetUtland(personRepository.finnEnhetId(fødselsnummer))) return this + UTLAND
        return this
    }

    private fun List<Egenskap>.vurderMottaker(): List<Egenskap> {
        return when {
            utbetaling.delvisRefusjon() -> this + DELVIS_REFUSJON
            utbetaling.kunUtbetalingTilSykmeldt() -> this + UTBETALING_TIL_SYKMELDT
            utbetaling.kunUtbetalingTilArbeidsgiver() -> this + UTBETALING_TIL_ARBEIDSGIVER
            utbetaling.ingenUtbetaling() -> this + INGEN_UTBETALING
            else -> this
        }
    }

    private fun List<Egenskap>.vurderInntektskilde(inntektskilde: Inntektskilde): List<Egenskap> {
        return when (inntektskilde) {
            Inntektskilde.EN_ARBEIDSGIVER -> this + EN_ARBEIDSGIVER
            Inntektskilde.FLERE_ARBEIDSGIVERE -> this + FLERE_ARBEIDSGIVERE
        }
    }

    private fun List<Egenskap>.vurderPeriodetype(periodetype: Periodetype): List<Egenskap> {
        return when (periodetype) {
            Periodetype.FØRSTEGANGSBEHANDLING -> this + FORSTEGANGSBEHANDLING
            Periodetype.FORLENGELSE -> this + FORLENGELSE
            Periodetype.INFOTRYGDFORLENGELSE -> this + INFOTRYGDFORLENGELSE
            Periodetype.OVERGANG_FRA_IT -> this + OVERGANG_FRA_IT
        }
    }

    private fun List<Egenskap>.vurderSpesialsak(vedtaksperiodeId: UUID): List<Egenskap> {
        if (vedtakDao.erSpesialsak(vedtaksperiodeId)) return this + SPESIALSAK
        return this
    }

    private fun List<Egenskap>.vurderPåVent(vedtaksperiodeId: UUID): List<Egenskap> {
        if (påVentRepository.erPåVent(vedtaksperiodeId)) return this + PÅ_VENT
        return this
    }

    private fun List<Egenskap>.vurderSkjønnsfastsettelse(vedtaksperiodeId: UUID): List<Egenskap> {
        if (sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId)) return this + SKJØNNSFASTSETTELSE
        return this
    }

    private fun List<Egenskap>.vurderTilbakedatert(vedtaksperiodeId: UUID): List<Egenskap> {
        if (sykefraværstilfelle.erTilbakedatert(vedtaksperiodeId)) return this + TILBAKEDATERT
        return this
    }

    private fun List<Egenskap>.vurderKunÅpenGosysOppgave(vedtaksperiodeId: UUID): List<Egenskap> {
        if (sykefraværstilfelle.harKunÅpenGosysOppgave(vedtaksperiodeId)) return this + GOSYS
        return this
    }

    private fun List<Egenskap>.vurderMedlemskap(vedtaksperiodeId: UUID): List<Egenskap> {
        if (sykefraværstilfelle.harMedlemskapsvarsel(vedtaksperiodeId)) return this + MEDLEMSKAP
        return this
    }

    private fun List<Egenskap>.vurderHaster(vedtaksperiodeId: UUID): List<Egenskap> {
        if (sykefraværstilfelle.haster(vedtaksperiodeId) && utbetaling.harEndringIUtbetalingTilSykmeldt()) return this + HASTER
        return this
    }
}
