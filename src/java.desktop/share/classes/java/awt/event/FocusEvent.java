/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.awt.event;

import java.awt.Component;
import java.io.ObjectStreamException;
import java.io.Serial;

import sun.awt.AWTAccessor;
import sun.awt.AppContext;
import sun.awt.SunToolkit;

/**
 * A low-level event which indicates that a Component has gained or lost the
 * input focus. This low-level event is generated by a Component (such as a
 * TextField). The event is passed to every {@code FocusListener} or
 * {@code FocusAdapter} object which registered to receive such events
 * using the Component's {@code addFocusListener} method.
 * ({@code FocusAdapter} objects implement the {@code FocusListener}
 * interface.) Each such listener object gets this {@code FocusEvent} when
 * the event occurs.
 * <p>
 * There are two levels of focus events: permanent and temporary. Permanent
 * focus change events occur when focus is directly moved from one Component to
 * another, such as through a call to requestFocus() or as the user uses the
 * TAB key to traverse Components. Temporary focus change events occur when
 * focus is temporarily lost for a Component as the indirect result of another
 * operation, such as Window deactivation or a Scrollbar drag. In this case,
 * the original focus state will automatically be restored once that operation
 * is finished, or, for the case of Window deactivation, when the Window is
 * reactivated. Both permanent and temporary focus events are delivered using
 * the FOCUS_GAINED and FOCUS_LOST event ids; the level may be distinguished in
 * the event using the isTemporary() method.
 * <p>
 * Every {@code FocusEvent} records its cause - the reason why this event was
 * generated. The cause is assigned during the focus event creation and may be
 * retrieved by calling {@link #getCause}.
 * <p>
 * An unspecified behavior will be caused if the {@code id} parameter
 * of any particular {@code FocusEvent} instance is not
 * in the range from {@code FOCUS_FIRST} to {@code FOCUS_LAST}.
 *
 * @see FocusAdapter
 * @see FocusListener
 * @see <a href="https://docs.oracle.com/javase/tutorial/uiswing/events/focuslistener.html">Tutorial: Writing a Focus Listener</a>
 *
 * @author Carl Quinn
 * @author Amy Fowler
 * @since 1.1
 */
public class FocusEvent extends ComponentEvent {

    /**
     * This enum represents the cause of a {@code FocusEvent}- the reason why it
     * occurred. Possible reasons include mouse events, keyboard focus
     * traversal, window activation.
     * If no cause is provided then the reason is {@code UNKNOWN}.
     *
     * @since 9
     */
    public enum Cause {
        /**
         * The default value.
         */
        UNKNOWN,
        /**
         * An activating mouse event.
         */
        MOUSE_EVENT,
        /**
         * A focus traversal action with unspecified direction.
         */
        TRAVERSAL,
        /**
         * An up-cycle focus traversal action.
         */
        TRAVERSAL_UP,
        /**
         * A down-cycle focus traversal action.
         */
        TRAVERSAL_DOWN,
        /**
         * A forward focus traversal action.
         */
        TRAVERSAL_FORWARD,
        /**
         * A backward focus traversal action.
         */
        TRAVERSAL_BACKWARD,
        /**
         * Restoring focus after a focus request has been rejected.
         */
        ROLLBACK,
        /**
         * A system action causing an unexpected focus change.
         */
        UNEXPECTED,
        /**
         * An activation of a toplevel window.
         */
        ACTIVATION,
        /**
         * Clearing global focus owner.
         */
        CLEAR_GLOBAL_FOCUS_OWNER
    }

    /**
     * The first number in the range of ids used for focus events.
     */
    public static final int FOCUS_FIRST         = 1004;

    /**
     * The last number in the range of ids used for focus events.
     */
    public static final int FOCUS_LAST          = 1005;

    /**
     * This event indicates that the Component is now the focus owner.
     */
    public static final int FOCUS_GAINED = FOCUS_FIRST; //Event.GOT_FOCUS

    /**
     * This event indicates that the Component is no longer the focus owner.
     */
    public static final int FOCUS_LOST = 1 + FOCUS_FIRST; //Event.LOST_FOCUS

