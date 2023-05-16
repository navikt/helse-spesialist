CREATE TABLE oppgave_behandling_kobling(
    oppgave_id BIGINT NOT NULL,
    behandling_id uuid NOT NULL,
    PRIMARY KEY (oppgave_id, behandling_id)
);