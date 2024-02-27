package no.nav.helse.mediator

import java.time.LocalDate
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkDao
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse
import no.nav.helse.spesialist.api.behandlingsstatistikk.Statistikk
import no.nav.helse.spesialist.api.oppgave.Egenskap
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Mottakertype
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype

class BehandlingsstatistikkMediator(private val behandlingsstatistikkDao: BehandlingsstatistikkDao):
    IBehandlingsstatistikkMediator {

    override fun getBehandlingsstatistikk(fom: LocalDate): BehandlingsstatistikkResponse {
        val automatisertPerInntektPeriodetypeOgMottaker = behandlingsstatistikkDao.getAutomatiseringerPerInntektOgPeriodetype(fom)
        val manueltUtførteOppgaver = behandlingsstatistikkDao.getManueltUtførteOppgaverPerInntektOgPeriodetype(fom)
        val tilgjengeligeOppgaver = behandlingsstatistikkDao.getTilgjengeligeOppgaverPerInntektOgPeriodetype()

        val enArbeidsgiver = {
            Statistikk(
                automatisk = automatisertPerInntektPeriodetypeOgMottaker.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER]
                    ?: 0,
                manuelt = manueltUtførteOppgaver.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER] ?: 0,
                tilgjengelig = tilgjengeligeOppgaver.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER] ?: 0
            )
        }

        val flereArbeidsgivere = {
            Statistikk(
                automatisk = automatisertPerInntektPeriodetypeOgMottaker.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE]
                    ?: 0,
                manuelt = manueltUtførteOppgaver.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE] ?: 0,
                tilgjengelig = tilgjengeligeOppgaver.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE] ?: 0
            )
        }

        val forstegangsbehandling = {
            Statistikk(
                automatisk = automatisertPerInntektPeriodetypeOgMottaker.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING]
                    ?: 0,
                manuelt = manueltUtførteOppgaver.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING] ?: 0,
                tilgjengelig = tilgjengeligeOppgaver.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING] ?: 0
            )
        }

        val forlengelser = {
            Statistikk(
                automatisk = (automatisertPerInntektPeriodetypeOgMottaker.perPeriodetype[Periodetype.FORLENGELSE] ?: 0)
                        + (automatisertPerInntektPeriodetypeOgMottaker.perPeriodetype[Periodetype.INFOTRYGDFORLENGELSE]
                    ?: 0),
                manuelt = (manueltUtførteOppgaver.perPeriodetype[Periodetype.FORLENGELSE] ?: 0)
                        + (manueltUtførteOppgaver.perPeriodetype[Periodetype.INFOTRYGDFORLENGELSE] ?: 0),
                tilgjengelig = (tilgjengeligeOppgaver.perPeriodetype[Periodetype.FORLENGELSE] ?: 0)
                        + (tilgjengeligeOppgaver.perPeriodetype[Periodetype.INFOTRYGDFORLENGELSE] ?: 0),
            )
        }

        val forlengelseIt = {
            Statistikk(
                automatisk = automatisertPerInntektPeriodetypeOgMottaker.perPeriodetype[Periodetype.OVERGANG_FRA_IT]
                    ?: 0,
                manuelt = manueltUtførteOppgaver.perPeriodetype[Periodetype.OVERGANG_FRA_IT] ?: 0,
                tilgjengelig = tilgjengeligeOppgaver.perPeriodetype[Periodetype.OVERGANG_FRA_IT] ?: 0,
            )
        }

        return BehandlingsstatistikkResponse(
            enArbeidsgiver = enArbeidsgiver(),
            flereArbeidsgivere = flereArbeidsgivere(),
            forstegangsbehandling = forstegangsbehandling(),
            forlengelser = forlengelser(),
            forlengelseIt = forlengelseIt(),
            utbetalingTilArbeidsgiver = Statistikk(
                automatisertPerInntektPeriodetypeOgMottaker.perMottakertype[Mottakertype.ARBEIDSGIVER] ?: 0,
                behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(Egenskap.SØKNAD, fom),
                behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(Egenskap.SØKNAD),
            ),
            utbetalingTilSykmeldt = Statistikk(
                automatisertPerInntektPeriodetypeOgMottaker.perMottakertype[Mottakertype.SYKMELDT] ?: 0,
                behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(Egenskap.UTBETALING_TIL_SYKMELDT, fom),
                behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(Egenskap.UTBETALING_TIL_SYKMELDT)
            ),
            faresignaler = Statistikk(
                0,
                behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(Egenskap.RISK_QA, fom),
                behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(Egenskap.RISK_QA)
            ),
            fortroligAdresse = Statistikk(
                0,
                behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(Egenskap.FORTROLIG_ADRESSE, fom),
                behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(Egenskap.FORTROLIG_ADRESSE)
            ),
            stikkprover = Statistikk(
                0,
                behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(Egenskap.STIKKPRØVE, fom),
                behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(Egenskap.STIKKPRØVE)
            ),
            revurdering = Statistikk(
                0,
                behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(Egenskap.REVURDERING, fom),
                behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(Egenskap.REVURDERING)
            ),
            delvisRefusjon = Statistikk(
                automatisertPerInntektPeriodetypeOgMottaker.perMottakertype[Mottakertype.BEGGE] ?: 0,
                behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(Egenskap.DELVIS_REFUSJON, fom),
                behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(Egenskap.DELVIS_REFUSJON)
            ),
            beslutter = Statistikk(
                0,
                behandlingsstatistikkDao.getAntallFullførteBeslutteroppgaver(fom),
                behandlingsstatistikkDao.getAntallTilgjengeligeBeslutteroppgaver()
            ),
            egenAnsatt = Statistikk(
                0,
                behandlingsstatistikkDao.getAntallManueltFullførteEgenAnsattOppgaver(fom),
                behandlingsstatistikkDao.getAntallTilgjengeligeEgenAnsattOppgaver()
            ),
            antallAnnulleringer = behandlingsstatistikkDao.getAntallAnnulleringer(fom),
        )
    }
}