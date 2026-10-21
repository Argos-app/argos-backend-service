create table permission
(
    id          UUID primary key     default gen_random_uuid(),
    code        VARCHAR(30) not null unique,
    name        VARCHAR(40) not null,
    description TEXT,
    active      BOOLEAN     not null default true,
    constraint ck_permission_code check (code = UPPER(code))
);

create table app_user
(
    id            UUID primary key      default gen_random_uuid(),
    permission_id UUID         not null references permission (id) on delete restrict,
    name          VARCHAR(120) not null,
    cpf           CHAR(11)     not null unique,
    email         VARCHAR(120) not null unique,
    firebase_uid  VARCHAR(128),

    photo_url     TEXT,

    active        BOOLEAN      not null default true,
    first_login   BOOLEAN      not null default true,
    blocked_until TIMESTAMP,
    source_id     UUID,
    created_at    TIMESTAMP    not null default current_timestamp,
    updated_at    TIMESTAMP    not null default current_timestamp,

    constraint ck_app_user_cpf check (cpf ~ '^[0-9]{11}$'),
    constraint ck_app_user_email check (email like '%_@_%._%')
);


