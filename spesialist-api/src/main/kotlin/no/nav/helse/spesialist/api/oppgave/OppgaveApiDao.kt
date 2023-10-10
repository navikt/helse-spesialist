package no.nav.helse.spesialist.api.oppgave

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao
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
        """ SELECT o.id, o.kan_avvises
            FROM oppgave o
            INNER JOIN vedtak v ON o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId 
                AND status = 'AvventerSaksbehandler'::oppgavestatus 
        """,
        mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single { OppgaveForPeriodevisningDto(id = it.string("id"), kanAvvises = it.boolean("kan_avvises")) }

    fun finnOppgavetype(vedtaksperiodeId: UUID) = asSQL(
        """ SELECT type
            FROM oppgave
            WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            ORDER BY id LIMIT 1
        """,
        mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single { Oppgavetype.valueOf(it.string("type")) }

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
    }
}
