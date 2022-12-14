package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.toSchema
import graphql.schema.GraphQLSchema
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.mutation.NotatMutation
import no.nav.helse.spesialist.api.graphql.mutation.VarselMutation
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQuery
import no.nav.helse.spesialist.api.graphql.query.NotatQuery
import no.nav.helse.spesialist.api.graphql.query.OppdragQuery
import no.nav.helse.spesialist.api.graphql.query.OppgaverQuery
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.experimental.OppgaveService
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.utbetaling.UtbetalingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao

internal class SchemaBuilder(
    val personApiDao: PersonApiDao,
    val egenAnsattApiDao: EgenAnsattApiDao,
    val tildelingDao: TildelingDao,
    val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    val overstyringApiDao: OverstyringApiDao,
    val risikovurderingApiDao: RisikovurderingApiDao,
    val varselDao: VarselDao,
    val varselRepository: ApiVarselRepository,
    val utbetalingApiDao: UtbetalingApiDao,
    val oppgaveApiDao: OppgaveApiDao,
    val periodehistorikkDao: PeriodehistorikkDao,
    val snapshotMediator: SnapshotMediator,
    val notatDao: NotatDao,
    val reservasjonClient: ReservasjonClient,
    val oppgaveService: OppgaveService,
    val behandlingsstatistikkMediator: BehandlingsstatistikkMediator,
) {
    fun build(): GraphQLSchema {
        val schemaConfig = SchemaGeneratorConfig(
            supportedPackages = listOf(
                "no.nav.helse.spesialist.api.graphql",
            )
        )
        return toSchema(
            config = schemaConfig,
            queries = listOf(
                TopLevelObject(
                    PersonQuery(
                        personApiDao = personApiDao,
                        egenAnsattApiDao = egenAnsattApiDao,
                        tildelingDao = tildelingDao,
                        arbeidsgiverApiDao = arbeidsgiverApiDao,
                        overstyringApiDao = overstyringApiDao,
                        risikovurderingApiDao = risikovurderingApiDao,
                        varselDao = varselDao,
                        varselRepository = varselRepository,
                        oppgaveApiDao = oppgaveApiDao,
                        periodehistorikkDao = periodehistorikkDao,
                        snapshotMediator = snapshotMediator,
                        notatDao = notatDao,
                        reservasjonClient = reservasjonClient,
                    )
                ),
                TopLevelObject(
                    OppdragQuery(
                        personApiDao = personApiDao,
                        egenAnsattApiDao = egenAnsattApiDao,
                        utbetalingApiDao = utbetalingApiDao,
                    )
                ),
                TopLevelObject(
                    OppgaverQuery(
                        oppgaveApiDao = oppgaveApiDao,
                        oppgaveService = oppgaveService,
                    )
                ),
                TopLevelObject(
                    BehandlingsstatistikkQuery(
                        behandlingsstatistikkMediator = behandlingsstatistikkMediator,
                    )
                ),
                TopLevelObject(
                    NotatQuery(notatDao = notatDao)
                )
            ),
            mutations = listOf(
                TopLevelObject(
                    NotatMutation(notatDao = notatDao)
                ),
                TopLevelObject(
                    VarselMutation(varselRepository = varselRepository)
                )
            )
        )
    }
}
