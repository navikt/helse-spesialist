package no.nav.helse.mediator.api

import UtbetalingshistorikkElementApiDto
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDto
import no.nav.helse.arbeidsgiver.ArbeidsgiverDto
import no.nav.helse.measureAsHistogram
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.objectMapper
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.overstyring.OverstyringApiDto
import no.nav.helse.overstyring.OverstyrtDagApiDto
import no.nav.helse.person.*
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.utbetaling.AnnullertAvSaksbehandlerApiDto
import no.nav.helse.utbetaling.OppdragApiDto
import no.nav.helse.utbetaling.UtbetalingApiDto
import no.nav.helse.utbetaling.UtbetalingslinjeApiDto
import no.nav.helse.vedtaksperiode.VarselDao
import java.util.*

internal class VedtaksperiodeMediator(
    private val personsnapshotDao: PersonsnapshotDao,
    private val varselDao: VarselDao,
    private val personDao: PersonApiDao,
    private val arbeidsgiverDao: ArbeidsgiverApiDao,
    private val overstyringDao: OverstyringApiDao,
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val utbetalingDao: UtbetalingDao,
    private val snapshotDao: SnapshotDao,
    private val speilSnapshotRestClient: SpeilSnapshotRestClient
) {
    fun byggSpeilSnapshotForFnr(fnr: String) =
        measureAsHistogram("byggSpeilSnapshotForFnr") {
            byggSnapshot(fnr)
        }

    fun byggSpeilSnapshotForAktørId(aktørId: String) =
        measureAsHistogram("byggSpeilSnapshotForAktørId") {
            personsnapshotDao.finnFnrByAktørId(aktørId)?.let(::byggSnapshot)
        }

    fun byggSpeilSnapshotForVedtaksperiodeId(vedtaksperiodeId: UUID) =
        measureAsHistogram("byggSpeilSnapshotForVedtaksperiodeId") {
            personsnapshotDao.finnFnrByVedtaksperiodeId(vedtaksperiodeId)?.let(::byggSnapshot)
        }

    private fun byggSnapshot(fnr: String): PersonForSpeilDto? {
        if (!personDao.finnesPersonMedFødselsnummer(fnr)) {
            return null
        }
        if (snapshotDao.utdatert(fnr)) {
            val nyttSnapshot = speilSnapshotRestClient.hentSpeilSpapshot(fnr)
            snapshotDao.lagre(fnr, nyttSnapshot)
        }
        return personsnapshotDao.finnPersonByFnr(fnr)?.let(::byggSpeilSnapshot)
    }

    private fun byggSpeilSnapshot(personsnapshot: Pair<PersonMetadataApiDto, SnapshotDto>) =
        measureAsHistogram("byggSpeilSnapshot") {
            val (personMetadata, speilSnapshot) = personsnapshot
            val infotrygdutbetalinger = measureAsHistogram("byggSpeilSnapshot_findInfotrygdutbetalinger") {
                personDao.finnInfotrygdutbetalinger(personMetadata.fødselsnummer)?.let { objectMapper.readTree(it) }
            }
            val utbetalinger = measureAsHistogram("byggSpeilSnapshot_findUtbetaling") {
                utbetalingDao.findUtbetalinger(personMetadata.fødselsnummer).map { utbetaling ->
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
                val overstyringer = overstyringDao.finnOverstyringerAvTidslinjer(personMetadata.fødselsnummer, it.organisasjonsnummer)
                    .map { overstyring ->
                        OverstyringApiDto(
                            hendelseId = overstyring.hendelseId,
                            begrunnelse = overstyring.begrunnelse,
                            timestamp = overstyring.timestamp,
                            saksbehandlerNavn = overstyring.saksbehandlerNavn,
                            saksbehandlerIdent = overstyring.saksbehandlerIdent,
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
                    utbetalingshistorikk = mapUtbetalingshistorikk(it),
                    generasjoner = it.generasjoner
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
                            vedtaksperiode.set<ObjectNode>(
                                "risikovurdering",
                                objectMapper.convertValue(it, ObjectNode::class.java)
                            )
                        }
                        vedtaksperiode.set<ArrayNode>("varsler", objectMapper.convertValue<ArrayNode>(varsler))
                    }
                }
            }

            val enhet = measureAsHistogram("byggSpeilSnapshot_findEnhet") {
                personDao.finnEnhet(personMetadata.fødselsnummer)
            }

            val tildeling = tildelingDao.tildelingForPerson(personMetadata.fødselsnummer)

            val arbeidsforhold = arbeidsgivere
                .map { arbeidsgiverDao.finnArbeidsforhold(personMetadata.fødselsnummer, it.organisasjonsnummer) }
                .flatten()

            PersonForSpeilDto(
                aktørId = speilSnapshot.aktørId,
                fødselsnummer = speilSnapshot.fødselsnummer,
                dødsdato = speilSnapshot.dødsdato,
                personinfo = personMetadata.personinfo,
                arbeidsgivere = arbeidsgivere,
                infotrygdutbetalinger = infotrygdutbetalinger,
                enhet = enhet,
                utbetalinger = utbetalinger,
                arbeidsforhold = arbeidsforhold,
                inntektsgrunnlag = speilSnapshot.inntektsgrunnlag,
                tildeling = tildeling,
                vilkårsgrunnlagHistorikk = speilSnapshot.vilkårsgrunnlagHistorikk
            )
        }

    private fun mapUtbetalingshistorikk(it: ArbeidsgiverDto) =
        UtbetalingshistorikkElementApiDto.toSpeilMap(it.utbetalingshistorikk)

    fun erAktivOppgave(oppgaveId: Long) = oppgaveDao.venterPåSaksbehandler(oppgaveId)
}
