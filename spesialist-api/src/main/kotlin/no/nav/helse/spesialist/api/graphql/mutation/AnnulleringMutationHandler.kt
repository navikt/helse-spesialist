package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.modell.melding.AnnullertUtbetalingEvent
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringArsak
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import no.nav.helse.modell.saksbehandler.handlinger.HandlingType
import no.nav.helse.spesialist.api.feilhåndtering.AlleredeAnnullert
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper

class AnnulleringMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : AnnulleringMutationSchema {
    override fun annuller(
        annullering: ApiAnnulleringData,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        saksbehandlerMediator.utførHandling(HandlingType.ANNULLER_UTBETALING, env) { saksbehandler, _, tx, kø ->
            if (tx.annulleringRepository.finnAnnullering(
                    annullering.arbeidsgiverFagsystemId,
                    annullering.personFagsystemId,
                ) != null
            ) {
                throw AlleredeAnnullert(annullering.vedtaksperiodeId)
            }
            tx.annulleringRepository.lagreAnnullering(
                annulleringDto =
                    AnnulleringDto(
                        aktørId = annullering.aktorId,
                        fødselsnummer = annullering.fodselsnummer,
                        organisasjonsnummer = annullering.organisasjonsnummer,
                        vedtaksperiodeId = annullering.vedtaksperiodeId,
                        utbetalingId = annullering.utbetalingId,
                        arbeidsgiverFagsystemId = annullering.arbeidsgiverFagsystemId,
                        personFagsystemId = annullering.personFagsystemId,
                        årsaker =
                            annullering.arsaker.map { arsak ->
                                AnnulleringArsak(
                                    key = arsak._key,
                                    arsak = arsak.arsak,
                                )
                            },
                        kommentar = annullering.kommentar,
                    ),
                saksbehandlerWrapper = SaksbehandlerWrapper(saksbehandler = saksbehandler),
            )
            kø.publiser(
                fødselsnummer = annullering.fodselsnummer,
                hendelse =
                    AnnullertUtbetalingEvent(
                        fødselsnummer = annullering.fodselsnummer,
                        aktørId = annullering.aktorId,
                        organisasjonsnummer = annullering.organisasjonsnummer,
                        saksbehandlerOid = saksbehandler.id().value,
                        saksbehandlerNavn = saksbehandler.navn,
                        saksbehandlerIdent = saksbehandler.ident,
                        saksbehandlerEpost = saksbehandler.epost,
                        vedtaksperiodeId = annullering.vedtaksperiodeId,
                        utbetalingId = annullering.utbetalingId,
                        arbeidsgiverFagsystemId = annullering.arbeidsgiverFagsystemId,
                        personFagsystemId = annullering.personFagsystemId,
                        begrunnelser = annullering.arsaker.map { arsak -> arsak.arsak },
                        arsaker =
                            annullering.arsaker.map { arsak ->
                                AnnulleringArsak(
                                    key = arsak._key,
                                    arsak = arsak.arsak,
                                )
                            },
                        kommentar = annullering.kommentar,
                    ),
                årsak = "annullering av utbetaling",
            )
        }

        return byggRespons(true)
    }
}
