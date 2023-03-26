/*
 * Copyright 2023 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.diagnostic;

/**
 * This class imitates CoroutineDumperKt from IDEA for the purpose
 * of testing jstack.
 */
public class CoroutineDumperKt {
    public static final String MAGIC_STRING = "Coroutines dump from CoroutineDumperKt";
    public static String dumpCoroutines() {
        return MAGIC_STRING;
    }
}