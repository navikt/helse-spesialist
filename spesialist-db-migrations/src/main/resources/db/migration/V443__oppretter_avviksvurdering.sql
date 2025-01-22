CREATE TABLE SAMMENLIGNINGSGRUNNLAG
(
    id                     BIGSERIAL PRIMARY KEY,
    unik_id                UUID        NOT NULL,
    fødselsnummer          VARCHAR(11) NOT NULL,
    skjæringstidspunkt     DATE        NOT NULL,
    opprettet              TIMESTAMP   NOT NULL,
    sammenligningsgrunnlag json        NOT NULL
);

CREATE TABLE AVVIKSVURDERING
(
    id                         BIGSERIAL PRIMARY KEY,
    unik_id                    UUID                                                             NOT NULL,
    fødselsnummer              VARCHAR(11)                                                      NOT NULL,
    skjæringstidspunkt         DATE                                                             NOT NULL,
    opprettet                  TIMESTAMP                                                        NOT NULL,
    avviksprosent              DECIMAL                                                          NOT NULL,
    beregningsgrunnlag         json                                                             NOT NULL,
    sammenligningsgrunnlag_ref INTEGER REFERENCES SAMMENLIGNINGSGRUNNLAG (id) ON DELETE CASCADE NOT NULL
);

