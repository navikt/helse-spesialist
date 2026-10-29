package no.nav.helse.spesialist.db

import no.nav.helse.db.Daos
import no.nav.helse.spesialist.db.dao.*
import no.nav.helse.spesialist.db.dao.api.*
import javax.sql.DataSource

class DBDaos(
    dataSource: DataSource,
) : Daos {
    override val behandlingsstatistikkDao = PgBehandlingsstatistikkDao(dataSource)
    override val commandContextDao = PgCommandContextDao(dataSource)
    override val dokumentDao = PgDokumentDao(dataSource)
    override val meldingDao = PgMeldingDao(dataSource)
    override val meldingDuplikatkontrollDao = PgMeldingDuplikatkontrollDao(dataSource)
    override val oppgaveDao = PgOppgaveDao(dataSource)
    override val personDao = PgPersonDao(dataSource)
    override val poisonPillDao = PgPoisonPillDao(dataSource)
    override val vedtakBegrunnelseDao = PgVedtakBegrunnelseDao(dataSource)

    override val overstyringApiDao = PgOverstyringApiDao(dataSource)
    override val periodehistorikkApiDao = PgPeriodehistorikkApiDao(dataSource)
    override val varselApiRepository = PgVarselApiRepository(dataSource)
}
