/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

/*
 * The certificates used by this test.
 * They are generated by script gen-certs.sh.
 */
public enum Cert {

    /*
     * Version: 3 (0x2)
     * Serial Number:
     *     65:24:13:3c:7a:98:0c:16:a2:91:9c:8e:42:84:cf:be:be:d2:f1:42
     * Signature Algorithm: sha256WithRSAEncryption
     * Issuer: CN = evil
     * Validity
     *     Not Before: Feb  8 03:59:27 2020 GMT
     *     Not After : Feb  5 03:59:27 2030 GMT
     * Subject: CN = evil
     * X509v3 extensions:
     *     X509v3 Subject Key Identifier:
     *         09:D0:E8:51:6C:0F:88:59:47:D1:FD:05:C2:00:10:D6:A4:80:04:07
     *     X509v3 Authority Key Identifier:
     *         keyid:09:D0:E8:51:6C:0F:88:59:47:D1:FD:05:C2:00:10:D6:A4:80:04:07
     */
    BAD_CERT(
        "RSA",
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC7jCCAdagAwIBAgIUZSQTPHqYDBaikZyOQoTPvr7S8UIwDQYJKoZIhvcNAQEL\n" +
        "BQAwDzENMAsGA1UEAwwEZXZpbDAeFw0yMDAyMDgwMzU5MjdaFw0zMDAyMDUwMzU5\n" +
        "MjdaMA8xDTALBgNVBAMMBGV2aWwwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK\n" +
        "AoIBAQCmirsTOW1G+LoI/Aj59lMk3KLywAbXASeTdnBoWkchuJ0QJWO/5b5kgf6Q\n" +
        "VFfe9lXof9psGIKaCGq6KsI0uqj7+7y++//l+E6GB8UshVB8MXc1SLFe8AxPYhWC\n" +
        "TXaKWyWGl7PXvugzbByFrf4IwE9+6phYkvl/zHvaMKqdwnkpXuyuBgT3BiYTSNsx\n" +
        "k1Ma+s5rqiwsOODSzwhadwmU9T4z11KypYb/DixJgHvUET4gTB+i3ll+PllVdQtX\n" +
        "zBLpEuj5HadK0PsqlOIok3eoSU+MpRqsz0gFEQ95y+Les3MlBeQ7fVKBz8GbrFDB\n" +
        "Atzca+iknEh8fkLIUUuCjTjUtLvfAgMBAAGjQjBAMB0GA1UdDgQWBBQJ0OhRbA+I\n" +
        "WUfR/QXCABDWpIAEBzAfBgNVHSMEGDAWgBQJ0OhRbA+IWUfR/QXCABDWpIAEBzAN\n" +
        "BgkqhkiG9w0BAQsFAAOCAQEAQMfPfYfVSSdsiEUOlVg6M5D90HRONzqlg/v0RqQI\n" +
        "fb3uufXJs20dg8iamVORXIIeUpGv1OQ2Rx4ndnV3bRLK6ep3gswIkOnD8z/CeNgl\n" +
        "odZPvWyklHTMenGqU2TR3ceFep/DvQkrP4aZWyr3e2fjatKR/s4pXgBwHs/hR76O\n" +
        "vDYLRDyCG/+MtUClFsc9HLedbU4Wp8JyaafFZ63/VjaIcvdHoDGNILRu5AIN/JVM\n" +
        "Sgz4blkWJxS1dlqBYwxvbpJWrHUcktsa3Bzw2zWOkTVGQJi3pMvzRBkgliNaXPi3\n" +
        "qcPViqgzVoB4QdOQBnvDtQ9+8Nt/dQY1VJFSBLxZQIefiQ==\n" +
        "-----END CERTIFICATE-----",
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCmirsTOW1G+LoI\n" +
        "/Aj59lMk3KLywAbXASeTdnBoWkchuJ0QJWO/5b5kgf6QVFfe9lXof9psGIKaCGq6\n" +
        "KsI0uqj7+7y++//l+E6GB8UshVB8MXc1SLFe8AxPYhWCTXaKWyWGl7PXvugzbByF\n" +
        "rf4IwE9+6phYkvl/zHvaMKqdwnkpXuyuBgT3BiYTSNsxk1Ma+s5rqiwsOODSzwha\n" +
        "dwmU9T4z11KypYb/DixJgHvUET4gTB+i3ll+PllVdQtXzBLpEuj5HadK0PsqlOIo\n" +
        "k3eoSU+MpRqsz0gFEQ95y+Les3MlBeQ7fVKBz8GbrFDBAtzca+iknEh8fkLIUUuC\n" +
        "jTjUtLvfAgMBAAECggEATyu2QS5Un5+QOMMvtTx/TA/DOulElyNKYBS23TTFiedM\n" +
        "ayeLIuehuf/+NziRSUILlupouGhyda04p2W6SvzNZnTGxnffr8B5+8dn2YFKwK93\n" +
        "PxJel4ZAI+C53ubaSm2IClLFwPNVSVTEvlv3XsulPu1hHQJJr5JS8meeRD72AE8G\n" +
        "brKbLlq6OGey6u9teao0m4Wo05MzaEoOx4fztPP4BiJJobuPYrdthUwfXJ2mQYeg\n" +
        "fJKl+JeLUnAXmq8e+6Zs88NzGK8Gmd2TvGnUahxSDtXHuRkB2lOrGFrEJKkAXDBx\n" +
        "2q8r3vvcay6+k95fS2HOvggFDALS37BGckWg4+HYuQKBgQDXkxw0u2G7rCYbF691\n" +
        "jil++DLNUzejZonAvA/cceVHShfAMlWCBf58cLNsY33rkRsFazhrdDoDAFqKxejB\n" +
        "xWM8U7UHiHZSznuXTL0YbUombfz+0lp/KwXcirnB7O3AdIW4lfMo/ozeMMIuEzsL\n" +
        "G/MDvbNSdawEso/qtxFvz87ctQKBgQDFxcCSyWb/SQVr3RkZkO3BW2efuANxNeUh\n" +
        "35L4inWTa8ml8UL4SrTrkfUHzu5TnBGbSb2n8CdkPnInA81dKagX6LXuONeLkX/e\n" +
        "RXyWIwWRiBkpYSaw2OGApl49DRvk2kCzwoVRWwh8qfhpC0P6AClFRaVAovYcTxm3\n" +
        "vhCJL3jmwwKBgGMLvTbhLStMEgn6nOwXECu9H6JE7NhPgVUjUupHDj/t4/GzbqQZ\n" +
        "2u4T3ewb3jwAZHjd5YNBWHIOlIsUGTgGV+zczN0ULsEnC5Pddzgk5p+3gzkVLu0k\n" +
        "uEG3H1fhYu882j+P7bPVGKXxoxYGUedtxP7gBucJF6rk28jMqd9EjFfNAoGBAKcc\n" +
        "ASwGodDzknEh0SOZIkxPP6/lfIMcVw/YKgd4dwCqAykEQuIpvdWO7sw6PYbISNg9\n" +
        "5tMQSTiayznMLKqbmD0blR5FSVvVBYZ6kFsMHJhrt1cPj/G+UEy0RsyvVvJ4uFMr\n" +
        "+hpUIUe1FwErU7TajgTKZGfJSsuAyupG3xIL2syhAoGALv+ulZAY/gUKH8NigsXo\n" +
        "pFPTpiXMyTD/O4RUH/5LcxDELVZ8cnV2q3qEX+ep24y0AtNiBx4oHpZ/vIxtwBCR\n" +
        "JKU2xmIGC6NyQMRSzfmNgi0X450rgKbTAxn/LAU8syXmNpBUrFZ8+02pQvWzxqfU\n" +
        "zGaMEK3+f1sq8Byzau/qhKU="),

