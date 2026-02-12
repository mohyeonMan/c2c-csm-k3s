package com.c2c.csm.infrastructure.registry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.c2c.csm.common.util.IdGenerator;
import com.c2c.csm.infrastructure.registry.dto.Room;
import com.c2c.csm.infrastructure.registry.dto.RoomEntry;
import com.c2c.csm.infrastructure.registry.dto.RoomSummary;

import lombok.RequiredArgsConstructor;

/**
 * Redis 기반 방 레지스트리.
 * - port 구현 없이 내부 레지스트리 용도로 사용.
 * - 다중 연산이 필요한 곳은 Lua 스크립트로 원자성을 보장한다.
 */
@Component
@RequiredArgsConstructor
public class RoomRegistry {
	private static final String DEFAULT_ROOM_PREFIX = "room:";
	private static final String DEFAULT_USER_PREFIX = "user:";
	private static final String ALL_ROOMS_KEY = "rooms:all";
	private static final String META_SUFFIX = ":meta";
	private static final String MEMBERS_SUFFIX = ":members";
	private static final String ONLINE_SUFFIX = ":online";
	private static final String LAST_TOUCH_SUFFIX = ":lastTouch";
	private static final String APPROVED_SUFFIX = ":approved";
	private static final String ROOMS_SUFFIX = ":rooms";
	private static final String JOIN_APPROVE_PREFIX = "join:approve:";
	private static final int CLEANUP_BATCH_SIZE = 100;

	private final StringRedisTemplate redisTemplate;

	@Value("${c2c.join.approve-ttl:24h}")
	private Duration joinApproveTtl;

	@Value("${c2c.room.auto-delete-ttl:72h}")
	private Duration roomAutoDeleteTtl;

	private static final DefaultRedisScript<Long> CREATE_ROOM_SCRIPT = script("""
			if redis.call('EXISTS', KEYS[1]) == 1 then
			  return 0
			end
			redis.call('HSET', KEYS[1], 'ownerId', ARGV[1], 'createdAt', ARGV[2])
			return 1
			""");

	private static final DefaultRedisScript<Long> SAVE_JOIN_APPROVE_SCRIPT = script("""
			if redis.call('EXISTS', KEYS[1]) == 0 then
			  return 0
			end
			redis.call('SET', KEYS[2], '1', 'EX', ARGV[1])
			redis.call('SADD', KEYS[3], ARGV[2])
			return 1
			""");

	private static final DefaultRedisScript<Long> ADD_MEMBER_WITH_NICKNAME_SCRIPT = script("""
			if redis.call('EXISTS', KEYS[3]) == 0 then
			  return 0
			end
			redis.call('SADD', KEYS[1], ARGV[1])
			redis.call('SADD', KEYS[2], ARGV[2])
			redis.call('SET', KEYS[4], ARGV[3])
			return 1
			""");

	private static final DefaultRedisScript<Long> REMOVE_MEMBER_SCRIPT = script("""
			if redis.call('EXISTS', KEYS[1]) == 0 then
			  return 0
			end
			local ownerId = redis.call('HGET', KEYS[3], 'ownerId')
			redis.call('SREM', KEYS[1], ARGV[1])
			redis.call('SREM', KEYS[2], ARGV[2])
			redis.call('SREM', KEYS[6], ARGV[1])
			redis.call('DEL', KEYS[4])
			if redis.call('SCARD', KEYS[1]) == 0 then
			  redis.call('DEL', KEYS[1])
			  redis.call('DEL', KEYS[3])
			  redis.call('DEL', KEYS[5])
			  redis.call('DEL', KEYS[6])
			  redis.call('DEL', KEYS[7])
			  redis.call('SREM', KEYS[8], ARGV[4])
			  return 1
			end
			if ownerId == ARGV[1] then
			  local newOwner = redis.call('SRANDMEMBER', KEYS[1])
			  if newOwner then
				redis.call('HSET', KEYS[3], 'ownerId', newOwner)
			  end
			end
			return 1
			""");

	// 방 생성 (메타만 기록)
	public Optional<Room> createRoom(String ownerId) {
		if (ownerId == null || ownerId.isBlank()) {
			return Optional.empty();
		}
		Room room = Room.builder()
						.ownerId(ownerId)
						.roomId(IdGenerator.generateId("room"))
						.createdAt(Instant.now())
						.build();
						
		Long result = redisTemplate.execute(
			CREATE_ROOM_SCRIPT,
			List.of(roomMetaKey(room.getRoomId())),
			room.getOwnerId(),
			Long.toString(room.getCreatedAt().toEpochMilli())
		);

		if(isSuccess(result)){
			saveJoinApproveToken(room.getRoomId(), ownerId);
			touchRoom(room.getRoomId(), room.getCreatedAt());
			redisTemplate.opsForSet().add(ALL_ROOMS_KEY, room.getRoomId());
			return Optional.of(room);
		}
		else return Optional.empty();
	}

