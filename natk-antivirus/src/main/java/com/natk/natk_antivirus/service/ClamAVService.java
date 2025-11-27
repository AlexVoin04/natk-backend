package com.natk.natk_antivirus.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.InputStream;

@Service
public class ClamAVService {

    private final ClamavClient client;

    public ClamAVService(@Value("${clamav.host}") String host,
                         @Value("${clamav.port}") int port) {
        this.client = new ClamavClient(host, port);
    }

    public ScanResult scan(InputStream stream) {
        return client.scan(stream);
    }
}
