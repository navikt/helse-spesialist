package no.nav.helse.spesialist.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.kafka.RiverSetup
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.varsel.LegacyVarselRepository
import no.nav.helse.spesialist.application.ForsikringHenter
import no.nav.helse.spesialist.application.tilgangskontroll.Brukerrollehenter

class KafkaModule(
    configuration: Configuration,
    private val rapidsConnection: RapidsConnection,
    sessionFactory: SessionFactory,
    daos: Daos,
    stikkprøver: Stikkprøver,
    brukerrollehenter: Brukerrollehenter,
    forsikringHenter: ForsikringHenter,
    environmentToggles: EnvironmentToggles,
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
                                    oppgaveRepository = daos.oppgaveRepository,
                                    brukerrollehenter = brukerrollehenter,
                                )
                            },
                            subsumsjonsmelderProvider = {
                                Subsumsjonsmelder(
                                    configuration.versjonAvKode,
                                    meldingPubliserer,
                                )
                            },
                            stikkprøver = stikkprøver,
                        ),
                    dokumentDao = daos.dokumentDao,
                    legacyVarselRepository =
                        LegacyVarselRepository(
                            legacyVarselDao = daos.legacyVarselDao,
                            definisjonDao = daos.definisjonDao,
                        ),
                    poisonPillDao = daos.poisonPillDao,
                    ignorerMeldingerForUkjentePersoner = configuration.ignorerMeldingerForUkjentePersoner,
                ),
            meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
            sessionFactory = sessionFactory,
            versjonAvKode = configuration.versjonAvKode,
            forsikringHenter = forsikringHenter,
            environmentToggles = environmentToggles,
        )

    fun kobleOppRivers() {
        riverSetup.registrerRivers(rapidsConnection)
    }
}
