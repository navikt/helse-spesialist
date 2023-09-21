package no.nav.helse.spesialist.api.oppgave

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.schema.Boenhet
import no.nav.helse.spesialist.api.graphql.schema.Kjonn
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.Mottaker.BEGGE
import no.nav.helse.spesialist.api.graphql.schema.Mottaker.SYKMELDT
import no.nav.helse.spesialist.api.graphql.schema.OppgaveForOversiktsvisning
import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import no.nav.helse.spesialist.api.graphql.schema.Totrinnsvurdering
import no.nav.helse.spesialist.api.graphql.schema.tilAdressebeskyttelse
import no.nav.helse.spesialist.api.graphql.schema.tilKjonn
import no.nav.helse.spesialist.api.graphql.schema.tilOppgavetype
import no.nav.helse.spesialist.api.graphql.schema.tilPeriodetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype

class OppgaveApiDao(dataSource: DataSource) : HelseDao(dataSource) {

    fun lagreBehandlingsreferanse(oppgaveId: Long, behandlingId: UUID) {
        asSQL(
            "INSERT INTO oppgave_behandling_kobling(oppgave_id, behandling_id) VALUES (:oppgaveId, :behandlingId)",
            mapOf("oppgaveId" to oppgaveId, "behandlingId" to behandlingId)
        ).update()
    }