	// 방 소유자 조회
	public Optional<String> findOwnerId(String roomId) {
		if (roomId == null || roomId.isBlank()) {
			return Optional.empty();
		}
		Object ownerId = redisTemplate.opsForHash().get(roomMetaKey(roomId), "ownerId");
		if (ownerId == null) {
			return Optional.empty();
		}
		String value = ownerId.toString();
		return value.isBlank() ? Optional.empty() : Optional.of(value);
	}

	public Optional<RoomSummary> getRoomSummary(String roomId) {
		if (roomId == null || roomId.isBlank()) {
			return Optional.empty();
		}
		Optional<String> ownerId = findOwnerId(roomId);
		if (ownerId.isEmpty()) {
			return Optional.empty();
		}
		Set<String> members = findMembers(roomId);
		Set<String> onlineMembers = findOnlineMembers(roomId);
		Instant lastTouch = findLastTouch(roomId).orElse(null);
		Instant autoDeleteAt = calculateAutoDeleteAt(lastTouch);
		List<String> memberIds = members.stream()
			.filter(memberId -> memberId != null && !memberId.isBlank())
			.sorted(Comparator.nullsLast(String::compareTo))
			.toList();
		Map<String, String> nicknameByUserId = readMemberNicknames(roomId, memberIds);
		List<RoomEntry> entries = memberIds.stream()
			.map(memberId -> RoomEntry.builder()
				.userId(memberId)
				.nickname(nicknameByUserId.get(memberId))
				.online(onlineMembers.contains(memberId))
				.build())
			.toList();

		RoomSummary summary = RoomSummary.builder()
			.roomId(roomId)
			.ownerId(ownerId.get())
			.entries(entries)
			.autoDeleteAt(autoDeleteAt)
			.build();

		return Optional.of(summary);
	}

	// 참여 승인 토큰 저장 (방 존재 확인 후 TTL 적용)
	public boolean saveJoinApproveToken(String roomId, String userId) {
		if (roomId == null || roomId.isBlank() || userId == null || userId.isBlank()) {
			return false;
		}
		if (joinApproveTtl == null || joinApproveTtl.isZero() || joinApproveTtl.isNegative()) {
			return false;
		}
		Long result = redisTemplate.execute(
			SAVE_JOIN_APPROVE_SCRIPT,
			List.of(roomMetaKey(roomId), joinApproveKey(roomId, userId), roomApprovedKey(roomId)),
			Long.toString(joinApproveTtl.toSeconds()),
			userId
		);
		return isSuccess(result);
	}

	// 참여 승인 토큰 확인
	public boolean hasJoinApproveToken(String roomId, String userId) {
		if (roomId == null || roomId.isBlank() || userId == null || userId.isBlank()) {
			return false;
		}
		Boolean exists = redisTemplate.hasKey(joinApproveKey(roomId, userId));
		return Boolean.TRUE.equals(exists);
	}

	// 참여 승인 토큰 폐기
	public void revokeJoinApproveToken(String roomId, String userId) {
		if (roomId == null || roomId.isBlank() || userId == null || userId.isBlank()) {
			return;
		}
		redisTemplate.delete(joinApproveKey(roomId, userId));
		redisTemplate.opsForSet().remove(roomApprovedKey(roomId), userId);
	}

	// 멤버 닉네임 조회 (방-유저 단위 저장)
	public Optional<String> findMemberNickname(String roomId, String userId) {
		if (roomId == null || roomId.isBlank() || userId == null || userId.isBlank()) {
			return Optional.empty();
		}
		String nickname = redisTemplate.opsForValue().get(roomUserNicknameKey(roomId, userId));
		return nickname == null || nickname.isBlank() ? Optional.empty() : Optional.of(nickname);
	}

	// 멤버 추가 + 닉네임 저장 (원자적)
	public boolean addMemberWithNickname(String roomId, String userId, String nickname) {
		if (roomId == null || roomId.isBlank()
				|| userId == null || userId.isBlank()
				|| nickname == null || nickname.isBlank()) {
			return false;
		}
		Long result = redisTemplate.execute(
			ADD_MEMBER_WITH_NICKNAME_SCRIPT,
			List.of(roomMembersKey(roomId), userRoomsKey(userId), roomMetaKey(roomId), roomUserNicknameKey(roomId, userId)),
			userId,
			roomId,
			nickname
		);
		return isSuccess(result);
	}

