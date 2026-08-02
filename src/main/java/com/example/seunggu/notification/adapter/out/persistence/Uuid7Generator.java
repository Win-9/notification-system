package com.example.seunggu.notification.adapter.out.persistence;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.uuid.UuidValueGenerator;

/**
 * UUIDv7(시간 정렬 UUID) 생성기.
 * Hibernate 6.6 에는 v7 내장 생성기가 없어(7.0 예정) UuidValueGenerator 확장점에
 * uuid-creator 라이브러리를 연결한다. 같은 밀리초 내에서도 단조 증가가 보장되어
 * auto-increment 처럼 인덱스 친화적이다.
 */
public class Uuid7Generator implements UuidValueGenerator {

    @Override
    public UUID generateUuid(SharedSessionContractImplementor session) {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
