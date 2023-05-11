package no.nav.helse.spesialist.api

import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsforholdKafkaDto
import no.nav.helse.spesialist.api.overstyring.OverstyrInntektOgRefusjonKafkaDto
import no.nav.helse.spesialist.api.overstyring.OverstyrTidslinjeKafkaDto
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.utbetaling.AnnulleringDto
import no.nav.helse.spesialist.api.utbetaling.AnnulleringKafkaDto
import org.slf4j.LoggerFactory

class SaksbehandlerMediator(
    dataSource: DataSource,
    private val rapidsConnection: RapidsConnection
) {
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)

    internal fun håndter(annullering: AnnulleringDto, saksbehandler: Saksbehandler) {
        tellAnnullering()
        saksbehandler.persister(saksbehandlerDao)
        val kafkamelding = AnnulleringKafkaDto(
            saksbehandler = saksbehandler,
            organisasjonsnummer = annullering.organisasjonsnummer,
            fødselsnummer = annullering.fødselsnummer,
            aktørId = annullering.aktørId,
            fagsystemId = annullering.fagsystemId,
            begrunnelser = annullering.begrunnelser,
            kommentar = annullering.kommentar
        )
        val message = kafkamelding.somKafkaMessage().also {
            sikkerlogg.info(
                "Publiserer annullering fra api: {}, {}, {}\n${it.toJson()}",
                kv("fødselsnummer", kafkamelding.fødselsnummer),
                kv("aktørId", kafkamelding.aktørId),
                kv("organisasjonsnummer", kafkamelding.organisasjonsnummer)
            )
        }

        rapidsConnection.publish(kafkamelding.fødselsnummer, message.toJson())
    }

    internal fun håndter(message: OverstyrTidslinjeKafkaDto) {
        tellOverstyrTidslinje()
        val overstyring = message.somKafkaMessage().also {
            sikkerlogg.info(
                "Publiserer overstyring av tidslinje fra api: {}, {}\n${it.toJson()}",
                kv("fødselsnummer", message.fødselsnummer),
                kv("aktørId", message.aktørId),
                kv("organisasjonsnummer", message.organisasjonsnummer)
            )
        }
        rapidsConnection.publish(message.fødselsnummer, overstyring.toJson())
    }

    internal fun håndter(message: OverstyrInntektOgRefusjonKafkaDto) {
        tellOverstyrInntektOgRefusjon()
        val overstyring = message.somKafkaMessage().also {
            sikkerlogg.info(
                "Publiserer overstyring av inntekt og refusjon fra api: {}, {}\n${it.toJson()}",
                kv("fødselsnummer", message.fødselsnummer),
                kv("aktørId", message.aktørId),
            )
        }
        rapidsConnection.publish(message.fødselsnummer, overstyring.toJson())
    }

    internal fun håndter(message: OverstyrArbeidsforholdKafkaDto) {
        tellOverstyrArbeidsforhold()
        val overstyring = message.somKafkaMessage().also {
            sikkerlogg.info(
                "Publiserer overstyring av arbeidsforhold fra api: {}, {}\n${it.toJson()}",
                kv("fødselsnummer", message.fødselsnummer),
                kv("aktørId", message.aktørId),
            )
        }
        rapidsConnection.publish(message.fødselsnummer, overstyring.toJson())
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}