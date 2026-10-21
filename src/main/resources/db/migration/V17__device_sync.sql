alter table device
    alter column push_token type TEXT,
    alter column push_token drop not null,
    add column updated_at TIMESTAMP not null default current_timestamp;

alter table device add constraint ck_device_active_token
    check (not active or (push_token is not null and length(trim(push_token)) > 0));

create index idx_device_user_active on device (user_id) where active = true;
