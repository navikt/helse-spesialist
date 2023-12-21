package no.nav.helse.modell.automatisering

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vergemal.VergemålDao
import org.slf4j.LoggerFactory

internal class AutomatiskAvvisningCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val personDao: PersonDao,
    private val vergemålDao: VergemålDao,
    private val vedtakDao: VedtakDao,
    private val godkjenningMediator: GodkjenningMediator,
    private val hendelseId: UUID,
    private val utbetaling: Utbetaling,
    private val kanAvvises: Boolean,
    private val sykefraværstilfelle: Sykefraværstilfelle,
) : Command {

    override fun execute(context: CommandContext): Boolean {
        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val avvisGrunnetEnhetUtland = tilhørerEnhetUtland && kanAvvises
        val underVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val avvisGrunnetVergemål = underVergemål && kanAvvises
        val erSkjønnsfastsettelse = sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId)
        val enArbeidsgiver = vedtakDao.finnInntektskilde(vedtaksperiodeId) == Inntektskilde.EN_ARBEIDSGIVER

        val kandidatForBehandlingAvSkjønnsfastsettelse = Toggle.Avviksvurdering.enabled
                && fødselsnummer.startsWith("31")
                && enArbeidsgiver
        val avvisGrunnetSkjønnsfastsettelse = erSkjønnsfastsettelse
                && !kandidatForBehandlingAvSkjønnsfastsettelse
                && kanAvvises

        if (avvisGrunnetSkjønnsfastsettelse) {
            logg.info("Avviser vedtaksperiode $vedtaksperiodeId grunnet krav om skjønnsfastsetting.")
        }

        val avvisningsårsaker = mutableListOf<String>()
        if (avvisGrunnetSkjønnsfastsettelse) avvisningsårsaker.add("Skjønnsfastsettelse")
        if (tilhørerEnhetUtland) avvisningsårsaker.add("Utland")
        if (underVergemål) avvisningsårsaker.add("Vergemål")
        if (!avvisGrunnetEnhetUtland && !avvisGrunnetVergemål && !avvisGrunnetSkjønnsfastsettelse) {
            if (avvisningsårsaker.size > 0) {
                logg.info(
                    "Avviser ikke {} som har $avvisningsårsaker, fordi: {}",
                    kv("vedtaksperiodeId", vedtaksperiodeId),
                    kv("kanAvvises", kanAvvises)
                )
            }
            return true
        }

        godkjenningMediator.automatiskAvvisning(
            context::publiser,
            vedtaksperiodeId,
            avvisningsårsaker.toList(),
            utbetaling,
            hendelseId,
        )
        logg.info("Automatisk avvisning av vedtaksperiode $vedtaksperiodeId pga:$avvisningsårsaker")
        return ferdigstill(context)
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiskAvvisningCommand::class.java)
    }
}
