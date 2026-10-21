create table load_run
(
    id            UUID primary key     default gen_random_uuid(),
    routine_name  VARCHAR(60) not null,
    source        VARCHAR(40) not null,
    start_time    TIMESTAMP   not null default current_timestamp,
    end_time      TIMESTAMP,
    rows_read     INTEGER     not null default 0,
    rows_inserted INTEGER     not null default 0,
    rows_updated  INTEGER     not null default 0,
    rows_rejected INTEGER     not null default 0,
    status        VARCHAR(15) not null default 'EXECUTANDO',
    loaded_until  TIMESTAMP,
    message       TEXT,
    constraint ck_load_run_status check (status in ('EXECUTANDO', 'SUCESSO', 'ERRO', 'PARCIAL'))
);


create table load_error
(
    id               UUID primary key      default gen_random_uuid(),
    load_run_id      UUID         not null references load_run (id) on delete cascade,
    source_table     VARCHAR(60)  not null,
    natural_key      VARCHAR(120) not null,
    original_content JSONB        not null,
    error_message    TEXT         not null,
    attempts         INTEGER      not null default 1,
    reprocessed      BOOLEAN      not null default false
);


create table load_mapping
(
    id          UUID primary key      default gen_random_uuid(),
    table_name  VARCHAR(60)  not null,
    source_id   VARCHAR(40)  not null,
    target_id   UUID         not null,
    natural_key VARCHAR(120) not null,
    load_date   TIMESTAMP    not null default current_timestamp,
    constraint uq_load_mapping unique (table_name, source_id)
);


create table load_staging
(
    id             UUID primary key   default gen_random_uuid(),
    load_run_id    UUID      not null references load_run (id) on delete cascade,
    source_columns TEXT      not null,
    row_hash       CHAR(64)  not null,
    processed      BOOLEAN   not null default false,
    extract_date   TIMESTAMP not null default current_timestamp,
    constraint uq_load_staging unique (load_run_id, row_hash)
);




insert into therapeutic_category (name, description)
values ('Antibiótico', 'Combate infeccao bacteriana'),
       ('Anti-inflamatório', 'Reduz inflamacao, dor e febre'),
       ('Antiparasitário', 'Endo e ectoparasitas'),
       ('Vacina', 'Imunizacao preventiva'),
       ('Vitamínico', 'Suplementacao vitaminica e mineral'),
       ('Hormonal', 'Protocolo reprodutivo'),
       ('Anestésico', 'Sedacao e anestesia'),
       ('Cicatrizante', 'Tratamento topico de ferimento'),
       ('Ectoparasiticida', 'Carrapato, mosca e outros ectoparasitas'),
       ('Suplemento Mineral', 'Reposicao de minerais'),
       ('Não informado', 'Fallback da carga: grafia de origem nao reconhecida, revisar')
on conflict (name) do nothing;
insert into permission (code, name, description)
values ('ADMIN', 'Administrador', 'Acesso total, sem restricao de escopo'),
       ('GERENTE', 'Gerente', 'Gerencia as fazendas vinculadas'),
       ('PECUARISTA', 'Pecuarista', 'Dono da fazenda'),
       ('APLICADOR', 'Aplicador', 'Registra aplicacao no campo'),
       ('SISTEMA', 'Sistema', 'Usuario tecnico da RPA, nao faz login')
on conflict (code) do nothing;

insert into app_user (permission_id, name, cpf, email, firebase_uid, first_login)
select p.id, 'RPA Argos', '00000000000', 'rpa@argos.local', 'rpa-sistema', false
from permission p
where p.code = 'SISTEMA'
on conflict (cpf) do nothing;

