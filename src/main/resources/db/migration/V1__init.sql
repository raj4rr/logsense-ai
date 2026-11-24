create table if not exists log_source (
 id uuid primary key, name varchar(120) not null unique, type varchar(40) not null,
 enabled boolean not null default false, config jsonb not null default '{}'::jsonb,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table if not exists log_event (
 id bigserial primary key, source_id uuid references log_source(id), source_name varchar(120) not null,
 service_name varchar(200), environment varchar(80), severity varchar(20) not null,
 occurred_at timestamptz not null, ingested_at timestamptz not null default now(),
 message text not null, logger varchar(300), trace_id varchar(128), span_id varchar(128),
 request_id varchar(128), host varchar(255), attributes jsonb not null default '{}'::jsonb,
 fingerprint varchar(128) not null
);
create index if not exists idx_log_event_time on log_event(occurred_at desc);
create index if not exists idx_log_event_source_time on log_event(source_name, occurred_at desc);
create index if not exists idx_log_event_trace on log_event(trace_id) where trace_id is not null;
create index if not exists idx_log_event_severity on log_event(severity, occurred_at desc);
create table if not exists audit_event (
 id bigserial primary key, actor varchar(200) not null, action varchar(120) not null,
 resource varchar(300), outcome varchar(30) not null, details jsonb not null default '{}'::jsonb,
 created_at timestamptz not null default now()
);
