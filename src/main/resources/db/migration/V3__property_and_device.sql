create table property
(
    id            UUID primary key      default gen_random_uuid(),
    user_id       UUID         not null references app_user (id) on delete restrict,
    name          VARCHAR(120) not null,
    cnpj          CHAR(14),
    address       VARCHAR(255) not null,
    street        VARCHAR(150),
    number        VARCHAR(10),
    complement    VARCHAR(100),
    zip_code      CHAR(8),
    address_full  TEXT generated always as (COALESCE(street, '') || ', ' || COALESCE(number, 'S/N') ||
                                            case when complement is null then '' else ' - ' || complement end) stored,
    city          VARCHAR(100) not null,
    state         CHAR(2)      not null,
    area_hectares NUMERIC(10, 2),
    phone         VARCHAR(20),
    source_id     UUID,
    active        BOOLEAN      not null default true,
    created_at    TIMESTAMP    not null default current_timestamp,
    updated_at    TIMESTAMP    not null default current_timestamp,
    constraint ck_property_cnpj check (cnpj is null or cnpj ~ '^[0-9]{14}$'),
    constraint ck_property_cep check (zip_code is null or zip_code ~ '^[0-9]{8}$'),
    constraint ck_property_area check (area_hectares is null or area_hectares > 0),
    constraint ck_property_uf check (state is null or state in
                                     ('AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG',
                                      'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE',
                                      'TO'))
);


create table user_property
(
    user_id     UUID    not null references app_user (id) on delete cascade,
    property_id UUID    not null references property (id) on delete cascade,
    link_date   DATE    not null default current_date,
    unlink_date DATE,
    active      BOOLEAN not null default true,
    primary key (user_id, property_id),
    constraint ck_user_property_datas check (unlink_date is null or unlink_date >= link_date)
);

create table device
(
    id         UUID primary key      default gen_random_uuid(),
    user_id    UUID         not null references app_user (id) on delete cascade,
    push_token VARCHAR(255) not null unique,
    platform   VARCHAR(10)  not null,
    model      VARCHAR(60),
    last_sync  TIMESTAMP,
    active     BOOLEAN      not null default true,
    created_at TIMESTAMP    not null default current_timestamp,
    constraint ck_device_platform check (platform in ('ANDROID', 'IOS'))
);


-- tabelas legadas do 1o ano (rpa)

