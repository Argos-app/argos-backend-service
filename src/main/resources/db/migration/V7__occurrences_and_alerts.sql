
create table occurrence
(
    id                  UUID primary key     default gen_random_uuid(),
    animal_exception_id UUID        not null references animal_exception (id) on delete restrict,
    user_id             UUID        not null references app_user (id) on delete restrict,
    type                VARCHAR(20) not null,
    occurrence_date     DATE        not null default current_date,
    description         TEXT        not null,
    diagnosis           TEXT,
    resolved            BOOLEAN     not null default false,
    resolved_at         DATE,
    created_at          TIMESTAMP   not null default current_timestamp,
    constraint ck_occurrence_type check (type in ('DOENCA', 'MORTE', 'REACAO_ADVERSA', 'FERIMENTO')),
    constraint ck_occurrence_resolved check (resolved = false or resolved_at is not null)
);


create table animal_lot_history
(
    id                  UUID primary key     default gen_random_uuid(),
    animal_exception_id UUID        not null references animal_exception (id) on delete cascade,
    lot_id              UUID        not null references lot (id) on delete restrict,
    entry_date          DATE        not null default current_date,
    exit_date           DATE,
    reason              VARCHAR(60) not null,
    created_at          TIMESTAMP   not null default current_timestamp,
    constraint ck_animal_lot_history_datas check (exit_date is null or exit_date >= entry_date)
);

create table alert
(
    id                  UUID primary key      default gen_random_uuid(),
    user_id             UUID         not null references app_user (id) on delete cascade,
    lot_id              UUID references lot (id) on delete cascade,
    animal_exception_id UUID references animal_exception (id) on delete cascade,
    type                VARCHAR(30)  not null,
    severity            VARCHAR(10)  not null default 'MEDIA',
    title               VARCHAR(120) not null,
    message             TEXT         not null,
    created_at          TIMESTAMP    not null default current_timestamp,
    read_at             TIMESTAMP,
    status              VARCHAR(15)  not null default 'NOVO',
    constraint ck_alert_type check (type in ('CARENCIA', 'VALIDADE', 'ESTOQUE_BAIXO', 'OCORRENCIA')),
    constraint ck_alert_severity check (severity in ('BAIXA', 'MEDIA', 'ALTA')),
    constraint ck_alert_status check (status in ('NOVO', 'LIDO', 'RESOLVIDO'))
);

create table notification_delivery
(
    id        UUID primary key     default gen_random_uuid(),
    alert_id  UUID        not null references alert (id) on delete cascade,
    device_id UUID        not null references device (id) on delete cascade,
    channel   VARCHAR(15) not null,
    status    VARCHAR(15) not null default 'PENDENTE',
    attempts  INTEGER     not null default 0,
    sent_at   TIMESTAMP,
    error     TEXT,
    constraint ck_notification_channel check (channel in ('PUSH', 'SMS', 'EMAIL')),
    constraint ck_notification_status check (status in ('PENDENTE', 'ENVIADO', 'FALHOU'))
);


