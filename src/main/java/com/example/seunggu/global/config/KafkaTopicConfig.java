package com.example.seunggu.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 알림 토픽 생성. 토픽명·파티션 수는 application.properties 의 notification.topic.* 로 설정한다.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic notificationTopic(@Value("${notification.topic}") String topic,
                                      @Value("${notification.topic.partitions}") int partitions) {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}
