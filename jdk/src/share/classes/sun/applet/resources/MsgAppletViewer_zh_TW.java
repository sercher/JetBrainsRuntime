/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
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
package sun.applet.resources;

import java.util.ListResourceBundle;

public class MsgAppletViewer_zh_TW extends ListResourceBundle {

    public Object[][] getContents() {
        Object[][] temp = new Object[][] {
            {"textframe.button.dismiss", "\u95DC\u9589"},
            {"appletviewer.tool.title", "Applet \u6AA2\u8996\u5668: {0}"},
            {"appletviewer.menu.applet", "Applet"},
            {"appletviewer.menuitem.restart", "\u91CD\u65B0\u555F\u52D5"},
            {"appletviewer.menuitem.reload", "\u91CD\u65B0\u8F09\u5165"},
            {"appletviewer.menuitem.stop", "\u505C\u6B62"},
            {"appletviewer.menuitem.save", "\u5132\u5B58..."},
            {"appletviewer.menuitem.start", "\u555F\u52D5"},
            {"appletviewer.menuitem.clone", "\u8907\u88FD..."},
            {"appletviewer.menuitem.tag", "\u6A19\u8A18..."},
            {"appletviewer.menuitem.info", "\u8CC7\u8A0A..."},
            {"appletviewer.menuitem.edit", "\u7DE8\u8F2F"},
            {"appletviewer.menuitem.encoding", "\u5B57\u5143\u7DE8\u78BC"},
            {"appletviewer.menuitem.print", "\u5217\u5370..."},
            {"appletviewer.menuitem.props", "\u5C6C\u6027..."},
            {"appletviewer.menuitem.close", "\u95DC\u9589"},
            {"appletviewer.menuitem.quit", "\u7D50\u675F"},
            {"appletviewer.label.hello", "\u60A8\u597D..."},
            {"appletviewer.status.start", "\u6B63\u5728\u555F\u52D5 Applet..."},
            {"appletviewer.appletsave.filedialogtitle","\u5C07 Applet \u5E8F\u5217\u5316\u70BA\u6A94\u6848"},
            {"appletviewer.appletsave.err1", "\u5C07 {0} \u5E8F\u5217\u5316\u70BA {1}"},
            {"appletviewer.appletsave.err2", "\u5728 appletSave \u4E2D: {0}"},
            {"appletviewer.applettag", "\u986F\u793A\u7684\u6A19\u8A18"},
            {"appletviewer.applettag.textframe", "Applet HTML \u6A19\u8A18"},
            {"appletviewer.appletinfo.applet", "-- \u7121 Applet \u8CC7\u8A0A --"},
            {"appletviewer.appletinfo.param", "-- \u7121\u53C3\u6578\u8CC7\u8A0A --"},
            {"appletviewer.appletinfo.textframe", "Applet \u8CC7\u8A0A"},
            {"appletviewer.appletprint.fail", "\u5217\u5370\u5931\u6557\u3002"},
            {"appletviewer.appletprint.finish", "\u5B8C\u6210\u5217\u5370\u3002"},
            {"appletviewer.appletprint.cancel", "\u5217\u5370\u53D6\u6D88\u3002"},
            {"appletviewer.appletencoding", "\u5B57\u5143\u7DE8\u78BC: {0}"},
            {"appletviewer.parse.warning.requiresname", "\u8B66\u544A: <\u53C3\u6578\u540D\u7A31=... \u503C=...> \u6A19\u8A18\u9700\u8981\u540D\u7A31\u5C6C\u6027\u3002"},
            {"appletviewer.parse.warning.paramoutside", "\u8B66\u544A: <param> \u6A19\u8A18\u5728 <applet> ... </applet> \u4E4B\u5916\u3002"},
            {"appletviewer.parse.warning.applet.requirescode", "\u8B66\u544A: <applet> \u6A19\u8A18\u9700\u8981\u4EE3\u78BC\u5C6C\u6027\u3002"},
            {"appletviewer.parse.warning.applet.requiresheight", "\u8B66\u544A: <applet> \u6A19\u8A18\u9700\u8981\u9AD8\u5EA6\u5C6C\u6027\u3002"},
            {"appletviewer.parse.warning.applet.requireswidth", "\u8B66\u544A: <applet> \u6A19\u8A18\u9700\u8981\u5BEC\u5EA6\u5C6C\u6027\u3002"},
            {"appletviewer.parse.warning.object.requirescode", "\u8B66\u544A: <object> \u6A19\u8A18\u9700\u8981\u4EE3\u78BC\u5C6C\u6027\u3002"},
            {"appletviewer.parse.warning.object.requiresheight", "\u8B66\u544A: <object> \u6A19\u8A18\u9700\u8981\u9AD8\u5EA6\u5C6C\u6027\u3002"},
            {"appletviewer.parse.warning.object.requireswidth", "\u8B66\u544A: <object> \u6A19\u8A18\u9700\u8981\u5BEC\u5EA6\u5C6C\u6027\u3002"},
            {"appletviewer.parse.warning.embed.requirescode", "\u8B66\u544A: <embed> \u6A19\u8A18\u9700\u8981\u4EE3\u78BC\u5C6C\u6027\u3002"},
            {"appletviewer.parse.warning.embed.requiresheight", "\u8B66\u544A: <embed> \u6A19\u8A18\u9700\u8981\u9AD8\u5EA6\u5C6C\u6027\u3002"},
            {"appletviewer.parse.warning.embed.requireswidth", "\u8B66\u544A: <embed> \u6A19\u8A18\u9700\u8981\u5BEC\u5EA6\u5C6C\u6027\u3002"},
            {"appletviewer.parse.warning.appnotLongersupported", "\u8B66\u544A: \u4E0D\u518D\u652F\u63F4 <app> \u6A19\u8A18\uFF0C\u8ACB\u6539\u7528 <applet>:"},
            {"appletviewer.usage", "\u7528\u6CD5: appletviewer <\u9078\u9805> url(s)\n\n\u5176\u4E2D\u7684 <\u9078\u9805> \u5305\u62EC:\n  -debug                  \u5728 Java \u9664\u932F\u7A0B\u5F0F\u4E2D\u555F\u52D5 Applet \u6AA2\u8996\u5668\n  -encoding <\u7DE8\u78BC>    \u6307\u5B9A HTML \u6A94\u6848\u4F7F\u7528\u7684\u5B57\u5143\u7DE8\u78BC\n  -J<\u57F7\u884C\u968E\u6BB5\u65D7\u6A19>        \u5C07\u5F15\u6578\u50B3\u9001\u81F3 java \u89E3\u8B6F\u5668\n\n -J \u9078\u9805\u4E0D\u662F\u6A19\u6E96\u9078\u9805\uFF0C\u82E5\u6709\u8B8A\u66F4\u4E0D\u53E6\u884C\u901A\u77E5\u3002"},
            {"appletviewer.main.err.unsupportedopt", "\u4E0D\u652F\u63F4\u7684\u9078\u9805: {0}"},
            {"appletviewer.main.err.unrecognizedarg", "\u7121\u6CD5\u8FA8\u8B58\u7684\u5F15\u6578: {0}"},
            {"appletviewer.main.err.dupoption", "\u91CD\u8907\u4F7F\u7528\u9078\u9805: {0}"},
            {"appletviewer.main.err.inputfile", "\u672A\u6307\u5B9A\u8F38\u5165\u6A94\u6848\u3002"},
            {"appletviewer.main.err.badurl", "\u932F\u8AA4\u7684 URL: {0} ( {1} )"},
            {"appletviewer.main.err.io", "\u8B80\u53D6\u6642\u767C\u751F I/O \u7570\u5E38\u72C0\u6CC1: {0}"},
            {"appletviewer.main.err.readablefile", "\u78BA\u8A8D {0} \u70BA\u6A94\u6848\u4E14\u53EF\u8B80\u53D6\u3002"},
            {"appletviewer.main.err.correcturl", "{0} \u662F\u5426\u70BA\u6B63\u78BA\u7684 URL\uFF1F"},
            {"appletviewer.main.prop.store", "AppletViewer \u7684\u4F7F\u7528\u8005\u7279\u5B9A\u5C6C\u6027"},
            {"appletviewer.main.err.prop.cantread", "\u7121\u6CD5\u8B80\u53D6\u4F7F\u7528\u8005\u5C6C\u6027\u6A94\u6848: {0}"},
            {"appletviewer.main.err.prop.cantsave", "\u7121\u6CD5\u5132\u5B58\u4F7F\u7528\u8005\u5C6C\u6027\u6A94\u6848: {0}"},
            {"appletviewer.main.warn.nosecmgr", "\u8B66\u544A: \u505C\u7528\u5B89\u5168\u529F\u80FD\u3002"},
            {"appletviewer.main.debug.cantfinddebug", "\u627E\u4E0D\u5230\u9664\u932F\u7A0B\u5F0F\uFF01"},
            {"appletviewer.main.debug.cantfindmain", "\u5728\u9664\u932F\u7A0B\u5F0F\u4E2D\u627E\u4E0D\u5230\u4E3B\u8981\u65B9\u6CD5\uFF01"},
            {"appletviewer.main.debug.exceptionindebug", "\u9664\u932F\u7A0B\u5F0F\u767C\u751F\u7570\u5E38\u72C0\u6CC1\uFF01"},
            {"appletviewer.main.debug.cantaccess", "\u7121\u6CD5\u5B58\u53D6\u9664\u932F\u7A0B\u5F0F\uFF01"},
            {"appletviewer.main.nosecmgr", "\u8B66\u544A: \u672A\u5B89\u88DD SecurityManager\uFF01"},
            {"appletviewer.main.warning", "\u8B66\u544A: \u672A\u555F\u52D5 Applet\u3002\u8ACB\u78BA\u8A8D\u8F38\u5165\u5305\u542B <applet> \u6A19\u8A18\u3002"},
            {"appletviewer.main.warn.prop.overwrite", "\u8B66\u544A: \u4F9D\u7167\u4F7F\u7528\u8005\u8981\u6C42\uFF0C\u66AB\u6642\u8986\u5BEB\u7CFB\u7D71\u5C6C\u6027: \u7D22\u5F15\u9375: {0} \u820A\u503C: {1} \u65B0\u503C: {2}"},
            {"appletviewer.main.warn.cantreadprops", "\u8B66\u544A: \u7121\u6CD5\u8B80\u53D6 AppletViewer \u5C6C\u6027\u6A94\u6848: {0} \u4F7F\u7528\u9810\u8A2D\u503C\u3002"},
            {"appletioexception.loadclass.throw.interrupted", "\u985E\u5225\u8F09\u5165\u4E2D\u65B7: {0}"},
            {"appletioexception.loadclass.throw.notloaded", "\u672A\u8F09\u5165\u985E\u5225: {0}"},
            {"appletclassloader.loadcode.verbose", "\u958B\u555F {0} \u7684\u4E32\u6D41\u4EE5\u53D6\u5F97 {1}"},
            {"appletclassloader.filenotfound", "\u5C0B\u627E {0} \u6642\u627E\u4E0D\u5230\u6A94\u6848"},
            {"appletclassloader.fileformat", "\u8F09\u5165\u6642\u767C\u751F\u6A94\u6848\u683C\u5F0F\u7570\u5E38\u72C0\u6CC1: {0}"},
            {"appletclassloader.fileioexception", "\u8F09\u5165\u6642\u767C\u751F I/O \u7570\u5E38\u72C0\u6CC1: {0}"},
            {"appletclassloader.fileexception", "\u8F09\u5165\u6642\u767C\u751F {0} \u7570\u5E38\u72C0\u6CC1: {1}"},
            {"appletclassloader.filedeath", "\u8F09\u5165\u6642\u522A\u9664 {0}: {1}"},
            {"appletclassloader.fileerror", "\u8F09\u5165\u6642\u767C\u751F {0} \u932F\u8AA4: {1}"},
            {"appletclassloader.findclass.verbose.openstream", "\u958B\u555F {0} \u7684\u4E32\u6D41\u4EE5\u53D6\u5F97 {1}"},
            {"appletclassloader.getresource.verbose.forname", "AppletClassLoader.getResource \u7684\u540D\u7A31: {0}"},
            {"appletclassloader.getresource.verbose.found", "\u627E\u5230\u8CC7\u6E90: {0} \u4F5C\u70BA\u7CFB\u7D71\u8CC7\u6E90"},
            {"appletclassloader.getresourceasstream.verbose", "\u627E\u5230\u8CC7\u6E90: {0} \u4F5C\u70BA\u7CFB\u7D71\u8CC7\u6E90"},
            {"appletpanel.runloader.err", "\u7269\u4EF6\u6216\u4EE3\u78BC\u53C3\u6578\uFF01"},
            {"appletpanel.runloader.exception", "\u9084\u539F\u5E8F\u5217\u5316 {0} \u6642\u767C\u751F\u7570\u5E38\u72C0\u6CC1"},
            {"appletpanel.destroyed", "\u5DF2\u640D\u6BC0 Applet\u3002"},
            {"appletpanel.loaded", "\u5DF2\u8F09\u5165 Applet\u3002"},
            {"appletpanel.started", "\u5DF2\u555F\u7528 Applet\u3002"},
            {"appletpanel.inited", "\u5DF2\u8D77\u59CB Applet\u3002"},
            {"appletpanel.stopped", "\u5DF2\u505C\u6B62 Applet\u3002"},
            {"appletpanel.disposed", "\u5DF2\u8655\u7F6E Applet\u3002"},
            {"appletpanel.nocode", "APPLET \u6A19\u8A18\u907A\u6F0F CODE \u53C3\u6578\u3002"},
            {"appletpanel.notfound", "\u8F09\u5165: \u627E\u4E0D\u5230\u985E\u5225 {0}\u3002"},
            {"appletpanel.nocreate", "\u8F09\u5165: \u7121\u6CD5\u5EFA\u7ACB {0}\u3002"},
            {"appletpanel.noconstruct", "\u8F09\u5165: {0} \u975E\u516C\u7528\u6216\u6C92\u6709\u516C\u7528\u5EFA\u69CB\u5B50\u3002"},
            {"appletpanel.death", "\u5DF2\u522A\u9664"},
            {"appletpanel.exception", "\u7570\u5E38\u72C0\u6CC1: {0}\u3002"},
            {"appletpanel.exception2", "\u7570\u5E38\u72C0\u6CC1: {0}: {1}\u3002"},
            {"appletpanel.error", "\u932F\u8AA4: {0}\u3002"},
            {"appletpanel.error2", "\u932F\u8AA4: {0}: {1}\u3002"},
            {"appletpanel.notloaded", "\u8D77\u59CB: \u672A\u8F09\u5165 Applet\u3002"},
            {"appletpanel.notinited", "\u555F\u52D5: \u672A\u8D77\u59CB Applet\u3002"},
            {"appletpanel.notstarted", "\u505C\u6B62: \u672A\u555F\u52D5 Applet\u3002"},
            {"appletpanel.notstopped", "\u640D\u6BC0: \u672A\u505C\u6B62 Applet\u3002"},
            {"appletpanel.notdestroyed", "\u8655\u7F6E: \u672A\u640D\u6BC0 Applet\u3002"},
            {"appletpanel.notdisposed", "\u8F09\u5165: \u672A\u8655\u7F6E Applet\u3002"},
            {"appletpanel.bail", "\u5DF2\u4E2D\u65B7: \u6B63\u5728\u7D50\u675F\u3002"},
            {"appletpanel.filenotfound", "\u5C0B\u627E {0} \u6642\u627E\u4E0D\u5230\u6A94\u6848"},
            {"appletpanel.fileformat", "\u8F09\u5165\u6642\u767C\u751F\u6A94\u6848\u683C\u5F0F\u7570\u5E38\u72C0\u6CC1: {0}"},
            {"appletpanel.fileioexception", "\u8F09\u5165\u6642\u767C\u751F I/O \u7570\u5E38\u72C0\u6CC1: {0}"},
            {"appletpanel.fileexception", "\u8F09\u5165\u6642\u767C\u751F {0} \u7570\u5E38\u72C0\u6CC1: {1}"},
            {"appletpanel.filedeath", "\u8F09\u5165\u6642\u522A\u9664 {0}: {1}"},
            {"appletpanel.fileerror", "\u8F09\u5165\u6642\u767C\u751F {0} \u932F\u8AA4: {1}"},
            {"appletpanel.badattribute.exception", "HTML \u5256\u6790: \u5BEC\u5EA6/\u9AD8\u5EA6\u5C6C\u6027\u7684\u503C\u4E0D\u6B63\u78BA"},
            {"appletillegalargumentexception.objectinputstream", "AppletObjectInputStream \u9700\u8981\u975E\u7A7A\u503C\u8F09\u5165\u5668"},
            {"appletprops.title", "AppletViewer \u5C6C\u6027"},
            {"appletprops.label.http.server", "Http \u4EE3\u7406\u4E3B\u6A5F\u4F3A\u670D\u5668:"},
            {"appletprops.label.http.proxy", "Http \u4EE3\u7406\u4E3B\u6A5F\u9023\u63A5\u57E0:"},
            {"appletprops.label.network", "\u7DB2\u8DEF\u5B58\u53D6:"},
            {"appletprops.choice.network.item.none", "\u7121"},
            {"appletprops.choice.network.item.applethost", "Applet \u4E3B\u6A5F"},
            {"appletprops.choice.network.item.unrestricted", "\u4E0D\u53D7\u9650\u5236"},
            {"appletprops.label.class", "\u985E\u5225\u5B58\u53D6:"},
            {"appletprops.choice.class.item.restricted", "\u53D7\u9650\u5236"},
            {"appletprops.choice.class.item.unrestricted", "\u4E0D\u53D7\u9650\u5236"},
            {"appletprops.label.unsignedapplet", "\u5141\u8A31\u672A\u7C3D\u7F72\u7684 Applet:"},
            {"appletprops.choice.unsignedapplet.no", "\u5426"},
            {"appletprops.choice.unsignedapplet.yes", "\u662F"},
            {"appletprops.button.apply", "\u5957\u7528"},
            {"appletprops.button.cancel", "\u53D6\u6D88"},
            {"appletprops.button.reset", "\u91CD\u8A2D"},
            {"appletprops.apply.exception", "\u7121\u6CD5\u5132\u5B58\u5C6C\u6027: {0}"},
            /* 4066432 */
            {"appletprops.title.invalidproxy", "\u7121\u6548\u7684\u9805\u76EE"},
            {"appletprops.label.invalidproxy", "\u4EE3\u7406\u4E3B\u6A5F\u9023\u63A5\u57E0\u5FC5\u9808\u662F\u6B63\u6574\u6578\u503C\u3002"},
            {"appletprops.button.ok", "\u78BA\u5B9A"},
            /* end 4066432 */
            {"appletprops.prop.store", "AppletViewer \u7684\u4F7F\u7528\u8005\u7279\u5B9A\u5C6C\u6027"},
            {"appletsecurityexception.checkcreateclassloader", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: classloader"},
            {"appletsecurityexception.checkaccess.thread", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: thread"},
            {"appletsecurityexception.checkaccess.threadgroup", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: threadgroup: {0}"},
            {"appletsecurityexception.checkexit", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: exit: {0}"},
            {"appletsecurityexception.checkexec", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: exec: {0}"},
            {"appletsecurityexception.checklink", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: link: {0}"},
            {"appletsecurityexception.checkpropsaccess", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: \u5C6C\u6027"},
            {"appletsecurityexception.checkpropsaccess.key", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: \u5C6C\u6027\u5B58\u53D6 {0}"},
            {"appletsecurityexception.checkread.exception1", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: {0}\uFF0C{1}"},
            {"appletsecurityexception.checkread.exception2", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: file.read: {0}"},
            {"appletsecurityexception.checkread", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: file.read: {0} == {1}"},
            {"appletsecurityexception.checkwrite.exception", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: {0}\uFF0C{1}"},
            {"appletsecurityexception.checkwrite", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: file.write: {0} == {1}"},
            {"appletsecurityexception.checkread.fd", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: fd.read"},
            {"appletsecurityexception.checkwrite.fd", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: fd.write"},
            {"appletsecurityexception.checklisten", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: socket.listen: {0}"},
            {"appletsecurityexception.checkaccept", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: socket.accept: {0}:{1}"},
            {"appletsecurityexception.checkconnect.networknone", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: socket.connect: {0}->{1}"},
            {"appletsecurityexception.checkconnect.networkhost1", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: \u7121\u6CD5\u5F9E\u4F86\u6E90 {1} \u9023\u7DDA\u81F3 {0}\u3002"},
            {"appletsecurityexception.checkconnect.networkhost2", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: \u7121\u6CD5\u89E3\u6790\u4E3B\u6A5F {0} \u6216 {1} \u7684 IP\u3002"},
            {"appletsecurityexception.checkconnect.networkhost3", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: \u7121\u6CD5\u89E3\u6790\u4E3B\u6A5F {0} \u7684 IP\u3002\u8ACB\u53C3\u95B1 trustProxy \u5C6C\u6027\u3002"},
            {"appletsecurityexception.checkconnect", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: connect: {0}->{1}"},
            {"appletsecurityexception.checkpackageaccess", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: \u7121\u6CD5\u5B58\u53D6\u5957\u88DD\u7A0B\u5F0F: {0}"},
            {"appletsecurityexception.checkpackagedefinition", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: \u7121\u6CD5\u5B9A\u7FA9\u5957\u88DD\u7A0B\u5F0F: {0}"},
            {"appletsecurityexception.cannotsetfactory", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: \u7121\u6CD5\u8A2D\u5B9A\u8655\u7406\u7AD9"},
            {"appletsecurityexception.checkmemberaccess", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: \u6AA2\u67E5\u6210\u54E1\u5B58\u53D6"},
            {"appletsecurityexception.checkgetprintjob", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: getPrintJob"},
            {"appletsecurityexception.checksystemclipboardaccess", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: getSystemClipboard"},
            {"appletsecurityexception.checkawteventqueueaccess", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: getEventQueue"},
            {"appletsecurityexception.checksecurityaccess", "\u5B89\u5168\u7570\u5E38\u72C0\u6CC1: \u5B89\u5168\u4F5C\u696D: {0}"},
            {"appletsecurityexception.getsecuritycontext.unknown", "\u4E0D\u660E\u7684\u985E\u5225\u8F09\u5165\u5668\u985E\u578B\u3002\u7121\u6CD5\u6AA2\u67E5 getContext"},
            {"appletsecurityexception.checkread.unknown", "\u4E0D\u660E\u7684\u985E\u5225\u8F09\u5165\u5668\u985E\u578B\u3002\u7121\u6CD5\u6AA2\u67E5 read {0}"},
            {"appletsecurityexception.checkconnect.unknown", "\u4E0D\u660E\u7684\u985E\u5225\u8F09\u5165\u5668\u985E\u578B\u3002\u7121\u6CD5\u6AA2\u67E5\u9023\u7DDA"},
        };

        return temp;
    }
}
