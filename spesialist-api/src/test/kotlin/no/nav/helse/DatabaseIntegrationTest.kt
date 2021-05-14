package no.nav.helse

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.vedtaksperiode.VarselDao
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

internal abstract class DatabaseIntegrationTest: AbstractDatabaseTest() {
    protected companion object {
        val NAVN = Triple("Ola", "Kari", "Nordhen")
        val ENHET = Pair(101, "Halden")
        const val FØDSELSNUMMER = "01017011111"
        const val AKTØRID = "01017011111111"
        const val ARBEIDSGIVER_NAVN = "EN ARBEIDSGIVER"
        const val ORGANISASJONSNUMMER = "987654321"
        val PERIODE = Triple(UUID.randomUUID(), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val ARBEIDSFORHOLD = Quadruple(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2), "EN TITTEL", 100)
    }

    protected val varselDao: VarselDao = VarselDao(dataSource)
    protected val arbeidsgiverApiDao: ArbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    protected val risikovurderingApiDao: RisikovurderingApiDao = RisikovurderingApiDao(dataSource)

    protected fun nyVedtaksperiode() = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        val (id, fom, tom) = PERIODE
        val personid = person()
        val arbeidsgiverid = arbeidsgiver()
        val snapshotid = snapshot()
        @Language("PostgreSQL")
        val statement = "INSERT INTO vedtak(vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, speil_snapshot_ref) VALUES(?, ?, ?, ?, ?, ?)"
        session.run(
            queryOf(
                statement, id, fom, tom, arbeidsgiverid, personid, snapshotid
            ).asUpdateAndReturnGeneratedKey
        ).also {
            arbeidsforhold(personid, arbeidsgiverid)
        }
    }

    protected fun vedtakId(vedtaksperiodeId: UUID = PERIODE.first) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?"
        requireNotNull(session.run(queryOf(statement, vedtaksperiodeId).map { it.long("id") }.asSingle)) {
            "Finner ikke vedtak i db for vedtaksperiodeId=$vedtaksperiodeId"
        }
    }

    private fun person() = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        val personinfoid = personinfo()
        val infotrygdutbetalingerid = infotrygdutbetalinger()
        @Language("PostgreSQL")
        val statement = "INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref) VALUES(?, ?, ?, ?, ?)"
        requireNotNull(session.run(
            queryOf(
                statement, FØDSELSNUMMER.toLong(), AKTØRID.toLong(), infotrygdutbetalingerid, ENHET.first, personinfoid
            ).asUpdateAndReturnGeneratedKey
        ))
    }

    private fun personinfo() = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        val (fornavn, mellomnavn, etternavn) = NAVN
        @Language("PostgreSQL")
        val statement = "INSERT INTO person_info(fornavn, mellomnavn, etternavn, fodselsdato, kjonn) VALUES(?, ?, ?, ?::date, ?::person_kjonn)"
        requireNotNull(session.run(
            queryOf(
                statement,
                fornavn,
                mellomnavn,
                etternavn,
                LocalDate.of(1970, 1, 1),
                "Ukjent"
            ).asUpdateAndReturnGeneratedKey
        ))
    }

    protected fun arbeidsgiver(bransjer: List<String> = emptyList()) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        val bransjeid = bransjer(bransjer)
        val navnid = arbeidsgivernavn()

        @Language("PostgreSQL")
        val statement = "INSERT INTO arbeidsgiver(orgnummer, navn_ref, bransjer_ref) VALUES(?, ?, ?)"
        requireNotNull(session.run(queryOf(statement, ORGANISASJONSNUMMER.toLong(), navnid, bransjeid).asUpdateAndReturnGeneratedKey))
    }

    private fun arbeidsforhold(personid: Long, arbeidsgiverid: Long) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        val (startdato, sluttdato, tittel, prosent) = ARBEIDSFORHOLD
        @Language("PostgreSQL")
        val statement = "INSERT INTO arbeidsforhold(person_ref, arbeidsgiver_ref, startdato, sluttdato, stillingstittel, stillingsprosent) VALUES(?, ?, ?, ?, ?, ?)"
        requireNotNull(session.run(queryOf(statement, personid, arbeidsgiverid, startdato, sluttdato, tittel, prosent).asUpdateAndReturnGeneratedKey))
    }

    private fun bransjer(bransjer: List<String>) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO arbeidsgiver_bransjer(bransjer) VALUES(?)"
        requireNotNull(session.run(queryOf(statement, objectMapper.writeValueAsString(bransjer)).asUpdateAndReturnGeneratedKey))
    }

    private fun arbeidsgivernavn() = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO arbeidsgiver_navn(navn) VALUES(?)"
        requireNotNull(session.run(queryOf(statement, ARBEIDSGIVER_NAVN).asUpdateAndReturnGeneratedKey))
    }

    private fun infotrygdutbetalinger() = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO infotrygdutbetalinger(data) VALUES('[]')"
        requireNotNull(session.run(queryOf(statement).asUpdateAndReturnGeneratedKey))
    }

    private fun snapshot() = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO speil_snapshot(data) VALUES(?::json)"
        requireNotNull(session.run(queryOf(statement, snapshot).asUpdateAndReturnGeneratedKey))
    }

    @Language("JSON")
    private val snapshot = """
        {
          "version": "this_is_version_1",
          "aktørId": "123456789101112",
          "fødselsnummer": "12345612345",
          "arbeidsgivere": [
            {
              "organisasjonsnummer": "987654321",
              "id": "${UUID.randomUUID()}",
              "vedtaksperioder": [
                {
                  "id": "${UUID.randomUUID()}",
                  "aktivitetslogg": []
                }
              ]
            }
          ],
          "inntektsgrunnlag": {}
        }
    """

    protected class Quadruple<out A, out B, out C, out D>(val first: A, val second: B, val third: C, val fourth: D) {
        operator fun component1() = first
        operator fun component2() = second
        operator fun component3() = third
        operator fun component4() = fourth

        override fun toString() = "($first, $second, $third, $fourth)"
    }
}
