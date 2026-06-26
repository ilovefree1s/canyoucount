-- TimeIt online mode schema.
-- Run this in the Supabase SQL editor for your project, then copy the
-- Project URL and anon key into local.properties as SUPABASE_URL / SUPABASE_ANON_KEY.

create table rooms (
    id uuid primary key default gen_random_uuid(),
    code text not null unique,
    host_id uuid not null,
    status text not null default 'waiting' check (status in ('waiting', 'playing', 'finished')),
    config jsonb not null,
    target_time float8,
    current_round int not null default 1,
    created_at timestamptz not null default now()
);

create table room_players (
    id uuid primary key default gen_random_uuid(),
    room_id uuid not null references rooms(id) on delete cascade,
    player_name text not null,
    wins int not null default 0,
    joined_at timestamptz not null default now()
);

create table round_results (
    id uuid primary key default gen_random_uuid(),
    room_id uuid not null references rooms(id) on delete cascade,
    round int not null,
    player_id uuid not null references room_players(id) on delete cascade,
    player_time float8 not null,
    delta float8 not null,
    submitted_at timestamptz not null default now()
);

-- Enable Realtime on the tables clients need to subscribe to.
alter publication supabase_realtime add table rooms;
alter publication supabase_realtime add table room_players;
alter publication supabase_realtime add table round_results;

-- v1 ships with permissive RLS suitable for a party game with no auth.
-- Tighten this if you later add accounts/auth.
alter table rooms enable row level security;
alter table room_players enable row level security;
alter table round_results enable row level security;

create policy "rooms_all" on rooms for all using (true) with check (true);
create policy "room_players_all" on room_players for all using (true) with check (true);
create policy "round_results_all" on round_results for all using (true) with check (true);
