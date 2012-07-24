/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.util.prefs;

import java.util.Objects;

class MacOSXPreferences extends AbstractPreferences {
    // fixme need security checks?

    // CF preferences file name for Java nodes with short names
    // This value is also in MacOSXPreferencesFile.c
    private static final String defaultAppName = "com.apple.java.util.prefs";

    // true if this node is a child of userRoot or is userRoot
    private final boolean isUser;

    // true if this node is userRoot or systemRoot
    private final boolean isRoot;

    // CF's storage location for this node and its keys
    private final MacOSXPreferencesFile file;

    // absolutePath() + "/"
    private final String path;

    // User root and system root nodes
    private static MacOSXPreferences userRoot = null;
    private static MacOSXPreferences systemRoot = null;


    // Returns user root node, creating it if necessary.
    // Called by MacOSXPreferencesFactory
    static synchronized Preferences getUserRoot() {
        if (userRoot == null) {
            userRoot = new MacOSXPreferences(true);
        }
        return userRoot;
    }


    // Returns system root node, creating it if necessary.
    // Called by MacOSXPreferencesFactory
    static synchronized Preferences getSystemRoot() {
        if (systemRoot == null) {
            systemRoot = new MacOSXPreferences(false);
        }
        return systemRoot;
    }


    // Create a new root node. Called by getUserRoot() and getSystemRoot()
    // Synchronization is provided by the caller.
    private MacOSXPreferences(boolean newIsUser) {
        this(null, "", false, true, newIsUser);
    }


    // Create a new non-root node with the given parent.
    // Called by childSpi().
    private MacOSXPreferences(MacOSXPreferences parent, String name) {
        this(parent, name, false, false, false);
    }

    private MacOSXPreferences(MacOSXPreferences parent, String name,
                              boolean isNew)
    {
        this(parent, name, isNew, false, false);
    }

    private MacOSXPreferences(MacOSXPreferences parent, String name,
                              boolean isNew, boolean isRoot, boolean isUser)
    {
        super(parent, name);
        this.isRoot = isRoot;
        if (isRoot)
            this.isUser = isUser;
        else
            this.isUser = isUserNode();
        path = isRoot ? absolutePath() : absolutePath() + "/";
        file = cfFileForNode(isUser);
        if (isNew)
            newNode = isNew;
        else
            newNode = file.addNode(path);
    }

    // Create and return the MacOSXPreferencesFile for this node.
    // Does not write anything to the file.
    private MacOSXPreferencesFile cfFileForNode(boolean isUser)
    {
        String name = path;
        // /one/two/three/four/five/
        // The fourth slash is the end of the first three components.
        // If there is no fourth slash, the name has fewer than 3 components
        int componentCount = 0;
        int pos = -1;
        for (int i = 0; i < 4; i++) {
            pos = name.indexOf('/', pos+1);
            if (pos == -1) break;
        }

        if (pos == -1) {
            // fewer than three components - use default name
            name = defaultAppName;
        } else {
            // truncate to three components, no leading or trailing '/'
            // replace '/' with '.' to make filesystem happy
            // convert to all lowercase to survive on HFS+
            name = name.substring(1, pos);
            name = name.replace('/', '.');
            name = name.toLowerCase();
        }

        return MacOSXPreferencesFile.getFile(name, isUser);
    }


    // AbstractPreferences implementation
    @Override
    protected void putSpi(String key, String value)
    {
        file.addKeyToNode(path, key, value);
    }

    // AbstractPreferences implementation
    @Override
    protected String getSpi(String key)
    {
        return file.getKeyFromNode(path, key);
    }

    // AbstractPreferences implementation
    @Override
    protected void removeSpi(String key)
    {
        Objects.requireNonNull(key, "Specified key cannot be null");
        file.removeKeyFromNode(path, key);
    }


    // AbstractPreferences implementation
    @Override
    protected void removeNodeSpi()
    throws BackingStoreException
    {
        // Disallow flush or sync between these two operations
        // (they may be manipulating two different files)
        synchronized(MacOSXPreferencesFile.class) {
            ((MacOSXPreferences)parent()).removeChild(name());
            file.removeNode(path);
        }
    }

    // Erase knowledge about a child of this node. Called by removeNodeSpi.
    private void removeChild(String child)
    {
        file.removeChildFromNode(path, child);
    }


    // AbstractPreferences implementation
    @Override
    protected String[] childrenNamesSpi()
    throws BackingStoreException
    {
        String[] result = file.getChildrenForNode(path);
        if (result == null) throw new BackingStoreException("Couldn't get list of children for node '" + path + "'");
        return result;
    }

    // AbstractPreferences implementation
    @Override
    protected String[] keysSpi()
    throws BackingStoreException
    {
        String[] result = file.getKeysForNode(path);
        if (result == null) throw new BackingStoreException("Couldn't get list of keys for node '" + path + "'");
        return result;
    }

    // AbstractPreferences implementation
    @Override
    protected AbstractPreferences childSpi(String name)
    {
        // Add to parent's child list here and disallow sync
        // because parent and child might be in different files.
        synchronized(MacOSXPreferencesFile.class) {
            boolean isNew = file.addChildToNode(path, name);
            return new MacOSXPreferences(this, name, isNew);
        }
    }

    // AbstractPreferences override
    @Override
    public void flush()
    throws BackingStoreException
    {
        // Flush should *not* check for removal, unlike sync, but should
        // prevent simultaneous removal.
        synchronized(lock) {
            if (isUser) {
                if (!MacOSXPreferencesFile.flushUser()) {
                    throw new BackingStoreException("Synchronization failed for node '" + path + "'");
                }
            } else {
                if (!MacOSXPreferencesFile.flushWorld()) {
                    throw new BackingStoreException("Synchronization failed for node '" + path + "'");
                }
            }
        }
    }

    // AbstractPreferences implementation
    @Override
    protected void flushSpi()
    throws BackingStoreException
    {
        // nothing here - overridden flush() doesn't call this
    }

    // AbstractPreferences override
    @Override
    public void sync()
    throws BackingStoreException
    {
        synchronized(lock) {
            if (isRemoved())
                throw new IllegalStateException("Node has been removed");
            // fixme! overkill
            if (isUser) {
                if (!MacOSXPreferencesFile.syncUser()) {
                    throw new BackingStoreException("Synchronization failed for node '" + path + "'");
                }
            } else {
                if (!MacOSXPreferencesFile.syncWorld()) {
                    throw new BackingStoreException("Synchronization failed for node '" + path + "'");
                }
            }
        }
    }

    // AbstractPreferences implementation
    @Override
    protected void syncSpi()
    throws BackingStoreException
    {
        // nothing here - overridden sync() doesn't call this
    }
}

