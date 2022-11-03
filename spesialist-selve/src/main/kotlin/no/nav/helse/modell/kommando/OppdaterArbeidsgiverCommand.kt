package no.nav.helse.modell.kommando

import java.time.LocalDate
import no.nav.helse.mediator.meldinger.Arbeidsgiverinformasjonløsning
import no.nav.helse.mediator.meldinger.HentPersoninfoløsninger
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.slf4j.LoggerFactory

internal class OppdaterArbeidsgiverCommand(
    orgnummere: List<String>,
    private val arbeidsgiverDao: ArbeidsgiverDao
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterArbeidsgiverCommand::class.java)
    }

    // ignorerer fnr/aktørId/dnr ettersom bransje/navn er ganske så statisk for dem
    private val orgnummere = orgnummere.filter { it.length == 9 }
    private val personidenter = orgnummere.filter { it.length > 9 }

    override fun execute(context: CommandContext) =
        when {
            (ikkeOppdaterteBransjer() + ikkeOppdaterteNavn() + ikkeOppdaterteNavnForPersonidenter()).isEmpty() -> true
            else -> behandle(context)
        }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun ikkeOppdaterteNavn() = orgnummere.filterNot { orgnummer ->
        val sistOppdatert = arbeidsgiverDao.findNavnSistOppdatert(orgnummer) ?: return@filterNot false
        sistOppdatert > LocalDate.now().minusDays(14)
    }

    private fun ikkeOppdaterteBransjer() = orgnummere.filterNot { orgnummer ->
        val sistOppdatert = arbeidsgiverDao.findBransjerSistOppdatert(orgnummer) ?: return@filterNot false
        sistOppdatert > LocalDate.now().minusDays(14)
    }

    private fun ikkeOppdaterteNavnForPersonidenter() = personidenter.filterNot { personlignr ->
        val sistOppdatert = arbeidsgiverDao.findNavnSistOppdatert(personlignr) ?: return@filterNot false
        sistOppdatert > LocalDate.now().minusDays(14)
    }

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<Arbeidsgiverinformasjonløsning>()
        context.get<HentPersoninfoløsninger>()?.also { personinfo ->
            log.info("oppretter arbeidsgiver fra personer")
            personinfo.opprett(arbeidsgiverDao)
        }

        if (
            løsning == null ||
            !løsning.harSvarForAlle(ikkeOppdaterteBransjer() + ikkeOppdaterteNavn()) ||
            ikkeOppdaterteNavnForPersonidenter().isNotEmpty()
        ) return trengerMerInformasjon(context)

        løsning.oppdater(arbeidsgiverDao)

        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        val ikkeOppdaterteArbeidsgivere = (ikkeOppdaterteBransjer() + ikkeOppdaterteNavn()).distinct()
        val ikkeOppdatertePersonArbeidsgivere = ikkeOppdaterteNavnForPersonidenter().distinct()

        if (ikkeOppdaterteArbeidsgivere.isNotEmpty()) {
            context.behov(
                "Arbeidsgiverinformasjon",
                mapOf("organisasjonsnummer" to ikkeOppdaterteArbeidsgivere)
            )
        }
        if (ikkeOppdatertePersonArbeidsgivere.isNotEmpty()) {
            context.behov("HentPersoninfoV2", mapOf("ident" to ikkeOppdatertePersonArbeidsgivere))
        }

        return false
    }
}
