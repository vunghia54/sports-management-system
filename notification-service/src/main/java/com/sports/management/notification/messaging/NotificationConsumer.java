package com.sports.management.notification.messaging;

import com.sports.management.notification.model.Notification;
import com.sports.management.notification.repository.NotificationRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;

    public NotificationConsumer(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION)
    @Transactional
    public void handleMatchFinished(MatchFinishedEvent event) {
        if (event == null || event.getMatchId() == null) {
            return;
        }
        if (notificationRepository.existsByTypeAndReferenceId("MATCH_FINISHED", event.getMatchId())) {
            System.out.println(">>> [NOTIFICATION SERVICE] Skipped duplicate MATCH_FINISHED event for Match ID: " + event.getMatchId());
            return;
        }
        System.out.println(">>> [NOTIFICATION SERVICE] Received MatchFinishedEvent for Match ID: " + event.getMatchId());

        Notification notification = new Notification();
        notification.setRecipient("ALL_USERS");
        boolean hasScore = event.getScoreParticipant1() != null && event.getScoreParticipant2() != null;
        boolean hasPenalties = event.getPenaltyScore1() != null && event.getPenaltyScore2() != null;
        String scoreBadge = hasScore
                ? " (" + event.getScoreParticipant1() + "-" + event.getScoreParticipant2()
                        + (hasPenalties ? ", pen " + event.getPenaltyScore1() + "-" + event.getPenaltyScore2() : "")
                        + ")"
                : "";
        String scoreSentence = hasScore
                ? " Tỉ số: " + event.getScoreParticipant1() + " - " + event.getScoreParticipant2()
                        + (hasPenalties ? " (luân lưu " + event.getPenaltyScore1() + " - " + event.getPenaltyScore2() + ")" : "")
                        + "."
                : "";
        notification.setTitle("Trận đấu đã kết thúc: " + event.getParticipant1() + " vs " + event.getParticipant2() + scoreBadge);
        notification.setMessage("Trận đấu thuộc giải đấu #" + event.getTournamentId() + " giữa "
                + event.getParticipant1() + " và " + event.getParticipant2() + " đã có kết quả chính thức!" + scoreSentence);
        notification.setType("MATCH_FINISHED");
        notification.setReferenceId(event.getMatchId());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);
        System.out.println(">>> [NOTIFICATION SERVICE] Created Notification ID: " + saved.getId());
    }
}
