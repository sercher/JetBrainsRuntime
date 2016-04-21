/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.util.jar;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.*;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.security.CodeSource;
import jdk.internal.misc.SharedSecrets;
import sun.security.action.GetPropertyAction;
import sun.security.util.ManifestEntryVerifier;
import sun.security.util.SignatureFileVerifier;

/**
 * The {@code JarFile} class is used to read the contents of a jar file
 * from any file that can be opened with {@code java.io.RandomAccessFile}.
 * It extends the class {@code java.util.zip.ZipFile} with support
 * for reading an optional {@code Manifest} entry, and support for
 * processing multi-release jar files.  The {@code Manifest} can be used
 * to specify meta-information about the jar file and its entries.
 *
 * <p>A multi-release jar file is a jar file that contains
 * a manifest with a main attribute named "Multi-Release",
 * a set of "base" entries, some of which are public classes with public
 * or protected methods that comprise the public interface of the jar file,
 * and a set of "versioned" entries contained in subdirectories of the
 * "META-INF/versions" directory.  The versioned entries are partitioned by the
 * major version of the Java platform.  A versioned entry, with a version
 * {@code n}, {@code 8 < n}, in the "META-INF/versions/{n}" directory overrides
 * the base entry as well as any entry with a version number {@code i} where
 * {@code 8 < i < n}.
 *
 * <p>By default, a {@code JarFile} for a multi-release jar file is configured
 * to process the multi-release jar file as if it were a plain (unversioned) jar
 * file, and as such an entry name is associated with at most one base entry.
 * The {@code JarFile} may be configured to process a multi-release jar file by
 * creating the {@code JarFile} with the
 * {@link JarFile#JarFile(File, boolean, int, Release)} constructor.  The
 * {@code Release} object sets a maximum version used when searching for
 * versioned entries.  When so configured, an entry name
 * can correspond with at most one base entry and zero or more versioned
 * entries. A search is required to associate the entry name with the latest
 * versioned entry whose version is less than or equal to the maximum version
 * (see {@link #getEntry(String)}).
 *
 * <p>Class loaders that utilize {@code JarFile} to load classes from the
 * contents of {@code JarFile} entries should construct the {@code JarFile}
 * by invoking the {@link JarFile#JarFile(File, boolean, int, Release)}
 * constructor with the value {@code Release.RUNTIME} assigned to the last
 * argument.  This assures that classes compatible with the major
 * version of the running JVM are loaded from multi-release jar files.
 *
 * <p>If the verify flag is on when opening a signed jar file, the content of
 * the file is verified against its signature embedded inside the file. Please
 * note that the verification process does not include validating the signer's
 * certificate. A caller should inspect the return value of
 * {@link JarEntry#getCodeSigners()} to further determine if the signature
 * can be trusted.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * @implNote
 * <div class="block">
 * If the API can not be used to configure a {@code JarFile} (e.g. to override
 * the configuration of a compiled application or library), two {@code System}
 * properties are available.
 * <ul>
 * <li>
 * {@code jdk.util.jar.version} can be assigned a value that is the
 * {@code String} representation of a non-negative integer
 * {@code <= Version.current().major()}.  The value is used to set the effective
 * runtime version to something other than the default value obtained by
 * evaluating {@code Version.current().major()}. The effective runtime version
 * is the version that the {@link JarFile#JarFile(File, boolean, int, Release)}
 * constructor uses when the value of the last argument is
 * {@code Release.RUNTIME}.
 * </li>
 * <li>
 * {@code jdk.util.jar.enableMultiRelease} can be assigned one of the three
 * {@code String} values <em>true</em>, <em>false</em>, or <em>force</em>.  The
 * value <em>true</em>, the default value, enables multi-release jar file
 * processing.  The value <em>false</em> disables multi-release jar processing,
 * ignoring the "Multi-Release" manifest attribute, and the versioned
 * directories in a multi-release jar file if they exist.  Furthermore,
 * the method {@link JarFile#isMultiRelease()} returns <em>false</em>. The value
 * <em>force</em> causes the {@code JarFile} to be initialized to runtime
 * versioning after construction.  It effectively does the same as this code:
 * {@code (new JarFile(File, boolean, int, Release.RUNTIME)}.
 * </li>
 * </ul>
 * </div>
 *
 * @author  David Connelly
 * @see     Manifest
 * @see     java.util.zip.ZipFile
 * @see     java.util.jar.JarEntry
 * @since   1.2
 */
