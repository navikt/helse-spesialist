package no.nav.helse.spesialist.api

import io.ktor.server.application.Application
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.BehandlingsstatistikkService
import no.nav.helse.mediator.GodkjenningService
import no.nav.helse.mediator.PersonhåndtererImpl
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlinghåndtererImpl
import no.nav.helse.spesialist.api.graphql.kobleOppApi
import no.nav.helse.spesialist.api.graphql.lagSchemaMedResolversOgHandlers
import no.nav.helse.spesialist.api.rest.OpphevStansController
import no.nav.helse.spesialist.api.rest.RestDelegator
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgruppehenter

class ApiModule(
    private val configuration: Configuration,
    private val tilgangsgruppeUuider: TilgangsgruppeUuider,
    daos: Daos,
    meldingPubliserer: MeldingPubliserer,
    tilgangsgruppehenter: Tilgangsgruppehenter,
    sessionFactory: SessionFactory,
    versjonAvKode: String,
    environmentToggles: EnvironmentToggles,
    snapshothenter: Snapshothenter,
    reservasjonshenter: Reservasjonshenter,
) {
    data class Configuration(
        val clientId: String,
        val issuerUrl: String,
        val jwkProviderUri: String,
        val tokenEndpoint: String,
    )

    val oppgaveService =
        OppgaveService(
            oppgaveDao = daos.oppgaveDao,
            reservasjonDao = daos.reservasjonDao,
            meldingPubliserer = meldingPubliserer,
            oppgaveRepository = daos.oppgaveRepository,
            tilgangsgruppehenter = tilgangsgruppehenter,
        )

    private val apiOppgaveService =
        ApiOppgaveService(
            oppgaveDao = daos.oppgaveDao,
            oppgaveService = oppgaveService,
        )

    private val stansAutomatiskBehandlinghåndterer = StansAutomatiskBehandlinghåndtererImpl(daos.stansAutomatiskBehandlingDao)

    private val saksbehandlerMediator =
        SaksbehandlerMediator(
            daos = daos,
            versjonAvKode = versjonAvKode,
            meldingPubliserer = meldingPubliserer,
            oppgaveService = oppgaveService,
            apiOppgaveService = apiOppgaveService,
            environmentToggles = environmentToggles,
            sessionFactory = sessionFactory,
            tilgangsgruppeUuider = tilgangsgruppeUuider,
        )

    private val restDelegator =
        RestDelegator(
            sessionFactory = sessionFactory,
            tilgangsgruppeUuider = tilgangsgruppeUuider,
            meldingPubliserer = meldingPubliserer,
        )

    private val spesialistSchema =
        lagSchemaMedResolversOgHandlers(
            daos = daos,
            apiOppgaveService = apiOppgaveService,
            saksbehandlerMediator = saksbehandlerMediator,
            stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
            personhåndterer = PersonhåndtererImpl(publiserer = meldingPubliserer),
            snapshothenter = snapshothenter,
            reservasjonshenter = reservasjonshenter,
            sessionFactory = sessionFactory,
            behandlingstatistikk = BehandlingsstatistikkService(behandlingsstatistikkDao = daos.behandlingsstatistikkDao),
            dokumenthåndterer = DokumentMediator(daos.dokumentDao, meldingPubliserer),
            godkjenninghåndterer =
                GodkjenningService(
                    oppgaveDao = daos.oppgaveDao,
                    publiserer = meldingPubliserer,
                    oppgaveService = oppgaveService,
                    reservasjonDao = daos.reservasjonDao,
                    periodehistorikkDao = daos.periodehistorikkDao,
                    sessionFactory = sessionFactory,
                ),
            meldingPubliserer = meldingPubliserer,
        )

    fun setUpApi(application: Application) {
        kobleOppApi(
            ktorApplication = application,
            apiModuleConfiguration = configuration,
            tilgangsgruppeUuider = tilgangsgruppeUuider,
            spesialistSchema = spesialistSchema,
            opphevStansController = OpphevStansController(saksbehandlerMediator),
            restDelegator = restDelegator,
        )
    }
}
