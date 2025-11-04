package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.modell.Annullering
import no.nav.helse.modell.melding.AnnullertUtbetalingEvent
import no.nav.helse.modell.saksbehandler.handlinger.HandlingType
import no.nav.helse.spesialist.api.feilhåndtering.AlleredeAnnullert
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData

class AnnulleringMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : AnnulleringMutationSchema {
    override fun annuller(
        annullering: ApiAnnulleringData,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        saksbehandlerMediator.utførHandling(HandlingType.ANNULLER_UTBETALING, env) { saksbehandler, _, tx, outbox ->
            if (tx.annulleringRepository.finnAnnullering(vedtaksperiodeId = annullering.vedtaksperiodeId) != null) {
                throw AlleredeAnnullert(annullering.vedtaksperiodeId)
            }

            tx.annulleringRepository.lagreAnnullering(
                annullering =
                    Annullering.Factory.ny(
                        arbeidsgiverFagsystemId = annullering.arbeidsgiverFagsystemId,
                        personFagsystemId = annullering.personFagsystemId,
                        saksbehandlerOid = saksbehandler.id(),
                        vedtaksperiodeId = annullering.vedtaksperiodeId,
                        årsaker = annullering.arsaker.map { it.arsak },
                        kommentar = annullering.kommentar,
                    ),
            )

            outbox.leggTil(
                fødselsnummer = annullering.fodselsnummer,
                hendelse =
                    AnnullertUtbetalingEvent(
                        fødselsnummer = annullering.fodselsnummer,
                        organisasjonsnummer = annullering.organisasjonsnummer,
                        saksbehandlerOid = saksbehandler.id().value,
                        saksbehandlerIdent = saksbehandler.ident,
                        saksbehandlerEpost = saksbehandler.epost,
                        vedtaksperiodeId = annullering.vedtaksperiodeId,
                        begrunnelser = annullering.arsaker.map { arsak -> arsak.arsak },
                        arsaker =
                            annullering.arsaker.map { arsak ->
                                AnnullertUtbetalingEvent.Årsak(
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
