package org.terasology.events.input;

import org.terasology.entitySystem.EntityRef;
import org.terasology.input.ButtonState;

public class KeyRepeatEvent extends KeyEvent {

    private static KeyRepeatEvent event = new KeyRepeatEvent(0, 0);

    public static KeyRepeatEvent create(int key, float delta) {
        event.reset(delta);
        event.setKey(key);
        return event;
    }

    private KeyRepeatEvent(int key, float delta) {
        super(key, ButtonState.REPEAT, delta);
    }
}
