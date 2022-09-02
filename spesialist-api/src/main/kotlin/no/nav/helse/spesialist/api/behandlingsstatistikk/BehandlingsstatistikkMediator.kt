package no.nav.helse.spesialist.api.behandlingsstatistikk

import java.time.LocalDate
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype

class BehandlingsstatistikkMediator(private val behandlingsstatistikkDao: BehandlingsstatistikkDao) {

    fun getBehandlingsstatistikk(fom: LocalDate = LocalDate.now()): Behandlingsstatistikk {
        val automatiseringer = behandlingsstatistikkDao.getAutomatiseringerPerInntektOgPeriodetype(fom)
        val manueltUtførteOppgaver = behandlingsstatistikkDao.getManueltUtførteOppgaverPerInntektOgPeriodetype(fom)
        val tilgjengeligeOppgaver = behandlingsstatistikkDao.getTilgjengeligeOppgaverPerInntektOgPeriodetype()

        val enArbeidsgiver = {
            Antall(
                automatisk = automatiseringer.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER] ?: 0,
                manuelt = manueltUtførteOppgaver.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER] ?: 0,
                tilgjengelig = tilgjengeligeOppgaver.perInntekttype[Inntektskilde.EN_ARBEIDSGIVER] ?: 0
            )
        }

        val flereArbeidsgivere = {
            Antall(
                automatisk = automatiseringer.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE] ?: 0,
                manuelt = manueltUtførteOppgaver.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE] ?: 0,
                tilgjengelig = tilgjengeligeOppgaver.perInntekttype[Inntektskilde.FLERE_ARBEIDSGIVERE] ?: 0
            )
        }

        val forstegangsbehandling = {
            Antall(
                automatisk = automatiseringer.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING] ?: 0,
                manuelt = manueltUtførteOppgaver.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING] ?: 0,
                tilgjengelig = tilgjengeligeOppgaver.perPeriodetype[Periodetype.FØRSTEGANGSBEHANDLING] ?: 0
            )
        }

        val forlengelser = {
            Antall(
                automatisk = (automatiseringer.perPeriodetype[Periodetype.FORLENGELSE]
                    ?: 0) + (automatiseringer.perPeriodetype[Periodetype.INFOTRYGDFORLENGELSE] ?: 0),
                manuelt = (manueltUtførteOppgaver.perPeriodetype[Periodetype.FORLENGELSE]
                    ?: 0) + (manueltUtførteOppgaver.perPeriodetype[Periodetype.INFOTRYGDFORLENGELSE] ?: 0),
                tilgjengelig = (tilgjengeligeOppgaver.perPeriodetype[Periodetype.FORLENGELSE]
                    ?: 0) + (tilgjengeligeOppgaver.perPeriodetype[Periodetype.INFOTRYGDFORLENGELSE] ?: 0),
            )
        }

        val manueltUtførteOppgavetyper = behandlingsstatistikkDao.getManueltUtførteOppgaverPerOppgavetype(fom)
        val tilgjengeligeOppgavetyper = behandlingsstatistikkDao.getTilgjengeligeOppgaverPerOppgavetype()

        return Behandlingsstatistikk(
            enArbeidsgiver = enArbeidsgiver(),
            flereArbeidsgivere = flereArbeidsgivere(),
            forstegangsbehandling = forstegangsbehandling(),
            forlengelser = forlengelser(),
            utbetalingTilSykmeldt = Antall(
                0,
                manueltUtførteOppgavetyper[Oppgavetype.UTBETALING_TIL_SYKMELDT] ?: 0,
                tilgjengeligeOppgavetyper[Oppgavetype.UTBETALING_TIL_SYKMELDT] ?: 0
            ),
            faresignaler = Antall(
                0,
                manueltUtførteOppgavetyper[Oppgavetype.RISK_QA] ?: 0,
                tilgjengeligeOppgavetyper[Oppgavetype.RISK_QA] ?: 0
            ),
            fortroligAdresse = Antall(
                0,
                manueltUtførteOppgavetyper[Oppgavetype.FORTROLIG_ADRESSE] ?: 0,
                tilgjengeligeOppgavetyper[Oppgavetype.FORTROLIG_ADRESSE] ?: 0
            ),
            stikkprover = Antall(
                0,
                manueltUtførteOppgavetyper[Oppgavetype.STIKKPRØVE] ?: 0,
                tilgjengeligeOppgavetyper[Oppgavetype.STIKKPRØVE] ?: 0
            ),
            revurdering = Antall(
                0,
                manueltUtførteOppgavetyper[Oppgavetype.REVURDERING] ?: 0,
                tilgjengeligeOppgavetyper[Oppgavetype.REVURDERING] ?: 0
            ),
            delvisRefusjon = Antall(
                0,
                manueltUtførteOppgavetyper[Oppgavetype.DELVIS_REFUSJON] ?: 0,
                tilgjengeligeOppgavetyper[Oppgavetype.DELVIS_REFUSJON] ?: 0
            ),
            beslutter = Antall(
                0,
                behandlingsstatistikkDao.getAntallFullførteBeslutteroppgaver(fom),
                behandlingsstatistikkDao.getAntallTilgjengeligeBeslutteroppgaver()
            ),
        )
    }

    fun hentSaksbehandlingsstatistikk() =
        behandlingsstatistikkDao.oppgavestatistikk().let(BehandlingstatistikkForSpeilDto::toSpeilMap)
}
