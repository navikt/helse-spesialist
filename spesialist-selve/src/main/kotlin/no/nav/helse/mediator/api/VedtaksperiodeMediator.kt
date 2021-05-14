package no.nav.helse.mediator.api

import UtbetalingshistorikkElementApiDto
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDto
import no.nav.helse.arbeidsgiver.ArbeidsgiverDto
import no.nav.helse.measureAsHistogram
import no.nav.helse.mediator.FeatureToggle.REVURDERING_TOGGLE
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.objectMapper
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.overstyring.OverstyringApiDto
import no.nav.helse.overstyring.OverstyrtDagApiDto
import no.nav.helse.person.PersonApiDao
import no.nav.helse.person.PersonDto
import no.nav.helse.person.PersonForSpeilDto
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.utbetaling.AnnullertAvSaksbehandlerApiDto
import no.nav.helse.utbetaling.OppdragApiDto
import no.nav.helse.utbetaling.UtbetalingApiDto
import no.nav.helse.utbetaling.UtbetalingslinjeApiDto
import no.nav.helse.vedtaksperiode.VarselDao
import no.nav.helse.vedtaksperiode.VedtaksperiodeApiDao
import no.nav.helse.vedtaksperiode.VedtaksperiodeApiDto
import java.util.*

internal class VedtaksperiodeMediator(
    private val vedtaksperiodeDao: VedtaksperiodeApiDao,
    private val varselDao: VarselDao,
    private val personDao: PersonApiDao,
    private val arbeidsgiverDao: ArbeidsgiverApiDao,
    private val overstyringDao: OverstyringApiDao,
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val utbetalingDao: UtbetalingDao
) {
    fun byggSpeilSnapshotForFnr(fnr: String) =
        measureAsHistogram("byggSpeilSnapshotForFnr") {
            vedtaksperiodeDao.findVedtakByFnr(fnr)?.let(::byggSpeilSnapshot)
        }

    fun byggSpeilSnapshotForAktørId(aktørId: String) =
        measureAsHistogram("byggSpeilSnapshotForAktørId") {
            vedtaksperiodeDao.findVedtakByAktørId(aktørId)?.let(::byggSpeilSnapshot)
        }

    fun byggSpeilSnapshotForVedtaksperiodeId(vedtaksperiodeId: UUID) =
        measureAsHistogram("byggSpeilSnapshotForVedtaksperiodeId") {
            vedtaksperiodeDao.findVedtakByVedtaksperiodeId(vedtaksperiodeId)?.let(::byggSpeilSnapshot)
        }

    private fun byggSpeilSnapshot(vedtakinfo: Pair<VedtaksperiodeApiDto, PersonDto>) =
        measureAsHistogram("byggSpeilSnapshot") {
            val (vedtak, speilSnapshot) = vedtakinfo
            val infotrygdutbetalinger = measureAsHistogram("byggSpeilSnapshot_findInfotrygdutbetalinger") {
                personDao.finnInfotrygdutbetalinger(vedtak.fødselsnummer)?.let { objectMapper.readTree(it) }
            }
            val utbetalinger = measureAsHistogram("byggSpeilSnapshot_findUtbetaling") {
                utbetalingDao.findUtbetalinger(vedtak.fødselsnummer).map { utbetaling ->
                    UtbetalingApiDto(
                        type = utbetaling.type,
                        status = utbetaling.status.toString(),
                        arbeidsgiverOppdrag = OppdragApiDto(
                            organisasjonsnummer = utbetaling.arbeidsgiverOppdrag.organisasjonsnummer,
                            fagsystemId = utbetaling.arbeidsgiverOppdrag.fagsystemId,
                            utbetalingslinjer = utbetaling.arbeidsgiverOppdrag.linjer.map { linje ->
                                UtbetalingslinjeApiDto(
                                    fom = linje.fom,
                                    tom = linje.tom
                                )
                            }
                        ),
                        annullertAvSaksbehandler = utbetaling.annullertAvSaksbehandler?.let {
                            AnnullertAvSaksbehandlerApiDto(
                                annullertTidspunkt = it.annullertTidspunkt,
                                saksbehandlerNavn = it.saksbehandlerNavn
                            )
                        },
                        totalbeløp = utbetaling.totalbeløp
                    )
                }
            }

            val arbeidsgivere = speilSnapshot.arbeidsgivere.map {
                val navn = arbeidsgiverDao.finnNavn(it.organisasjonsnummer)
                val bransjer = arbeidsgiverDao.finnBransjer(it.organisasjonsnummer)
                val overstyringer = overstyringDao.finnOverstyring(vedtak.fødselsnummer, it.organisasjonsnummer)
                    .map { overstyring ->
                        OverstyringApiDto(
                            hendelseId = overstyring.hendelseId,
                            begrunnelse = overstyring.begrunnelse,
                            timestamp = overstyring.timestamp,
                            saksbehandlerNavn = overstyring.saksbehandlerNavn,
                            overstyrteDager = overstyring.overstyrteDager.map { dag ->
                                OverstyrtDagApiDto(
                                    dato = dag.dato,
                                    dagtype = dag.type,
                                    grad = dag.grad
                                )
                            }
                        )
                    }
                ArbeidsgiverApiDto(
                    organisasjonsnummer = it.organisasjonsnummer,
                    navn = navn ?: "Ikke tilgjengelig",
                    id = it.id,
                    overstyringer = overstyringer,
                    vedtaksperioder = it.vedtaksperioder,
                    bransjer = bransjer,
                    utbetalingshistorikk = if (REVURDERING_TOGGLE.enabled) mapUtbetalingshistorikk(it) else emptyList()
                )
            }
            measureAsHistogram("byggSpeilSnapshot_behovForVedtaksperiode_akkumulert") {
                speilSnapshot.arbeidsgivere.forEach { arbeidsgiver ->
                    arbeidsgiver.vedtaksperioder.forEach { vedtaksperiode ->
                        val vedtaksperiodeId = UUID.fromString(vedtaksperiode["id"].asText())
                        val oppgaveId = oppgaveDao.finnOppgaveId(vedtaksperiodeId)
                        val risikovurdering = risikovurderingApiDao.finnRisikovurdering(vedtaksperiodeId)
                        val varsler = varselDao.finnVarsler(vedtaksperiodeId)

                        vedtaksperiode as ObjectNode
                        vedtaksperiode.put("oppgavereferanse", oppgaveId?.toString())
                        risikovurdering?.let {
                            vedtaksperiode.set<ObjectNode>("risikovurdering", objectMapper.convertValue(it, ObjectNode::class.java))
                        }
                        vedtaksperiode.set<ArrayNode>("varsler", objectMapper.convertValue<ArrayNode>(varsler))
                    }
                }
            }

            val enhet = measureAsHistogram("byggSpeilSnapshot_findEnhet") {
                personDao.finnEnhet(vedtak.fødselsnummer)
            }

            val tildeling = tildelingDao.tildelingForPerson(vedtak.fødselsnummer)

            val arbeidsforhold = arbeidsgivere
                .map { arbeidsgiverDao.finnArbeidsforhold(vedtak.fødselsnummer, it.organisasjonsnummer) }
                .flatten()

            PersonForSpeilDto(
                aktørId = speilSnapshot.aktørId,
                fødselsnummer = speilSnapshot.fødselsnummer,
                dødsdato = speilSnapshot.dødsdato,
                personinfo = vedtak.personinfo,
                arbeidsgivere = arbeidsgivere,
                infotrygdutbetalinger = infotrygdutbetalinger,
                enhet = enhet,
                utbetalinger = utbetalinger,
                arbeidsforhold = arbeidsforhold,
                inntektsgrunnlag = speilSnapshot.inntektsgrunnlag,
                erPåVent = tildeling?.påVent ?: false,
                tildeling = tildeling
            )
        }

    private fun mapUtbetalingshistorikk(it: ArbeidsgiverDto) =
        it.utbetalingshistorikk?.let { utbetalingshistorikk ->
            UtbetalingshistorikkElementApiDto.toSpeilMap(utbetalingshistorikk)
        } ?: emptyList()

    fun erAktivOppgave(oppgaveId: Long) = oppgaveDao.venterPåSaksbehandler(oppgaveId)
}
