package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.GraderteAndreYtelserRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserDag
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserEndretEvent
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import java.time.LocalDate

class PgGraderteAndreYtelserRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    GraderteAndreYtelserRepository {
    override fun finnAlleForIdentitetsnummer(identitetsnummer: Identitetsnummer): List<GraderteAndreYtelser> =
        asSQL(
            """
            SELECT * FROM graderte_andre_ytelser_events
            WHERE fødselsnummer = :fodselsnummer
            ORDER BY versjon
            """.trimIndent(),
            "fodselsnummer" to identitetsnummer.value,
        ).list { it.tilGraderteAndreYtelserIdOgEvent() }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .map { (id, events) -> GraderteAndreYtelser.fraLagring(id, events) }

    override fun finnOrNull(id: GraderteAndreYtelserId): GraderteAndreYtelser? =
        asSQL(
            """
            SELECT * FROM graderte_andre_ytelser_events
            WHERE fødselsnummer = :fodselsnummer
              AND yrkesaktivitetstype = :yrkesaktivitetstype
              AND organisasjonsnummer IS NOT DISTINCT FROM :organisasjonsnummer
            ORDER BY versjon
            """.trimIndent(),
            "fodselsnummer" to id.identitetsnummer.value,
            "yrkesaktivitetstype" to id.yrkesaktivitet.tilDBYrkesaktivitetstype().name,
            "organisasjonsnummer" to id.organisasjonsnummer,
        ).list { it.tilGraderteAndreYtelserIdOgEvent().second }
            .takeUnless { it.isEmpty() }
            ?.let { events -> GraderteAndreYtelser.fraLagring(id, events) }

    override fun lagre(graderteAndreYtelser: GraderteAndreYtelser) {
        val id = graderteAndreYtelser.id
        graderteAndreYtelser.events.forEach { event ->
            asSQL(
                """
                INSERT INTO graderte_andre_ytelser_events (
                  versjon,
                  fødselsnummer,
                  yrkesaktivitetstype,
                  organisasjonsnummer,
                  tidspunkt,
                  utført_av_saksbehandler_ident,
                  notat_til_beslutter,
                  data_json
                )
                VALUES (
                  :versjon,
                  :fodselsnummer,
                  :yrkesaktivitetstype,
                  :organisasjonsnummer,
                  :tidspunkt,
                  :utfort_av_saksbehandler_ident,
                  :notat_til_beslutter,
                  :data_json::jsonb
                )
                ON CONFLICT (versjon) DO NOTHING
                """.trimIndent(),
                "versjon" to event.versjon,
                "fodselsnummer" to id.identitetsnummer.value,
                "yrkesaktivitetstype" to id.yrkesaktivitet.tilDBYrkesaktivitetstype().name,
                "organisasjonsnummer" to id.organisasjonsnummer,
                "tidspunkt" to event.tidspunkt,
                "utfort_av_saksbehandler_ident" to event.utførtAvSaksbehandlerIdent.value,
                "notat_til_beslutter" to event.notatTilBeslutter,
                "data_json" to objectMapper.writeValueAsString(DBEventData(dager = event.dager.map { it.tilDBGraderteAndreYtelserDag() })),
            ).update()
        }
    }

    private fun Row.tilGraderteAndreYtelserIdOgEvent(): Pair<GraderteAndreYtelserId, GraderteAndreYtelserEndretEvent> {
        val id =
            GraderteAndreYtelserId(
                identitetsnummer = Identitetsnummer.fraString(string("fødselsnummer")),
                yrkesaktivitet = enumValueOf<DBYrkesaktivitetstype>(string("yrkesaktivitetstype")).tilYrkesaktivitetstype(),
                organisasjonsnummer = stringOrNull("organisasjonsnummer"),
            )
        val data = objectMapper.readValue(string("data_json"), DBEventData::class.java)
        val event =
            GraderteAndreYtelserEndretEvent(
                versjon = uuid("versjon"),
                tidspunkt = instant("tidspunkt"),
                utførtAvSaksbehandlerIdent = NAVIdent(string("utført_av_saksbehandler_ident")),
                notatTilBeslutter = string("notat_til_beslutter"),
                dager = data.dager.map { it.tilGraderteAndreYtelserDag() },
            )
        return id to event
    }