public
class JarFile extends ZipFile {
    private final static int BASE_VERSION;
    private final static int RUNTIME_VERSION;
    private final static boolean MULTI_RELEASE_ENABLED;
    private final static boolean MULTI_RELEASE_FORCED;
    private SoftReference<Manifest> manRef;
    private JarEntry manEntry;
    private JarVerifier jv;
    private boolean jvInitialized;
    private boolean verify;
    private final int version;
    private boolean notVersioned;
    private final boolean runtimeVersioned;
    private boolean isMultiRelease;    // is jar multi-release?

    // indicates if Class-Path attribute present
    private boolean hasClassPathAttribute;
    // true if manifest checked for special attributes
    private volatile boolean hasCheckedSpecialAttributes;

    static {
        // Set up JavaUtilJarAccess in SharedSecrets
        SharedSecrets.setJavaUtilJarAccess(new JavaUtilJarAccessImpl());

        BASE_VERSION = 8;  // one less than lowest version for versioned entries
        int runtimeVersion = jdk.Version.current().major();
        String jarVersion =
                GetPropertyAction.getProperty("jdk.util.jar.version");
        if (jarVersion != null) {
            int jarVer = Integer.parseInt(jarVersion);
            runtimeVersion = (jarVer > runtimeVersion)
                    ? runtimeVersion : Math.max(jarVer, 0);
        }
        RUNTIME_VERSION = runtimeVersion;
        String enableMultiRelease = GetPropertyAction
                .getProperty("jdk.util.jar.enableMultiRelease", "true");
        switch (enableMultiRelease) {
            case "true":
            default:
                MULTI_RELEASE_ENABLED = true;
                MULTI_RELEASE_FORCED = false;
                break;
            case "false":
                MULTI_RELEASE_ENABLED = false;
                MULTI_RELEASE_FORCED = false;
                break;
            case "force":
                MULTI_RELEASE_ENABLED = true;
                MULTI_RELEASE_FORCED = true;
                break;
        }
    }

    /**
     * A set of constants that represent the entries in either the base directory
     * or one of the versioned directories in a multi-release jar file.  It's
     * possible for a multi-release jar file to contain versioned directories
     * that are not represented by the constants of the {@code Release} enum.
     * In those cases, the entries will not be located by this {@code JarFile}
     * through the aliasing mechanism, but they can be directly accessed by
     * specifying the full path name of the entry.
     *
     * @since 9
     */
    public enum Release {
        /**
         * Represents unversioned entries, or entries in "regular", as opposed
         * to multi-release jar files.
         */
        BASE(BASE_VERSION),

        /**
         * Represents entries found in the META-INF/versions/9 directory of a
         * multi-release jar file.
         */
        VERSION_9(9),

        // fill in the "blanks" for future releases

        /**
         * Represents entries found in the META-INF/versions/{n} directory of a
         * multi-release jar file, where {@code n} is the effective runtime
         * version of the jar file.
         *
         * @implNote
         * <div class="block">
         * The effective runtime version is determined
         * by evaluating {@code Version.current().major()} or by using the value
         * of the {@code jdk.util.jar.version} System property if it exists.
         * </div>
         */
        RUNTIME(RUNTIME_VERSION);

        Release(int version) {
            this.version = version;
        }

        private static Release valueOf(int version) {
            return version <= BASE.value() ? BASE : valueOf("VERSION_" + version);
        }

        private final int version;

        private int value() {
            return this.version;
        }
    }

    private static final String META_INF = "META-INF/";

    private static final String META_INF_VERSIONS = META_INF + "versions/";

    /**
     * The JAR manifest file name.
     */
    public static final String MANIFEST_NAME = META_INF + "MANIFEST.MF";