	// 멤버 제거 (방 비면 정리 + 소유자 변경)
	public boolean removeMember(String roomId, String userId) {
		if (roomId == null || roomId.isBlank() || userId == null || userId.isBlank()) {
			return false;
		}
		Long result = redisTemplate.execute(
			REMOVE_MEMBER_SCRIPT,
			List.of(
				roomMembersKey(roomId),
				userRoomsKey(userId),
				roomMetaKey(roomId),
				roomUserNicknameKey(roomId, userId),
				roomApprovedKey(roomId),
				roomOnlineKey(roomId),
				roomLastTouchKey(roomId),
				ALL_ROOMS_KEY
			),
			userId,
			roomId,
			joinApprovePrefix(roomId),
			roomId
		);
		return isSuccess(result);
	}

	// 방 멤버 목록 조회
	public Set<String> findMembers(String roomId) {
		if (roomId == null || roomId.isBlank()) {
			return Collections.emptySet();
		}
		Set<String> members = redisTemplate.opsForSet().members(roomMembersKey(roomId));
		return members == null ? Collections.emptySet() : members;
	}

	public Set<String> findOnlineMembers(String roomId) {
		if (roomId == null || roomId.isBlank()) {
			return Collections.emptySet();
		}
		Set<String> members = redisTemplate.opsForSet().members(roomOnlineKey(roomId));
		return members == null ? Collections.emptySet() : members;
	}

	public boolean markOnline(String roomId, String userId) {
		if (roomId == null || roomId.isBlank() || userId == null || userId.isBlank()) {
			return false;
		}
		Long result = redisTemplate.opsForSet().add(roomOnlineKey(roomId), userId);
		touchRoom(roomId, Instant.now());
		return result != null && result > 0;
	}

	public boolean markOffline(String roomId, String userId) {
		if (roomId == null || roomId.isBlank() || userId == null || userId.isBlank()) {
			return false;
		}
		Long result = redisTemplate.opsForSet().remove(roomOnlineKey(roomId), userId);
		touchRoom(roomId, Instant.now());
		return result != null && result > 0;
	}

	// 유저가 속한 방 목록 조회
	public Set<String> findRooms(String userId) {
		if (userId == null || userId.isBlank()) {
			return Collections.emptySet();
		}
		Set<String> rooms = redisTemplate.opsForSet().members(userRoomsKey(userId));
		return rooms == null ? Collections.emptySet() : rooms;
	}

	// 멤버 여부 확인
	public boolean isMember(String roomId, String userId) {
		if (roomId == null || roomId.isBlank() || userId == null || userId.isBlank()) {
			return false;
		}
		Boolean member = redisTemplate.opsForSet().isMember(roomMembersKey(roomId), userId);
		return Boolean.TRUE.equals(member);
	}

	// 방 삭제 (멤버 역참조/승인 토큰 정리 포함)
	public void deleteRoom(String roomId) {
		if (roomId == null || roomId.isBlank()) {
			return;
		}
		cleanupMemberReferences(roomId);
		cleanupJoinApproveTokens(roomId);
		redisTemplate.delete(List.of(
			roomMembersKey(roomId),
			roomMetaKey(roomId),
			roomApprovedKey(roomId),
			roomOnlineKey(roomId),
			roomLastTouchKey(roomId)
		));
		redisTemplate.opsForSet().remove(ALL_ROOMS_KEY, roomId);
	}

	private String roomMetaKey(String roomId) {
		return DEFAULT_ROOM_PREFIX + roomId + META_SUFFIX;
	}

	private String roomMembersKey(String roomId) {
		return DEFAULT_ROOM_PREFIX + roomId + MEMBERS_SUFFIX;
	}

	private String roomOnlineKey(String roomId) {
		return DEFAULT_ROOM_PREFIX + roomId + ONLINE_SUFFIX;
	}

	private String roomLastTouchKey(String roomId) {
		return DEFAULT_ROOM_PREFIX + roomId + LAST_TOUCH_SUFFIX;
	}

	private String roomApprovedKey(String roomId) {
		return DEFAULT_ROOM_PREFIX + roomId + APPROVED_SUFFIX;
	}

	private String joinApproveKey(String roomId, String userId) {
		return DEFAULT_ROOM_PREFIX + JOIN_APPROVE_PREFIX + roomId + ":" + userId;
	}

	private String joinApprovePrefix(String roomId) {
		return DEFAULT_ROOM_PREFIX + JOIN_APPROVE_PREFIX + roomId + ":";
	}

	private String userRoomsKey(String userId) {
		return DEFAULT_USER_PREFIX + userId + ROOMS_SUFFIX;
	}

	private String roomUserNicknameKey(String roomId, String userId) {
		return DEFAULT_ROOM_PREFIX + roomId + ":user:" + userId + ":nickname";
	}

