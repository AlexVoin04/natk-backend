package com.natk.natk_antivirus;

import com.natk.natk_antivirus.service.ClamAVService;
import com.natk.natk_antivirus.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import xyz.capybara.clamav.commands.scan.result.ScanResult;
import com.natk.common.messaging.ScanTask;

import java.io.InputStream;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class ScanWorker {

    private final ClamAVService clam;
    private final MinioService minio;
    private final FileStatusUpdater updater;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void process(ScanTask task) {

        try (InputStream is = minio.download("incoming", task.storageKey())) {

            ScanResult result = clam.scan(is);

            if (result instanceof ScanResult.VirusFound vf) {

                String virus = String.join(", ",
                        vf.getFoundViruses().values().stream()
                                .flatMap(Collection::stream)
                                .toList()
                );

                updater.markInfected(task.fileId(), virus);
                return;
            }

            updater.markClean(task.fileId(), task.storageKey());

        } catch (Exception e) {
            updater.markError(task.fileId(), e.getMessage());
        }
    }
}