    /**
     * A focus event has the reason why this event was generated.
     * The cause is set during the focus event creation.
     *
     * @serial
     * @see #getCause()
     * @since 9
     */
    private final Cause cause;

    /**
     * A focus event can have two different levels, permanent and temporary.
     * It will be set to true if some operation takes away the focus
     * temporarily and intends on getting it back once the event is completed.
     * Otherwise it will be set to false.
     *
     * @serial
     * @see #isTemporary
     */
    boolean temporary;

    /**
     * The other Component involved in this focus change. For a FOCUS_GAINED
     * event, this is the Component that lost focus. For a FOCUS_LOST event,
     * this is the Component that gained focus. If this focus change occurs
     * with a native application, a Java application in a different VM, or with
     * no other Component, then the opposite Component is null.
     *
     * @see #getOppositeComponent
     * @since 1.4
     */
    transient Component opposite;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 523753786457416396L;

    /**
     * Constructs a {@code FocusEvent} object with the
     * specified temporary state, opposite {@code Component} and the
     * {@code Cause.UNKNOWN} cause.
     * The opposite {@code Component} is the other
     * {@code Component} involved in this focus change.
     * For a {@code FOCUS_GAINED} event, this is the
     * {@code Component} that lost focus. For a
     * {@code FOCUS_LOST} event, this is the {@code Component}
     * that gained focus. If this focus change occurs with a native
     * application, with a Java application in a different VM,
     * or with no other {@code Component}, then the opposite
     * {@code Component} is {@code null}.
     * <p> This method throws an
     * {@code IllegalArgumentException} if {@code source}
     * is {@code null}.
     *
     * @param source     The {@code Component} that originated the event
     * @param id         An integer indicating the type of event.
     *                     For information on allowable values, see
     *                     the class description for {@link FocusEvent}
     * @param temporary  Equals {@code true} if the focus change is temporary;
     *                   {@code false} otherwise
     * @param opposite   The other Component involved in the focus change,
     *                   or {@code null}
     * @throws IllegalArgumentException if {@code source} equals {@code null}
     * @see #getSource()
     * @see #getID()
     * @see #isTemporary()
     * @see #getOppositeComponent()
     * @see Cause#UNKNOWN
     * @since 1.4
     */
    public FocusEvent(Component source, int id, boolean temporary,
                      Component opposite) {
        this(source, id, temporary, opposite, Cause.UNKNOWN);
    }

    /**
     * Constructs a {@code FocusEvent} object with the
     * specified temporary state, opposite {@code Component} and the cause.
     * The opposite {@code Component} is the other
     * {@code Component} involved in this focus change.
     * For a {@code FOCUS_GAINED} event, this is the
     * {@code Component} that lost focus. For a
     * {@code FOCUS_LOST} event, this is the {@code Component}
     * that gained focus. If this focus change occurs with a native
     * application, with a Java application in a different VM,
     * or with no other {@code Component}, then the opposite
     * {@code Component} is {@code null}.
     * <p> This method throws an
     * {@code IllegalArgumentException} if {@code source} or {@code cause}
     * is {@code null}.
     *
     * @param source    The {@code Component} that originated the event
     * @param id        An integer indicating the type of event.
     *                  For information on allowable values, see
     *                  the class description for {@link FocusEvent}
     * @param temporary Equals {@code true} if the focus change is temporary;
     *                  {@code false} otherwise
     * @param opposite  The other Component involved in the focus change,
     *                  or {@code null}
     * @param cause     The focus event cause.
     * @throws IllegalArgumentException if {@code source} equals {@code null}
     *                                  or if {@code cause} equals {@code null}
     * @see #getSource()
     * @see #getID()
     * @see #isTemporary()
     * @see #getOppositeComponent()
     * @see Cause
     * @since 9
     */
    public FocusEvent(Component source, int id, boolean temporary,
                      Component opposite, Cause cause) {
        super(source, id);
        if (cause == null) {
            throw new IllegalArgumentException("null cause");
        }
        this.temporary = temporary;
        this.opposite = opposite;
        this.cause = cause;
    }

