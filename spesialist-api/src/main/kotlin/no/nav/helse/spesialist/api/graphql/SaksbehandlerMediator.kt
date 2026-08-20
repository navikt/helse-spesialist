package no.nav.helse.spesialist.api.graphql

import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.overstyring.Saksbehandlingsmelder
import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.saksbehandler.handlinger.Handling
import no.nav.helse.modell.saksbehandler.handlinger.Personhandling
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.overstyringer.Arbeidsforhold
import no.nav.helse.spesialist.domain.overstyringer.Overstyring
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtArbeidsforhold
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtArbeidsgiver
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtInntektOgRefusjon
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinje
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinjedag
import no.nav.helse.spesialist.domain.overstyringer.Refusjonselement
import no.nav.helse.tell
import no.nav.helse.spesialist.api.graphql.Modellfeil as ApiModellfeil

class SaksbehandlerMediator(
    private val versjonAvKode: String,
    private val meldingPubliserer: MeldingPubliserer,
    private val sessionFactory: SessionFactory,
) {
    fun håndter(
        handlingFraApi: HandlingFraApi,
        saksbehandler: Saksbehandler,
    ) {
        val modellhandling =
            handlingFraApi.tilModellversjon(
                saksbehandlerOid = saksbehandler.id,
            )
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        tell(modellhandling)
        val saksbehandlerWrapper = SaksbehandlerWrapper(saksbehandler = saksbehandler)
        saksbehandlerWrapper.register(Saksbehandlingsmelder(meldingPubliserer))
        saksbehandlerWrapper.register(Subsumsjonsmelder(versjonAvKode, meldingPubliserer))

        loggInfo(
            "Utfører handling ${modellhandling.loggnavn()} på vegne av saksbehandler",
            "saksbehandlerIdent" to saksbehandler.ident.value,
        )
        when (modellhandling) {
            is Overstyring -> {
                overstyringUnitOfWork(
                    loggnavn = modellhandling.loggnavn(),
                    overstyring = modellhandling,
                    saksbehandler = saksbehandler,
                    sessionFactory = sessionFactory,
                )
                modellhandling.utførAv(saksbehandlerWrapper)
            }

            is Personhandling -> {
                håndter(modellhandling, saksbehandlerWrapper)
            }

            else -> {
                modellhandling.utførAv(saksbehandlerWrapper)
            }
        }
        loggInfo("Handling ${modellhandling.loggnavn()} utført")
    }

    private fun håndter(
        handling: Personhandling,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) = try {
        handling.utførAv(saksbehandlerWrapper)
    } catch (e: Modellfeil) {
        throw e.tilApiversjon()
    }

    private fun Modellfeil.tilApiversjon(): ApiModellfeil =
        when (this) {
            is no.nav.helse.modell.OppgaveIkkeTildelt -> {
                OppgaveIkkeTildelt(oppgaveId)
            }

            is OppgaveAlleredeSendtBeslutter -> {
                no.nav.helse.spesialist.api.graphql.OppgaveAlleredeSendtBeslutter(
                    oppgaveId,
                )
            }

            is OppgaveAlleredeSendtIRetur -> {
                no.nav.helse.spesialist.api.graphql.OppgaveAlleredeSendtIRetur(
                    oppgaveId,
                )
            }

            is OppgaveKreverVurderingAvToSaksbehandlere -> {
                no.nav.helse.spesialist.api.graphql.OppgaveKreverVurderingAvToSaksbehandlere(
                    oppgaveId,
                )
            }

            is ManglerTilgang -> {
                IkkeTilgang(oid, oppgaveId)
            }
        }

    private fun HandlingFraApi.tilModellversjon(
        saksbehandlerOid: SaksbehandlerOid,
    ): Handling =
        when (this) {
            is ApiArbeidsforholdOverstyringHandling -> this.tilModellversjon(saksbehandlerOid)
            is ApiInntektOgRefusjonOverstyring -> this.tilModellversjon(saksbehandlerOid)
            is ApiTidslinjeOverstyring -> this.tilModellversjon(saksbehandlerOid)
            else -> throw IllegalStateException("Støtter ikke handling ${this::class.simpleName}")
        }

    private fun ApiArbeidsforholdOverstyringHandling.tilModellversjon(saksbehandlerOid: SaksbehandlerOid): OverstyrtArbeidsforhold =
        OverstyrtArbeidsforhold.ny(
            fødselsnummer = fodselsnummer,
            aktørId = aktorId,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
            overstyrteArbeidsforhold =
                overstyrteArbeidsforhold.map { overstyrtArbeidsforhold ->
                    Arbeidsforhold(
                        organisasjonsnummer = overstyrtArbeidsforhold.orgnummer,
                        deaktivert = overstyrtArbeidsforhold.deaktivert,
                        begrunnelse = overstyrtArbeidsforhold.begrunnelse,
                        forklaring = overstyrtArbeidsforhold.forklaring,
                        lovhjemmel =
                            overstyrtArbeidsforhold.lovhjemmel?.let {
                                Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                            },
                    )
                },
        )

    private fun ApiInntektOgRefusjonOverstyring.tilModellversjon(saksbehandlerOid: SaksbehandlerOid): OverstyrtInntektOgRefusjon =
        OverstyrtInntektOgRefusjon.ny(
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
            arbeidsgivere =
                arbeidsgivere.map { overstyrArbeidsgiver ->
                    OverstyrtArbeidsgiver(
                        overstyrArbeidsgiver.organisasjonsnummer,
                        overstyrArbeidsgiver.manedligInntekt,
                        overstyrArbeidsgiver.fraManedligInntekt,
                        overstyrArbeidsgiver.refusjonsopplysninger?.map {
                            Refusjonselement(it.fom, it.tom, it.belop)
                        },
                        overstyrArbeidsgiver.fraRefusjonsopplysninger?.map {
                            Refusjonselement(it.fom, it.tom, it.belop)
                        },
                        begrunnelse = overstyrArbeidsgiver.begrunnelse,
                        forklaring = overstyrArbeidsgiver.forklaring,
                        lovhjemmel =
                            overstyrArbeidsgiver.lovhjemmel?.let {
                                Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                            },
                        fom = overstyrArbeidsgiver.fom,
                        tom = overstyrArbeidsgiver.tom,
                    )
                },
        )

    private fun ApiTidslinjeOverstyring.tilModellversjon(saksbehandlerOid: SaksbehandlerOid): OverstyrtTidslinje =
        OverstyrtTidslinje.ny(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            saksbehandlerOid = saksbehandlerOid,
            dager =
                dager.map {
                    OverstyrtTidslinjedag(
                        dato = it.dato,
                        type = it.type,
                        fraType = it.fraType,
                        grad = it.grad,
                        fraGrad = it.fraGrad,
                        lovhjemmel =
                            it.lovhjemmel?.let { lovhjemmel ->
                                Lovhjemmel(
                                    paragraf = lovhjemmel.paragraf,
                                    ledd = lovhjemmel.ledd,
                                    bokstav = lovhjemmel.bokstav,
                                    lovverk = lovhjemmel.lovverk,
                                    lovverksversjon = lovhjemmel.lovverksversjon,
                                )
                            },
                    )
                },
            begrunnelse = begrunnelse,
        )
}

private fun overstyringUnitOfWork(
    overstyring: Overstyring,
    saksbehandler: Saksbehandler,
    sessionFactory: SessionFactory,
    loggnavn: String,
) {
    sessionFactory.transactionalSessionScope { session ->
        overstyring.loggInfo(
            "Utfører overstyring $loggnavn på vegne av saksbehandler",
            "saksbehandlerIdent" to saksbehandler.ident.value,
        )
        session.saksbehandlerRepository.lagre(saksbehandler)

        val fødselsnummer = overstyring.fødselsnummer
        overstyring.loggInfo(
            "Reserverer person til saksbehandler",
            "fødselsnummer" to fødselsnummer,
            "saksbehandlerIdent" to saksbehandler.ident.value,
        )
        session.reservasjonDao.reserverPerson(saksbehandler.id.value, fødselsnummer)

        val totrinnsvurdering =
            session.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer) ?: Totrinnsvurdering.ny(fødselsnummer)
        totrinnsvurdering.nyOverstyring(overstyring)
        session.totrinnsvurderingRepository.lagre(totrinnsvurdering)
    }
}
