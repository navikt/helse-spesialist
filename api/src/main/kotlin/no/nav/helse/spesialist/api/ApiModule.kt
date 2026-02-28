package no.nav.helse.spesialist.api

import io.ktor.server.application.Application
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.BehandlingsstatistikkService
import no.nav.helse.mediator.PersonhåndtererImpl
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.spesialist.api.graphql.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.SpesialistSchema
import no.nav.helse.spesialist.api.graphql.SpesialistSchema.MutationHandlers
import no.nav.helse.spesialist.api.graphql.SpesialistSchema.QueryHandlers
import no.nav.helse.spesialist.api.graphql.StansAutomatiskBehandlinghåndtererImpl
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.PaVentMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.PersonMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.SkjonnsfastsettelseMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.StansAutomatiskBehandlingMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TildelingMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutationHandler
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQueryHandler
import no.nav.helse.spesialist.api.graphql.query.OppgaverQueryHandler
import no.nav.helse.spesialist.api.graphql.query.PersonQueryHandler
import no.nav.helse.spesialist.api.rest.DokumentMediator
import no.nav.helse.spesialist.application.AlleIdenterHenter
import no.nav.helse.spesialist.application.ForsikringHenter
import no.nav.helse.spesialist.application.InngangsvilkårHenter
import no.nav.helse.spesialist.application.InngangsvilkårInnsender
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.PersoninfoHenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.tilgangskontroll.Brukerrollehenter
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilTilganger

class ApiModule(
    private val configuration: Configuration,
    daos: Daos,
    private val meldingPubliserer: MeldingPubliserer,
    brukerrollehenter: Brukerrollehenter,
    private val sessionFactory: SessionFactory,
    private val environmentToggles: EnvironmentToggles,
    snapshothenter: Snapshothenter,
    private val krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
    private val forsikringHenter: ForsikringHenter,
    private val inngangsvilkårHenter: InngangsvilkårHenter,
    private val inngangsvilkårInnsender: InngangsvilkårInnsender,
    private val alleIdenterHenter: AlleIdenterHenter,
    private val personinfoHenter: PersoninfoHenter,
    private val tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
    private val tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
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

    val oppgaveService =
        OppgaveService(
            oppgaveDao = daos.oppgaveDao,
            reservasjonDao = daos.reservasjonDao,
            meldingPubliserer = meldingPubliserer,
            oppgaveRepository = daos.oppgaveRepository,
            brukerrollehenter = brukerrollehenter,
        )

    private val apiOppgaveService =
        ApiOppgaveService(
            oppgaveDao = daos.oppgaveDao,
            oppgaveService = oppgaveService,
            sessionFactory = sessionFactory,
        )

    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(daos.stansAutomatiskBehandlingDao)

    private val saksbehandlerMediator =
        SaksbehandlerMediator(
            daos = daos,
            versjonAvKode = configuration.versjonAvKode,
            meldingPubliserer = meldingPubliserer,
            oppgaveService = oppgaveService,
            apiOppgaveService = apiOppgaveService,
            sessionFactory = sessionFactory,
        )

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
                                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                                personhåndterer = personhåndterer,
                                snapshothenter = snapshothenter,
                                sessionFactory = sessionFactory,
                            ),
                        oppgaver =
                            OppgaverQueryHandler(
                                apiOppgaveService = apiOppgaveService,
                            ),
                        behandlingsstatistikk =
                            BehandlingsstatistikkQueryHandler(
                                behandlingsstatistikkMediator = BehandlingsstatistikkService(behandlingsstatistikkDao = daos.behandlingsstatistikkDao),
                            ),
                    ),
                mutationHandlers =
                    MutationHandlers(
                        tildeling = TildelingMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                        overstyring =
                            OverstyringMutationHandler(
                                saksbehandlerMediator = saksbehandlerMediator,
                            ),
                        skjonnsfastsettelse = SkjonnsfastsettelseMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                        totrinnsvurdering =
                            TotrinnsvurderingMutationHandler(
                                saksbehandlerMediator = saksbehandlerMediator,
                            ),
                        person = PersonMutationHandler(personhåndterer = personhåndterer),
                        paVent = PaVentMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                        stansAutomatiskBehandling = StansAutomatiskBehandlingMutationHandler(sessionFactory = sessionFactory),
                    ),
            )
        }

    fun setUpApi(application: Application) {
        configureKtorApplication(
            ktorApplication = application,
            apiModuleConfiguration = configuration,
            spesialistSchema = spesialistSchema,
            dokumentMediator = dokumentMediator,
            environmentToggles = environmentToggles,
            sessionFactory = sessionFactory,
            meldingPubliserer = meldingPubliserer,
            krrRegistrertStatusHenter = krrRegistrertStatusHenter,
            forsikringHenter = forsikringHenter,
            inngangsvilkårHenter = inngangsvilkårHenter,
            inngangsvilkårInnsender = inngangsvilkårInnsender,
            tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
            tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger,
            alleIdenterHenter = alleIdenterHenter,
            personinfoHenter = personinfoHenter,
        )
    }
}
