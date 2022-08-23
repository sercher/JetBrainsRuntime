/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

/**

<p>The scripting API consists of interfaces and classes that define
Java Scripting Engines and provides
a framework for their use in Java applications. This API is intended
for use by application programmers who wish to execute programs
written in scripting languages in their Java applications. The
scripting language programs are usually provided by the end-users of
the applications.
</p>
<p>The main areas of functionality of <code>javax.script</code>
package include
</p>
<ol>
<li><p><b>Script execution</b>: Scripts
are streams of characters used as sources for  programs executed by
script engines. Script execution uses
{@link javax.script.ScriptEngine#eval eval} methods of
{@link javax.script.ScriptEngine ScriptEngine} and methods of the
{@link javax.script.Invocable Invocable} interface.
</p>
<li><p><b>Binding</b>: This facility
allows Java objects to be exposed to script programs as named
variables. {@link javax.script.Bindings Bindings} and
{@link javax.script.ScriptContext ScriptContext}
classes are used for this purpose.
</p>
<li><p><b>Compilation</b>: This
functionality allows the intermediate code generated by the
front-end of a script engine to be stored and executed repeatedly.
This benefits applications that execute the same script multiple
times. These applications can gain efficiency since the engines'
front-ends only need to execute once per script rather than once per
script execution. Note that this functionality is optional and
script engines may choose not to implement it. Callers need to check
for availability of the {@link javax.script.Compilable Compilable}
interface using an <I>instanceof</I> check.
</p>
<li><p><b>Invocation</b>: This
functionality allows the reuse of intermediate code generated by a
script engine's front-end. Whereas Compilation allows entire scripts
represented by intermediate code to be re-executed, Invocation
functionality allows individual procedures/methods in the scripts to
be re-executed. As in the case with compilation, not all script
engines are required to provide this facility. Caller has to check
for {@link javax.script.Invocable Invocable} availability.
</p>
<li><p><b>Script engine discovery</b>: Applications
written to the Scripting API might have specific requirements on
script engines. Some may require a specific scripting language
and/or version while others may require a specific implementation
engine and/or version. Script engines are packaged in a specified
way so that engines can be discovered at runtime and queried for
attributes. The Engine discovery mechanism is based on the service-provider
loading facility described in the {@link java.util.ServiceLoader} class.
{@link javax.script.ScriptEngineManager ScriptEngineManager}
includes
{@link javax.script.ScriptEngineManager#getEngineFactories getEngineFactories} method to get all
{@link javax.script.ScriptEngineFactory ScriptEngineFactory} instances
discovered using this mechanism. <code>ScriptEngineFactory</code> has
methods to query attributes about script engine.
</p>
</ol>

@since 1.6
*/

package javax.script;

