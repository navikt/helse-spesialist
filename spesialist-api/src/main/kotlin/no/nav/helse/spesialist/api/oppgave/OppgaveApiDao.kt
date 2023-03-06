package no.nav.helse.spesialist.api.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.schema.Boenhet
import no.nav.helse.spesialist.api.graphql.schema.DateString
import no.nav.helse.spesialist.api.graphql.schema.InntektFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.Kjonn
import no.nav.helse.spesialist.api.graphql.schema.OppgaveForOversiktsvisning
import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import no.nav.helse.spesialist.api.graphql.schema.UUIDString
import no.nav.helse.spesialist.api.graphql.schema.tilAdressebeskyttelse
import no.nav.helse.spesialist.api.graphql.schema.tilKjonn
import no.nav.helse.spesialist.api.graphql.schema.tilOppgavetype
import no.nav.helse.spesialist.api.graphql.schema.tilPeriodetype
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import org.intellij.lang.annotations.Language

class OppgaveApiDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun finnOppgaveId(vedtaksperiodeId: UUID) = queryize(
        """ SELECT id FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            AND status = 'AvventerSaksbehandler'::oppgavestatus
        """
    ).single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.long("id") }

    fun finnOppgaveId(fødselsnummer: String) = queryize(
        """
            SELECT o.id as oppgaveId FROM oppgave o
            JOIN vedtak v ON v.id = o.vedtak_ref
            JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer AND status = 'AvventerSaksbehandler'::oppgavestatus;
        """
    ).single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { it.long("oppgaveId") }

    fun erBeslutteroppgave(vedtaksperiodeId: UUID): Boolean = queryize(
        """
            SELECT er_beslutteroppgave FROM oppgave
                    WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
                    AND status = 'AvventerSaksbehandler'::oppgavestatus
            """
    ).single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.boolean("er_beslutteroppgave") } ?: false

    fun erReturOppgave(vedtaksperiodeId: UUID): Boolean = queryize(
        """ SELECT er_returoppgave FROM oppgave
                WHERE vedtak_ref =
                    (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
                AND status = 'AvventerSaksbehandler'::oppgavestatus
            """
    ).single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.boolean("er_returoppgave") } ?: false

    fun hentBeslutterSaksbehandlerOid(vedtaksperiodeId: UUID): UUID? = queryize(
        """ SELECT beslutter_saksbehandler_oid FROM oppgave
                WHERE vedtak_ref =
                    (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
                 AND status = 'AvventerSaksbehandler'::oppgavestatus   
            """
    ).single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.uuidOrNull("beslutter_saksbehandler_oid") }

    fun hentTidligereSaksbehandlerOid(vedtaksperiodeId: UUID): UUID? = queryize(
        """ SELECT tidligere_saksbehandler_oid FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
             AND status = 'AvventerSaksbehandler'::oppgavestatus   
        """
    ).single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.uuidOrNull("tidligere_saksbehandler_oid") }

    fun trengerTotrinnsvurdering(vedtaksperiodeId: UUID): Boolean = queryize(
        """ SELECT er_totrinnsoppgave FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            AND status = 'AvventerSaksbehandler'::oppgavestatus   
        """
    ).single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.boolean("er_totrinnsoppgave") } ?: false

    fun finnPeriodeoppgave(vedtaksperiodeId: UUID): OppgaveForPeriodevisningDto? {
        @Language("PostgreSQL")
        val query = """
            SELECT id, er_beslutteroppgave, er_returoppgave, er_totrinnsoppgave, tidligere_saksbehandler_oid
            FROM oppgave
            WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
                AND status = 'AvventerSaksbehandler'::oppgavestatus
        """.trimIndent()
        return query.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) {
            OppgaveForPeriodevisningDto(
                id = it.string("id"),
                erBeslutter = it.boolean("er_beslutteroppgave"),
                erRetur = it.boolean("er_returoppgave"),
                trengerTotrinnsvurdering = it.boolean("er_totrinnsoppgave"),
                tidligereSaksbehandler = it.stringOrNull("tidligere_saksbehandler_oid"),
            )
        }
    }

    fun finnPeriodensInntekterFraAordningen(vedtaksperiodeId: UUIDString, skjæringstidspunkt: DateString, orgnummer: String): List<InntektFraAOrdningen> =
        queryize(
            """
                SELECT inntekter FROM inntekt
                WHERE person_ref=(SELECT person_ref FROM vedtak v WHERE v.vedtaksperiode_id = :vedtaksperiodeId)
                AND skjaeringstidspunkt = :skjaeringstidspunkt
            """.trimIndent()
        ).single(
            mapOf(
                "vedtaksperiodeId" to UUID.fromString(vedtaksperiodeId),
                "skjaeringstidspunkt" to LocalDate.parse(skjæringstidspunkt)
            )
        ){ row ->
            objectMapper.readValue<List<Inntekter>>(row.string("inntekter"))
                .mapNotNull { inntekter ->
                    inntekter.inntektsliste
                        .filter { it.orgnummer == orgnummer }
                        .takeUnless { it.isEmpty() }
                        ?.let { inntekter.copy(inntektsliste = it) }
                }.map { inntekter -> InntektFraAOrdningen(maned = inntekter.årMåned.toString(), sum = inntekter.inntektsliste.sumOf { it.beløp }.toDouble()) }
        } ?: emptyList()

    fun finnOppgaver(tilganger: SaksbehandlerTilganger): List<OppgaveForOversiktsvisning> =
        sessionOf(dataSource).use { session ->
            // bruk av const direkte i @Language-annotert sql fører til snodige fantom-compile-feil i IntelliJ
            val beslutterOppgaveHackyWorkaround = BESLUTTEROPPGAVE_PREFIX

            @Language("PostgreSQL")
            val query = """
            WITH aktiv_oppgave AS (select o.* from oppgave o where o.status = 'AvventerSaksbehandler'),
                 aktiv_tildeling AS (select t.* from tildeling t where t.oppgave_id_ref in (select o.id from aktiv_oppgave o))

            SELECT o.id as oppgave_id, o.type AS oppgavetype, o.opprettet, os.soknad_mottatt AS opprinneligSoknadsdato, o.er_beslutteroppgave, o.er_returoppgave, o.er_totrinnsoppgave, o.tidligere_saksbehandler_oid, o.sist_sendt,
                s.epost, s.navn as saksbehandler_navn, s.oid, v.vedtaksperiode_id, v.fom, v.tom, pi.fornavn, pi.mellomnavn, pi.etternavn, pi.fodselsdato,
                pi.kjonn, pi.adressebeskyttelse, p.aktor_id, p.fodselsnummer, sot.type as saksbehandleroppgavetype, sot.inntektskilde, e.id AS enhet_id, e.navn AS enhet_navn, t.på_vent,
                (SELECT COUNT(DISTINCT melding) from warning w where w.melding not like '$beslutterOppgaveHackyWorkaround%' and w.vedtak_ref = o.vedtak_ref and (w.inaktiv_fra is null or w.inaktiv_fra > now())) AS antall_varsler
            FROM aktiv_oppgave o
                INNER JOIN vedtak v ON o.vedtak_ref = v.id
                INNER JOIN person p ON v.person_ref = p.id
                INNER JOIN person_info pi ON p.info_ref = pi.id
                INNER JOIN opprinnelig_soknadsdato os ON os.vedtaksperiode_id = v.vedtaksperiode_id
                LEFT JOIN enhet e ON p.enhet_ref = e.id
                LEFT JOIN saksbehandleroppgavetype sot ON v.id = sot.vedtak_ref
                LEFT JOIN aktiv_tildeling t ON o.id = t.oppgave_id_ref
                LEFT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE status = 'AvventerSaksbehandler'::oppgavestatus
                AND CASE WHEN :harTilgangTilRisk 
                    THEN true
                    ELSE o.type != 'RISK_QA' END
                AND CASE WHEN :harTilgangTilKode7 
                    THEN pi.adressebeskyttelse IN ('Ugradert', 'Fortrolig') 
                    ELSE pi.adressebeskyttelse = 'Ugradert' END
                AND CASE WHEN :harTilgangTilBeslutter
                    THEN true
                    ELSE o.er_beslutteroppgave = false END
            ORDER BY
                CASE WHEN t.saksbehandler_ref IS NOT NULL THEN 0 ELSE 1 END,
                CASE WHEN o.type = 'STIKKPRØVE' THEN 0 ELSE 1 END,
                CASE WHEN o.type = 'RISK_QA' THEN 0 ELSE 1 END,
                opprettet
                ;
            """
            val parameters = mapOf(
                "harTilgangTilRisk" to tilganger.harTilgangTilRiskOppgaver(),
                "harTilgangTilKode7" to tilganger.harTilgangTilKode7(),
                "harTilgangTilBeslutter" to tilganger.harTilgangTilBeslutterOppgaver(),
            )
            session.run(
                queryOf(query, parameters)
                    .map(::tilOppgaveForOversiktsvisning)
                    .asList
            )
        }


    fun hentBehandledeOppgaver(
        behandletAvIdent: String,
        behandletAvOid: UUID,
        fom: LocalDate?
    ): List<FerdigstiltOppgaveDto> {
        val erFerdigstiltAvSaksbehandler =
            "((o.status = 'Ferdigstilt' OR o.status = 'AvventerSystem') AND s.ident = :ident)"
        val erTidligereBehandletAvSaksbehandler =
            "((o.er_beslutteroppgave = true OR o.er_returoppgave = true) AND o.tidligere_saksbehandler_oid = :oid)"

        return queryize(
            """
            SELECT o.id                                                     as oppgave_id,
                   o.type                                                   as oppgavetype,
                   o.status,
                   o.er_beslutteroppgave,
                   s2.navn                                                  as ferdigstilt_av,
                   o.oppdatert                                              as ferdigstilt_tidspunkt,
                   pi.fornavn                                               as soker_fornavn,
                   pi.mellomnavn                                            as soker_mellomnavn,
                   pi.etternavn                                             as soker_etternavn,
                   p.aktor_id                                               as soker_aktor_id,
                   sot.type                                                 as periodetype,
                   sot.inntektskilde                                        as inntektstype,
                   e.navn                                                   as bosted,
                   (SELECT count(distinct melding)
                    FROM warning w
                    WHERE w.vedtak_ref = o.vedtak_ref
                      AND (w.inaktiv_fra is null OR w.inaktiv_fra > now())) as antall_varsler
            FROM oppgave o
                     INNER JOIN vedtak v ON o.vedtak_ref = v.id
                     INNER JOIN person p ON v.person_ref = p.id
                     INNER JOIN person_info pi ON p.info_ref = pi.id
                     LEFT JOIN enhet e ON p.enhet_ref = e.id
                     LEFT JOIN saksbehandleroppgavetype sot ON v.id = sot.vedtak_ref
                     LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref
                     LEFT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
                     LEFT JOIN saksbehandler s2 on o.ferdigstilt_av = s2.ident
            WHERE ($erFerdigstiltAvSaksbehandler OR $erTidligereBehandletAvSaksbehandler)
              AND o.oppdatert >= :fom
            ORDER BY o.oppdatert;
        """.trimIndent()
        ).list(mapOf("ident" to behandletAvIdent, "oid" to behandletAvOid, "fom" to (fom ?: LocalDate.now()))) {
            FerdigstiltOppgaveDto(
                id = it.string("oppgave_id"),
                type = Oppgavetype.valueOf(it.string("oppgavetype")),
                ferdigstiltTidspunkt = it.localDateTime("ferdigstilt_tidspunkt"),
                ferdigstiltAv = it.stringOrNull("ferdigstilt_av"),
                personinfo = Personnavn(
                    fornavn = it.string("soker_fornavn"),
                    mellomnavn = it.stringOrNull("soker_mellomnavn"),
                    etternavn = it.string("soker_etternavn"),
                ),
                aktørId = it.string("soker_aktor_id"),
                antallVarsler = it.int("antall_varsler"),
                periodetype = Periodetype.valueOf(it.string("periodetype")),
                inntektskilde = Inntektskilde.valueOf(it.string("inntektstype")),
                bosted = it.string("bosted"),
            )
        }
    }

    companion object {
        private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()

        internal fun tilOppgaveForOversiktsvisning(it: Row) = OppgaveForOversiktsvisning(
            id = it.string("oppgave_id"),
            type = Oppgavetype.valueOf(it.string("oppgavetype")).tilOppgavetype(),
            opprettet = it.string("opprettet"),
            opprinneligSoknadsdato = it.string("opprinneligSoknadsdato"),
            vedtaksperiodeId = it.string("vedtaksperiode_id"),
            personinfo = Personinfo(
                fornavn = it.string("fornavn"),
                mellomnavn = it.stringOrNull("mellomnavn"),
                etternavn = it.string("etternavn"),
                fodselsdato = it.string("fodselsdato"),
                kjonn = it.stringOrNull("kjonn")?.let { Kjønn.valueOf(it).tilKjonn() } ?: Kjonn.Ukjent,
                adressebeskyttelse = it.string("adressebeskyttelse").let(Adressebeskyttelse::valueOf)
                    .tilAdressebeskyttelse(),
            ),
            aktorId = it.long("aktor_id").toString(),
            fodselsnummer = it.long("fodselsnummer").toFødselsnummer(),
            antallVarsler = it.int("antall_varsler"),
            flereArbeidsgivere = it.stringOrNull("inntektskilde") == Inntektskilde.FLERE_ARBEIDSGIVERE.name,
            boenhet = Boenhet(id = it.string("enhet_id"), navn = it.string("enhet_navn")),
            erBeslutter = it.boolean("er_beslutteroppgave"),
            erRetur = it.boolean("er_returoppgave"),
            trengerTotrinnsvurdering = it.boolean("er_totrinnsoppgave"),
            tildeling = it.stringOrNull("epost")?.let { epost ->
                Tildeling(
                    navn = it.string("saksbehandler_navn"),
                    epost = epost,
                    oid = it.string("oid"),
                    reservert = it.boolean("på_vent")
                )
            },
            periodetype = it.stringOrNull("saksbehandleroppgavetype")?.let(Periodetype::valueOf)?.tilPeriodetype(),
            tidligereSaksbehandler = it.stringOrNull("tidligere_saksbehandler_oid"),
            sistSendt = it.stringOrNull("sist_sendt"),
        )
    }
}

private data class Inntekter(val årMåned: YearMonth, val inntektsliste: List<Inntekt>) {
    data class Inntekt(val beløp: Int, val orgnummer: String)
}

const val BESLUTTEROPPGAVE_PREFIX = "Beslutteroppgave:"