    /*
     * Version: 3 (0x2)
     * Serial Number:
     *     70:41:2f:71:43:d1:67:b5:29:c6:3e:ce:62:ba:d5:aa:4a:f1:f7:f0
     * Signature Algorithm: sha256WithRSAEncryption
     * Issuer: CN = localhost
     * Validity
     *     Not Before: Feb  8 03:59:18 2020 GMT
     *     Not After : Feb  5 03:59:18 2030 GMT
     * Subject: CN = localhost
     * X509v3 extensions:
     *     X509v3 Subject Key Identifier:
     *         12:65:C7:4B:D8:77:D8:55:6E:2D:AF:C4:F8:09:FE:08:F4:22:EA:D5
     *     X509v3 Authority Key Identifier:
     *         keyid:12:65:C7:4B:D8:77:D8:55:6E:2D:AF:C4:F8:09:FE:08:F4:22:EA:D5
     */
    GOOD_CERT(
        "RSA",
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC+DCCAeCgAwIBAgIUcEEvcUPRZ7Upxj7OYrrVqkrx9/AwDQYJKoZIhvcNAQEL\n" +
        "BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTIwMDIwODAzNTkxOFoXDTMwMDIw\n" +
        "NTAzNTkxOFowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF\n" +
        "AAOCAQ8AMIIBCgKCAQEAtSOmfkF0zjPeZ4DDsJZO3OaDq+XHtPLB+xvri1iuL9b+\n" +
        "dZDXOqPZ5+koWM9NzDR6Um+IN46oTU+8eJw+hYcZaE9tzS9kH+6qOBk/827yEyVa\n" +
        "jh9Wqw164xj16QPyQJuHEeeDJ7elNfaOQXRu2UqZB9suKbolqsHe42hbg0/tbln7\n" +
        "C8C6qEJOpnEaapFHi3/3AeoQQ57zywqrzopeiiuUDWmBhXY30ve33RrJl/OIM1sB\n" +
        "QSoVCPcaF0mXaDwUTYIksxelon1K9PJa76p9ybGnsxkYfCAGZ8O+fTjJfQONU+Gu\n" +
        "zOmcyXL5D5O/nI8lxN8hbZwVIAYXLYRUonECIOJ/iQIDAQABo0IwQDAdBgNVHQ4E\n" +
        "FgQUEmXHS9h32FVuLa/E+An+CPQi6tUwHwYDVR0jBBgwFoAUEmXHS9h32FVuLa/E\n" +
        "+An+CPQi6tUwDQYJKoZIhvcNAQELBQADggEBAFatzXsT9YZ0TF66G6apSbbs6mH9\n" +
        "PMVE9IuE4yv2zyKofSMmDHFdmfNdkMHWkIxcZKuiL00IPFL76LAb9DWNQVy4otq6\n" +
        "3+n0CCi808gDNUMYMQLlXVooZsByXuMuokyg29F5mWEH4rswU6ru33lAB7CT7BuN\n" +
        "z5/eUhxTcXcJV6pLgcEM68NIc755PULevmqmd8SrVgcFjkxAFOsYd9L86wYLdiPO\n" +
        "uXfN/EjLMGHG2gpEqHEzQpEEAA/IsCJ1HQ8vvGkeggUIXPrwlIMbQcz/8WBSDel5\n" +
        "hvVRmADJCLf/0IwxKsSOMWZ4OMmcXMjxnae3lWPQomlzWHMZlFraG2rE/Vo=\n" +
        "-----END CERTIFICATE-----",
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC1I6Z+QXTOM95n\n" +
        "gMOwlk7c5oOr5ce08sH7G+uLWK4v1v51kNc6o9nn6ShYz03MNHpSb4g3jqhNT7x4\n" +
        "nD6FhxloT23NL2Qf7qo4GT/zbvITJVqOH1arDXrjGPXpA/JAm4cR54Mnt6U19o5B\n" +
        "dG7ZSpkH2y4puiWqwd7jaFuDT+1uWfsLwLqoQk6mcRpqkUeLf/cB6hBDnvPLCqvO\n" +
        "il6KK5QNaYGFdjfS97fdGsmX84gzWwFBKhUI9xoXSZdoPBRNgiSzF6WifUr08lrv\n" +
        "qn3JsaezGRh8IAZnw759OMl9A41T4a7M6ZzJcvkPk7+cjyXE3yFtnBUgBhcthFSi\n" +
        "cQIg4n+JAgMBAAECggEAD2O4AYIOKna9ro2CEr6ydJIhHbmn/feiA3Obz3r5UZcy\n" +
        "h0qG/rRtDwcAJot2UKMkwVw4dn/oTKk5mgWsSivwPKyC56vfFddxHtMGW+hRKM9D\n" +
        "ok+HTYEXr7OvMNzk+Bg+oYbJ3dX8c1k/PNBnmo578e7tPR5TlO5jwW5cWAuyYG2f\n" +
        "+YUCqMNe02yZvvlvK1kOSSgqlNH0S14/hVZTYkyxXMCCrkxPFXh5j8w6ZUzVipXg\n" +
        "99EYcRdq7dA3XVBSgQQ4m5772FIIzlBn8LdIIfw3VQrtZ9HapowLk6QdcHSHBKMK\n" +
        "0rqb1PlG2ynD2n8hKn4MssJ+tkzvbGrQcLjL/+XHAQKBgQDmiOIke90T8puq3FkN\n" +
        "NlgdBA9Zem5U2t3TvtQa36cuO/paYrLaVK5U0ucNZ9d4i6avdyp8TyKJrUHDcazi\n" +
        "QkDpjxb0KBhisutDZ4o1JFW4ZtB3zwIGIYWBBIE1kRIc0ucYoAurSdOmAsKq6XJQ\n" +
        "B0CQYBJPrTHq5niCl0tKPtrISwKBgQDJJfNcKSz46zdnqsNZAmL+q+cMQf4txiCS\n" +
        "v0JefOeKKlwNcYWxRgf1yTNENamKKh8wyqOhc/OkxXjglRo9BFMt6BFFARzDddWE\n" +
        "Wo18cyLc2WvTTv2FCZ0J/eF1jPTGJsTpCU6Prbt4XPjZpzSTF2cQR7CxLp15FsJm\n" +
        "2LMcQ8ma+wKBgQC72So8hFme2X+S+D3wECo4aoh/Zs3kgvtigQqgY0H84I6de/M1\n" +
        "CO+M2tW/DLB8336RV87cwDbqbK07rrMrIsV2C0yu4sUMF7Kwl/v8VYEr40tXdOy3\n" +
        "RjVc7ejDV1Sk/A2m+TLI/j1h9rndPqARKfeoLUB+gCg+ulHUR6fn9dOchQKBgByx\n" +
        "uj6qbQzxWQ0D0iwvZ/nWgfZAr8bN3bWxbQFXphwSoOEWEbFRQS9xzUtssEvSaHKo\n" +
        "ZaFRji8yMGUxP/X2WPtSgKwsVXMYqyXfWRGoxw9kQLp7KTVCQtG7Et+XBRADVdG8\n" +
        "jyV17ilkcedyr9BP5VbwMyeDc9ljQsYzIZHlpavjAoGAct8Wktj0hegCoTxSukU1\n" +
        "SkJ7t4376sSfxVbbUrH86Y1Z55le1O+VkGtqETmk+Q8qf5Ymnal3W9zZ0O9mOE04\n" +
        "otFbiB3ifUbpBAipyxS06SIFwMctmSk2EqBcXa3nZ9eUGqx0JhoQahfyDkFzfwJY\n" +
        "hiBTWnlMjCiJ40yRYAWDzZg="),

