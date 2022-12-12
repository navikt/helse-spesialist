package no.nav.helse.migrering.domene

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.migrering.db.SpesialistDao
import no.nav.helse.migrering.domene.Utbetaling.Companion.fjernGammelMoro
import no.nav.helse.migrering.domene.Utbetaling.Companion.sortert
import no.nav.helse.migrering.domene.Utbetaling.Vurdering
import no.nav.helse.migrering.domene.Varsel.Companion.sortert
import no.nav.helse.migrering.domene.Varsel.Companion.varslerFor

internal class Vedtaksperiode(
    private val id: UUID,
    private val opprettet: LocalDateTime,
    private val oppdatert: LocalDateTime,
    private val utbetalinger: List<Utbetaling>,
    private val tilstand: String,
    personVarsler: List<Varsel>,
    spesialistDao: SpesialistDao
) {

    private val varsler = personVarsler.varslerFor(id).sortert().toMutableList().onEach {
        it.definisjon(spesialistDao)
    }

    internal fun generasjoner(spesialistDao: SpesialistDao): List<Generasjon> {
        var sistOpprettet: LocalDateTime? = opprettet
        if (utbetalinger.isEmpty()) {
            val eksisterendeGenerasjon = spesialistDao.finnSisteGenerasjonFor(id)
            if (eksisterendeGenerasjon != null) {
                if (!eksisterendeGenerasjon.erLåst()) return emptyList()
                if (tilstand == "AVSLUTTET_UTEN_UTBETALING") return emptyList()
            }
            if (tilstand == "AVSLUTTET_UTEN_UTBETALING") return listOf(auu(oppdatert, varsler))
            return listOf(åpen(varsler))
        }

        val generasjoner = mutableListOf<Generasjon>()

        utbetalinger
            .sortert()
            .fjernGammelMoro()
            .onEach {
                val generasjon = it.lagGenerasjon(id, sistOpprettet, varsler)
                sistOpprettet = null
                generasjoner.add(generasjon)
            }

        if (generasjoner.periodeISpillEtterSisteUtbetaling()) {
            generasjoner.add(Generasjon(UUID.randomUUID(), id, null, oppdatert, null, null, emptyList()))
        }

        if (generasjoner.sisteErÅpen()) {
            generasjoner.last().nyeVarsler(varsler)
        }

        return generasjoner
    }

    private fun List<Generasjon>.sisteErÅpen() = isNotEmpty() && !last().erLåst()

    private fun List<Generasjon>.periodeISpillEtterSisteUtbetaling() =
        isNotEmpty()
                && last().erLåst()
                && tilstand !in listOf("AVSLUTTET", "TIL_INFOTRYGD, TIL_UTBETALING", "AVSLUTTET_UTEN_UTBETALING")

    private fun åpen(varsler: List<Varsel>): Generasjon {
        return Generasjon(
            id = UUID.randomUUID(),
            vedtaksperiodeId = id,
            utbetalingId = null,
            opprettet = opprettet,
            låstTidspunkt = null,
            vurdering = null,
            varsler = varsler,
        )
    }

    private fun auu(låstTidspunkt: LocalDateTime, varsler: List<Varsel>): Generasjon {
        return Generasjon(
            id = UUID.randomUUID(),
            vedtaksperiodeId = id,
            utbetalingId = null,
            opprettet = opprettet,
            låstTidspunkt = låstTidspunkt,
            vurdering = Vurdering(
                ident = "Automatisk behandlet",
                tidspunkt = oppdatert,
                automatiskBehandling = true,
                godkjent = true
            ),
            varsler = varsler,
        )
    }
}