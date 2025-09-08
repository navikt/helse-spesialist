package no.nav.helse.spesialist.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.FeatureToggles
import no.nav.helse.Gruppekontroll
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.kafka.RiverSetup
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.TilgangskontrollørForReservasjon
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper

class KafkaModule(
    configuration: Configuration,
    private val rapidsConnection: RapidsConnection,
    sessionFactory: SessionFactory,
    daos: Daos,
    tilgangsgrupper: Tilgangsgrupper,
    stikkprøver: Stikkprøver,
    featureToggles: FeatureToggles,
    gruppekontroll: Gruppekontroll,
) {
    data class Configuration(
        val versjonAvKode: String,
        val ignorerMeldingerForUkjentePersoner: Boolean,
    )

    val meldingPubliserer: MeldingPubliserer = MessageContextMeldingPubliserer(rapidsConnection)

    private val riverSetup =
        RiverSetup(
            mediator =
                MeldingMediator(
                    sessionFactory = sessionFactory,
                    personDao = daos.personDao,
                    commandContextDao = daos.commandContextDao,
                    meldingDao = daos.meldingDao,
                    meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
                    kommandofabrikk =
                        Kommandofabrikk(
                            oppgaveService = {
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
                            },
                            godkjenningMediator = GodkjenningMediator(daos.opptegnelseDao),
                            subsumsjonsmelderProvider = {
                                Subsumsjonsmelder(
                                    configuration.versjonAvKode,
                                    meldingPubliserer,
                                )
                            },
                            stikkprøver = stikkprøver,
                            featureToggles = featureToggles,
                        ),
                    dokumentDao = daos.dokumentDao,
                    varselRepository =
                        VarselRepository(
                            varselDao = daos.varselDao,
                            definisjonDao = daos.definisjonDao,
                        ),
                    poisonPillDao = daos.poisonPillDao,
                    ignorerMeldingerForUkjentePersoner = configuration.ignorerMeldingerForUkjentePersoner,
                ),
            meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
            sessionFactory = sessionFactory,
            featureToggles = featureToggles,
        )

    fun kobleOppRivers() {
        riverSetup.registrerRivers(rapidsConnection)
    }
}