    /*
     * Version: 3 (0x2)
     * Serial Number:
     *     3f:62:91:39:7e:02:e9:77:20:61:ce:7e:a2:3c:c0:6c:3f:2e:08:49
     * Signature Algorithm: sha256WithRSAEncryption
     * Issuer: CN = UNKOWN
     * Validity
     *     Not Before: Feb  8 04:00:04 2020 GMT
     *     Not After : Feb  5 04:00:04 2030 GMT
     * Subject: CN = unknown
     * X509v3 extensions:
     *     X509v3 Subject Key Identifier:
     *         F7:D7:AE:80:DF:EC:7A:60:5A:E8:62:60:70:03:B6:BD:23:05:19:62
     *     X509v3 Authority Key Identifier:
     *         keyid:F7:D7:AE:80:DF:EC:7A:60:5A:E8:62:60:70:03:B6:BD:23:05:19:62
     *     X509v3 Subject Alternative Name:
     *         IP Address:127.0.0.1
     */
    LOOPBACK_CERT(
        "RSA",
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDBTCCAe2gAwIBAgIUP2KROX4C6XcgYc5+ojzAbD8uCEkwDQYJKoZIhvcNAQEL\n" +
        "BQAwEjEQMA4GA1UEAwwHdW5rbm93bjAeFw0yMDAyMDgwNDAwMDRaFw0zMDAyMDUw\n" +
        "NDAwMDRaMBIxEDAOBgNVBAMMB3Vua25vd24wggEiMA0GCSqGSIb3DQEBAQUAA4IB\n" +
        "DwAwggEKAoIBAQC8dBwc+nhzuGOcqmeQkcms6JrUPDPcvq6gEEH3dxorzngfxrsl\n" +
        "lfM6SPJBV4A7HVEcsGhcMoPzzpFVISi3XyLkGuw2WnEW6nKcB2QgaS0Ub8PoDZ7P\n" +
        "erWGOIjHF1slKxX40tZBiEp1oJANDq7CzSGWiyTorCjbX6OiWZCbhQkw+SpXrAdD\n" +
        "fzjEAr3y8cgsC7qqTxoz/T9C1+UMmzc88kpAqih7jj2L/i6387dBmV+zrMsNyO0Q\n" +
        "UPGACzMiSZV3tiwYA6cvDY3WS3fCwLSYUWdHi1orerHQuGOHLK4eyPVDcvuQdUJ/\n" +
        "T0+jbNZa51scqrBUT/aDlCMCxFUY3vquz2xfAgMBAAGjUzBRMB0GA1UdDgQWBBT3\n" +
        "166A3+x6YFroYmBwA7a9IwUZYjAfBgNVHSMEGDAWgBT3166A3+x6YFroYmBwA7a9\n" +
        "IwUZYjAPBgNVHREECDAGhwR/AAABMA0GCSqGSIb3DQEBCwUAA4IBAQBcfcv2J73T\n" +
        "nHFsCPU3WM6UW2uE8BIM/s/VbjkV1nalFyHi/TU6CN01sDymTABhzIlx5N6PW0HP\n" +
        "Z0q1C7l1nsoQHwmJO+avOHu3ZjDrLMpU6wTQLEemTd3R5HTyA3/I/FUVFHeuLwJg\n" +
        "L7OLNc8ouT1hkiIZD+xKwfCEdT3o+ldB+9L4WYRJPt2W3bf3W/yM8JmwW8uf6+U3\n" +
        "V46xiE5GoOKoIkeAkBAaIbepsZH9rPb7alBSgYgwQYDft9wuGMeNcvPvgVsXjA7I\n" +
        "RafJVdxVinVMEaOjckIZ5WlrR5667aIJapZH1r7/tiSQCRaJcILx7pL4x8C+x34z\n" +
        "dPHbbyP/Rdq9\n" +
        "-----END CERTIFICATE-----",
        "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC8dBwc+nhzuGOc\n" +
        "qmeQkcms6JrUPDPcvq6gEEH3dxorzngfxrsllfM6SPJBV4A7HVEcsGhcMoPzzpFV\n" +
        "ISi3XyLkGuw2WnEW6nKcB2QgaS0Ub8PoDZ7PerWGOIjHF1slKxX40tZBiEp1oJAN\n" +
        "Dq7CzSGWiyTorCjbX6OiWZCbhQkw+SpXrAdDfzjEAr3y8cgsC7qqTxoz/T9C1+UM\n" +
        "mzc88kpAqih7jj2L/i6387dBmV+zrMsNyO0QUPGACzMiSZV3tiwYA6cvDY3WS3fC\n" +
        "wLSYUWdHi1orerHQuGOHLK4eyPVDcvuQdUJ/T0+jbNZa51scqrBUT/aDlCMCxFUY\n" +
        "3vquz2xfAgMBAAECggEAEcYNpLoGxDs+wdbcf6kQUUt61st6xLYDODtwTUuhX0JQ\n" +
        "2AZhPjE/SF764ijDgk/Ih6EnppJpGYSA9ntzIKBLZSIY5yNuiQ/BkW+tBNWGl+fW\n" +
        "nTszoDPdjPQmCkjsorvGjbos1O9qvl9PVrvsxZidM1qaN4uNKuuBPl2eItzQOhsM\n" +
        "YFbmw1nqSX31gukv9a6yM2VgDUiGMlEGwkOphutbqt+wTO+9hEopGZHB7mNc5NO9\n" +
        "foWVVI1rzS2yR2d85lsG4YBqBMDp2s2cBofIAe/SSSpBYPR4RfEBDpSaVceR4+cL\n" +
        "Lq52DhLVe/zgVj7LEGdyTZTQxw414sRBIz8KXcRIkQKBgQDon26R0/vToZcxgnpr\n" +
        "ososGh+iTov883DCxX30sntl/GbyUhg50q7Tq5mXGyzodoFHGTL+bOcU3g5O4MOV\n" +
        "6HlTFe1dUjyC7M0ah6NaCSsl4SPTxtWjeHMBMhNisInDAO+ju4MJAhgoHuYL6p39\n" +
        "NDmKSDtpaegFz1Q64C1Ea9fsFwKBgQDPZFvQNjSCm06ObsfXLZKS6IEqgGbihMfM\n" +
        "cv/HjIpAKXNp/Y6Y/YmdFBpdHDkOJ9BXwJqTuMuM69BuldvNXkkY7zrhPFPawWyF\n" +
        "O/N1aMNCT89AreBwXMYmgG9yLm1EF1FOuz2oAnWWpcUHBups+cZQikYSQxcOSqrL\n" +
        "bNTEWffG+QKBgDTk+8lhAGQQ3EY/uwJ6k6oPjp3jamVsHXnMWmWnp/N6vxXeoO+U\n" +
        "/nfXDyeS4FVDjQXTrwq3TJwsGejJpu+RWvUPiVes+WFz4vdjXDt+1jbYyMLA9Zck\n" +
        "LlJZRpssNUcIEXWTj6oetct5qymOgbovg93zqr6/fCjGCgsRKnniY8ilAoGAcWGH\n" +
        "hGQt/v1TTDEqVexXRrOP8iFyngJDjPWN+pVN+9ftfhOeAuwRcOvNofvNAX0ovODS\n" +
        "YVJVDfzZ3atWGIekZNpdEUg++8hlQM3OwvB8V2N0hgLJQgSmW+Q5iW3yVJh+3hEl\n" +
        "mxWFHdAQ0E+ql9tR3TRLLK67CxgtGbus8o/RE1kCgYAuf9o6Q++l8H0vNZTnzBNu\n" +
        "bt0QnLxyh7RuViYuCkzLK+jGftgadVfsRgnOKvxQkMzcXfBgpV5JcVKXtaxDhPxM\n" +
        "xHwblgOEGlrD4tAwvtPw3GLhmD4Shy8zcT0Lwto81fquskA5yyDGJxbq9CMzWk3w\n" +
        "dSOT2C7lwW+hkycUio/fTQ==");

    public final String keyAlgo;
    public final String certStr;
    public final String keyStr;

    private Cert(String keyAlgo, String certStr, String keyStr) {
        this.keyAlgo = keyAlgo;
        this.certStr = certStr;
        this.keyStr = keyStr;
    }
}
