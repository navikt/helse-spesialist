package no.nav.helse.mediator.overstyring

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.OverstyrtTidslinjeForDatabase
import no.nav.helse.db.OverstyrtTidslinjedagForDatabase
import no.nav.helse.db.SubsumsjonForDatabase
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.dto.OverstyrtTidslinjeDto
import no.nav.helse.modell.saksbehandler.handlinger.dto.SubsumsjonDto

class Overstyringlagrer(private val overstyringDao: OverstyringDao) {
    internal fun lagre(overstyring: Overstyring, saksbehandlerOid: UUID) {
        when (overstyring) {
            is OverstyrtTidslinje -> lagreOverstyrTidslinje(overstyring, saksbehandlerOid)
        }
    }

    private fun lagreOverstyrTidslinje(overstyring: OverstyrtTidslinje, saksbehandlerOid: UUID) {
        overstyringDao.persisterOverstyringTidslinje(overstyring.toDto().tilDatabase(), saksbehandlerOid)
    }

    private fun OverstyrtTidslinjeDto.tilDatabase() = OverstyrtTidslinjeForDatabase(
        id = id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        dager = dager.map {
            OverstyrtTidslinjedagForDatabase(
                dato = it.dato,
                type = it.type,
                fraType = it.fraType,
                grad = it.grad,
                fraGrad = it.fraGrad,
                subsumsjon = it.subsumsjon?.tilDatabase(),
            )
        },
        begrunnelse = begrunnelse,
        opprettet = LocalDateTime.now(),
    )

    private fun SubsumsjonDto.tilDatabase() = SubsumsjonForDatabase(paragraf = paragraf, ledd = ledd, bokstav = bokstav)
}