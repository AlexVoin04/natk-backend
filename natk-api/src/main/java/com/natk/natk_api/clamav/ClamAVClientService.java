package com.natk.natk_api.clamav;

import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.ClamavException;
import xyz.capybara.clamav.commands.scan.result.ScanResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ClamAVClientService {

    private final ClamavClient clamavClient;

    public ClamAVClientService(
            @Value("${clamav.host:localhost}") String host,
            @Value("${clamav.port:3310}") int port
    ) {
        this.clamavClient = new ClamavClient(host, port);
    }

    /**
     * Сканирование файла на вирусы.
     *
     * @param inputStream поток файла
     * @throws IOException если соединение с ClamAV не удалось
     * @throws VirusFoundException если найден вирус
     */
    public void scan(InputStream inputStream) throws IOException, VirusFoundException {
        try {
            ScanResult scanResult = clamavClient.scan(inputStream);

            if (scanResult instanceof ScanResult.OK) {
                return;
            }

            if (scanResult instanceof ScanResult.VirusFound virusResult) {
                throw new VirusFoundException(
                        "Обнаружены вирусы: " + virusResult.getFoundViruses()
                );
            }

            throw new IOException("Неизвестный тип ответа ClamAV: " + scanResult.getClass().getSimpleName());

        } catch (ClamavException e) {
            throw new IOException("Ошибка ClamAV: " + e.getMessage(), e);
        }
    }

    public static class VirusFoundException extends RuntimeException {
        public VirusFoundException(String message) {
            super(message);
        }
    }
}