    /**
     * Constructs a {@code FocusEvent} object and identifies
     * whether or not the change is temporary.
     * <p> This method throws an
     * {@code IllegalArgumentException} if {@code source}
     * is {@code null}.
     *
     * @param source    The {@code Component} that originated the event
     * @param id        An integer indicating the type of event.
     *                     For information on allowable values, see
     *                     the class description for {@link FocusEvent}
     * @param temporary Equals {@code true} if the focus change is temporary;
     *                  {@code false} otherwise
     * @throws IllegalArgumentException if {@code source} equals {@code null}
     * @see #getSource()
     * @see #getID()
     * @see #isTemporary()
     */
    public FocusEvent(Component source, int id, boolean temporary) {
        this(source, id, temporary, null);
    }

    /**
     * Constructs a {@code FocusEvent} object and identifies it
     * as a permanent change in focus.
     * <p> This method throws an
     * {@code IllegalArgumentException} if {@code source}
     * is {@code null}.
     *
     * @param source    The {@code Component} that originated the event
     * @param id        An integer indicating the type of event.
     *                     For information on allowable values, see
     *                     the class description for {@link FocusEvent}
     * @throws IllegalArgumentException if {@code source} equals {@code null}
     * @see #getSource()
     * @see #getID()
     */
    public FocusEvent(Component source, int id) {
        this(source, id, false);
    }

    /**
     * Identifies the focus change event as temporary or permanent.
     *
     * @return {@code true} if the focus change is temporary;
     *         {@code false} otherwise
     */
    public boolean isTemporary() {
        return temporary;
    }

    /**
     * Returns the other Component involved in this focus change. For a
     * FOCUS_GAINED event, this is the Component that lost focus. For a
     * FOCUS_LOST event, this is the Component that gained focus. If this
     * focus change occurs with a native application, with a Java application
     * in a different VM or context, or with no other Component, then null is
     * returned.
     *
     * @return the other Component involved in the focus change, or null
     * @since 1.4
     */
    public Component getOppositeComponent() {
        if (opposite == null) {
            return null;
        }

        return (SunToolkit.targetToAppContext(opposite) ==
                AppContext.getAppContext())
                ? opposite
                : null;
    }

    /**
     * Returns a parameter string identifying this event.
     * This method is useful for event-logging and for debugging.
     *
     * @return a string identifying the event and its attributes
     */
    public String paramString() {
        String typeStr;
        switch(id) {
            case FOCUS_GAINED:
                typeStr = "FOCUS_GAINED";
                break;
            case FOCUS_LOST:
                typeStr = "FOCUS_LOST";
                break;
            default:
                typeStr = "unknown type";
        }
        return typeStr + (temporary ? ",temporary" : ",permanent") +
                ",opposite=" + getOppositeComponent() + ",cause=" + getCause();
    }

    /**
     * Returns the event cause.
     *
     * @return one of {@link Cause} values
     * @since 9
     */
    public final Cause getCause() {
        return cause;
    }

    /**
     * Checks if this deserialized {@code FocusEvent} instance is compatible
     * with the current specification which implies that focus event has
     * non-null {@code cause} value. If the check fails a new {@code FocusEvent}
     * instance is returned which {@code cause} field equals to
     * {@link Cause#UNKNOWN} and its other fields have the same values as in
     * this {@code FocusEvent} instance.
     *
     * @return a newly created object from deserialized data
     * @throws ObjectStreamException if a new object replacing this object could
     *         not be created
     * @serial
     * @see #cause
     * @since 9
     */
    @Serial
    @SuppressWarnings("serial")
    Object readResolve() throws ObjectStreamException {
        if (cause != null) {
            return this;
        }
        FocusEvent focusEvent = new FocusEvent(new Component(){}, getID(),
                isTemporary(), getOppositeComponent());
        focusEvent.setSource(null);
        focusEvent.consumed = consumed;

        AWTAccessor.AWTEventAccessor accessor =
                AWTAccessor.getAWTEventAccessor();
        accessor.setBData(focusEvent, accessor.getBData(this));
        return focusEvent;
    }


}