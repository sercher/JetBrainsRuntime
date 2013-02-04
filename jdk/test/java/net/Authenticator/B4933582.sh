#
# Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

# @test
# @bug 4933582

OS=`uname -s`
case "$OS" in
  SunOS | Linux | Darwin )
    PS=":"
    FS="/"
    ;;
  CYGWIN* )
    PS=";"
    FS="/"
    ;;
  Windows* )
    PS=";"
    FS="\\"
    ;;
  * )
    echo "Unrecognized system!"
    exit 1;
    ;;
esac
${COMPILEJAVA}${FS}bin${FS}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d . \
    -classpath "${TESTSRC}${FS}..${FS}..${FS}..${FS}sun${FS}net${FS}www${FS}httptest" ${TESTSRC}${FS}B4933582.java
rm -f cache.ser auth.save
${TESTJAVA}${FS}bin${FS}java ${TESTVMOPTS} -classpath "${TESTSRC}${FS}..${FS}..${FS}..${FS}sun${FS}net${FS}www${FS}httptest${PS}." B4933582 first
${TESTJAVA}${FS}bin${FS}java ${TESTVMOPTS} -classpath "${TESTSRC}${FS}..${FS}..${FS}..${FS}sun${FS}net${FS}www${FS}httptest${PS}." B4933582 second
