create table attachment
(
    id         UUID primary key      default gen_random_uuid(),
    user_id    UUID         not null references app_user (id) on delete restrict,
    entity     VARCHAR(30)  not null,
    entity_id  UUID         not null,
    type       VARCHAR(20)  not null,
    file_path  VARCHAR(255) not null,
    file_hash  CHAR(64)     not null,
    size_bytes INTEGER      not null,
    created_at TIMESTAMP    not null default current_timestamp,
    constraint ck_attachment_type check (type in ('FOTO_BRINCO', 'BULA', 'NOTA_FISCAL', 'LAUDO', 'RECEITA')),
    constraint ck_attachment_size check (size_bytes > 0)
);

-- auditoria e monitoramento de carga

create table change_audit
(
    id          UUID primary key     default gen_random_uuid(),
    table_name  VARCHAR(60) not null,
    operation   CHAR(6)     not null,
    record_id   VARCHAR(40) not null,
    old_data    JSONB,
    new_data    JSONB,
    db_user     VARCHAR(60) not null,
    app_user_id UUID,
    ip_address  INET,
    happened_at TIMESTAMP   not null default current_timestamp,
    constraint ck_change_audit_op check (operation in ('INSERT', 'UPDATE', 'DELETE'))
);

create table access_audit
(
    id             UUID primary key      default gen_random_uuid(),
    user_id        UUID         references app_user (id) on delete set null,
    login_tried    VARCHAR(120) not null,
    success        BOOLEAN      not null,
    failure_reason VARCHAR(60),
    source         VARCHAR(10)  not null default 'MOBILE',
    ip_address     INET,
    user_agent     VARCHAR(255),
    happened_at    TIMESTAMP    not null default current_timestamp,
    constraint ck_access_audit_motivo check (
        failure_reason is null or failure_reason in
                                  ('SENHA_ERRADA', 'CONTA_BLOQUEADA', 'USUARIO_INEXISTENTE', 'TOKEN_INVALIDO')
        ),
    constraint ck_access_audit_source check (source in ('WEB', 'MOBILE', 'API'))
);


create table interaction_audit
(
    id                  UUID primary key     default gen_random_uuid(),
    user_id             UUID        not null references app_user (id) on delete cascade,
    device_id           UUID        references device (id) on delete set null,
    action_type         VARCHAR(20) not null,
    element_type        VARCHAR(20) not null,
    element_id          VARCHAR(60) not null,
    element_name        VARCHAR(60) not null,
    element_description VARCHAR(120),
    created_at          TIMESTAMP   not null default current_timestamp,
    constraint ck_interaction_action check (action_type in ('CLIQUE', 'ENTRAR_TELA', 'SAIR_TELA', 'OCULTAR', 'SCROLL')),
    constraint ck_interaction_element check (element_type in ('BOTAO', 'ACTIVITY', 'FRAGMENT', 'CAMPO', 'LISTA'))
);

create table daily_access
(
    id               UUID primary key     default gen_random_uuid(),
    user_id          UUID        not null references app_user (id) on delete cascade,
    date             DATE        not null default current_date,
    source           VARCHAR(10) not null,
    session_count    INTEGER     not null default 1,
    first_login_time TIMESTAMP   not null default current_timestamp,
    constraint uq_daily_access unique (user_id, date, source),
    constraint ck_daily_access_src check (source in ('WEB', 'MOBILE', 'API'))
);
