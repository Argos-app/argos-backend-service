
create table supplier
(
    id           UUID primary key      default gen_random_uuid(),
    company_name VARCHAR(120) not null,
    cnpj         CHAR(14)     not null unique,
    email        VARCHAR(120),
    phone        VARCHAR(20),
    address      VARCHAR(255),
    source_id    UUID,
    active       BOOLEAN      not null default true,
    created_at   TIMESTAMP    not null default current_timestamp,
    updated_at   TIMESTAMP    not null default current_timestamp,
    constraint ck_supplier_cnpj check (cnpj ~ '^[0-9]{14}$')
);

create table therapeutic_category
(
    id          UUID primary key default gen_random_uuid(),
    name        VARCHAR(40) not null unique,
    name_norm   TEXT generated always as (fn_norm_texto(name)) stored,
    description TEXT,
    constraint uq_therapeutic_category_norm unique (name_norm)
);

create table medicine
(
    id                      UUID primary key        default gen_random_uuid(),
    supplier_id             UUID           not null references supplier (id) on delete restrict,
    therapeutic_category_id UUID           not null references therapeutic_category (id) on delete restrict,
    brand_name              VARCHAR(80)    not null,
    active_ingredient       VARCHAR(80)    not null,
    mapa_registration       VARCHAR(20),
    indication              TEXT           not null,
    description             TEXT,
    needs_prescription      BOOLEAN        not null default false,
    meat_withdrawal_days    INTEGER        not null default 0,
    dose                    NUMERIC(10, 3) not null,
    unit                    VARCHAR(10)    not null,
    route                   VARCHAR(20),
    source_id               UUID,
    active                  BOOLEAN        not null default true,
    created_at              TIMESTAMP      not null default current_timestamp,
    updated_at              TIMESTAMP      not null default current_timestamp,
    constraint ck_medicine_carencia check (meat_withdrawal_days >= 0),
    constraint ck_medicine_dose check (dose > 0),
    constraint ck_medicine_unit check (unit in ('ml', 'mg', 'g', 'UI', 'dose')),
    constraint ck_medicine_route check (route is null or
                                        route in ('INTRAMUSCULAR', 'SUBCUTANEA', 'ORAL', 'TOPICA', 'INTRAVENOSA'))
);

create table medicine_batch
(
    id               UUID primary key        default gen_random_uuid(),
    medicine_id      UUID           not null references medicine (id) on delete restrict,
    batch_number     VARCHAR(30)    not null,
    manufacture_date DATE           not null,
    expiry_date      DATE           not null,
    delivery_date    DATE           not null,
    initial_quantity NUMERIC(10, 3) not null,
    min_stock        NUMERIC(10, 3),
    max_stock        NUMERIC(10, 3),
    storage_place    VARCHAR(60),
    status           VARCHAR(15)    not null default 'DISPONIVEL',
    source_id        UUID,
    created_at       TIMESTAMP      not null default current_timestamp,
    updated_at       TIMESTAMP      not null default current_timestamp,
    constraint uq_medicine_batch unique (medicine_id, batch_number),
    constraint ck_medicine_batch_qt check (initial_quantity > 0),
    constraint ck_medicine_batch_dt check (expiry_date > manufacture_date),
    constraint ck_medicine_batch_st check (status in ('DISPONIVEL', 'VENCIDO', 'ESGOTADO', 'RECOLHIDO')),
    constraint ck_medicine_batch_minmax check (min_stock is null or max_stock is null or max_stock >= min_stock)
);

