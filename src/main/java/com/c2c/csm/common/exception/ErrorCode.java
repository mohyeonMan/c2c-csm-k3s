package com.c2c.csm.common.exception;

public enum ErrorCode {
    CSM_UNSUPPORTED_ACTION("CSM-CMD-001", "지원하지 않는 타입의 액션입니다."),
    CSM_NICKNAME_REQUIRED("CSM-REQ-001", "닉네임이 필요합니다."),
    CSM_JOIN_PERMISSION_REQUIRED("CSM-REQ-002", "입장 권한이 없습니다."),
    CSM_ALREADY_JOINED("CSM-REQ-003", "이미 참여한 상태입니다."),
    CSM_JOIN_FAILED("CSM-REQ-004", "입장에 실패했습니다."),
    CSM_ROOM_NOT_FOUND("CSM-REQ-005", "방을 찾을 수 없습니다."),
    CSM_NOT_ROOM_OWNER("CSM-REQ-006", "방장이 아닙니다."),
    CSM_NOT_ROOM_MEMBER("CSM-REQ-007", "방 참여자가 아닙니다."),
    CSM_NICKNAME_NOT_FOUND("CSM-REQ-008", "닉네임을 찾을 수 없습니다."),
    CSM_LEAVE_FAILED("CSM-REQ-009", "나가기에 실패했습니다."),
    CSM_ROOM_ID_REQUIRED("CSM-REQ-010", "roomId가 필요합니다."),
    CSM_ROOM_CREATE_FAILED("CSM-SRV-001", "방 생성에 실패했습니다."),
    CSM_ROOM_SUMMARY_FAILED("CSM-SRV-002", "방 요약 정보를 가져올 수 없습니다."),
    CSM_INTERNAL_ERROR("CSM-SRV-500", "서버 오류가 발생했습니다.");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
