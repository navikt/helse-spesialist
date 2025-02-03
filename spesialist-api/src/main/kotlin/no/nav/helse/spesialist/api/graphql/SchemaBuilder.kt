package no.nav.helse.spesialist.api.graphql

import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.RisikovurderingApiDao
import no.nav.helse.db.api.TildelingApiDao
import no.nav.helse.db.api.TotrinnsvurderingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VergemålApiDao
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.graphql.mutation.AnnulleringMutation
import no.nav.helse.spesialist.api.graphql.mutation.AnnulleringMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.MinimumSykdomsgradMutation
import no.nav.helse.spesialist.api.graphql.mutation.MinimumSykdomsgradMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.NotatMutation
import no.nav.helse.spesialist.api.graphql.mutation.NotatMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OpphevStansMutation
import no.nav.helse.spesialist.api.graphql.mutation.OpphevStansMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OpptegnelseMutation
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutation
import no.nav.helse.spesialist.api.graphql.mutation.PaVentMutation
import no.nav.helse.spesialist.api.graphql.mutation.PersonMutation
import no.nav.helse.spesialist.api.graphql.mutation.SkjonnsfastsettelseMutation
import no.nav.helse.spesialist.api.graphql.mutation.TildelingMutation
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutation
import no.nav.helse.spesialist.api.graphql.mutation.VarselMutation
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutation
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQuery
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQueryHandler
import no.nav.helse.spesialist.api.graphql.query.DokumentQuery
import no.nav.helse.spesialist.api.graphql.query.DokumentQueryHandler
import no.nav.helse.spesialist.api.graphql.query.OppgaverQuery
import no.nav.helse.spesialist.api.graphql.query.OppgaverQueryHandler
import no.nav.helse.spesialist.api.graphql.query.OpptegnelseQuery
import no.nav.helse.spesialist.api.graphql.query.OpptegnelseQueryHandler
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.graphql.query.PersonQueryHandler
import no.nav.helse.spesialist.api.person.PersonService
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.snapshot.SnapshotService

class SchemaBuilder(
    personApiDao: PersonApiDao,
    egenAnsattApiDao: EgenAnsattApiDao,
    tildelingApiDao: TildelingApiDao,
    arbeidsgiverApiDao: ArbeidsgiverApiDao,
    overstyringApiDao: OverstyringApiDao,
    risikovurderingApiDao: RisikovurderingApiDao,
    varselRepository: VarselApiRepository,
    oppgaveApiDao: OppgaveApiDao,
    periodehistorikkApiDao: PeriodehistorikkApiDao,
    påVentApiDao: PåVentApiDao,
    vergemålApiDao: VergemålApiDao,
    snapshotService: SnapshotService,
    notatDao: NotatApiDao,
    totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    reservasjonClient: ReservasjonClient,
    avviksvurderinghenter: Avviksvurderinghenter,
    behandlingsstatistikkMediator: IBehandlingsstatistikkService,
    saksbehandlerhåndterer: Saksbehandlerhåndterer,
    apiOppgaveService: ApiOppgaveService,
    totrinnsvurderinghåndterer: Totrinnsvurderinghåndterer,
    godkjenninghåndterer: Godkjenninghåndterer,
    personhåndterer: Personhåndterer,
    dokumenthåndterer: Dokumenthåndterer,
    stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    env: Environment,
) {
    val queries =
        listOf(
            PersonQuery(
                handler =
                    PersonQueryHandler(
                        personoppslagService =
                            PersonService(
                                personApiDao = personApiDao,
                                egenAnsattApiDao = egenAnsattApiDao,
                                tildelingApiDao = tildelingApiDao,
                                arbeidsgiverApiDao = arbeidsgiverApiDao,
                                overstyringApiDao = overstyringApiDao,
                                risikovurderingApiDao = risikovurderingApiDao,
                                varselRepository = varselRepository,
                                oppgaveApiDao = oppgaveApiDao,
                                periodehistorikkApiDao = periodehistorikkApiDao,
                                notatDao = notatDao,
                                totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                                påVentApiDao = påVentApiDao,
                                vergemålApiDao = vergemålApiDao,
                                snapshotService = snapshotService,
                                reservasjonClient = reservasjonClient,
                                apiOppgaveService = apiOppgaveService,
                                saksbehandlerhåndterer = saksbehandlerhåndterer,
                                avviksvurderinghenter = avviksvurderinghenter,
                                personhåndterer = personhåndterer,
                                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                                env = env,
                            ),
                    ),
            ),
            OppgaverQuery(
                handler =
                    OppgaverQueryHandler(
                        apiOppgaveService = apiOppgaveService,
                    ),
            ),
            BehandlingsstatistikkQuery(
                handler =
                    BehandlingsstatistikkQueryHandler(
                        behandlingsstatistikkMediator = behandlingsstatistikkMediator,
                    ),
            ),
            OpptegnelseQuery(
                handler =
                    OpptegnelseQueryHandler(
                        saksbehandlerhåndterer = saksbehandlerhåndterer,
                    ),
            ),
            DokumentQuery(
                handler =
                    DokumentQueryHandler(
                        personApiDao = personApiDao,
                        egenAnsattApiDao = egenAnsattApiDao,
                        dokumenthåndterer = dokumenthåndterer,
                    ),
            ),
        )

    val mutations =
        listOf(
            NotatMutation(
                handler = NotatMutationHandler(notatDao = notatDao),
            ),
            VarselMutation(varselRepository = varselRepository),
            TildelingMutation(saksbehandlerhåndterer = saksbehandlerhåndterer),
            OpptegnelseMutation(saksbehandlerhåndterer = saksbehandlerhåndterer),
            OverstyringMutation(saksbehandlerhåndterer = saksbehandlerhåndterer),
            SkjonnsfastsettelseMutation(saksbehandlerhåndterer = saksbehandlerhåndterer),
            MinimumSykdomsgradMutation(
                handler = MinimumSykdomsgradMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
            ),
            TotrinnsvurderingMutation(
                saksbehandlerhåndterer = saksbehandlerhåndterer,
                apiOppgaveService = apiOppgaveService,
                totrinnsvurderinghåndterer = totrinnsvurderinghåndterer,
            ),
            VedtakMutation(
                saksbehandlerhåndterer = saksbehandlerhåndterer,
                godkjenninghåndterer = godkjenninghåndterer,
            ),
            PersonMutation(
                personhåndterer = personhåndterer,
            ),
            AnnulleringMutation(
                handler =
                    AnnulleringMutationHandler(
                        saksbehandlerhåndterer = saksbehandlerhåndterer,
                    ),
            ),
            PaVentMutation(
                saksbehandlerhåndterer = saksbehandlerhåndterer,
            ),
            OpphevStansMutation(
                handler = OpphevStansMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
            ),
        )
}
