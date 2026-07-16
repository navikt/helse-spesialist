package no.nav.helse.spesialist.api

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import io.ktor.server.application.Application
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.BehandlingsstatistikkService
import no.nav.helse.mediator.PersonhåndtererImpl
import no.nav.helse.spesialist.api.graphql.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.SpesialistSchema
import no.nav.helse.spesialist.api.graphql.SpesialistSchema.MutationHandlers
import no.nav.helse.spesialist.api.graphql.SpesialistSchema.QueryHandlers
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutationHandler
import no.nav.helse.spesialist.api.graphql.query.PersonQueryHandler
import no.nav.helse.spesialist.api.rest.dokumenter.DokumentMediator
import no.nav.helse.spesialist.application.AlleIdenterHenter
import no.nav.helse.spesialist.application.BehandlendeEnhetHenter
import no.nav.helse.spesialist.application.ForsikringsvurderingHenter
import no.nav.helse.spesialist.application.InfotrygdperiodeHenter
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.OpptegnelseListener
import no.nav.helse.spesialist.application.PersonPseudoIdProvider
import no.nav.helse.spesialist.application.PersoninfoHenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilTilganger

class ApiModule(
    private val configuration: Configuration,
    daos: Daos,
    private val meldingPubliserer: MeldingPubliserer,
    private val sessionFactory: SessionFactory,
    private val opptegnelseListener: OpptegnelseListener,
    private val environmentToggles: EnvironmentToggles,
    snapshothenter: Snapshothenter,
    private val krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
    private val behandlendeEnhetHenter: BehandlendeEnhetHenter,
    private val forsikringsvurderingHenter: ForsikringsvurderingHenter,
    private val alleIdenterHenter: AlleIdenterHenter,
    private val personinfoHenter: PersoninfoHenter,
    private val infotrygdperiodeHenter: InfotrygdperiodeHenter,
    private val tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
    private val tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
    private val personPseudoIdProvider: PersonPseudoIdProvider,
    private val populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
) {
    data class Configuration(
        val clientId: String,
        val issuerUrl: String,
        val jwkProviderUri: String,
        val tokenEndpoint: String,
        val eksponerOpenApi: Boolean,
        val versjonAvKode: String,
    )

    val dokumentMediator =
        DokumentMediator(
            publiserer = meldingPubliserer,
        )

    private val apiOppgaveService =
        ApiOppgaveService(
            oppgaveDao = daos.oppgaveDao,
        )

    private val saksbehandlerMediator =
        SaksbehandlerMediator(
            daos = daos,
            versjonAvKode = configuration.versjonAvKode,
            meldingPubliserer = meldingPubliserer,
            sessionFactory = sessionFactory,
        )

    private val behandlingsstatistikkService = BehandlingsstatistikkService(behandlingsstatistikkDao = daos.behandlingsstatistikkDao)

    private val spesialistSchema =
        run {
            val personhåndterer = PersonhåndtererImpl(publiserer = meldingPubliserer)
            SpesialistSchema(
                queryHandlers =
                    QueryHandlers(
                        person =
                            PersonQueryHandler(
                                daos = daos,
                                apiOppgaveService = apiOppgaveService,
                                personhåndterer = personhåndterer,
                                snapshothenter = snapshothenter,
                                sessionFactory = sessionFactory,
                                personPseudoIdProvider = personPseudoIdProvider,
                                populasjonstilgangskontrollProvider = populasjonstilgangskontrollProvider,
                            ),
                    ),
                mutationHandlers =
                    MutationHandlers(
                        overstyring =
                            OverstyringMutationHandler(
                                saksbehandlerMediator = saksbehandlerMediator,
                            ),
                        totrinnsvurdering =
                            TotrinnsvurderingMutationHandler(
                                saksbehandlerMediator = saksbehandlerMediator,
                            ),
                    ),
            )
        }

    fun setUpApi(application: Application) {
        configureKtorApplication(
            ktorApplication = application,
            apiModuleConfiguration = configuration,
            spesialistSchema = spesialistSchema,
            sessionFactory = sessionFactory,
            opptegnelseListener = opptegnelseListener,
            meldingPubliserer = meldingPubliserer,
            dokumentMediator = dokumentMediator,
            forsikringsvurderingHenter = forsikringsvurderingHenter,
            environmentToggles = environmentToggles,
            krrRegistrertStatusHenter = krrRegistrertStatusHenter,
            behandlendeEnhetHenter = behandlendeEnhetHenter,
            tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
            tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger,
            alleIdenterHenter = alleIdenterHenter,
            personinfoHenter = personinfoHenter,
            infotrygdperiodeHenter = infotrygdperiodeHenter,
            personPseudoIdProvider = personPseudoIdProvider,
            behandlingsstatistikkService = behandlingsstatistikkService,
            populasjonstilgangskontrollProvider = populasjonstilgangskontrollProvider,
        )
    }
}
