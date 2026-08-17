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
    active        BOOLEAN      not null default true,
    created_at    TIMESTAMP    not null default current_timestamp,
    updated_at    TIMESTAMP    not null default current_timestamp,
    constraint ck_app_user_cpf check (cpf ~ '^[0-9]{11}$'),
    constraint ck_app_user_email check (email like '%_@_%._%')
);

create table user_credential
(
    user_id             UUID primary key references app_user (id) on delete cascade,
    password_hash       VARCHAR(255) not null,
    first_login         BOOLEAN               default true,
    two_factor_on       BOOLEAN      not null default false,
    failed_attempts     INTEGER      not null default 0,
    blocked_until       TIMESTAMP,
    password_changed_at TIMESTAMP,
    updated_at          TIMESTAMP    not null default current_timestamp,
    constraint ck_user_credential_attempts check (failed_attempts >= 0)
);
