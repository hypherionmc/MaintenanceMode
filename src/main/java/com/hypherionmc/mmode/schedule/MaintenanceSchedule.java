package com.hypherionmc.mmode.schedule;

import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.schedule.cron.CronExpressionSchedule;
import com.hypherionmc.craterlib.core.event.CraterEventBus;
import com.hypherionmc.mmode.CommonClass;
import com.hypherionmc.mmode.ModConstants;
import com.hypherionmc.mmode.api.events.MaintenanceModeEvent;
import com.hypherionmc.mmode.config.MaintenanceModeConfig;

import java.time.Duration;

public class MaintenanceSchedule {

    public static final MaintenanceSchedule INSTANCE = new MaintenanceSchedule();
    private final Scheduler scheduler = new Scheduler(2);

    MaintenanceSchedule() {}

    public void initScheduler() {
        try {
            scheduler.findJob("mmodestart").ifPresent(job -> {
                scheduler.cancel(job.name());
                scheduler.remove(job.name());
            });
            scheduler.findJob("mmodeend").ifPresent(job -> {
                scheduler.cancel(job.name());
                scheduler.remove(job.name());
            });
        } catch (Exception ignored) {}

        try {
            if (!MaintenanceModeConfig.INSTANCE.getSchedule().getStartTime().isEmpty()) {
                scheduler.schedule("mmodestart", this::startMaintenance, CronExpressionSchedule.parse(MaintenanceModeConfig.INSTANCE.getSchedule().getStartTime()));
            }
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to schedule maintenance", e);
        }

        try {
            if (!MaintenanceModeConfig.INSTANCE.getSchedule().getEndTime().isEmpty()) {
                scheduler.schedule("mmodeend", this::endMaintenance, CronExpressionSchedule.parse(MaintenanceModeConfig.INSTANCE.getSchedule().getEndTime()));
            }
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to schedule maintenance", e);
        }
    }

    private void startMaintenance() {
        CraterEventBus.INSTANCE.postEvent(new MaintenanceModeEvent.MaintenanceStart());
        CommonClass.INSTANCE.broadcastMessage("Maintenance is starting");
        MaintenanceModeConfig.INSTANCE.setEnabled(true);
        CommonClass.INSTANCE.resetOnStartup = MaintenanceModeConfig.INSTANCE.getSchedule().isDisableOnRestart();
        CommonClass.INSTANCE.kickAllPlayers(MaintenanceModeConfig.INSTANCE.getMessage());
        CommonClass.INSTANCE.isDirty.set(true);
        MaintenanceModeConfig.INSTANCE.saveConfig(MaintenanceModeConfig.INSTANCE);
    }

    private void endMaintenance() {
        CraterEventBus.INSTANCE.postEvent(new MaintenanceModeEvent.MaintenanceEnd());
        CommonClass.INSTANCE.broadcastMessage("Maintenance is ending");
        MaintenanceModeConfig.INSTANCE.setEnabled(false);
        CommonClass.INSTANCE.isDirty.set(true);
        MaintenanceModeConfig.INSTANCE.saveConfig(MaintenanceModeConfig.INSTANCE);
    }

    public void shutDown() {
        scheduler.gracefullyShutdown(Duration.ZERO);
    }

}