	private static DefaultRedisScript<Long> script(String text) {
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setResultType(Long.class);
		script.setScriptText(text);
		return script;
	}

	private boolean isSuccess(Long result) {
		return result != null && result > 0;
	}

	private void touchRoom(String roomId, Instant at) {
		if (roomId == null || roomId.isBlank() || at == null) {
			return;
		}
		redisTemplate.opsForValue().set(roomLastTouchKey(roomId), Long.toString(at.toEpochMilli()));
	}

	private Optional<Instant> findLastTouch(String roomId) {
		if (roomId == null || roomId.isBlank()) {
			return Optional.empty();
		}
		String value = redisTemplate.opsForValue().get(roomLastTouchKey(roomId));
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		try {
			long epochMilli = Long.parseLong(value);
			return Optional.of(Instant.ofEpochMilli(epochMilli));
		} catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

	private Map<String, String> readMemberNicknames(String roomId, List<String> memberIds) {
		if (memberIds == null || memberIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<String> nicknameKeys = memberIds.stream()
			.map(memberId -> roomUserNicknameKey(roomId, memberId))
			.toList();
		List<String> nicknames = redisTemplate.opsForValue().multiGet(nicknameKeys);
		if (nicknames == null || nicknames.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, String> nicknameByUserId = new HashMap<>();
		for (int i = 0; i < memberIds.size(); i++) {
			String nickname = i < nicknames.size() ? nicknames.get(i) : null;
			if (nickname == null || nickname.isBlank()) {
				continue;
			}
			nicknameByUserId.put(memberIds.get(i), nickname);
		}
		return nicknameByUserId;
	}

	private void cleanupMemberReferences(String roomId) {
		String membersKey = roomMembersKey(roomId);
		ScanOptions options = ScanOptions.scanOptions().count(CLEANUP_BATCH_SIZE).build();
		try (Cursor<String> cursor = redisTemplate.opsForSet().scan(membersKey, options)) {
			List<String> batch = new ArrayList<>(CLEANUP_BATCH_SIZE);
			while (cursor.hasNext()) {
				String memberId = cursor.next();
				if (memberId == null || memberId.isBlank()) {
					continue;
				}
				batch.add(memberId);
				if (batch.size() >= CLEANUP_BATCH_SIZE) {
					removeMemberReferencesBatch(roomId, batch);
					batch.clear();
				}
			}
			if (!batch.isEmpty()) {
				removeMemberReferencesBatch(roomId, batch);
			}
		}
	}

	private void removeMemberReferencesBatch(String roomId, List<String> memberIds) {
		for (String memberId : memberIds) {
			redisTemplate.opsForSet().remove(userRoomsKey(memberId), roomId);
			redisTemplate.delete(roomUserNicknameKey(roomId, memberId));
		}
	}

	private void cleanupJoinApproveTokens(String roomId) {
		String approvedKey = roomApprovedKey(roomId);
		ScanOptions options = ScanOptions.scanOptions().count(CLEANUP_BATCH_SIZE).build();
		try (Cursor<String> cursor = redisTemplate.opsForSet().scan(approvedKey, options)) {
			List<String> tokenKeys = new ArrayList<>(CLEANUP_BATCH_SIZE);
			while (cursor.hasNext()) {
				String userId = cursor.next();
				if (userId == null || userId.isBlank()) {
					continue;
				}
				tokenKeys.add(joinApproveKey(roomId, userId));
				if (tokenKeys.size() >= CLEANUP_BATCH_SIZE) {
					redisTemplate.delete(tokenKeys);
					tokenKeys.clear();
				}
			}
			if (!tokenKeys.isEmpty()) {
				redisTemplate.delete(tokenKeys);
			}
		}
	}

	private Instant calculateAutoDeleteAt(Instant lastTouch) {
		if (lastTouch == null) {
			return null;
		}
		if (roomAutoDeleteTtl == null || roomAutoDeleteTtl.isZero() || roomAutoDeleteTtl.isNegative()) {
			return null;
		}
		return lastTouch.plus(roomAutoDeleteTtl);
	}

	public Set<String> findAllRooms() {
		Set<String> rooms = redisTemplate.opsForSet().members(ALL_ROOMS_KEY);
		return rooms == null ? Collections.emptySet() : rooms;
	}

	public Optional<Instant> findAutoDeleteAt(String roomId) {
		Instant lastTouch = findLastTouch(roomId).orElse(null);
		return Optional.ofNullable(calculateAutoDeleteAt(lastTouch));
	}

	public boolean isAutoDeleteDue(String roomId, Instant now) {
		if (now == null) {
			return false;
		}
		Instant autoDeleteAt = findAutoDeleteAt(roomId).orElse(null);
		return autoDeleteAt != null && !autoDeleteAt.isAfter(now);
	}
}
