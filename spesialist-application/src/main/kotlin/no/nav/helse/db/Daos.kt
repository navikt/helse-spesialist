package no.nav.helse.db

import no.nav.helse.db.api.*

interface Daos {
    val behandlingsstatistikkDao: BehandlingsstatistikkDao
    val commandContextDao: CommandContextDao
    val dokumentDao: DokumentDao
    val meldingDao: MeldingDao
    val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao
    val oppgaveDao: OppgaveDao
    val personDao: PersonDao
    val poisonPillDao: PoisonPillDao
    val vedtakBegrunnelseDao: VedtakBegrunnelseDao

    val periodehistorikkApiDao: PeriodehistorikkApiDao
    val varselApiRepository: VarselApiRepository
}
