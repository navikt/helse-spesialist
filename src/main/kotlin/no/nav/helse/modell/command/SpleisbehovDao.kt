package no.nav.helse.modell.command

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import java.util.*

internal fun Session.insertBehov(id: UUID, spleisReferanse: UUID, behov: String, original: String) {
    this.run(
        queryOf(
            "INSERT INTO spleisbehov(id, spleis_referanse, data, original) VALUES(?, ?, CAST(? as json), CAST(? as json))",
            id,
            spleisReferanse,
            behov,
            original
        ).asUpdate
    )
}

fun Session.insertWarning(melding: String, spleisbehovRef: UUID) = this.run(
    queryOf(
        "INSERT INTO warning (melding, spleisbehov_ref) VALUES (?, ?)",
        melding,
        spleisbehovRef
    ).asUpdate
)

fun Session.insertSaksbehandleroppgavetype(type: Saksbehandleroppgavetype, spleisbehovRef: UUID) =
    this.run(
        queryOf(
            "INSERT INTO saksbehandleroppgavetype (type, spleisbehov_ref) VALUES (?, ?)",
            type.name,
            spleisbehovRef
        ).asUpdate
    )

internal fun Session.updateBehov(id: UUID, behov: String) {
    this.run(
        queryOf(
            "UPDATE spleisbehov SET data=CAST(? as json) WHERE id=?", behov, id
        ).asUpdate
    )
}

internal fun Session.findBehov(id: UUID): SpleisbehovDBDto? =
    this.run(queryOf("SELECT spleis_referanse, data FROM spleisbehov WHERE id=?", id)
        .map {
            SpleisbehovDBDto(
                id = id,
                spleisReferanse = UUID.fromString(it.string("spleis_referanse")),
                data = it.string("data")
            )
        }
        .asSingle
    )

internal fun Session.findBehovMedSpleisReferanse(spleisReferanse: UUID): SpleisbehovDBDto? =
    this.run(queryOf("SELECT  id, data FROM spleisbehov WHERE spleis_referanse=?", spleisReferanse)
        .map {
            SpleisbehovDBDto(
                id = UUID.fromString(it.string("id")),
                spleisReferanse = spleisReferanse,
                data = it.string("data")
            )
        }
        .asSingle
    )

internal fun Session.findOriginalBehov(id: UUID): String? =
    this.run(queryOf("SELECT original FROM spleisbehov WHERE id=?", id)
        .map { it.string("original") }
        .asSingle)
