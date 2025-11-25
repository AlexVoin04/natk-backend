package com.natk.natk_api.rabbit;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.natk.common.messaging.ScanTask;

@Service
@RequiredArgsConstructor
public class ScanTaskPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(ScanTask task) {
        rabbitTemplate.convertAndSend(RabbitConfig.QUEUE, task);
    }
}
