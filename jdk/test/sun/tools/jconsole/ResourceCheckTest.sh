#
# Copyright 2004-2007 Sun Microsystems, Inc.  All Rights Reserved.
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
# Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
# CA 95054 USA or visit www.sun.com if you need additional information or
# have any questions.
#

#   @test
#   @bug        5023573 5062569
#   @summary    jtreg test ResourceCheckTest.java must be
#               able to find jconsole.jar
#
#   @run shell ResourceCheckTest.sh

# Beginning of subroutines:
status=1

#Call this from anywhere to fail the test with an error message
# usage: fail "reason why the test failed"
fail() 
 { echo "The test failed :-("
   echo "$*" 1>&2
   echo "exit status was $status"
   exit $status
 } #end of fail()

#Call this from anywhere to pass the test with a message
# usage: pass "reason why the test passed if applicable"
pass() 
 { echo "The test passed!!!"
   echo "$*" 1>&2
   exit 0
 } #end of pass()

# end of subroutines

# The beginning of the script proper

OS=`uname -s`
case "$OS" in
   SunOS | Linux )
      PATHSEP=":"
      ;;

   Windows* | CYGWIN*)
      PATHSEP=";"
      ;;

   # catch all other OSs
   * )
      echo "Unrecognized system!  $OS"
      fail "Unrecognized system!  $OS"
      ;;
esac

TARGETCLASS="ResourceCheckTest"
if [ -z "${TESTJAVA}" ] ; then
   # TESTJAVA is not set, so the test is running stand-alone.
   # TESTJAVA holds the path to the root directory of the build of the JDK
   # to be tested.  That is, any java files run explicitly in this shell
   # should use TESTJAVA in the path to the java interpreter.
   # So, we'll set this to the JDK spec'd on the command line.  If none
   # is given on the command line, tell the user that and use a default.
   # THIS IS THE JDK BEING TESTED.
   if [ -n "$1" ] ; then
          TESTJAVA=$1
      else
	  TESTJAVA=$JAVA_HOME
   fi
   TESTSRC=.
   TESTCLASSES=.
   #Deal with .class files:
fi
#
echo "JDK under test is: $TESTJAVA"
#
CP="-classpath ${TESTCLASSES}${PATHSEP}${TESTJAVA}/lib/jconsole.jar"
# Compile the test class using the classpath we need:
#
env
#
set -vx
#
#Compile.  jconsole.jar is required on the classpath.
${TESTJAVA}/bin/javac -d "${TESTCLASSES}" ${CP} -g \
                         "${TESTSRC}"/"${TARGETCLASS}".java
#
#Run the test class, again with the classpath we need:
${TESTJAVA}/bin/java ${CP} ${TARGETCLASS}
status=$?
echo "test status was: $status"
if [ $status -eq "0" ];
   then pass ""

   else fail "unspecified test failure"
fi
