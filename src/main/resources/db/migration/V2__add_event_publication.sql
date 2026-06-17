-- V2 - Spring Modulith event_publication table
-- Required by spring-modulith-events-jpa for the event publication registry

CREATE TABLE event_publication (
    id                     UUID                        NOT NULL,
    publication_date       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    listener_id            VARCHAR(255)                NOT NULL,
    event_type             VARCHAR(255)                NOT NULL,
    serialized_event       VARCHAR(255)                NOT NULL,
    completion_attempts    INTEGER                     NOT NULL DEFAULT 0,
    completion_date        TIMESTAMP(6) WITH TIME ZONE,
    last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    status                 VARCHAR(255) CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED')),
    PRIMARY KEY (id)
);
