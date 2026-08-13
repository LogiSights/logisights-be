-- Logisights core schema

create extension if not exists "uuid-ossp";

create type user_role as enum ('SENDER', 'DRIVER', 'PICKUP', 'ADMIN');
create type user_status as enum ('ACTIVE', 'SUSPENDED');
create type parcel_city as enum ('NAIROBI', 'MOMBASA', 'KISUMU');
create type parcel_status as enum ('PENDING', 'IN_TRANSIT', 'DELIVERED', 'FAILED');
create type payment_status as enum ('PENDING', 'SUCCESS', 'FAILED');
create type payment_provider as enum ('MPESA');
create type pickup_item_status as enum ('AWAITING_PICKUP', 'PICKED_UP');
create type pickup_activity_action as enum ('CHECK_IN', 'CHECK_OUT');

create table users (
    id uuid primary key default uuid_generate_v4(),
    name varchar(255) not null,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    role user_role not null,
    phone varchar(32),
    avatar_url varchar(512),
    status user_status not null default 'ACTIVE',
    email_verified_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_users_role on users(role);

create table email_verification_tokens (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(255) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);
create index idx_evt_user on email_verification_tokens(user_id);

create table password_reset_tokens (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(255) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);
create index idx_prt_user on password_reset_tokens(user_id);

create table pickup_points (
    id uuid primary key default uuid_generate_v4(),
    name varchar(255) not null,
    address varchar(512) not null,
    city parcel_city not null,
    staff_user_id uuid references users(id),
    created_at timestamptz not null default now()
);

create table parcels (
    id uuid primary key default uuid_generate_v4(),
    tracking_id varchar(32) not null unique,
    sender_id uuid not null references users(id),
    recipient_name varchar(255) not null,
    recipient_phone varchar(32) not null,
    destination_address varchar(512) not null,
    city parcel_city not null,
    weight_kg numeric(10,2) not null,
    length_cm numeric(10,2),
    width_cm numeric(10,2),
    height_cm numeric(10,2),
    parcel_type varchar(64) not null,
    pickup_point_id uuid references pickup_points(id),
    status parcel_status not null default 'PENDING',
    cost_kes numeric(12,2) not null,
    driver_id uuid references users(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_parcels_sender on parcels(sender_id);
create index idx_parcels_driver on parcels(driver_id);
create index idx_parcels_status on parcels(status);
create index idx_parcels_city on parcels(city);

create table parcel_status_history (
    id uuid primary key default uuid_generate_v4(),
    parcel_id uuid not null references parcels(id) on delete cascade,
    status parcel_status not null,
    changed_by uuid references users(id),
    note varchar(512),
    created_at timestamptz not null default now()
);
create index idx_psh_parcel_created on parcel_status_history(parcel_id, created_at);

create table pickup_inventory (
    id uuid primary key default uuid_generate_v4(),
    parcel_id uuid not null references parcels(id) on delete cascade,
    pickup_point_id uuid not null references pickup_points(id),
    date_arrived timestamptz not null default now(),
    status pickup_item_status not null default 'AWAITING_PICKUP'
);
create index idx_pi_pickup_point on pickup_inventory(pickup_point_id);

create table pickup_activity_log (
    id uuid primary key default uuid_generate_v4(),
    pickup_point_id uuid not null references pickup_points(id),
    staff_user_id uuid not null references users(id),
    parcel_id uuid not null references parcels(id),
    action pickup_activity_action not null,
    type varchar(64),
    created_at timestamptz not null default now()
);
create index idx_pal_pickup_point on pickup_activity_log(pickup_point_id);

create table driver_earnings (
    id uuid primary key default uuid_generate_v4(),
    driver_id uuid not null references users(id),
    parcel_id uuid not null references parcels(id),
    amount_kes numeric(12,2) not null,
    created_at timestamptz not null default now()
);
create index idx_de_driver on driver_earnings(driver_id);

create table payments (
    id uuid primary key default uuid_generate_v4(),
    parcel_id uuid not null references parcels(id),
    provider payment_provider not null default 'MPESA',
    provider_reference varchar(128),
    phone varchar(32) not null,
    amount_kes numeric(12,2) not null,
    status payment_status not null default 'PENDING',
    raw_callback jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_payments_parcel on payments(parcel_id);
create index idx_payments_provider_ref on payments(provider_reference);
