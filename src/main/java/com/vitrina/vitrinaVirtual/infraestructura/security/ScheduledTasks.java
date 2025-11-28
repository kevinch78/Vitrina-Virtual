package com.vitrina.vitrinaVirtual.infraestructura.security;

import com.vitrina.vitrinaVirtual.domain.service.BlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    @Autowired
    private BlacklistService blacklistService;

    // Se ejecuta cada 24 horas (86400000 ms)
    // Puedes ajustar el cron para que se ejecute a una hora específica, por ejemplo, "0 0 3 * * ?" para las 3 AM
    @Scheduled(fixedRate = 86400000)
    public void cleanExpiredBlacklistedTokens() {
        blacklistService.cleanExpiredTokens();
    }
}