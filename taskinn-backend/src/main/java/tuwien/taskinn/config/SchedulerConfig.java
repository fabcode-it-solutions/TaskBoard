package tuwien.taskinn.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerConfig {

    @Getter
    @Value("${scheduler.task.attempts:100}")
    private int taskAttempts;

    @Getter
    @Value("${scheduler.task.restarts:110}")
    private int taskRestarts;

    @Getter
    @Value("${scheduler.task.annealing-iterations:900}")
    private int taskAnnealingIterations;

}
