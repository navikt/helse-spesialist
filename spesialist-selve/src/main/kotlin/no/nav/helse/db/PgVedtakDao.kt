package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto
import java.util.UUID
import javax.sql.DataSource

class PgVedtakDao(queryRunner: QueryRunner) : VedtakDao, QueryRunner by queryRunner {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto? {
        return asSQL(
            """
            SELECT 
            vedtaksperiode_id,
            (SELECT orgnummer FROM arbeidsgiver WHERE id = arbeidsgiver_ref),
            forkastet
            from vedtak WHERE vedtaksperiode_id = :vedtaksperiode_id
            """,
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).singleOrNull {
            VedtaksperiodeDto(
                organisasjonsnummer = it.long("orgnummer").toString(),
                vedtaksperiodeId = it.uuid("vedtaksperiode_id"),
                forkastet = it.boolean("forkastet"),
                generasjoner = emptyList(),
            )
        }
    }

    override fun lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiodeDto: VedtaksperiodeDto,
    ) {
        asSQL(
            """
            INSERT INTO vedtak(vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, forkastet)
            VALUES (:vedtaksperiode_id, :fom, :tom, (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer), (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer), :forkastet)
            ON CONFLICT (vedtaksperiode_id) DO UPDATE SET forkastet = excluded.forkastet
            """,
            "fodselsnummer" to fødselsnummer.toLong(),
            "organisasjonsnummer" to vedtaksperiodeDto.organisasjonsnummer.toLong(),
            "vedtaksperiode_id" to vedtaksperiodeDto.vedtaksperiodeId,
            "fom" to vedtaksperiodeDto.generasjoner.last().fom,
            "tom" to vedtaksperiodeDto.generasjoner.last().tom,
            "forkastet" to vedtaksperiodeDto.forkastet,
        ).update()
    }

    override fun lagreOpprinneligSøknadsdato(vedtaksperiodeId: UUID) {
        asSQL(
            """
            INSERT INTO opprinnelig_soknadsdato 
            SELECT vedtaksperiode_id, opprettet_tidspunkt
            FROM selve_vedtaksperiode_generasjon
            WHERE vedtaksperiode_id = :vedtaksperiode_id
            ORDER BY opprettet_tidspunkt LIMIT 1
            ON CONFLICT DO NOTHING;
            """,
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).update()
    }

    override fun spesialsakFerdigbehandlet(vedtaksperiodeId: UUID): Int {
        return asSQL(
            """UPDATE spesialsak SET ferdigbehandlet = true WHERE vedtaksperiode_id = :vedtaksperiode_id""",
            "vedtaksperiode_id" to vedtaksperiodeId,
        )
            .update()
    }

    override fun finnVedtaksperiodetype(vedtaksperiodeId: UUID): Periodetype {
        val vedtakRef =
            checkNotNull(finnVedtakId(vedtaksperiodeId)) { "Finner ikke vedtakRef for $vedtaksperiodeId" }
        return asSQL("SELECT type FROM saksbehandleroppgavetype WHERE vedtak_ref = :vedtakRef", "vedtakRef" to vedtakRef)
            .single { enumValueOf<Periodetype>(it.string("type")) }
    }

    override fun leggTilVedtaksperiodetype(
        vedtaksperiodeId: UUID,
        type: Periodetype,
        inntektskilde: Inntektskilde,
    ) {
        val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return
        asSQL(
            """
            INSERT INTO saksbehandleroppgavetype (type, inntektskilde, vedtak_ref) VALUES (:type, :inntektskilde, :vedtak_ref)
            ON CONFLICT (vedtak_ref) DO UPDATE SET type = :type, inntektskilde = :inntektskilde
            """.trimIndent(),
            "type" to type.name,
            "inntektskilde" to inntektskilde.name,
            "vedtak_ref" to vedtakRef,
        ).update()
    }

    override fun erSpesialsak(vedtaksperiodeId: UUID): Boolean {
        return asSQL("""SELECT true FROM spesialsak WHERE vedtaksperiode_id = :vedtaksperiode_id AND ferdigbehandlet = false""", "vedtaksperiode_id" to vedtaksperiodeId)
            .singleOrNull {
                it.boolean(1)
            } ?: false
    }

    override fun erAutomatiskGodkjent(utbetalingId: UUID) =
        asSQL(
            """
            SELECT automatisert FROM automatisering 
            WHERE utbetaling_id = :utbetalingId
            AND (inaktiv_fra IS NULL OR inaktiv_fra > now())
            ORDER BY id DESC
            LIMIT 1
            """.trimIndent(),
            "utbetalingId" to utbetalingId,
        ).singleOrNull {
            it.boolean("automatisert")
        } ?: false

    override fun opprettKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        asSQL(
            """
            INSERT INTO vedtaksperiode_hendelse(vedtaksperiode_id, hendelse_ref) VALUES (:vedtaksperiodeId, :hendelseId)
            ON CONFLICT DO NOTHING
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "hendelseId" to hendelseId,
        ).update()
    }

    override fun fjernKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        asSQL(
            "DELETE FROM vedtaksperiode_hendelse WHERE hendelse_ref = :hendelseId AND vedtaksperiode_id = :vedtaksperiodeId",
            "vedtaksperiodeId" to vedtaksperiodeId,
            "hendelseId" to hendelseId,
        )
            .update()
    }

    override fun finnOrgnummer(vedtaksperiodeId: UUID): String? {
        return asSQL(
            """
            SELECT orgnummer FROM arbeidsgiver a
            INNER JOIN vedtak v ON a.id = v.arbeidsgiver_ref
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId
        """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        )
            .singleOrNull {
                it.long("orgnummer").toString()
            }
    }

    override fun finnInntektskilde(vedtaksperiodeId: UUID): Inntektskilde? {
        return asSQL(
            "SELECT inntektskilde FROM saksbehandleroppgavetype WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)",
            "vedtaksperiodeId" to vedtaksperiodeId,
        )
            .singleOrNull {
                enumValueOf<Inntektskilde>(it.string("inntektskilde"))
            }
    }

    internal fun finnVedtakId(vedtaksperiodeId: UUID): Long? {
        return asSQL("SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId", "vedtaksperiodeId" to vedtaksperiodeId)
            .singleOrNull {
                it.long("id")
            }
    }
}
