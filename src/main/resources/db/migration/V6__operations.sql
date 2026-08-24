create table application
(
    id                  UUID primary key        default gen_random_uuid(),
    lot_id              UUID           not null references lot (id) on delete restrict,
    medicine_batch_id   UUID           not null references medicine_batch (id) on delete restrict,
    user_id             UUID           not null references app_user (id) on delete restrict,
    application_date    DATE           not null default current_date,
    application_time    TIME,
    dose                NUMERIC(10, 3) not null,
    unit                VARCHAR(10)    not null,
    route               VARCHAR(20)    not null,
    head_count_applied  INTEGER        not null,
    meat_withdrawal_end DATE,
    goal                VARCHAR(120),
    notes               TEXT,
    synced_at           TIMESTAMP,
    created_at          TIMESTAMP      not null default current_timestamp,
    constraint ck_application_dose check (dose > 0),
    constraint ck_application_count check (head_count_applied > 0),
    constraint ck_application_data check (application_date <= current_date)
);

create table application_animal
(
    id                     UUID primary key        default gen_random_uuid(),
    application_id         UUID           not null references application (id) on delete cascade,
    animal_exception_id    UUID           not null references animal_exception (id) on delete restrict,
    dose_given             NUMERIC(10, 3) not null,
    weight_at_time         NUMERIC(7, 2),
    meat_withdrawal_end    DATE,
    released_for_slaughter BOOLEAN        not null default false,
    created_at             TIMESTAMP      not null default current_timestamp,
    constraint uq_application_animal unique (application_id, animal_exception_id),
    constraint ck_application_animal_dose check (dose_given > 0)
);

create table stock_movement
(
    id                UUID primary key        default gen_random_uuid(),
    medicine_batch_id UUID           not null references medicine_batch (id) on delete restrict,
    application_id    UUID           references application (id) on delete set null,
    user_id           UUID           not null references app_user (id) on delete restrict,
    type              CHAR(1)        not null,
    quantity          NUMERIC(10, 3) not null,
    movement_date     TIMESTAMP      not null default current_timestamp,
    reason            VARCHAR(60)    not null,
    created_at        TIMESTAMP      not null default current_timestamp,
    constraint ck_stock_movement_type check (type in ('E', 'S')),
    constraint ck_stock_movement_qt check (quantity > 0)
);