    fun finnOppgaveId(vedtaksperiodeId: UUID) = asSQL(
        """ SELECT id FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            AND status = 'AvventerSaksbehandler'::oppgavestatus
        """,
        mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single { it.long("id") }

    fun finnOppgaveId(fødselsnummer: String) = asSQL(
        """ SELECT o.id as oppgaveId FROM oppgave o
            JOIN vedtak v ON v.id = o.vedtak_ref
            JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer AND status = 'AvventerSaksbehandler'::oppgavestatus;
        """,
        mapOf("fodselsnummer" to fødselsnummer.toLong())
    ).single { it.long("oppgaveId") }

    fun finnPeriodeoppgave(vedtaksperiodeId: UUID) = asSQL(
        """ SELECT o.id
            FROM oppgave o
            INNER JOIN vedtak v ON o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId 
                AND status = 'AvventerSaksbehandler'::oppgavestatus 
        """,
        mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single { OppgaveForPeriodevisningDto(id = it.string("id")) }

    fun finnOppgavetype(vedtaksperiodeId: UUID) = asSQL(
        """ SELECT type
            FROM oppgave
            WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            ORDER BY id LIMIT 1
        """,
        mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single { Oppgavetype.valueOf(it.string("type")) }

    fun finnOppgaver(tilganger: SaksbehandlerTilganger) = asSQL(
        """
            WITH aktiv_oppgave AS NOT MATERIALIZED (select o.* from oppgave o where o.status = 'AvventerSaksbehandler'),
                 aktiv_tildeling AS NOT MATERIALIZED (select t.* from tildeling t where t.oppgave_id_ref in (select o.id from aktiv_oppgave o)),
                 har_varsel_om_negativt_belop AS NOT MATERIALIZED (SELECT sv.vedtaksperiode_id FROM selve_varsel sv
                    INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                    INNER JOIN vedtak v ON v.vedtaksperiode_id = sv.vedtaksperiode_id
                    INNER JOIN aktiv_oppgave o ON o.vedtak_ref = v.id
                    WHERE sv.vedtaksperiode_id = v.vedtaksperiode_id
                    AND sv.status != 'INAKTIV'
                    AND sv.kode = 'RV_UT_23'
                    AND svg.utbetaling_id = o.utbetaling_id
                ),
                har_vergemal AS NOT MATERIALIZED (SELECT har_vergemal, vm.person_ref FROM vergemal vm
                    INNER JOIN vedtak v ON v.person_ref = vm.person_ref
                    INNER JOIN aktiv_oppgave o ON o.vedtak_ref = v.id
                )

            SELECT o.id as oppgave_id, o.type AS oppgavetype, o.opprettet, o.mottaker, os.soknad_mottatt AS opprinneligSoknadsdato, o.sist_sendt,
                s.epost, s.navn as saksbehandler_navn, s.oid, v.vedtaksperiode_id, v.fom, v.tom, pi.fornavn, pi.mellomnavn, pi.etternavn, pi.fodselsdato,
                pi.kjonn, pi.adressebeskyttelse, p.aktor_id, p.fodselsnummer, sot.type as saksbehandleroppgavetype, sot.inntektskilde, e.id AS enhet_id, e.navn AS enhet_navn, t.på_vent,
                ttv.vedtaksperiode_id AS totrinnsvurdering_vedtaksperiode_id, ttv.saksbehandler, ttv.beslutter, ttv.er_retur,
                h.vedtaksperiode_id IS NOT NULL AS har_varsel_om_negativt_belop, hv.har_vergemal, p.enhet_ref,
                sps.vedtaksperiode_id is not null as er_spesialsak
            FROM aktiv_oppgave o
                INNER JOIN vedtak v ON o.vedtak_ref = v.id
                INNER JOIN person p ON v.person_ref = p.id
                INNER JOIN person_info pi ON p.info_ref = pi.id
                INNER JOIN opprinnelig_soknadsdato os ON os.vedtaksperiode_id = v.vedtaksperiode_id
                INNER JOIN egen_ansatt ea on p.id = ea.person_ref
                LEFT JOIN utbetaling_id ui ON ui.utbetaling_id = o.utbetaling_id  
                LEFT JOIN enhet e ON p.enhet_ref = e.id
                LEFT JOIN saksbehandleroppgavetype sot ON v.id = sot.vedtak_ref
                LEFT JOIN aktiv_tildeling t ON o.id = t.oppgave_id_ref
                LEFT JOIN saksbehandler s ON t.saksbehandler_ref = s.oid
                LEFT JOIN totrinnsvurdering ttv ON (ttv.vedtaksperiode_id = v.vedtaksperiode_id AND ttv.utbetaling_id_ref IS NULL)
                LEFT JOIN har_varsel_om_negativt_belop h ON h.vedtaksperiode_id = v.vedtaksperiode_id
                LEFT JOIN har_vergemal hv ON hv.person_ref = p.id
                LEFT JOIN spesialsak sps on sps.vedtaksperiode_id = v.vedtaksperiode_id
            WHERE status = 'AvventerSaksbehandler'::oppgavestatus
                AND NOT ea.er_egen_ansatt
                AND CASE WHEN :harTilgangTilRisk 
                    THEN true
                    ELSE o.type != 'RISK_QA' END
                AND CASE WHEN :harTilgangTilKode7 
                    THEN pi.adressebeskyttelse IN ('Ugradert', 'Fortrolig') 
                    ELSE pi.adressebeskyttelse = 'Ugradert' END
                AND CASE WHEN :harTilgangTilBeslutter
                    THEN true
                    ELSE (ttv.er_retur = true OR ttv.saksbehandler IS NULL) END
                AND CASE WHEN :harTilgangTilStikkprove
                    THEN true
                    ELSE o.type != 'STIKKPRØVE' END
                AND CASE WHEN :harTilgangTilSpesialsaker
                    THEN true
                    ELSE sps.vedtaksperiode_id is null END
                    ;
        """,
        mapOf(
            "harTilgangTilRisk" to tilganger.harTilgangTilRiskOppgaver(),
            "harTilgangTilKode7" to tilganger.harTilgangTilKode7(),
            "harTilgangTilBeslutter" to tilganger.harTilgangTilBeslutterOppgaver(),
            "harTilgangTilStikkprove" to tilganger.hartilgangTilStikkprøve(),
            "harTilgangTilSpesialsaker" to tilganger.hartilgangTilSpesialsaker(),
        )
    ).list(::tilOppgaveForOversiktsvisning)

    fun hentBehandledeOppgaver(
        behandletAvOid: UUID,
        fom: LocalDate?,
    ): List<FerdigstiltOppgaveDto> {
        val erFerdigstiltAvSaksbehandler =
            "((o.status = 'Ferdigstilt' OR o.status = 'AvventerSystem') AND s.oid = :oid)"

        return asSQL(
            """
            SELECT o.id                                                     as oppgave_id,
                   o.type                                                   as oppgavetype,
                   o.status,
                   s2.navn                                                  as ferdigstilt_av,
                   o.oppdatert                                              as ferdigstilt_tidspunkt,
                   pi.fornavn                                               as soker_fornavn,
                   pi.mellomnavn                                            as soker_mellomnavn,
                   pi.etternavn                                             as soker_etternavn,
                   p.aktor_id                                               as soker_aktor_id,
                   sot.type                                                 as periodetype,
                   sot.inntektskilde                                        as inntektstype,
                   e.navn                                                   as bosted
            FROM oppgave o
                     INNER JOIN vedtak v ON o.vedtak_ref = v.id
                     INNER JOIN person p ON v.person_ref = p.id
                     INNER JOIN person_info pi ON p.info_ref = pi.id
                     LEFT JOIN enhet e ON p.enhet_ref = e.id
                     LEFT JOIN saksbehandleroppgavetype sot ON v.id = sot.vedtak_ref
                     LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref
                     LEFT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
                     LEFT JOIN saksbehandler s2 on o.ferdigstilt_av = s2.ident
                     LEFT JOIN (SELECT DISTINCT ON (vedtaksperiode_id) vedtaksperiode_id, saksbehandler
                         FROM totrinnsvurdering
                         WHERE utbetaling_id_ref IS NOT NULL
                         ORDER BY vedtaksperiode_id, id DESC
                     ) ttv ON ttv.vedtaksperiode_id = v.vedtaksperiode_id
            WHERE ($erFerdigstiltAvSaksbehandler OR ttv.saksbehandler = :oid)
              AND o.oppdatert >= :fom
            ORDER BY o.oppdatert;
        """,
            mapOf("oid" to behandletAvOid, "fom" to (fom ?: LocalDate.now()))
        ).list {
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
                periodetype = Periodetype.valueOf(it.string("periodetype")),
                inntektskilde = Inntektskilde.valueOf(it.string("inntektstype")),
                bosted = it.string("bosted"),
            )
        }
    }

    fun finnFødselsnummer(oppgaveId: Long) = requireNotNull(asSQL(
        """ SELECT fodselsnummer from person
            INNER JOIN vedtak v on person.id = v.person_ref
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
        """,
        mapOf("oppgaveId" to oppgaveId)
    ).single { it.long("fodselsnummer").toFødselsnummer() })

    fun invaliderOppgaveFor(fødselsnummer: String) = asSQL(
        """
        UPDATE oppgave o
        SET status = 'Invalidert'
        FROM oppgave o2
        JOIN vedtak v on v.id = o2.vedtak_ref
        JOIN person p on v.person_ref = p.id
        WHERE p.fodselsnummer = :fodselsnummer
        and o.id = o2.id
        AND o.status = 'AvventerSaksbehandler'::oppgavestatus; 
    """, mapOf("fodselsnummer" to fødselsnummer.toLong())
    ).update()

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
            flereArbeidsgivere = it.stringOrNull("inntektskilde") == Inntektskilde.FLERE_ARBEIDSGIVERE.name,
            boenhet = Boenhet(id = it.string("enhet_id"), navn = it.string("enhet_navn")),
            tildeling = it.stringOrNull("epost")?.let { epost ->
                Tildeling(
                    navn = it.string("saksbehandler_navn"),
                    epost = epost,
                    oid = it.string("oid"),
                    paaVent = it.boolean("på_vent"),
                )
            },
            periodetype = it.stringOrNull("saksbehandleroppgavetype")?.let(Periodetype::valueOf)?.tilPeriodetype(),
            sistSendt = it.stringOrNull("sist_sendt"),
            totrinnsvurdering = it.stringOrNull("totrinnsvurdering_vedtaksperiode_id")?.let { _ ->
                val erRetur = it.boolean("er_retur")
                val saksbehandler = it.stringOrNull("saksbehandler")
                Totrinnsvurdering(
                    erRetur = erRetur,
                    saksbehandler = saksbehandler,
                    beslutter = it.stringOrNull("beslutter"),
                    erBeslutteroppgave = !erRetur && saksbehandler != null
                )
            },
            mottaker = it.stringOrNull("mottaker")?.let(Mottaker::valueOf),
            navn = no.nav.helse.spesialist.api.graphql.schema.Personnavn(
                fornavn = it.string("fornavn"),
                mellomnavn = it.stringOrNull("mellomnavn"),
                etternavn = it.string("etternavn"),
            ),
            haster = it.boolean("har_varsel_om_negativt_belop") && harUtbetalingTilSykmeldt(it.stringOrNull("mottaker")),
            harVergemal = it.boolean("har_vergemal"),
            tilhorerEnhetUtland = setOf(393, 2101).contains(it.int("enhet_ref")),
            spesialsak = it.boolean("er_spesialsak"),
        )

        private fun harUtbetalingTilSykmeldt(mottaker: String?): Boolean {
            if (mottaker == null) return false
            return when (Mottaker.valueOf(mottaker)) {
                BEGGE, SYKMELDT -> true
                else -> false
            }
        }
    }

}

private data class Inntekter(val årMåned: YearMonth, val inntektsliste: List<Inntekt>) {
    data class Inntekt(val beløp: Int, val orgnummer: String)
}
