package no.nav.helse.spesialist.domain.testfixtures.testdata

import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID
import kotlin.random.Random

fun lagSaksbehandler(
    id: SaksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
    navn: String = listOfNotNull(lagFornavn(), lagMellomnavnOrNull(), lagEtternavn()).joinToString(separator = " "),
    epost: String = navn.split(" ").joinToString(".").lowercase() + "@nav.no",
    ident: String = navn.substringAfterLast(' ').first() + "${Random.nextInt(from = 200_000, until = 999_999)}",
): Saksbehandler =
    Saksbehandler(
        id = id,
        navn = navn,
        epost = epost,
        ident = NAVIdent(ident),
    )

@Deprecated("Bruk lagSaksbehandler() og ta felter derfra")
fun lagSaksbehandlerident() = lagSaksbehandler().ident
