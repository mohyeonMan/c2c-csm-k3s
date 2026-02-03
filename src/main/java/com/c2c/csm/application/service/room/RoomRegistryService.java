package com.c2c.csm.application.service.room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.c2c.csm.infrastructure.registry.RoomRegistry;
import com.c2c.csm.infrastructure.registry.dto.RoomSummary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomRegistryService {
    private final RoomRegistry roomRegistry;

    public record JoinResult(RoomSummary summary, Map<String, Object> notifyPayload) {}
    public record JoinRequestResult(boolean directApprove, String targetUserId, Map<String, Object> payload) {}
    public record LeaveResult(String roomId, Map<String, Object> notifyPayload, Set<String> remainingMembers) {}
    public record LeaveAllResult(Set<String> rooms, List<LeaveResult> results) {}
    public record PresenceResult(String roomId, Map<String, Object> notifyPayload, Set<String> onlineMembers) {}
    public record PresenceAllResult(Set<String> rooms, List<PresenceResult> results) {}

    public JoinResult joinRoom(String roomId, String userId, String nickname) {
        if (!roomRegistry.hasJoinApproveToken(roomId, userId)) {
            throw new RuntimeException("입장권한 없음.");
        }
        if (roomRegistry.isMember(roomId, userId)) {
            throw new RuntimeException("이미 join한 상태.");
        }
        boolean joined = roomRegistry.addMemberWithNickname(roomId, userId, nickname);
        if (!joined) {
            throw new RuntimeException("입장 실패.");
        }

        RoomSummary summary = roomRegistry.getRoomSummary(roomId)
            .orElseThrow(RuntimeException::new);

        Map<String, Object> notifyPayload = Map.of(
            "userId", userId,
            "nickname", nickname
        );

        return new JoinResult(summary, notifyPayload);
    }

    public JoinRequestResult prepareJoinRequest(String roomId, String requestedUserId, String nickname) {
        if (roomRegistry.hasJoinApproveToken(roomId, requestedUserId)) {
            return new JoinRequestResult(
                true,
                requestedUserId,
                Map.of(
                    "requestedUserId", requestedUserId,
                    "roomId", roomId,
                    "approved", true
                )
            );
        }

        String ownerId = roomRegistry.findOwnerId(roomId)
            .orElseThrow(() -> new RuntimeException(" 방을 찾을 수 없음."));
        if (ownerId.equals(requestedUserId)) {
            return new JoinRequestResult(
                true,
                requestedUserId,
                Map.of(
                    "requestedUserId", requestedUserId,
                    "roomId", roomId,
                    "approved", true
                )
            );
        }

        Map<String, Object> payload = Map.of(
            "requestedUserId", requestedUserId,
            "nickname", nickname,
            "roomId", roomId
        );

        return new JoinRequestResult(false, ownerId, payload);
    }

    public Map<String, Object> approveJoin(String roomId, String ownerId, String requestedUserId, boolean approved) {
        String actualOwnerId = roomRegistry.findOwnerId(roomId)
            .orElseThrow(() -> new RuntimeException("방을 찾을 수 없음."));
        if (!actualOwnerId.equals(ownerId)) {
            throw new RuntimeException("방장 아님.");
        }

        if (approved) {
            roomRegistry.saveJoinApproveToken(roomId, requestedUserId);
        } else {
            roomRegistry.revokeJoinApproveToken(roomId, requestedUserId);
        }

        return Map.of(
            "requestedUserId", requestedUserId,
            "roomId", roomId,
            "approved", approved
        );
    }

    public LeaveResult leaveRoom(String roomId, String userId) {
        String previousOwnerId = roomRegistry.findOwnerId(roomId)
            .orElseThrow(() -> new RuntimeException("방 없음."));

        if (!roomRegistry.isMember(roomId, userId)) {
            throw new RuntimeException("참여자 아님.");
        }

        String nickname = roomRegistry.findMemberNickname(roomId, userId)
            .orElseThrow(() -> new RuntimeException("닉네임을 찾을 수 없음."));

        boolean removed = roomRegistry.removeMember(roomId, userId);
        if (!removed) {
            throw new RuntimeException("나가기 실패");
        }

        Map<String, Object> notifyPayload = new HashMap<>();
        notifyPayload.put("userId", userId);
        notifyPayload.put("nickname", nickname);

        if (previousOwnerId != null && previousOwnerId.equals(userId)) {
            roomRegistry.findOwnerId(roomId).ifPresent(newOwnerId ->
                notifyPayload.put("newOwnerId", newOwnerId)
            );
        }

        return new LeaveResult(roomId, notifyPayload, roomRegistry.findOnlineMembers(roomId));
    }

    public Optional<LeaveResult> leaveRoomIfMember(String roomId, String userId) {
        if (roomId == null || roomId.isBlank()) {
            return Optional.empty();
        }
        if (!roomRegistry.isMember(roomId, userId)) {
            return Optional.empty();
        }
        try {
            return Optional.of(leaveRoom(roomId, userId));
        } catch (RuntimeException ex) {
            log.warn("room registry leave failed userId={}, roomId={}", userId, roomId, ex);
            return Optional.empty();
        }
    }

    public LeaveAllResult leaveAllRooms(String userId) {
        Set<String> rooms = roomRegistry.findRooms(userId);
        List<LeaveResult> results = new ArrayList<>();
        for (String roomId : rooms) {
            leaveRoomIfMember(roomId, userId).ifPresent(results::add);
        }
        return new LeaveAllResult(rooms, results);
    }

    public Optional<LeaveResult> leaveRoomForDisconnect(String roomId, String userId) {
        if (roomId == null || roomId.isBlank()) {
            return Optional.empty();
        }
        if (!roomRegistry.isMember(roomId, userId)) {
            return Optional.empty();
        }

        String previousOwnerId = roomRegistry.findOwnerId(roomId).orElse(null);
        String nickname = roomRegistry.findMemberNickname(roomId, userId).orElse(null);

        boolean removed = roomRegistry.removeMember(roomId, userId);
        if (!removed) {
            log.warn("room registry disconnect leave failed userId={}, roomId={}", userId, roomId);
            return Optional.empty();
        }

        Map<String, Object> notifyPayload = new HashMap<>();
        notifyPayload.put("userId", userId);
        if (nickname != null) {
            notifyPayload.put("nickname", nickname);
        }

        if (previousOwnerId != null && previousOwnerId.equals(userId)) {
            roomRegistry.findOwnerId(roomId).ifPresent(newOwnerId ->
                notifyPayload.put("newOwnerId", newOwnerId)
            );
        }

        return Optional.of(new LeaveResult(roomId, notifyPayload, roomRegistry.findOnlineMembers(roomId)));
    }

    public LeaveAllResult leaveAllRoomsForDisconnect(String userId) {
        Set<String> rooms = roomRegistry.findRooms(userId);
        List<LeaveResult> results = new ArrayList<>();
        for (String roomId : rooms) {
            leaveRoomForDisconnect(roomId, userId).ifPresent(results::add);
        }
        return new LeaveAllResult(rooms, results);
    }

    public List<RoomSummary> listRoomSummaries(String userId) {
        Set<String> rooms = roomRegistry.findRooms(userId);
        return rooms.stream()
            .map(roomRegistry::getRoomSummary)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .sorted((left, right) -> left.getRoomId().compareTo(right.getRoomId()))
            .collect(Collectors.toList());
    }

    public PresenceResult markOnline(String roomId, String userId) {
        if (roomId == null || roomId.isBlank()) {
            throw new RuntimeException("roomId required.");
        }
        if (!roomRegistry.isMember(roomId, userId)) {
            throw new RuntimeException("not a room member.");
        }
        String nickname = roomRegistry.findMemberNickname(roomId, userId)
            .orElse(null);
        roomRegistry.markOnline(roomId, userId);

        Map<String, Object> notifyPayload = new HashMap<>();
        notifyPayload.put("userId", userId);
        if (nickname != null) {
            notifyPayload.put("nickname", nickname);
        }

        return new PresenceResult(roomId, notifyPayload, roomRegistry.findOnlineMembers(roomId));
    }

    public Optional<PresenceResult> markOfflineIfMember(String roomId, String userId) {
        if (roomId == null || roomId.isBlank()) {
            return Optional.empty();
        }
        if (!roomRegistry.isMember(roomId, userId)) {
            return Optional.empty();
        }

        boolean marked = roomRegistry.markOffline(roomId, userId);
        if (!marked) {
            return Optional.empty();
        }

        String nickname = roomRegistry.findMemberNickname(roomId, userId)
            .orElse(null);

        Map<String, Object> notifyPayload = new HashMap<>();
        notifyPayload.put("userId", userId);
        if (nickname != null) {
            notifyPayload.put("nickname", nickname);
        }

        return Optional.of(new PresenceResult(roomId, notifyPayload, roomRegistry.findOnlineMembers(roomId)));
    }

    public PresenceAllResult markAllRoomsOffline(String userId) {
        Set<String> rooms = roomRegistry.findRooms(userId);
        List<PresenceResult> results = new ArrayList<>();
        for (String roomId : rooms) {
            markOfflineIfMember(roomId, userId).ifPresent(results::add);
        }
        return new PresenceAllResult(rooms, results);
    }

    public int deleteExpiredRooms(Instant now) {
        int deleted = 0;
        Set<String> rooms = roomRegistry.findAllRooms();
        for (String roomId : rooms) {
            if (roomRegistry.isAutoDeleteDue(roomId, now)) {
                roomRegistry.deleteRoom(roomId);
                deleted++;
            }
        }
        return deleted;
    }
}
