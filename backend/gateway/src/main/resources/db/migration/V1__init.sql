create table users (
    id bigint primary key auto_increment,
    version bigint,
    created_at timestamp(6) not null default current_timestamp(6),
    updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    email varchar(190) not null,
    full_name varchar(120) not null,
    password_hash varchar(255) not null,
    role varchar(32) not null,
    enabled boolean not null default true,
    failed_attempts int not null default 0,
    locked_until timestamp(6) null,
    device_fingerprint varchar(160) null,
    constraint uk_users_email unique (email),
    constraint chk_users_failed_attempts check (failed_attempts >= 0)
);

create index idx_users_email on users(email);
create index idx_users_role on users(role);

create table refresh_tokens (
    id bigint primary key auto_increment,
    version bigint,
    created_at timestamp(6) not null default current_timestamp(6),
    updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    user_id bigint not null,
    token_hash varchar(96) not null,
    revoked boolean not null default false,
    expires_at timestamp(6) not null,
    rotated_to_hash varchar(96) null,
    device_fingerprint varchar(160) null,
    constraint uk_refresh_token_hash unique (token_hash),
    constraint fk_refresh_user foreign key (user_id) references users(id) on delete cascade
);

create index idx_refresh_token_hash on refresh_tokens(token_hash);
create index idx_refresh_user on refresh_tokens(user_id);

create table audit_log (
    id bigint primary key auto_increment,
    version bigint,
    created_at timestamp(6) not null default current_timestamp(6),
    updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    actor_email varchar(190) null,
    action varchar(80) not null,
    target varchar(190) null,
    ip_address varchar(80) null,
    user_agent varchar(256) null,
    status varchar(40) not null,
    details varchar(1000) null
);

create index idx_audit_actor on audit_log(actor_email);
create index idx_audit_action on audit_log(action);

create table import_jobs (
    id bigint primary key auto_increment,
    version bigint,
    created_at timestamp(6) not null default current_timestamp(6),
    updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    user_id bigint not null,
    file_name varchar(180) not null,
    total_rows int not null default 0,
    success_rows int not null default 0,
    failed_rows int not null default 0,
    status varchar(24) not null,
    row_errors longtext null,
    constraint fk_import_user foreign key (user_id) references users(id) on delete cascade,
    constraint chk_import_counts check (total_rows >= 0 and success_rows >= 0 and failed_rows >= 0)
);

create table transactions (
    id bigint primary key auto_increment,
    version bigint,
    created_at timestamp(6) not null default current_timestamp(6),
    updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    user_id bigint not null,
    type varchar(24) not null,
    category varchar(80) not null,
    merchant varchar(140) not null,
    amount decimal(15,2) not null,
    occurred_at datetime(6) not null,
    description varchar(500) null,
    idempotency_key varchar(120) null,
    import_job_id bigint null,
    constraint fk_transactions_user foreign key (user_id) references users(id) on delete cascade,
    constraint fk_transactions_import_job foreign key (import_job_id) references import_jobs(id) on delete set null,
    constraint uk_tx_idempotency unique (user_id, idempotency_key),
    constraint chk_transactions_amount check (amount > 0)
);

create index idx_tx_user_time on transactions(user_id, occurred_at);
create index idx_tx_category on transactions(category);
create index idx_tx_merchant on transactions(merchant);

create table budgets (
    id bigint primary key auto_increment,
    version bigint,
    created_at timestamp(6) not null default current_timestamp(6),
    updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    user_id bigint not null,
    month_key varchar(7) not null,
    category varchar(80) not null,
    limit_amount decimal(15,2) not null,
    spent_amount decimal(15,2) not null default 0,
    active boolean not null default true,
    constraint fk_budget_user foreign key (user_id) references users(id) on delete cascade,
    constraint uk_budget_user_month_category unique (user_id, month_key, category),
    constraint chk_budget_amounts check (limit_amount >= 0 and spent_amount >= 0)
);

create index idx_budget_user_month on budgets(user_id, month_key);

create table alerts (
    id bigint primary key auto_increment,
    version bigint,
    created_at timestamp(6) not null default current_timestamp(6),
    updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    user_id bigint not null,
    budget_id bigint null,
    severity varchar(24) not null,
    message varchar(500) not null,
    read_flag boolean not null default false,
    constraint fk_alert_user foreign key (user_id) references users(id) on delete cascade,
    constraint fk_alert_budget foreign key (budget_id) references budgets(id) on delete set null
);

create index idx_alert_user_read on alerts(user_id, read_flag);

create table goals (
    id bigint primary key auto_increment,
    version bigint,
    created_at timestamp(6) not null default current_timestamp(6),
    updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    user_id bigint not null,
    name varchar(120) not null,
    category varchar(80) not null,
    target_amount decimal(15,2) not null,
    current_amount decimal(15,2) not null default 0,
    deadline date not null,
    completed boolean not null default false,
    constraint fk_goal_user foreign key (user_id) references users(id) on delete cascade,
    constraint chk_goal_amounts check (target_amount > 0 and current_amount >= 0)
);

create index idx_goal_user_deadline on goals(user_id, deadline);

create table scheduler_audit (
    id bigint primary key auto_increment,
    version bigint,
    created_at timestamp(6) not null default current_timestamp(6),
    updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    job_name varchar(100) not null,
    status varchar(24) not null,
    message varchar(1000) null,
    started_at timestamp(6) not null,
    completed_at timestamp(6) not null
);
