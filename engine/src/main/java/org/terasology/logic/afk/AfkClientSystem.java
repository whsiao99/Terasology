// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.logic.afk;

import org.terasology.assets.ResourceUrn;
import org.terasology.engine.Time;
import org.terasology.engine.subsystem.rpc.DiscordRPCSubSystem;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.game.Game;
import org.terasology.input.Keyboard;
import org.terasology.input.events.KeyDownEvent;
import org.terasology.logic.console.Console;
import org.terasology.logic.console.commandSystem.annotations.Command;
import org.terasology.logic.permission.PermissionManager;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.network.NetworkMode;
import org.terasology.network.NetworkSystem;
import org.terasology.physics.events.MovedEvent;
import org.terasology.registry.In;
import org.terasology.rendering.nui.NUIManager;

@RegisterSystem(RegisterMode.CLIENT)
public class AfkClientSystem extends BaseComponentSystem {

    /** Duration in milliseconds (ms). */
    public static final long AFK_FREEDOM = 5 * 1000;

    /** Duration in milliseconds (ms). */
    private static final long AFK_TIMEOUT = 60 * 1000;

    private static final ResourceUrn SCREEN_URL = new ResourceUrn("engine:afk");
    private static final ResourceUrn CONSOLE_SCREEN_URL = new ResourceUrn("engine:console");

    @In
    private Console console;

    @In
    private LocalPlayer localPlayer;

    @In
    private NetworkSystem networkSystem;

    @In
    private NUIManager nuiManager;

    @In
    private Game game;

    @In
    private Time time;

    private long lastActive;

    @Override
    public void postBegin() {
        updateActive();
    }

    @Command(
            value = "afk",
            shortDescription = "Say that you are AFK to others",
            requiredPermission = PermissionManager.NO_PERMISSION
    )
    public void onCommand() {
        if (requireConnection()) {
            console.addMessage("Failed! You need to be connected to use this command.");
            return;
        }
        updateActive();
        EntityRef entity = localPlayer.getClientEntity();
        AfkComponent component = entity.getComponent(AfkComponent.class);
        component.afk = !component.afk;
        entity.addOrSaveComponent(component);
        if (component.afk) {
            nuiManager.pushScreen(SCREEN_URL, AfkScreen.class).setAfkClientSystem(this);
            nuiManager.closeScreen(CONSOLE_SCREEN_URL);
            enableDiscord();
            console.addMessage("[AFK] You are AFK now!");
        } else {
            nuiManager.closeScreen(SCREEN_URL);
            disableDiscord();
            console.addMessage("[AFK] You are no longer AFK!");
        }
        AfkRequest request = new AfkRequest(entity, component.afk);
        entity.send(request);
    }

    @ReceiveEvent
    public void onDetectAfk(AfkDetectEvent event, EntityRef entityRef) {
        EntityRef entity = localPlayer.getClientEntity();
        long afkTime = time.getGameTimeInMs() - lastActive;
        if (afkTime >= AFK_TIMEOUT) {
            AfkComponent component = entity.getComponent(AfkComponent.class);
            if (!component.afk) {
                component.afk = true;
                nuiManager.pushScreen(SCREEN_URL, AfkScreen.class).setAfkClientSystem(this);
                enableDiscord();
                AfkRequest request = new AfkRequest(entity, true);
                entity.send(request);
            }
        }
    }

    @ReceiveEvent
    public void onAfk(AfkEvent event, EntityRef entityRef) {
        if (requireConnection()) {
            return;
        }
        EntityRef entity = event.getTarget();
        AfkComponent component = entity.getComponent(AfkComponent.class);
        if (component != null) {
            component.afk = event.isAfk();
            entity.addOrSaveComponent(component);
        }
    }

    @ReceiveEvent
    public void onMove(MovedEvent movedEvent, EntityRef entity) {
        if (requireConnection()) {
            return;
        }
        if (!disable()) {
            updateActive();
        }
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_HIGH)
    public void onKeyDown(KeyDownEvent event, EntityRef entity) {
        if (requireConnection()) {
            return;
        }
        AfkComponent component = entity.getComponent(AfkComponent.class);
        if (component != null && component.afk) {
            long afkTime = time.getGameTimeInMs() - lastActive;
            if (afkTime <= AFK_FREEDOM || event.getKey() == Keyboard.Key.ESCAPE) {
                return;
            }
            updateActive();
            disable();
        }
    }

    public void onAfkScreenClosed() {
        disable();
        updateActive();
    }

    public long getLastActive() {
        return lastActive;
    }

    private String getGame() {
        NetworkMode networkMode = networkSystem.getMode();
        String mode = "Playing Online";
        if (networkMode == NetworkMode.DEDICATED_SERVER) {
            mode = "Hosting | " + game.getName();
        }
        return mode;
    }

    private void updateActive() {
        lastActive = time.getGameTimeInMs();
    }

    private void enableDiscord() {
        DiscordRPCSubSystem.tryToDiscover();
        DiscordRPCSubSystem.setState("Idle", true);
    }

    private void disableDiscord() {
        DiscordRPCSubSystem.tryToDiscover();
        DiscordRPCSubSystem.setState(getGame(), true);
    }

    private boolean disable() {
        EntityRef clientEntity = localPlayer.getClientEntity();
        AfkComponent component = clientEntity.getComponent(AfkComponent.class);
        if (component != null && component.afk) {
            component.afk = false;
            nuiManager.closeScreen(SCREEN_URL);
            clientEntity.addOrSaveComponent(component);
            AfkRequest request = new AfkRequest(clientEntity, false);
            clientEntity.send(request);
            disableDiscord();
            return true;
        }
        return false;
    }

    private boolean requireConnection() {
        NetworkMode networkMode = networkSystem.getMode();
        if (networkMode != NetworkMode.CLIENT && networkMode != NetworkMode.DEDICATED_SERVER) {
            return true;
        }
        return false;
    }

}