    private fun DBGraderteAndreYtelserDag.tilGraderteAndreYtelserDag() =
        GraderteAndreYtelserDag(
            dato = dato,
            andreYtelserGrader = andreYtelserGrader.mapKeys { it.key.tilGraderteAndreYtelserType() },
            sykdomsgrad = sykdomsgrad,
            arbeidsgrad = arbeidsgrad,
        )

    private fun DBGraderteAndreYtelserType.tilGraderteAndreYtelserType() =
        when (this) {
            DBGraderteAndreYtelserType.FORELDREPENGER -> GraderteAndreYtelserType.FORELDREPENGER
            DBGraderteAndreYtelserType.SVANGERSKAPSPENGER -> GraderteAndreYtelserType.SVANGERSKAPSPENGER
            DBGraderteAndreYtelserType.OMSORGSPENGER -> GraderteAndreYtelserType.OMSORGSPENGER
            DBGraderteAndreYtelserType.PLEIEPENGER -> GraderteAndreYtelserType.PLEIEPENGER
            DBGraderteAndreYtelserType.OPPLARINGSPENGER -> GraderteAndreYtelserType.OPPLARINGSPENGER
        }

    private fun GraderteAndreYtelserDag.tilDBGraderteAndreYtelserDag() =
        DBGraderteAndreYtelserDag(
            dato = dato,
            andreYtelserGrader = andreYtelserGrader.mapKeys { it.key.tilDBGraderteAndreYtelserType() },
            sykdomsgrad = sykdomsgrad,
            arbeidsgrad = arbeidsgrad,
        )

    private fun GraderteAndreYtelserType.tilDBGraderteAndreYtelserType() =
        when (this) {
            GraderteAndreYtelserType.FORELDREPENGER -> DBGraderteAndreYtelserType.FORELDREPENGER
            GraderteAndreYtelserType.SVANGERSKAPSPENGER -> DBGraderteAndreYtelserType.SVANGERSKAPSPENGER
            GraderteAndreYtelserType.OMSORGSPENGER -> DBGraderteAndreYtelserType.OMSORGSPENGER
            GraderteAndreYtelserType.PLEIEPENGER -> DBGraderteAndreYtelserType.PLEIEPENGER
            GraderteAndreYtelserType.OPPLARINGSPENGER -> DBGraderteAndreYtelserType.OPPLARINGSPENGER
        }

    private fun Yrkesaktivitetstype.tilDBYrkesaktivitetstype() =
        when (this) {
            Yrkesaktivitetstype.ARBEIDSTAKER -> DBYrkesaktivitetstype.ARBEIDSTAKER
            Yrkesaktivitetstype.FRILANS -> DBYrkesaktivitetstype.FRILANS
            Yrkesaktivitetstype.ARBEIDSLEDIG -> DBYrkesaktivitetstype.ARBEIDSLEDIG
            Yrkesaktivitetstype.SELVSTENDIG -> DBYrkesaktivitetstype.SELVSTENDIG
        }

    private fun DBYrkesaktivitetstype.tilYrkesaktivitetstype() =
        when (this) {
            DBYrkesaktivitetstype.ARBEIDSTAKER -> Yrkesaktivitetstype.ARBEIDSTAKER
            DBYrkesaktivitetstype.FRILANS -> Yrkesaktivitetstype.FRILANS
            DBYrkesaktivitetstype.ARBEIDSLEDIG -> Yrkesaktivitetstype.ARBEIDSLEDIG
            DBYrkesaktivitetstype.SELVSTENDIG -> Yrkesaktivitetstype.SELVSTENDIG
        }

    private enum class DBGraderteAndreYtelserType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER,
        OMSORGSPENGER,
        PLEIEPENGER,
        OPPLARINGSPENGER,
    }

    private enum class DBYrkesaktivitetstype {
        ARBEIDSTAKER,
        FRILANS,
        ARBEIDSLEDIG,
        SELVSTENDIG,
    }

    private data class DBEventData(
        val dager: List<DBGraderteAndreYtelserDag>,
    )

    private data class DBGraderteAndreYtelserDag(
        val dato: LocalDate,
        val andreYtelserGrader: Map<DBGraderteAndreYtelserType, Int>,
        val sykdomsgrad: Int,
        val arbeidsgrad: Int,
    )
}
