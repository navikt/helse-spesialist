package no.nav.helse.spesialist.api

import io.ktor.server.application.Application
import no.nav.helse.Gruppekontroll
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.BehandlingsstatistikkService
import no.nav.helse.mediator.GodkjenningService
import no.nav.helse.mediator.PersonhåndtererImpl
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.TilgangskontrollørForReservasjon
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlinghåndtererImpl
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.graphql.kobleOppApi
import no.nav.helse.spesialist.api.graphql.lagSchemaMedResolversOgHandlers
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Snapshothenter

class ApiModule(
    private val configuration: Configuration,
    private val tilgangsgrupper: Tilgangsgrupper,
    daos: Daos,
    meldingPubliserer: MeldingPubliserer,
    gruppekontroll: Gruppekontroll,
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
            tilgangskontroll =
                TilgangskontrollørForReservasjon(
                    gruppekontroll,
                    tilgangsgrupper,
                ),
            tilgangsgrupper = tilgangsgrupper,
            oppgaveRepository = daos.oppgaveRepository,
        )

    private val apiOppgaveService =
        ApiOppgaveService(
            oppgaveDao = daos.oppgaveDao,
            tilgangsgrupper = tilgangsgrupper,
            oppgaveService = oppgaveService,
        )

    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(
            daos.stansAutomatiskBehandlingDao,
            daos.oppgaveDao,
            daos.notatDao,
            daos.dialogDao,
        )

    private val spesialistSchema =
        lagSchemaMedResolversOgHandlers(
            daos = daos,
            apiOppgaveService = apiOppgaveService,
            saksbehandlerMediator =
                SaksbehandlerMediator(
                    daos = daos,
                    versjonAvKode = versjonAvKode,
                    meldingPubliserer = meldingPubliserer,
                    oppgaveService = oppgaveService,
                    apiOppgaveService = apiOppgaveService,
                    tilgangsgrupper = tilgangsgrupper,
                    stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                    annulleringRepository = daos.annulleringRepository,
                    environmentToggles = environmentToggles,
                    sessionFactory = sessionFactory,
                    tilgangskontroll =
                        TilgangskontrollørForReservasjon(
                            gruppekontroll,
                            tilgangsgrupper,
                        ),
                ),
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
            tilgangsgrupper = tilgangsgrupper,
            spesialistSchema = spesialistSchema,
        )
    }
}
