package no.nav.helse.spesialist.domain.testfixtures.testdata

import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID
import kotlin.random.Random

fun lagSaksbehandler(
    id: SaksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
    navn: String =
        buildString {
            append(lagEtternavn())
            append(", ${lagFornavn()}")
            val mellomnavn = lagMellomnavnOrNull()
            if (mellomnavn != null) append(" $mellomnavn")
        },
    epost: String = navn.split(" ").joinToString(".").lowercase() + "@nav.no",
    navIdent: String = navn.also { println(it) }.substringAfterLast(' ').first() + "${Random.nextInt(from = 200_000, until = 999_999)}",
): Saksbehandler =
    Saksbehandler(
        id = id,
        navn = navn,
        epost = epost,
        ident = NAVIdent(navIdent),
    )