    /**
     * Creates a new {@code JarFile} to read from the specified
     * file {@code name}. The {@code JarFile} will be verified if
     * it is signed.
     * @param name the name of the jar file to be opened for reading
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     */
    public JarFile(String name) throws IOException {
        this(new File(name), true, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * file {@code name}.
     * @param name the name of the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     */
    public JarFile(String name, boolean verify) throws IOException {
        this(new File(name), verify, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object. The {@code JarFile} will be verified if
     * it is signed.
     * @param file the jar file to be opened for reading
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     */
    public JarFile(File file) throws IOException {
        this(file, true, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object.
     * @param file the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager.
     */
    public JarFile(File file, boolean verify) throws IOException {
        this(file, verify, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object in the specified mode.  The mode argument
     * must be either {@code OPEN_READ} or {@code OPEN_READ | OPEN_DELETE}.
     *
     * @param file the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @param mode the mode in which the file is to be opened
     * @throws IOException if an I/O error has occurred
     * @throws IllegalArgumentException
     *         if the {@code mode} argument is invalid
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     * @since 1.3
     */
    public JarFile(File file, boolean verify, int mode) throws IOException {
        this(file, verify, mode, Release.BASE);
        this.notVersioned = true;
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object in the specified mode.  The mode argument
     * must be either {@code OPEN_READ} or {@code OPEN_READ | OPEN_DELETE}.
     * The version argument configures the {@code JarFile} for processing
     * multi-release jar files.
     *
     * @param file the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @param mode the mode in which the file is to be opened
     * @param version specifies the release version for a multi-release jar file
     * @throws IOException if an I/O error has occurred
     * @throws IllegalArgumentException
     *         if the {@code mode} argument is invalid
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     * @throws NullPointerException if {@code version} is {@code null}
     * @since 9
     */
    public JarFile(File file, boolean verify, int mode, Release version) throws IOException {
        super(file, mode);
        Objects.requireNonNull(version);
        this.verify = verify;
        // version applies to multi-release jar files, ignored for regular jar files
        if (MULTI_RELEASE_FORCED) {
            this.version = RUNTIME_VERSION;
            version = Release.RUNTIME;
        } else {
            this.version = version.value();
        }
        this.runtimeVersioned = version == Release.RUNTIME;

        assert runtimeVersionExists();
    }

    private boolean runtimeVersionExists() {
        int version = jdk.Version.current().major();
        try {
            Release.valueOf(version);
            return true;
        } catch (IllegalArgumentException x) {
            System.err.println("No JarFile.Release object for release " + version);
            return false;
        }
    }

    /**
     * Returns the maximum version used when searching for versioned entries.
     *
     * @return the maximum version, or {@code Release.BASE} if this jar file is
     *         processed as if it is an unversioned jar file or is not a
     *         multi-release jar file
     * @since 9
     */
    public final Release getVersion() {
        if (isMultiRelease()) {
            return runtimeVersioned ? Release.RUNTIME : Release.valueOf(version);
        } else {
            return Release.BASE;
        }
    }

    /**
     * Indicates whether or not this jar file is a multi-release jar file.
     *
     * @return true if this JarFile is a multi-release jar file
     * @since 9
     */
    public final boolean isMultiRelease() {
        if (isMultiRelease) {
            return true;
        }
        if (MULTI_RELEASE_ENABLED && version != BASE_VERSION) {
            try {
                checkForSpecialAttributes();
            } catch (IOException io) {
                isMultiRelease = false;
            }
        }
        return isMultiRelease;
    }

    /**
     * Returns the jar file manifest, or {@code null} if none.
     *
     * @return the jar file manifest, or {@code null} if none
     *
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     * @throws IOException  if an I/O error has occurred
     */
    public Manifest getManifest() throws IOException {
        return getManifestFromReference();
    }

    private Manifest getManifestFromReference() throws IOException {
        Manifest man = manRef != null ? manRef.get() : null;

        if (man == null) {

            JarEntry manEntry = getManEntry();

            // If found then load the manifest
            if (manEntry != null) {
                if (verify) {
                    byte[] b = getBytes(manEntry);
                    man = new Manifest(new ByteArrayInputStream(b));
                    if (!jvInitialized) {
                        jv = new JarVerifier(b);
                    }
                } else {
                    man = new Manifest(super.getInputStream(manEntry));
                }
                manRef = new SoftReference<>(man);
            }
        }
        return man;
    }

    private String[] getMetaInfEntryNames() {
        return jdk.internal.misc.SharedSecrets.getJavaUtilZipFileAccess()
                                              .getMetaInfEntryNames((ZipFile)this);
    }

    /**
     * Returns the {@code JarEntry} for the given base entry name or
     * {@code null} if not found.
     *
     * <p>If this {@code JarFile} is a multi-release jar file and is configured
     * to be processed as such, then a search is performed to find and return
     * a {@code JarEntry} that is the latest versioned entry associated with the
     * given entry name.  The returned {@code JarEntry} is the versioned entry
     * corresponding to the given base entry name prefixed with the string
     * {@code "META-INF/versions/{n}/"}, for the largest value of {@code n} for
     * which an entry exists.  If such a versioned entry does not exist, then
     * the {@code JarEntry} for the base entry is returned, otherwise
     * {@code null} is returned if no entries are found.  The initial value for
     * the version {@code n} is the maximum version as returned by the method
     * {@link JarFile#getVersion()}.
     *
     * @param name the jar file entry name
     * @return the {@code JarEntry} for the given entry name, or
     *         the versioned entry name, or {@code null} if not found
     *
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     *
     * @see java.util.jar.JarEntry
     *
     * @implSpec
     * <div class="block">
     * This implementation invokes {@link JarFile#getEntry(String)}.
     * </div>
     */
    public JarEntry getJarEntry(String name) {
        return (JarEntry)getEntry(name);
    }

    /**
     * Returns the {@code ZipEntry} for the given base entry name or
     * {@code null} if not found.
     *
     * <p>If this {@code JarFile} is a multi-release jar file and is configured
     * to be processed as such, then a search is performed to find and return
     * a {@code ZipEntry} that is the latest versioned entry associated with the
     * given entry name.  The returned {@code ZipEntry} is the versioned entry
     * corresponding to the given base entry name prefixed with the string
     * {@code "META-INF/versions/{n}/"}, for the largest value of {@code n} for
     * which an entry exists.  If such a versioned entry does not exist, then
     * the {@code ZipEntry} for the base entry is returned, otherwise
     * {@code null} is returned if no entries are found.  The initial value for
     * the version {@code n} is the maximum version as returned by the method
     * {@link JarFile#getVersion()}.
     *
     * @param name the jar file entry name
     * @return the {@code ZipEntry} for the given entry name or
     *         the versioned entry name or {@code null} if not found
     *
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     *
     * @see java.util.zip.ZipEntry
     *
     * @implSpec
     * <div class="block">
     * This implementation may return a versioned entry for the requested name
     * even if there is not a corresponding base entry.  This can occur
     * if there is a private or package-private versioned entry that matches.
     * If a subclass overrides this method, assure that the override method
     * invokes {@code super.getEntry(name)} to obtain all versioned entries.
     * </div>
     */
    public ZipEntry getEntry(String name) {
        ZipEntry ze = super.getEntry(name);
        if (ze != null) {
            return new JarFileEntry(ze);
        }
        // no matching base entry, but maybe there is a versioned entry,
        // like a new private class
        if (isMultiRelease()) {
            ze = new ZipEntry(name);
            ZipEntry vze = getVersionedEntry(ze);
            if (ze != vze) {
                return new JarFileEntry(name, vze);
            }
        }
        return null;
    }

    private class JarEntryIterator implements Enumeration<JarEntry>,
            Iterator<JarEntry>
    {
        final Enumeration<? extends ZipEntry> e = JarFile.super.entries();
        ZipEntry ze;

        public boolean hasNext() {
            if (notVersioned) {
                return e.hasMoreElements();
            }
            if (ze != null) {
                return true;
            }
            return findNext();
        }

        private boolean findNext() {
            while (e.hasMoreElements()) {
                ZipEntry ze2 = e.nextElement();
                if (!ze2.getName().startsWith(META_INF_VERSIONS)) {
                    ze = ze2;
                    return true;
                }
            }
            return false;
        }

        public JarEntry next() {
            ZipEntry ze2;

            if (notVersioned) {
                ze2 = e.nextElement();
                return new JarFileEntry(ze2.getName(), ze2);
            }
            if (ze != null || findNext()) {
                ze2 = ze;
                ze = null;
                return new JarFileEntry(ze2);
            }
            throw new NoSuchElementException();
        }

        public boolean hasMoreElements() {
            return hasNext();
        }

        public JarEntry nextElement() {
            return next();
        }

        public Iterator<JarEntry> asIterator() {
            return this;
        }
    }

    /**
     * Returns an enumeration of the jar file entries.  The set of entries
     * returned depends on whether or not the jar file is a multi-release jar
     * file, and on the constructor used to create the {@code JarFile}.  If the
     * jar file is not a multi-release jar file, all entries are returned,
     * regardless of how the {@code JarFile} is created.  If the constructor
     * does not take a {@code Release} argument, all entries are returned.
     * If the jar file is a multi-release jar file and the constructor takes a
     * {@code Release} argument, then the set of entries returned is equivalent
     * to the set of entries that would be returned if the set was built by
     * invoking {@link JarFile#getEntry(String)} or
     * {@link JarFile#getJarEntry(String)} with the name of each base entry in
     * the jar file.  A base entry is an entry whose path name does not start
     * with "META-INF/versions/".
     *
     * @return an enumeration of the jar file entries
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     */
    public Enumeration<JarEntry> entries() {
        return new JarEntryIterator();
    }

    /**
     * Returns an ordered {@code Stream} over all the jar file entries.
     * Entries appear in the {@code Stream} in the order they appear in
     * the central directory of the jar file.  The set of entries
     * returned depends on whether or not the jar file is a multi-release jar
     * file, and on the constructor used to create the {@code JarFile}.  If the
     * jar file is not a multi-release jar file, all entries are returned,
     * regardless of how the {@code JarFile} is created.  If the constructor
     * does not take a {@code Release} argument, all entries are returned.
     * If the jar file is a multi-release jar file and the constructor takes a
     * {@code Release} argument, then the set of entries returned is equivalent
     * to the set of entries that would be returned if the set was built by
     * invoking {@link JarFile#getEntry(String)} or
     * {@link JarFile#getJarEntry(String)} with the name of each base entry in
     * the jar file.  A base entry is an entry whose path name does not start
     * with "META-INF/versions/".
     * @return an ordered {@code Stream} of entries in this jar file
     * @throws IllegalStateException if the jar file has been closed
     * @since 1.8
     */
    public Stream<JarEntry> stream() {
        return StreamSupport.stream(Spliterators.spliterator(
                new JarEntryIterator(), size(),
                Spliterator.ORDERED | Spliterator.DISTINCT |
                        Spliterator.IMMUTABLE | Spliterator.NONNULL), false);
    }

    private ZipEntry searchForVersionedEntry(final int version, String name) {
        ZipEntry vze = null;
        String sname = "/" + name;
        int i = version;
        while (i > BASE_VERSION) {
            vze = super.getEntry(META_INF_VERSIONS + i + sname);
            if (vze != null) break;
            i--;
        }
        return vze;
    }

    private ZipEntry getVersionedEntry(ZipEntry ze) {
        ZipEntry vze = null;
        if (version > BASE_VERSION && !ze.isDirectory()) {
            String name = ze.getName();
            if (!name.startsWith(META_INF)) {
                vze = searchForVersionedEntry(version, name);
            }
        }
        return vze == null ? ze : vze;
    }

    private class JarFileEntry extends JarEntry {
        final private String name;

        JarFileEntry(ZipEntry ze) {
            super(isMultiRelease() ? getVersionedEntry(ze) : ze);
            this.name = ze.getName();
        }
        JarFileEntry(String name, ZipEntry vze) {
            super(vze);
            this.name = name;
        }
        public Attributes getAttributes() throws IOException {
            Manifest man = JarFile.this.getManifest();
            if (man != null) {
                return man.getAttributes(super.getName());
            } else {
                return null;
            }
        }
        public Certificate[] getCertificates() {
            try {
                maybeInstantiateVerifier();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (certs == null && jv != null) {
                certs = jv.getCerts(JarFile.this, reifiedEntry());
            }
            return certs == null ? null : certs.clone();
        }
        public CodeSigner[] getCodeSigners() {
            try {
                maybeInstantiateVerifier();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (signers == null && jv != null) {
                signers = jv.getCodeSigners(JarFile.this, reifiedEntry());
            }
            return signers == null ? null : signers.clone();
        }
        JarFileEntry reifiedEntry() {
            if (isMultiRelease()) {
                String entryName = super.getName();
                return entryName.equals(this.name) ? this : new JarFileEntry(entryName, this);
            }
            return this;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /*
     * Ensures that the JarVerifier has been created if one is
     * necessary (i.e., the jar appears to be signed.) This is done as
     * a quick check to avoid processing of the manifest for unsigned
     * jars.
     */
    private void maybeInstantiateVerifier() throws IOException {
        if (jv != null) {
            return;
        }

        if (verify) {
            String[] names = getMetaInfEntryNames();
            if (names != null) {
                for (String nameLower : names) {
                    String name = nameLower.toUpperCase(Locale.ENGLISH);
                    if (name.endsWith(".DSA") ||
                        name.endsWith(".RSA") ||
                        name.endsWith(".EC") ||
                        name.endsWith(".SF")) {
                        // Assume since we found a signature-related file
                        // that the jar is signed and that we therefore
                        // need a JarVerifier and Manifest
                        getManifest();
                        return;
                    }
                }
            }
            // No signature-related files; don't instantiate a
            // verifier
            verify = false;
        }
    }


    /*
     * Initializes the verifier object by reading all the manifest
     * entries and passing them to the verifier.
     */
    private void initializeVerifier() {
        ManifestEntryVerifier mev = null;

        // Verify "META-INF/" entries...
        try {
            String[] names = getMetaInfEntryNames();
            if (names != null) {
                for (String name : names) {
                    String uname = name.toUpperCase(Locale.ENGLISH);
                    if (MANIFEST_NAME.equals(uname)
                            || SignatureFileVerifier.isBlockOrSF(uname)) {
                        JarEntry e = getJarEntry(name);
                        if (e == null) {
                            throw new JarException("corrupted jar file");
                        }
                        if (mev == null) {
                            mev = new ManifestEntryVerifier
                                (getManifestFromReference());
                        }
                        byte[] b = getBytes(e);
                        if (b != null && b.length > 0) {
                            jv.beginEntry(e, mev);
                            jv.update(b.length, b, 0, b.length, mev);
                            jv.update(-1, null, 0, 0, mev);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            // if we had an error parsing any blocks, just
            // treat the jar file as being unsigned
            jv = null;
            verify = false;
            if (JarVerifier.debug != null) {
                JarVerifier.debug.println("jarfile parsing error!");
                ex.printStackTrace();
            }
        }

        // if after initializing the verifier we have nothing
        // signed, we null it out.

        if (jv != null) {

            jv.doneWithMeta();
            if (JarVerifier.debug != null) {
                JarVerifier.debug.println("done with meta!");
            }

            if (jv.nothingToVerify()) {
                if (JarVerifier.debug != null) {
                    JarVerifier.debug.println("nothing to verify!");
                }
                jv = null;
                verify = false;
            }
        }
    }

    /*
     * Reads all the bytes for a given entry. Used to process the
     * META-INF files.
     */
    private byte[] getBytes(ZipEntry ze) throws IOException {
        try (InputStream is = super.getInputStream(ze)) {
            int len = (int)ze.getSize();
            int bytesRead;
            byte[] b;
            // trust specified entry sizes when reasonably small
            if (len != -1 && len <= 65535) {
                b = new byte[len];
                bytesRead = is.readNBytes(b, 0, len);
            } else {
                b = is.readAllBytes();
                bytesRead = b.length;
            }
            if (len != -1 && len != bytesRead) {
                throw new EOFException("Expected:" + len + ", read:" + bytesRead);
            }
            return b;
        }
    }

    /**
     * Returns an input stream for reading the contents of the specified
     * zip file entry.
     * @param ze the zip file entry
     * @return an input stream for reading the contents of the specified
     *         zip file entry
     * @throws ZipException if a zip file format error has occurred
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if any of the jar file entries
     *         are incorrectly signed.
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     */
    public synchronized InputStream getInputStream(ZipEntry ze)
        throws IOException
    {
        maybeInstantiateVerifier();
        if (jv == null) {
            return super.getInputStream(ze);
        }
        if (!jvInitialized) {
            initializeVerifier();
            jvInitialized = true;
            // could be set to null after a call to
            // initializeVerifier if we have nothing to
            // verify
            if (jv == null)
                return super.getInputStream(ze);
        }

        // wrap a verifier stream around the real stream
        return new JarVerifier.VerifierStream(
            getManifestFromReference(),
            verifiableEntry(ze),
            super.getInputStream(ze),
            jv);
    }

    private JarEntry verifiableEntry(ZipEntry ze) {
        if (ze instanceof JarFileEntry) {
            // assure the name and entry match for verification
            return ((JarFileEntry)ze).reifiedEntry();
        }
        ze = getJarEntry(ze.getName());
        if (ze instanceof JarFileEntry) {
            return ((JarFileEntry)ze).reifiedEntry();
        }
        return (JarEntry)ze;
    }

    // Statics for hand-coded Boyer-Moore search
    private static final byte[] CLASSPATH_CHARS =
            {'C','L','A','S','S','-','P','A','T','H', ':', ' '};

    // The bad character shift for "class-path:"
    private static final byte[] CLASSPATH_LASTOCC;

    private static final byte[] MULTIRELEASE_CHARS =
            {'M','U','L','T','I','-','R','E','L','E', 'A', 'S', 'E', ':',
                    ' ', 'T', 'R', 'U', 'E'};

    // The bad character shift for "multi-release: "
    private static final byte[] MULTIRELEASE_LASTOCC;

    static {
        CLASSPATH_LASTOCC = new byte[64];
        CLASSPATH_LASTOCC[(int)'C' - 32] = 1;
        CLASSPATH_LASTOCC[(int)'L' - 32] = 2;
        CLASSPATH_LASTOCC[(int)'S' - 32] = 5;
        CLASSPATH_LASTOCC[(int)'-' - 32] = 6;
        CLASSPATH_LASTOCC[(int)'P' - 32] = 7;
        CLASSPATH_LASTOCC[(int)'A' - 32] = 8;
        CLASSPATH_LASTOCC[(int)'T' - 32] = 9;
        CLASSPATH_LASTOCC[(int)'H' - 32] = 10;
        CLASSPATH_LASTOCC[(int)':' - 32] = 11;
        CLASSPATH_LASTOCC[(int)' ' - 32] = 12;

        MULTIRELEASE_LASTOCC = new byte[64];
        MULTIRELEASE_LASTOCC[(int)'M' - 32] = 1;
        MULTIRELEASE_LASTOCC[(int)'I' - 32] = 5;
        MULTIRELEASE_LASTOCC[(int)'-' - 32] = 6;
        MULTIRELEASE_LASTOCC[(int)'L' - 32] = 9;
        MULTIRELEASE_LASTOCC[(int)'A' - 32] = 11;
        MULTIRELEASE_LASTOCC[(int)'S' - 32] = 12;
        MULTIRELEASE_LASTOCC[(int)':' - 32] = 14;
        MULTIRELEASE_LASTOCC[(int)' ' - 32] = 15;
        MULTIRELEASE_LASTOCC[(int)'T' - 32] = 16;
        MULTIRELEASE_LASTOCC[(int)'R' - 32] = 17;
        MULTIRELEASE_LASTOCC[(int)'U' - 32] = 18;
        MULTIRELEASE_LASTOCC[(int)'E' - 32] = 19;
    }

    private JarEntry getManEntry() {
        if (manEntry == null) {
            // First look up manifest entry using standard name
            ZipEntry manEntry = super.getEntry(MANIFEST_NAME);
            if (manEntry == null) {
                // If not found, then iterate through all the "META-INF/"
                // entries to find a match.
                String[] names = getMetaInfEntryNames();
                if (names != null) {
                    for (String name : names) {
                        if (MANIFEST_NAME.equals(name.toUpperCase(Locale.ENGLISH))) {
                            manEntry = super.getEntry(name);
                            break;
                        }
                    }
                }
            }
            this.manEntry = (manEntry == null)
                    ? null
                    : new JarFileEntry(manEntry.getName(), manEntry);
        }
        return manEntry;
    }

   /**
    * Returns {@code true} iff this JAR file has a manifest with the
    * Class-Path attribute
    */
    boolean hasClassPathAttribute() throws IOException {
        checkForSpecialAttributes();
        return hasClassPathAttribute;
    }

    /**
     * Returns true if the pattern {@code src} is found in {@code b}.
     * The {@code lastOcc} array is the precomputed bad character shifts.
     * Since there are no repeated substring in our search strings,
     * the good suffix shifts can be replaced with a comparison.
     */
    private int match(byte[] src, byte[] b, byte[] lastOcc) {
        int len = src.length;
        int last = b.length - len;
        int i = 0;
        next:
        while (i <= last) {
            for (int j = (len - 1); j >= 0; j--) {
                byte c = b[i + j];
                if (c >= ' ' && c <= 'z') {
                    if (c >= 'a') c -= 32; // Canonicalize

                    if (c != src[j]) {
                        // no match
                        int goodShift = (j < len - 1) ? len : 1;
                        int badShift = lastOcc[c - 32];
                        i += Math.max(j + 1 - badShift, goodShift);
                        continue next;
                    }
                } else {
                    // no match, character not valid for name
                    i += len;
                    continue next;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * On first invocation, check if the JAR file has the Class-Path
     * and the Multi-Release attribute. A no-op on subsequent calls.
     */
    private void checkForSpecialAttributes() throws IOException {
        if (hasCheckedSpecialAttributes) {
            return;
        }
        synchronized (this) {
            if (hasCheckedSpecialAttributes) {
                return;
            }
            JarEntry manEntry = getManEntry();
            if (manEntry != null) {
                byte[] b = getBytes(manEntry);
                hasClassPathAttribute = match(CLASSPATH_CHARS, b,
                        CLASSPATH_LASTOCC) != -1;
                // is this a multi-release jar file
                if (MULTI_RELEASE_ENABLED && version != BASE_VERSION) {
                    int i = match(MULTIRELEASE_CHARS, b, MULTIRELEASE_LASTOCC);
                    if (i != -1) {
                        i += MULTIRELEASE_CHARS.length;
                        if (i < b.length) {
                            byte c = b[i++];
                            // Check that the value is followed by a newline
                            // and does not have a continuation
                            if (c == '\n' &&
                                    (i == b.length || b[i] != ' ')) {
                                isMultiRelease = true;
                            } else if (c == '\r') {
                                if (i == b.length) {
                                    isMultiRelease = true;
                                } else {
                                    c = b[i++];
                                    if (c == '\n') {
                                        if (i == b.length || b[i] != ' ') {
                                            isMultiRelease = true;
                                        }
                                    } else if (c != ' ') {
                                        isMultiRelease = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            hasCheckedSpecialAttributes = true;
        }
    }

    private synchronized void ensureInitialization() {
        try {
            maybeInstantiateVerifier();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (jv != null && !jvInitialized) {
            initializeVerifier();
            jvInitialized = true;
        }
    }

    JarEntry newEntry(ZipEntry ze) {
        return new JarFileEntry(ze);
    }

    Enumeration<String> entryNames(CodeSource[] cs) {
        ensureInitialization();
        if (jv != null) {
            return jv.entryNames(this, cs);
        }

        /*
         * JAR file has no signed content. Is there a non-signing
         * code source?
         */
        boolean includeUnsigned = false;
        for (CodeSource c : cs) {
            if (c.getCodeSigners() == null) {
                includeUnsigned = true;
                break;
            }
        }
        if (includeUnsigned) {
            return unsignedEntryNames();
        } else {
            return new Enumeration<>() {

                public boolean hasMoreElements() {
                    return false;
                }

                public String nextElement() {
                    throw new NoSuchElementException();
                }
            };
        }
    }

    /**
     * Returns an enumeration of the zip file entries
     * excluding internal JAR mechanism entries and including
     * signed entries missing from the ZIP directory.
     */
    Enumeration<JarEntry> entries2() {
        ensureInitialization();
        if (jv != null) {
            return jv.entries2(this, super.entries());
        }

        // screen out entries which are never signed
        final Enumeration<? extends ZipEntry> enum_ = super.entries();
        return new Enumeration<>() {

            ZipEntry entry;

            public boolean hasMoreElements() {
                if (entry != null) {
                    return true;
                }
                while (enum_.hasMoreElements()) {
                    ZipEntry ze = enum_.nextElement();
                    if (JarVerifier.isSigningRelated(ze.getName())) {
                        continue;
                    }
                    entry = ze;
                    return true;
                }
                return false;
            }

            public JarFileEntry nextElement() {
                if (hasMoreElements()) {
                    ZipEntry ze = entry;
                    entry = null;
                    return new JarFileEntry(ze);
                }
                throw new NoSuchElementException();
            }
        };
    }

    CodeSource[] getCodeSources(URL url) {
        ensureInitialization();
        if (jv != null) {
            return jv.getCodeSources(this, url);
        }

        /*
         * JAR file has no signed content. Is there a non-signing
         * code source?
         */
        Enumeration<String> unsigned = unsignedEntryNames();
        if (unsigned.hasMoreElements()) {
            return new CodeSource[]{JarVerifier.getUnsignedCS(url)};
        } else {
            return null;
        }
    }

    private Enumeration<String> unsignedEntryNames() {
        final Enumeration<JarEntry> entries = entries();
        return new Enumeration<>() {

            String name;

            /*
             * Grab entries from ZIP directory but screen out
             * metadata.
             */
            public boolean hasMoreElements() {
                if (name != null) {
                    return true;
                }
                while (entries.hasMoreElements()) {
                    String value;
                    ZipEntry e = entries.nextElement();
                    value = e.getName();
                    if (e.isDirectory() || JarVerifier.isSigningRelated(value)) {
                        continue;
                    }
                    name = value;
                    return true;
                }
                return false;
            }

            public String nextElement() {
                if (hasMoreElements()) {
                    String value = name;
                    name = null;
                    return value;
                }
                throw new NoSuchElementException();
            }
        };
    }

    CodeSource getCodeSource(URL url, String name) {
        ensureInitialization();
        if (jv != null) {
            if (jv.eagerValidation) {
                CodeSource cs = null;
                JarEntry je = getJarEntry(name);
                if (je != null) {
                    cs = jv.getCodeSource(url, this, je);
                } else {
                    cs = jv.getCodeSource(url, name);
                }
                return cs;
            } else {
                return jv.getCodeSource(url, name);
            }
        }

        return JarVerifier.getUnsignedCS(url);
    }

    void setEagerValidation(boolean eager) {
        try {
            maybeInstantiateVerifier();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (jv != null) {
            jv.setEagerValidation(eager);
        }
    }

    List<Object> getManifestDigests() {
        ensureInitialization();
        if (jv != null) {
            return jv.getManifestDigests();
        }
        return new ArrayList<>();
    }
}
