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
     */
    public ScanResult scan(InputStream inputStream) throws IOException {
        try {
            return clamavClient.scan(inputStream);
        } catch (ClamavException e) {
            throw new IOException("Ошибка ClamAV: " + e.getMessage(), e);
        }
    }

    /**
     * Исключение бизнес-логики — кидаем его вручную после scanResult
     */
    public static class VirusFoundException extends RuntimeException {
        public VirusFoundException(String message) {
            super(message);
        }
    }
}
