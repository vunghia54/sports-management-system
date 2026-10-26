package com.sports.management.notification.messaging;

import com.sports.management.notification.model.Notification;
import com.sports.management.notification.repository.NotificationRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;

    public NotificationConsumer(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION)
    public void handleMatchFinished(MatchFinishedEvent event) {
        System.out.println(">>> [NOTIFICATION SERVICE] Received MatchFinishedEvent for Match ID: " + event.getMatchId());

        Notification notification = new Notification();
        notification.setRecipient("ALL_USERS");
        notification.setTitle("Trận đấu đã kết thúc: " + event.getParticipant1() + " vs " + event.getParticipant2());
        notification.setMessage("Trận đấu thuộc giải đấu #" + event.getTournamentId() + " giữa " 
                + event.getParticipant1() + " và " + event.getParticipant2() + " đã có kết quả chính thức!");
        notification.setType("MATCH_FINISHED");
        notification.setReferenceId(event.getMatchId());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);
        System.out.println(">>> [NOTIFICATION SERVICE] Created Notification ID: " + saved.getId());
    }
}
