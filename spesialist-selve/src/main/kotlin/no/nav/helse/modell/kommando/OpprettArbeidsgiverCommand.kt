package no.nav.helse.modell.kommando

import no.nav.helse.mediator.meldinger.Arbeidsgiverinformasjonløsning
import no.nav.helse.mediator.meldinger.HentPersoninfoløsninger
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.slf4j.LoggerFactory

internal class OpprettArbeidsgiverCommand(
    private val orgnummere: List<String>,
    private val arbeidsgiverDao: ArbeidsgiverDao
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettArbeidsgiverCommand::class.java)
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        if (arbeidsgivereSomIkkeFinnes().isEmpty()) return ignorer()
        sikkerLog.info("Arbeidsgiver(e) med orgnr: $orgnummere finnes ikke fra før")
        return behandle(context)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun ignorer(): Boolean {
        log.info("arbeidsgiver finnes fra før, lager ikke ny")
        return true
    }

    private fun behandle(context: CommandContext): Boolean {
        val arbeidsgivereSomIkkeFinnes = arbeidsgivereSomIkkeFinnes()
        if (arbeidsgivereSomIkkeFinnes.isEmpty()) return ignorer()
        val arbeidsgiver = context.get<Arbeidsgiverinformasjonløsning>()?.also { arbeidsgiver ->
            log.info("oppretter arbeidsgiver fra orgnumre")
            arbeidsgiver.opprett(arbeidsgiverDao)
        }
        val personinfo = context.get<HentPersoninfoløsninger>()?.also { personinfo ->
            log.info("oppretter arbeidsgiver fra personer")
            personinfo.opprett(arbeidsgiverDao)
        }

        if (arbeidsgiver == null && personinfo == null) return trengerMerInformasjon(context, arbeidsgivereSomIkkeFinnes)

        return true
    }

    private fun arbeidsgivereSomIkkeFinnes() =
        orgnummere.filter { arbeidsgiverDao.findArbeidsgiverByOrgnummer(it) == null }

    private fun trengerMerInformasjon(context: CommandContext, arbeidsgivereSomIkkeFinnes: List<String>): Boolean {
        val (orgnumre, personer) = arbeidsgivereSomIkkeFinnes.partition { it.length == 9 }
        if (orgnumre.isNotEmpty()) context.behov("Arbeidsgiverinformasjon", mapOf("organisasjonsnummer" to orgnumre))
        if (personer.isNotEmpty()) context.behov("HentPersoninfoV2", mapOf("ident" to personer))
        return false
    }
}
