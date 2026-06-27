package com.canyoucount.timeit.data.repository

import com.canyoucount.timeit.BuildConfig
import com.canyoucount.timeit.data.model.GameConfig
import com.canyoucount.timeit.data.model.PlayerReadyUpdate
import com.canyoucount.timeit.data.model.PlayerWinsUpdate
import com.canyoucount.timeit.data.model.RoomPlayerRow
import com.canyoucount.timeit.data.model.RoomRoundUpdate
import com.canyoucount.timeit.data.model.RoomRow
import com.canyoucount.timeit.data.model.RoundResultRow
import com.canyoucount.timeit.util.RoomCodeUtil
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow

/**
 * Isolates all Supabase calls. Room/player/result rows mirror the schema in
 * the Supabase SQL setup script (see SUPABASE_SETUP.sql at the repo root).
 */
class SupabaseRepository {

    private val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Realtime)
    }

    suspend fun createRoom(hostId: String, config: GameConfig): RoomRow {
        val code = RoomCodeUtil.generate()
        val row = RoomRow(code = code, host_id = hostId, config = config)
        return client.from("rooms")
            .insert(row) { select() }
            .decodeSingle()
    }

    suspend fun findRoomByCode(code: String): RoomRow? =
        client.from("rooms")
            .select { filter { eq("code", code) } }
            .decodeSingleOrNull()

    suspend fun joinRoom(roomId: String, playerName: String): RoomPlayerRow {
        val row = RoomPlayerRow(room_id = roomId, player_name = playerName)
        return client.from("room_players")
            .insert(row) { select() }
            .decodeSingle()
    }

    suspend fun listPlayers(roomId: String): List<RoomPlayerRow> =
        client.from("room_players")
            .select { filter { eq("room_id", roomId) } }
            .decodeList()

    suspend fun startRound(roomId: String, targetTime: Double, round: Int) {
        client.from("rooms")
            .update(
                RoomRoundUpdate(
                    status = "playing",
                    target_time = targetTime,
                    current_round = round
                )
            ) { filter { eq("id", roomId) } }
    }

    suspend fun submitResult(result: RoundResultRow) {
        client.from("round_results").insert(result)
    }

    suspend fun listResults(roomId: String, round: Int): List<RoundResultRow> =
        client.from("round_results")
            .select { filter { eq("room_id", roomId); eq("round", round) } }
            .decodeList()

    suspend fun listAllResults(roomId: String): List<RoundResultRow> =
        client.from("round_results")
            .select { filter { eq("room_id", roomId) } }
            .decodeList()

    suspend fun incrementWins(playerIds: List<String>) {
        playerIds.forEach { playerId ->
            client.from("room_players").select {
                filter { eq("id", playerId) }
            }.decodeSingleOrNull<RoomPlayerRow>()?.let { player ->
                client.from("room_players")
                    .update(PlayerWinsUpdate(wins = player.wins + 1)) { filter { eq("id", playerId) } }
            }
        }
    }

    /** Emits whenever a row in `rooms` matching [roomId] changes. Returns the channel
     *  so the caller can unsubscribe when done. */
    suspend fun observeRoom(roomId: String): Pair<RealtimeChannel, Flow<PostgresAction>> {
        val channel = client.realtime.channel("room-$roomId")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "rooms"
            filter("id", FilterOperator.EQ, roomId)
        }
        channel.subscribe()
        return channel to flow
    }

    /** Emits whenever a player joins/leaves `room_players` for [roomId]. */
    suspend fun observePlayers(roomId: String): Pair<RealtimeChannel, Flow<PostgresAction>> {
        val channel = client.realtime.channel("room-players-$roomId")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "room_players"
            filter("room_id", FilterOperator.EQ, roomId)
        }
        channel.subscribe()
        return channel to flow
    }

    /** Emits whenever a result is submitted for [roomId]. */
    suspend fun observeResults(roomId: String): Pair<RealtimeChannel, Flow<PostgresAction>> {
        val channel = client.realtime.channel("room-results-$roomId")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "round_results"
            filter("room_id", FilterOperator.EQ, roomId)
        }
        channel.subscribe()
        return channel to flow
    }

    suspend fun unsubscribeChannel(channel: RealtimeChannel) {
        runCatching { channel.unsubscribe() }
    }

    suspend fun markReady(playerId: String) {
        client.from("room_players")
            .update(PlayerReadyUpdate(ready = true)) { filter { eq("id", playerId) } }
    }

    suspend fun resetReady(roomId: String) {
        client.from("room_players")
            .update(PlayerReadyUpdate(ready = false)) { filter { eq("room_id", roomId) } }
    }

    suspend fun connectRealtime() {
        runCatching { client.realtime.disconnect() }
        client.realtime.connect()
    }

    suspend fun disconnectRealtime() {
        client.realtime.disconnect()
    }
}
