/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

package com.sun.hotspot.igv.coordinator.actions;

import com.sun.hotspot.igv.coordinator.OutlineTopComponent;
import com.sun.hotspot.igv.data.GraphDocument;
import com.sun.hotspot.igv.data.serialization.BinaryParser;
import com.sun.hotspot.igv.data.serialization.GraphParser;
import com.sun.hotspot.igv.data.serialization.ParseMonitor;
import com.sun.hotspot.igv.data.serialization.Parser;
import com.sun.hotspot.igv.settings.Settings;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Thomas Wuerthinger
 */
public final class ImportAction extends CallableSystemAction {
    private static final int WORKUNITS = 10000;

    public static FileFilter getFileFilter() {
        return new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".xml") || f.getName().toLowerCase().endsWith(".bgv") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Graph files (*.xml, *.bgv)";
            }
        };
    }

    @Override
    public void performAction() {

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(ImportAction.getFileFilter());
        fc.setCurrentDirectory(new File(Settings.get().get(Settings.DIRECTORY, Settings.DIRECTORY_DEFAULT)));

        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();

            File dir = file;
            if (!dir.isDirectory()) {
                dir = dir.getParentFile();
            }

            Settings.get().put(Settings.DIRECTORY, dir.getAbsolutePath());
            try {
                final FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
                final ProgressHandle handle = ProgressHandleFactory.createHandle("Opening file " + file.getName());
                handle.start(WORKUNITS);
                final long start = channel.size();
                ParseMonitor monitor = new ParseMonitor() {
                    @Override
                    public void updateProgress() {
                        try {
                            int prog = (int) (WORKUNITS * (double) channel.position() / (double) start);
                            handle.progress(prog);
                        } catch (IOException ex) {
                        }
                    }
                    @Override
                    public void setState(String state) {
                        updateProgress();
                        handle.progress(state);
                    }
                };
                final GraphParser parser;
                final OutlineTopComponent component = OutlineTopComponent.findInstance();
                if (file.getName().endsWith(".xml")) {
                    parser = new Parser(channel, monitor, null);
                } else if (file.getName().endsWith(".bgv")) {
                    parser = new BinaryParser(channel, monitor, component.getDocument(), null);
                } else {
                    parser = null;
                }
                RequestProcessor.getDefault().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final GraphDocument document = parser.parse();
                            if (document != null) {
                                SwingUtilities.invokeLater(new Runnable(){
                                    @Override
                                    public void run() {
                                        component.requestActive();
                                        component.getDocument().addGraphDocument(document);
                                    }
                                });
                            }
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                        handle.finish();
                    }
                });
            } catch (FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(ImportAction.class, "CTL_ImportAction");
    }

    public ImportAction() {
        putValue(Action.SHORT_DESCRIPTION, "Open XML graph document...");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
    }

    @Override
    protected String iconResource() {
        return "com/sun/hotspot/igv/coordinator/images/import.png";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}
