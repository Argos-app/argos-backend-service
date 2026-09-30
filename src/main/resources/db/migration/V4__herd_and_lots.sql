
create table herd
(
    id          UUID primary key     default gen_random_uuid(),
    property_id UUID        not null references property (id) on delete restrict,
    herd_name   VARCHAR(60) not null,
    breed       VARCHAR(40) not null,
    head_count  INTEGER     not null default 0,
    source_id   UUID,
    active      BOOLEAN     not null default true,
    created_at  TIMESTAMP   not null default current_timestamp,
    updated_at  TIMESTAMP   not null default current_timestamp,
    constraint uq_herd_nome unique (property_id, herd_name),
    constraint ck_herd_count check (head_count >= 0)
);



create table lot
(
    id                     UUID primary key     default gen_random_uuid(),
    herd_id                UUID        not null references herd (id) on delete restrict,
    parent_lot_id          UUID        references lot (id) on delete set null,
    lot_code               VARCHAR(20) not null,
    category               VARCHAR(30) not null,
    is_exception_lot       BOOLEAN     not null default false,
    exception_reason       VARCHAR(255),
    head_count             INTEGER     not null default 0,
    slaughter_release_date DATE,
    fit_for_slaughter      BOOLEAN     not null default true,
    open_date              DATE        not null default current_date,
    close_date             DATE,
    source_id              UUID,
    active                 BOOLEAN     not null default true,
    created_at             TIMESTAMP   not null default current_timestamp,
    updated_at             TIMESTAMP   not null default current_timestamp,
    constraint uq_lot_code unique (herd_id, lot_code),
    constraint ck_lot_datas check (close_date is null or close_date >= open_date),
    constraint ck_lot_count check (head_count >= 0),
    constraint ck_lot_nao_ciclo check (parent_lot_id is null or parent_lot_id <> id),
    constraint ck_lot_excecao check (is_exception_lot = false or parent_lot_id is not null),
    constraint ck_lot_excecao_motivo check (is_exception_lot = false or exception_reason is not null),
    constraint ck_lot_category check (category in ('ENGORDA', 'CRIA', 'RECRIA'))
);



create table animal_exception
(
    id                     UUID primary key      default gen_random_uuid(),
    lot_id                 UUID         not null references lot (id) on delete restrict,
    origin_lot_id          UUID         not null references lot (id) on delete restrict,
    ear_tag                VARCHAR(20)  not null,
    category               VARCHAR(20)  not null default 'BOI',
    birth_date             DATE,
    current_weight         NUMERIC(7, 2),
    exception_date         DATE         not null default current_date,
    exception_end_date     DATE,
    exception_reason       VARCHAR(255) not null,
    slaughter_release_date DATE,
    fit_for_slaughter      BOOLEAN      not null default true,
    status                 VARCHAR(15)  not null default 'ATIVO',
    notes                  TEXT,
    source_id              UUID,
    created_at             TIMESTAMP    not null default current_timestamp,
    updated_at             TIMESTAMP    not null default current_timestamp,
    constraint ck_animal_exception_status check (status in ('ATIVO', 'VENDIDO', 'ABATIDO', 'MORTO', 'RECUPERADO')),
    constraint ck_animal_exception_peso check (current_weight is null or current_weight > 0),
    constraint ck_animal_exception_nascto check (birth_date is null or birth_date <= current_date),
    constraint ck_animal_exception_category check (category in ('BEZERRO', 'GARROTE', 'BOI', 'TOURO')),
    constraint ck_animal_exception_fim check (exception_end_date is null or exception_end_date >= exception_date)
);
