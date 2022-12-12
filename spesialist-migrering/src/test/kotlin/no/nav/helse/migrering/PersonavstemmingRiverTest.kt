package no.nav.helse.migrering

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.migrering.db.SparsomDao
import no.nav.helse.migrering.db.SpesialistDao
import no.nav.helse.migrering.domene.Varsel
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonavstemmingRiverTest : AbstractDatabaseTest() {

    private val sparsomDao: SparsomDao = mockk(relaxed = true)
    private val spesialistDao: SpesialistDao = SpesialistDao(dataSource)

    private val vedtaksperioder = mutableListOf<Vedtaksperiode>()
    private val utbetalinger = mutableListOf<Utbetaling>()

    private val Int.vedtaksperiode get() = vedtaksperioder[this - 1].id()

    private companion object {
        private val jacksonObjectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private const val LÅST: Boolean = true
        private const val ÅPEN: Boolean = false
    }

    private val testRapid = TestRapid().apply {
        Personavstemming.River(this, sparsomDao, spesialistDao)
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
        vedtaksperioder.clear()
        utbetalinger.clear()
        clearAllMocks()
    }

    @Test
    fun `leser person_avstemt`() {
        nyPeriode(1.januar) sistOppdatert 1.januar medUtbetalinger {
            listOf(nyUtbetaling(utbetalt = true, revurdering = false, dato = 1.januar))
        }
        nyPeriode(2.januar) sistOppdatert 2.januar medTilstand "AVVENTER_SIMULERING" medUtbetalinger {
            listOf(nyUtbetaling(utbetalt = false, revurdering = false, dato = 1.januar))
        }
        nyPeriode(3.januar) sistOppdatert 6.januar medTilstand "AVVENTER_SIMULERING" medUtbetalinger {
            listOf(
                nyUtbetaling(utbetalt = true, revurdering = false, dato = 3.januar),
                nyUtbetaling(utbetalt = true, revurdering = true, dato = 4.januar),
                nyUtbetaling(utbetalt = false, revurdering = true, dato = 5.januar),
            )
        }
        nyPeriode(7.januar) sistOppdatert 7.januar medTilstand "AVVENTER_HISTORIKK"
        nyPeriode(8.januar) sistOppdatert 8.januar medTilstand "AVSLUTTET_UTEN_UTBETALING"
        nyPeriode(9.januar) sistOppdatert 9.januar medTilstand "AVVENTER_GJENNOMFØRT_REVURDERING" medUtbetalinger {
            listOf(
                nyUtbetaling(utbetalt = true, revurdering = false, dato = 9.januar)
            )
        }
        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, forventetAntallUlåste = 0, forventetAntallLåste = 1)
        assertGenerasjoner(2.vedtaksperiode, forventetAntallUlåste = 1, forventetAntallLåste = 0)
        assertGenerasjoner(3.vedtaksperiode, forventetAntallUlåste = 1, forventetAntallLåste = 2)
        assertGenerasjoner(4.vedtaksperiode, forventetAntallUlåste = 1, forventetAntallLåste = 0)
        assertGenerasjoner(5.vedtaksperiode, forventetAntallUlåste = 0, forventetAntallLåste = 1)
        assertGenerasjoner(6.vedtaksperiode, forventetAntallUlåste = 1, forventetAntallLåste = 1)
    }

    @Test
    fun `lagrer varsler sammen med generasjoner`() {
        nyPeriode(1.januar) sistOppdatert 5.januar medTilstand "AVVENTER_SIMULERING" medUtbetalinger {
            listOf(
                nyUtbetaling(utbetalt = true, revurdering = false, dato = 2.januar),
                nyUtbetaling(utbetalt = true, revurdering = true, dato = 3.januar),
                nyUtbetaling(utbetalt = false, revurdering = true, dato = 4.januar),
            )
        } medSpleisVarsler {
            listOf(
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 1.januar,
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 2.januar,
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 3.januar,
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 4.januar
            )
        }

        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 1, 2)
        assertVarselPåGenerasjon(1.vedtaksperiode, 0, "RV_VV_1", "GODKJENT", 2.januar, "EN_IDENT")
        assertVarselPåGenerasjon(1.vedtaksperiode, 1, "RV_VV_1","GODKJENT", 3.januar, "EN_IDENT")
        assertVarselPåGenerasjon(1.vedtaksperiode, 2, "RV_VV_1","AKTIV", 4.januar, null, false)
    }

    @Test
    fun `opprett generasjon for periode der siste utbetaling er utbetalt og perioden ikke er i en sluttilstand`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medTilstand "AVVENTER_GJENNOMFØRT_REVURDERING" medUtbetalinger {
            listOf(nyUtbetaling(utbetalt = true, revurdering = false, dato = 2.januar))
        } medSpleisVarsler {
            listOf(
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 2.januar,
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 3.januar
            )
        }

        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 1, 1)
        assertVarselPåGenerasjon(1.vedtaksperiode, 0, "RV_VV_1", "GODKJENT", 2.januar, "EN_IDENT")
        assertVarselPåGenerasjon(1.vedtaksperiode, 1, "RV_VV_1", "AKTIV", 3.januar, null, false)
    }

    @Test
    fun `opprett generasjon for periode der siste utbetaling er utbetalt og perioden er i en sluttilstand`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medTilstand "AVSLUTTET" medUtbetalinger {
            listOf(nyUtbetaling(utbetalt = true, revurdering = false, dato = 2.januar))
        } medSpleisVarsler {
            listOf(
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 2.januar,
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 3.januar
            )
        }

        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 0, 1)
        assertVarselPåGenerasjon(1.vedtaksperiode, 0, "RV_VV_1", "GODKJENT", 2.januar, "EN_IDENT")
    }

    @Test
    fun `varsel blir avvist hvis utbetaling er avvist`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medTilstand "AVSLUTTET" medUtbetalinger {
            listOf(nyUtbetaling(utbetalt = true, revurdering = false, dato = 2.januar, godkjent = false))
        } medSpleisVarsler {
            listOf(
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 2.januar,
            )
        }

        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 0, 1)
        assertVarselPåGenerasjon(1.vedtaksperiode, 0, "RV_VV_1", "AVVIST", 2.januar, "EN_IDENT")
    }

    @Test
    fun `inaktive varsler i warning blir satt til inaktive i selve_varsel`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medTilstand "AVSLUTTET" medUtbetalinger {
            listOf(nyUtbetaling(utbetalt = true, revurdering = false, dato = 2.januar, godkjent = true))
        } medSpesialistVarsler {
            listOf(
                Triple("Registert fullmakt på personen.", 2. januar, true)
            )
        }

        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 0, 1)
        assertVarselPåGenerasjon(1.vedtaksperiode, 0, "SB_IK_1", "INAKTIV", 2.januar, "Spesialist", false)
    }

    @Test
    fun `tar med varsler fra spesialist`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medTilstand "AVSLUTTET" medUtbetalinger {
            listOf(nyUtbetaling(utbetalt = true, revurdering = false, dato = 2.januar, godkjent = true))
        } medSpleisVarsler {
            listOf(
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 2.januar,
            )
        } medSpesialistVarsler {
            listOf(
                Triple("Registert fullmakt på personen.", 2. januar, false)
            )
        }

        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 0, 1)
        assertVarselPåGenerasjon(1.vedtaksperiode, 0, "RV_VV_1", "GODKJENT", 2.januar, "EN_IDENT")
        assertVarselPåGenerasjon(1.vedtaksperiode, 0, "SB_IK_1", "GODKJENT", 2.januar, "EN_IDENT")
    }

    @Test
    fun `oppretter ikke åpen generasjon dersom det finnes en åpen generasjon fra før av`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medTilstand "AVVENTER_HISTORIKK" medEksisterendeGenerasjon ÅPEN
        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 1, 0)
    }

    @Test
    fun `oppretter ikke auu-generasjon dersom det finnes en låst generasjon fra før av`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medTilstand "AVSLUTTET_UTEN_UTBETALING" medEksisterendeGenerasjon LÅST
        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 0, 1)
    }

    @Test
    fun `lager ikke generasjoner av utbetalinger som ikke er siste utbetaling og har status IKKE_UTBETALT - Siste er ikke utbetalt`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medUtbetalinger {
            listOf(
                nyUtbetaling(utbetalt = false, revurdering = false, dato = 1.januar),
                nyUtbetaling(utbetalt = false, revurdering = false, dato = 2.januar),
            )
        }
        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 1, 0)
    }

    @Test
    fun `lager ikke generasjoner av utbetalinger som ikke er siste utbetaling og har status IKKE_UTBETALT - Siste er utbetalt`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medUtbetalinger {
            listOf(
                nyUtbetaling(utbetalt = false, revurdering = false, dato = 1.januar),
                nyUtbetaling(utbetalt = true, revurdering = false, dato = 2.januar),
            )
        }
        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 0, 1)
    }

    @Test
    fun `lager generasjoner av utbetalinger som kun har én utbetaling med status IKKE_UTBETALT`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medUtbetalinger {
            listOf(
                nyUtbetaling(utbetalt = false, revurdering = false, dato = 1.januar),
            )
        }
        testRapid.sendTestMessage(testevent())
        assertGenerasjoner(1.vedtaksperiode, 1, 0)
    }

    @Test
    fun `Lagrer ikke varsel _med_ referanse til definisjon dersom varselet er aktivt`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medUtbetalinger {
            listOf(
                nyUtbetaling(utbetalt = false, revurdering = false, dato = 1.januar),
            )
        } medSpleisVarsler {
            listOf(
                "Arbeidsgiver er ikke registrert i Aa-registeret." to 1.januar,
            )
        }
        testRapid.sendTestMessage(testevent())
        assertVarselPåGenerasjon(1.vedtaksperiode, 0, "RV_VV_1", "AKTIV", 1.januar, null, harDefinisjon = false)
    }

    @Test
    fun `Lagrer ikke varsel _med_ referanse til definisjon dersom varselet er inaktivt`() {
        nyPeriode(1.januar) sistOppdatert 3.januar medUtbetalinger {
            listOf(
                nyUtbetaling(utbetalt = false, revurdering = false, dato = 1.januar),
            )
        } medSpesialistVarsler {
            listOf(
                Triple("Registert fullmakt på personen.", 1.januar, true),
            )
        }
        testRapid.sendTestMessage(testevent())
        assertVarselPåGenerasjon(1.vedtaksperiode, 0, "SB_IK_1", "INAKTIV", 1.januar, "Spesialist", harDefinisjon = false)
    }

    private infix fun Vedtaksperiode.medEksisterendeGenerasjon(låst: Boolean): Vedtaksperiode {
        @Language("PostgreSQL")
        val query =
            """
                INSERT INTO selve_vedtaksperiode_generasjon(vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse, låst_tidspunkt, låst_av_hendelse, låst) 
                VALUES (?, ?, ?, ?, ?, ?, ?) 
            """
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    query,
                    id(),
                    null,
                    opprettet(),
                    UUID.randomUUID(),
                    if (låst) oppdatert() else null,
                    if (låst) UUID.randomUUID() else null,
                    låst
                ).asUpdate)
        }
        return this
    }

    private class Vedtaksperiode(
        private val id: UUID,
        private val opprettet: LocalDateTime,
    ) {
        private lateinit var oppdatert: LocalDateTime
        private val utbetalinger = mutableListOf<UUID>()
        private var tilstand: String = "AVSLUTTET"

        private val varsler: MutableList<Testvarsel> = mutableListOf()

        fun id() = id

        fun opprettet() = opprettet
        fun oppdatert() = oppdatert

        fun oppdatert(oppdatert: LocalDateTime) {
            this.oppdatert = oppdatert
        }

        fun tilstand(tilstand: String) {
            this.tilstand = tilstand
        }

        fun utbetalinger(utbetalingIder: List<UUID>) {
            utbetalinger.addAll(utbetalingIder)
        }

        fun varsler(varsler: List<Pair<String, LocalDate>>) {
            varsler.map { (tekst, dato) ->
                Testvarsel(id, tekst, dato)
            }.also {
                this.varsler.addAll(it)
            }
        }

        fun varsler(): List<Varsel> = varsler.map { it.varsel() }

        fun toJson(): String {
            return mapOf(
                "id" to id,
                "tilstand" to tilstand,
                "opprettet" to opprettet,
                "oppdatert" to oppdatert,
                "utbetalinger" to utbetalinger.toList()
            ).let {
                jacksonObjectMapper.writeValueAsString(it)
            }
        }
    }

    private class Testvarsel(
        private val vedtaksperiodeId: UUID,
        private val tekst: String,
        dato: LocalDate,
    ) {
        private val tidspunkt = dato.atTime(12, 0, 0)

        fun varsel(): Varsel {
            return Varsel(vedtaksperiodeId, tekst, tidspunkt, UUID.randomUUID(), null)
        }
    }

    private class Utbetaling(utbetalt: Boolean, private val revurdering: Boolean, dato: LocalDate, godkjent: Boolean) {
        private val id: UUID = UUID.randomUUID()
        private val opprettet: LocalDateTime = dato.atStartOfDay()
        private val oppdatert: LocalDateTime = dato.atTime(23, 59, 59)
        private val tilstand = if (utbetalt) "UTBETALT" else "IKKE_UTBETALT"
        private val vurdering = if (utbetalt) mapOf(
            "ident" to "EN_IDENT",
            "tidspunkt" to oppdatert,
            "automatiskBehandling" to false,
            "godkjent" to godkjent,
        ) else null
        fun id() = id

        fun toJson(): String {
            return mutableMapOf<String, Any>(
                "id" to id,
                "type" to if (revurdering) "REVURDERING" else "UTBETALING",
                "status" to tilstand,
                "opprettet" to opprettet,
                "oppdatert" to oppdatert
            ).let {
                it.compute("vurdering") { _, _ -> vurdering}
                jacksonObjectMapper.writeValueAsString(it)
            }
        }
    }

    private fun finnPerson(): Boolean {
        @Language("PostgreSQL")
        val query = """
            SELECT true FROM person WHERE fodselsnummer = ?;
        """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, 12029240045).map {
                it.boolean(1)
            }.asSingle)
        } ?: false

    }

    private fun opprettPerson() {
        if (finnPerson()) return
        @Language("PostgreSQL")
        val query = """
            INSERT INTO person (fodselsnummer, aktor_id, info_ref, enhet_ref, enhet_ref_oppdatert, personinfo_oppdatert, infotrygdutbetalinger_ref, infotrygdutbetalinger_oppdatert) 
            VALUES (12029240045, 42, null, null, null, null, null, null);
        """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query).asUpdate)
        }
    }

    private fun opprettArbeidsgiver() {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO arbeidsgiver (orgnummer, navn_ref, bransjer_ref) 
            VALUES (987654321, null, null) ON CONFLICT (orgnummer) DO NOTHING;
        """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query).asUpdate)
        }
    }

    private fun opprettVedtak(vedtaksperiodeId: UUID) {
        val fom = 1. januar
        val tom = 31. januar
        @Language("PostgreSQL")
        val query = """
            INSERT INTO vedtak (vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, snapshot_ref) 
            VALUES (?, ?, ?, (SELECT id FROM arbeidsgiver WHERE orgnummer = 987654321), (SELECT id FROM person WHERE fodselsnummer = 12029240045), null);
        """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, fom, tom).asUpdate)
        }
    }

    private fun nyPeriode(opprettet: LocalDate): Vedtaksperiode {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtak(vedtaksperiodeId)
        return Vedtaksperiode(vedtaksperiodeId, opprettet.atStartOfDay()).also { vedtaksperioder.add(it) }
    }

    private fun nyUtbetaling(utbetalt: Boolean, revurdering: Boolean, dato: LocalDate, godkjent: Boolean = true): Utbetaling {
        return Utbetaling(utbetalt, revurdering, dato, godkjent)
    }

    private infix fun Vedtaksperiode.sistOppdatert(oppdatert: LocalDate): Vedtaksperiode {
        oppdatert(oppdatert.atStartOfDay())
        return this
    }

    private infix fun Vedtaksperiode.medTilstand(tilstand: String): Vedtaksperiode {
        tilstand(tilstand)
        return this
    }

    private infix fun Vedtaksperiode.medUtbetalinger(utbetalinger: () -> List<Utbetaling>): Vedtaksperiode {
        utbetalinger().also {
            this@PersonavstemmingRiverTest.utbetalinger.addAll(it)
            this.utbetalinger(it.map(Utbetaling::id))
        }
        return this
    }

    private infix fun Vedtaksperiode.medSpleisVarsler(varsler: () -> List<Pair<String, LocalDate>>): Vedtaksperiode {
        this.varsler(varsler())
        return this
    }

    private infix fun Vedtaksperiode.medSpesialistVarsler(varsler: () -> List<Triple<String, LocalDate, Boolean>>): Vedtaksperiode {
        val spesialistVarsler = varsler()
        spesialistVarsler.forEach {(melding, opprettet, inaktiv) ->
            @Language("PostgreSQL")
            val query = """
                INSERT INTO warning (melding, vedtak_ref, kilde, opprettet, inaktiv_fra) 
                VALUES (?, (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?), 'Spesialist', ?, ?)    
            """

            sessionOf(dataSource).use { session ->
                session.run(queryOf(
                    query,
                    melding,
                    id(),
                    opprettet.atTime(12, 0, 0),
                    if (inaktiv) opprettet.atTime(12, 0, 0) else null
                ).asUpdate)
            }
        }
        return this
    }

    private fun assertVarselPåGenerasjon(
        vedtaksperiodeId: UUID,
        generasjonIndex: Int,
        varselkode: String,
        status: String,
        opprettet: LocalDate,
        ident: String?,
        harDefinisjon: Boolean = true
    ) {
        @Language("PostgreSQL")
        val query1 =
            "SELECT id, opprettet_tidspunkt FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? ORDER BY opprettet_tidspunkt;"

        val generasjonId = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query1,
                    vedtaksperiodeId
                ).map {
                    it.long("id")
                }.asList
            )[generasjonIndex]
        }

        @Language("PostgreSQL")
        val query2 = "SELECT kode, opprettet, status_endret_ident, status, definisjon_ref FROM selve_varsel WHERE generasjon_ref = ? AND kode = ?;"

        val varselkoder = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query2,
                    generasjonId,
                    varselkode,
                ).map {
                    mapOf(
                        "varselkode" to it.string("kode"),
                        "opprettet" to it.localDateTime("opprettet"),
                        "statusEndretIdent" to it.stringOrNull("status_endret_ident"),
                        "status" to it.string("status"),
                        "harDefinisjon" to (it.longOrNull("definisjon_ref") != null)
                    )
                }.asList
            )
        }

        assertEquals(1, varselkoder.size)
        assertEquals(
            mapOf(
                "varselkode" to varselkode,
                "opprettet" to opprettet.atTime(12,0,0),
                "statusEndretIdent" to ident,
                "status" to status,
                "harDefinisjon" to harDefinisjon
            ),
            varselkoder.firstOrNull()
        )
    }

    private fun assertGenerasjoner(vedtaksperiodeId: UUID, forventetAntallUlåste: Int, forventetAntallLåste: Int) {
        val antallUlåste = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND låst = false"
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        val antallLåste = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND låst = true"
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntallUlåste, antallUlåste) { "forventet $forventetAntallUlåste ulåste generasjoner. Fant $antallUlåste" }
        assertEquals(forventetAntallLåste, antallLåste) { "forventet $forventetAntallLåste låste generasjoner. Fant $antallLåste" }
    }

    @Language("JSON")
    private fun testevent(): String {
        every { sparsomDao.finnVarslerFor(any()) } returns vedtaksperioder.flatMap { it.varsler() }
        return """
            {
                  "@event_name": "person_avstemt",
                  "arbeidsgivere": [
                    {
                      "organisasjonsnummer": "987654321",
                      "vedtaksperioder": [
                        ${vedtaksperioder.joinToString { it.toJson() }}
                      ],
                      "utbetalinger": [
                        ${utbetalinger.joinToString { it.toJson() }}
                      ]
                    }
                  ],
                  "@id": "c405a203-264e-4496-99dc-785e76ede254",
                  "@opprettet": "2022-11-23T12:52:42.017867",
                  "fødselsnummer": "12029240045",
                  "aktørId": "42"
            }
            
        """.trimIndent()
    }
}