package com.natk.natk_antivirus;

import com.natk.natk_antivirus.config.RabbitConfig;
import com.natk.natk_antivirus.service.ClamAVService;
import com.natk.natk_antivirus.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import xyz.capybara.clamav.commands.scan.result.ScanResult;
import com.natk.common.messaging.ScanTask;

import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanWorker {

    private final ClamAVService clam;
    private final MinioService minio;
    private final FileStatusUpdater updater;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void process(ScanTask task) {

        try (InputStream is = minio.download("incoming", task.storageKey())) {

            ScanResult result = clam.scan(is);

            if (result instanceof ScanResult.VirusFound vf) {

                String virus = vf.getFoundViruses().values().stream()
                        .flatMap(Collection::stream)
                        .distinct()
                        .collect(Collectors.joining(", "));

                log.info("File {} infected: {}", task.fileId(), virus);

                updater.markInfected(task.fileId(), virus);
                return;
            }

            log.info("File {} clean", task.fileId());
            updater.markClean(task.fileId(), task.storageKey());

        } catch (Exception e) {
            log.error("Error processing scan for file {}", task.fileId(), e);
            updater.markError(task.fileId(), e.getMessage());
        }
    }
}