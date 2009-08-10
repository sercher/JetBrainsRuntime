/*
 * Copyright 2000-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package sun.awt.shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.Callable;

/**
 * @author Michael Martak
 * @since 1.4
 */

class ShellFolderManager {
    /**
     * Create a shell folder from a file.
     * Override to return machine-dependent behavior.
     */
    public ShellFolder createShellFolder(File file) throws FileNotFoundException {
        return new DefaultShellFolder(null, file);
    }

    /**
     * @param key a <code>String</code>
     *  "fileChooserDefaultFolder":
     *    Returns a <code>File</code> - the default shellfolder for a new filechooser
     *  "roots":
     *    Returns a <code>File[]</code> - containing the root(s) of the displayable hieararchy
     *  "fileChooserComboBoxFolders":
     *    Returns a <code>File[]</code> - an array of shellfolders representing the list to
     *    show by default in the file chooser's combobox
     *   "fileChooserShortcutPanelFolders":
     *    Returns a <code>File[]</code> - an array of shellfolders representing well-known
     *    folders, such as Desktop, Documents, History, Network, Home, etc.
     *    This is used in the shortcut panel of the filechooser on Windows 2000
     *    and Windows Me.
     *  "fileChooserIcon nn":
     *    Returns an <code>Image</code> - icon nn from resource 124 in comctl32.dll (Windows only).
     *
     * @return An Object matching the key string.
     */
    public Object get(String key) {
        if (key.equals("fileChooserDefaultFolder")) {
            // Return the default shellfolder for a new filechooser
            File homeDir = new File(System.getProperty("user.home"));
            try {
                return createShellFolder(homeDir);
            } catch (FileNotFoundException e) {
                return homeDir;
            }
        } else if (key.equals("roots")) {
            // The root(s) of the displayable hieararchy
            return File.listRoots();
        } else if (key.equals("fileChooserComboBoxFolders")) {
            // Return an array of ShellFolders representing the list to
            // show by default in the file chooser's combobox
            return get("roots");
        } else if (key.equals("fileChooserShortcutPanelFolders")) {
            // Return an array of ShellFolders representing well-known
            // folders, such as Desktop, Documents, History, Network, Home, etc.
            // This is used in the shortcut panel of the filechooser on Windows 2000
            // and Windows Me
            return new File[] { (File)get("fileChooserDefaultFolder") };
        }
        return null;
    }

    /**
     * Does <code>dir</code> represent a "computer" such as a node on the network, or
     * "My Computer" on the desktop.
     */
    public boolean isComputerNode(File dir) {
        return false;
    }

    public boolean isFileSystemRoot(File dir) {
        if (dir instanceof ShellFolder && !((ShellFolder) dir).isFileSystem()) {
            return false;
        }
        return (dir.getParentFile() == null);
    }

    protected ShellFolder.Invoker createInvoker() {
        return new DirectInvoker();
    }

    private static class DirectInvoker implements ShellFolder.Invoker {
        public <T> T invoke(Callable<T> task) throws Exception {
            return task.call();
        }
    }
}
