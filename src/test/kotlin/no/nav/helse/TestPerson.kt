package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.TilbakerullingMessage
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

class TestPerson(private val dataSource: DataSource) {
    val fødselsnummer = nyttFødselsnummer()
    val aktørId = nyAktørId(fødselsnummer)
    val orgnummer = nyttOrgnummer()

    internal val spleisMockClient = SpleisMockClient(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        organisasjonsnummer = orgnummer
    )

    private val speilSnapshotRestClient = SpeilSnapshotRestClient(
        spleisMockClient.client,
        accessTokenClient(),
        "spleisClientId"
    )

    val rapid = TestRapid()

    private val mediator = SpleisbehovMediator(
        dataSource = dataSource,
        speilSnapshotRestClient = speilSnapshotRestClient,
        spesialistOID = spesialistOID
    ).apply { init(rapid) }

    fun sendGodkjenningMessage(
        eventId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID()
    ) {
        mediator.håndter(
            GodkjenningMessage(
                id = eventId,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 31),
                warnings = emptyList()
            ), "{}"
        )
    }

    fun sendPersoninfo(
        eventId: UUID,
        infotrygdUtbetalingerJson: Any = "{}"
    ) {
        mediator.håndter(
            eventId,
            HentEnhetLøsning("1119"),
            HentPersoninfoLøsning(
                fornavn = "Test",
                mellomnavn = null,
                etternavn = "Testsen",
                fødselsdato = LocalDate.now(),
                kjønn = Kjønn.Mann
            ),
            HentInfotrygdutbetalingerLøsning(objectMapper.convertValue(infotrygdUtbetalingerJson))
        )
    }

    fun rullTilbake(vararg vedtaksperiodeIder: UUID) {
        mediator.håndter(TilbakerullingMessage(fødselsnummer, vedtaksperiodeIder.toList()))
    }

    fun settInfotrygdSistOppdatert(sistOppdatert: LocalDate) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE person SET infotrygdutbetalinger_oppdatert=? WHERE fodselsnummer=?;",
                sistOppdatert,
                fødselsnummer.toLong()
            ).asUpdate
        )
    }

    fun finnBehov(vararg forIder: UUID): List<JsonNode> {
        return (0.until(rapid.inspektør.size))
            .map { rapid.inspektør.message(it) }
            .filter { it["@event_name"].asText() == "behov" }
            .filter { UUID.fromString(it["vedtaksperiodeId"].asText()) in forIder }
    }

    companion object {
        private val spesialistOID = UUID.randomUUID()
        private var nåværendeFødselsnummer = 20000000000
        private var nåværendeOrgnummer = 900000000

        private fun nyttFødselsnummer(): String {
            nåværendeFødselsnummer++
            return nåværendeFødselsnummer.toString()
        }

        private fun nyttOrgnummer(): String {
            nåværendeOrgnummer++
            return nåværendeOrgnummer.toString()
        }

        private fun nyAktørId(fødselsnummer: String) = "90$fødselsnummer"
    }
}
