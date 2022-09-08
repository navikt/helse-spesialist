package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.toSchema
import graphql.schema.GraphQLSchema
import no.nav.helse.mediator.api.ReservasjonClient
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.spesialist.api.oppgave.experimental.OppgaveService
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao

internal class SchemaBuilder(
    val personApiDao: PersonApiDao,
    val egenAnsattDao: EgenAnsattDao,
    val tildelingDao: TildelingDao,
    val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    val overstyringApiDao: OverstyringApiDao,
    val risikovurderingApiDao: RisikovurderingApiDao,
    val varselDao: VarselDao,
    val utbetalingDao: UtbetalingDao,
    val oppgaveDao: OppgaveDao,
    val oppgaveApiDao: OppgaveApiDao,
    val periodehistorikkDao: PeriodehistorikkDao,
    val snapshotMediator: SnapshotMediator,
    val notatDao: NotatDao,
    val reservasjonClient: ReservasjonClient,
    val oppgaveMediator: OppgaveMediator,
    val oppgaveService: OppgaveService,
    val behandlingsstatistikkMediator: BehandlingsstatistikkMediator,
) {
    fun build(): GraphQLSchema {
        val schemaConfig = SchemaGeneratorConfig(
            supportedPackages = listOf(
                "no.nav.helse.spesialist.api.grapqhl",
                "no.nav.helse.mediator.api.graphql",
                "no.nav.helse.mediator.graphql"
            )
        )
        return toSchema(
            config = schemaConfig,
            queries = listOf(
                TopLevelObject(
                    PersonQuery(
                        personApiDao = personApiDao,
                        egenAnsattDao = egenAnsattDao,
                        tildelingDao = tildelingDao,
                        arbeidsgiverApiDao = arbeidsgiverApiDao,
                        overstyringApiDao = overstyringApiDao,
                        risikovurderingApiDao = risikovurderingApiDao,
                        varselDao = varselDao,
                        oppgaveDao = oppgaveDao,
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
                        egenAnsattDao = egenAnsattDao,
                        utbetalingDao = utbetalingDao,
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
                )
            )
        )
    }
}
