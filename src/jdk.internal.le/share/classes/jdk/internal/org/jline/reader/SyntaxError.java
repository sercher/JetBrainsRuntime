/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package jdk.internal.org.jline.reader;

public class SyntaxError extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public SyntaxError(int line, int column, String message) {
        super(message);
        this.line = line;
        this.column = column;
    }

    public int column() {
        return column;
    }

    public int line() {
        return line;
    }
}
