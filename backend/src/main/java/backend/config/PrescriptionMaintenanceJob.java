package backend.config;

import backend.service.PrescriptionLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class PrescriptionMaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionMaintenanceJob.class);

    private final PrescriptionLifecycleService lifecycleService;

    public PrescriptionMaintenanceJob(PrescriptionLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    // Runs 15 seconds after startup, then once an hour.
    @Scheduled(initialDelay = 15000, fixedDelay = 3600000)
    public void markFinishedPrescriptions() {
        int updated = lifecycleService.completeFinished();
        if (updated > 0) {
            log.info("Marked {} finished prescription(s) as completed", updated);
        }
    }
}