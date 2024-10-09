package com.hypherionmc.mmode.api.events;

import com.hypherionmc.craterlib.core.event.CraterEvent;
import lombok.NoArgsConstructor;

public class MaintenanceModeEvent {

    @NoArgsConstructor
    public static class MaintenanceStart extends CraterEvent {}

    @NoArgsConstructor
    public static class MaintenanceEnd extends CraterEvent {}

}
