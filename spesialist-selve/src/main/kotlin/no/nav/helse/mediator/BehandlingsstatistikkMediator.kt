package no.nav.helse.mediator

import java.time.LocalDate
import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse
import no.nav.helse.spesialist.api.behandlingsstatistikk.Statistikk
import no.nav.helse.spesialist.api.graphql.schema.Utbetalingtype
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Mottakertype
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype

class BehandlingsstatistikkMediator(private val behandlingsstatistikkDao: BehandlingsstatistikkDao):
    IBehandlingsstatistikkMediator {

    override fun getBehandlingsstatistikk(fom: LocalDate): BehandlingsstatistikkResponse {
        val automatisertPerKombinasjon = behandlingsstatistikkDao.getAutomatiseringPerKombinasjon(fom)
        val manueltUtførteOppgaver = behandlingsstatistikkDao.getManueltUtførteOppgaverPerInntektOgPeriodetype(fom)
        val tilgjengeligeOppgaver = behandlingsstatistikkDao.getTilgjengeligeOppgaverPerInntektOgPeriodetype()

        val enArbeidsgiver = {
            Statistikk(
                automatisk = automatisertPerKombinasjon.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER]
                    ?: 0,
                manuelt = manueltUtførteOppgaver.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER] ?: 0,
                tilgjengelig = tilgjengeligeOppgaver.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER] ?: 0
            )
        }

        val flereArbeidsgivere = {
            Statistikk(
                automatisk = automatisertPerKombinasjon.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE] ?: 0,
                manuelt = manueltUtførteOppgaver.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE] ?: 0,
                tilgjengelig = tilgjengeligeOppgaver.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE] ?: 0
            )
        }

        val forstegangsbehandling = {
            Statistikk(
                automatisk = automatisertPerKombinasjon.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING]
                    ?: 0,
                manuelt = manueltUtførteOppgaver.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING] ?: 0,
                tilgjengelig = tilgjengeligeOppgaver.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING] ?: 0
            )
        }

        val forlengelser = {
            Statistikk(
                automatisk = (automatisertPerKombinasjon.perPeriodetype[Periodetype.FORLENGELSE] ?: 0)
                        + (automatisertPerKombinasjon.perPeriodetype[Periodetype.INFOTRYGDFORLENGELSE]
                    ?: 0),
                manuelt = (manueltUtførteOppgaver.perPeriodetype[Periodetype.FORLENGELSE] ?: 0)
                        + (manueltUtførteOppgaver.perPeriodetype[Periodetype.INFOTRYGDFORLENGELSE] ?: 0),
                tilgjengelig = (tilgjengeligeOppgaver.perPeriodetype[Periodetype.FORLENGELSE] ?: 0)
                        + (tilgjengeligeOppgaver.perPeriodetype[Periodetype.INFOTRYGDFORLENGELSE] ?: 0),
            )
        }

        val forlengelseIt = {
            Statistikk(
                automatisk = automatisertPerKombinasjon.perPeriodetype[Periodetype.OVERGANG_FRA_IT]
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
                automatisk = automatisertPerKombinasjon.perMottakertype[Mottakertype.ARBEIDSGIVER] ?: 0,
                manuelt = behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER, fom),
                tilgjengelig = behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER),
            ),
            utbetalingTilSykmeldt = Statistikk(
                automatisk = automatisertPerKombinasjon.perMottakertype[Mottakertype.SYKMELDT] ?: 0,
                manuelt = behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(EgenskapForDatabase.UTBETALING_TIL_SYKMELDT, fom),
                tilgjengelig = behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(EgenskapForDatabase.UTBETALING_TIL_SYKMELDT)
            ),
            delvisRefusjon = Statistikk(
                automatisk = automatisertPerKombinasjon.perMottakertype[Mottakertype.BEGGE] ?: 0,
                manuelt = behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(EgenskapForDatabase.DELVIS_REFUSJON, fom),
                tilgjengelig = behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(EgenskapForDatabase.DELVIS_REFUSJON)
            ),
            faresignaler = Statistikk(
                automatisk = 0,
                manuelt = behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(EgenskapForDatabase.RISK_QA, fom),
                tilgjengelig = behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(EgenskapForDatabase.RISK_QA)
            ),
            fortroligAdresse = Statistikk(
                automatisk = 0,
                manuelt = behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(EgenskapForDatabase.FORTROLIG_ADRESSE, fom),
                tilgjengelig = behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(EgenskapForDatabase.FORTROLIG_ADRESSE)
            ),
            stikkprover = Statistikk(
                automatisk = 0,
                manuelt = behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(EgenskapForDatabase.STIKKPRØVE, fom),
                tilgjengelig = behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(EgenskapForDatabase.STIKKPRØVE)
            ),
            revurdering = Statistikk(
                automatisk = automatisertPerKombinasjon.perUtbetalingtype[Utbetalingtype.REVURDERING] ?: 0,
                manuelt = behandlingsstatistikkDao.antallFerdigstilteOppgaverFor(EgenskapForDatabase.REVURDERING, fom),
                tilgjengelig = behandlingsstatistikkDao.antallTilgjengeligeOppgaverFor(EgenskapForDatabase.REVURDERING)
            ),
            beslutter = Statistikk(
                automatisk = 0,
                manuelt = behandlingsstatistikkDao.getAntallFullførteBeslutteroppgaver(fom),
                tilgjengelig = behandlingsstatistikkDao.getAntallTilgjengeligeBeslutteroppgaver()
            ),
            egenAnsatt = Statistikk(
                automatisk = 0,
                manuelt = behandlingsstatistikkDao.getAntallManueltFullførteEgenAnsattOppgaver(fom),
                tilgjengelig = behandlingsstatistikkDao.getAntallTilgjengeligeEgenAnsattOppgaver()
            ),
            antallAnnulleringer = behandlingsstatistikkDao.getAntallAnnulleringer(fom),
        )
    }
}