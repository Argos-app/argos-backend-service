
create table data_catalog
(
    id               UUID primary key     default gen_random_uuid(),
    table_name       VARCHAR(60) not null,
    column_name      VARCHAR(60) not null,
    data_type        VARCHAR(30) not null,
    description      TEXT        not null,
    business_rule    TEXT,
    access_level     VARCHAR(15) not null default 'INTERNO',
    personal_data    BOOLEAN     not null default false,
    lgpd_legal_basis VARCHAR(60),
    updated_at       TIMESTAMP   not null default current_timestamp,
    constraint uq_data_catalog unique (table_name, column_name),
    constraint ck_data_catalog_nivel check (access_level in ('PUBLICO', 'INTERNO', 'RESTRITO', 'SENSIVEL')),
    constraint ck_data_catalog_lgpd check (personal_data = false or lgpd_legal_basis is not null)
);


