package com.c2c.csm.common.util;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommonMapper {
    private final ObjectMapper objectMapper;

    public <T> T read(byte[] payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception e) {
            log.error("JSON 변환 실패 : {} -> {}", payload, type);
            return null;
        }
    }

    public <T> T read(String payload, Class<T> type){
		try {
			return objectMapper.readValue(payload, type);
		} catch (Exception e) {
			log.error("JSON 변환 실패 : {} -> {}", payload, type);
			return null;
		}
	}

	public <T> T read(String payload, TypeReference<T> type){
		try {
			return objectMapper.readValue(payload, type);
		} catch (Exception e) {
			log.error("JSON 변환 실패 : {} -> {}", payload, type);
			return null;
		}
	}

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("JSON 작성 실패 : {}", value);
            return null;
        }
    }

    public ObjectMapper rawMapper() {
        return objectMapper;
    }
}