# IDV Certificate Generation Instructions for Webstart, Windows, and MacOS

This document describes how to generate various certificates for the IDV. Since certificates have long expiration dates and are therefore seldom generated, I wrote down these instructions down so that they don't have to be "rediscovered" every time.

There are three scenarios for which the IDV needs certificates:

1.  Webstart (deprecated in JDK 9)
2.  code-signing the IDV Windows executable
3.  code-signing the IDV MacOS executable


## Certificate Expiration Dates

| Certificate            | Expiration Date |
|---------------------- |--------------- |
| windows.p12            | May 30, 2020    |
| Java WebStart Keystore | May 30, 2020    |
| MacOS .p12             | Feb 26  2024    |

Note that IDV releases usually happen in the spring so think ahead.


## Webstart and Windows


### Send email to CISL Help Desk

Send an email to CISL Help Desk asking you would like a **code signing** certificate (**not** an SSL certificate, make sure you are clear about this detail). This process should **not** require a private key and CSR generation via the command line and `openssl` (see next).


### Certificate Services Manager

Once CISL initiates the process with a certificate authority (e.g., InCommon), you will get an email from "Certificate Services Manager <support@cert-manager.com>" entitled "Invitation - InCommon Code Signing Certificate Enrollment". This email will contain a link that will automatically generate the private key and CSR. Click on (or copy/paste) the link from **Safari** and not any other browser. This action will insert the private key and CSR in the MacOS Keychain Access app. I have no idea how to do this from Windows or Linux.


### Certificate Email Response

You will subsequently receive an email from "Certificate Services Manager <support@cert-manager.com>" entitled "ISSUED: InCommon Code Signing certificate". This email will contain a link. Again open the link from the Safari browser. This action will insert intermediate and root certificates in the MacOS Keychain Access app.


### Open the Keychain Access App

Go to the Keychains "login", Category "Certificates" on the left side panel of the Keychain app. This will list a few certificates including the intermediate and root certificates for the certificate you just requested. Unfortunately, this part is a little tricky as you have to do some guess work to find all the certificates you need but the following combination of certificates should be what you want:

-   AddTrust External CA Root
-   InCommon RSA Code Signing CA
-   USERTrust RSA Certification Authority
-   The University Corporation for Atmospheric Research


### Exporting .p12 file (Twice!)

In the Keychain app, select the certificates listed above (Command click on MacOS) and export them as a `.p12` file. This action will require a password. For whatever accidental/historical reasons, we use two different passwords for Webstart and Windows.

1.  Export it once with the Java keystore password that is stored in `fserv:/share/idv/.keypass`. Call the exported file `keystore.p12`.
2.  Export it again with the password that is define in `fserv:/share/idv/release/build.xml`. To fetch the password, search for `winKeystorePassword`. Call the exported file `windows.p12`.


### Copy windows.p12 onto fserv

First make a back up copy of `fserv:/share/idv/Certificates/windows.p12` e.g.,:

```sh
cp windows.p12 windows.p12.8.31.2018
```

Now copy the file to fserv:

```sh
scp windows.p12 me@fserv:/share/idv/Certificates/windows.p12
```

Tomorrow, ensure the IDV nightly build has successfully completed without errors. Also, install the nightly build on a Windows machine to verify it has been properly signed with the new certificate.


### Java keystore file

Java webstart jars are signed by the Install4J build with a valid Java keystore file.


### Fetch a Java Keystore

The easiest way forward is to grab an existing IDV Java keystore file (e.g., `fserv:/share/idv/.keystore-2015`), rename it (e.g., keystore-2019 ) and empty it:

```sh
scp me@fserv:/share/idv/.keystore-2015 keystore-2019
keytool -delete -alias idv -keystore keystore-2019 -storepass xxx
```

The Java keystore password (for `-storepass` argument) is stored in `fserv:/share/idv/.keypass`.


### Import .p12 file into a Java keystore

This command will ask you for the key store password and `.p12` password described previously.

```sh
keytool -importkeystore -destkeystore keystore-2019 -srckeystore \
        keystore.p12 -srcstoretype PKCS12 -storepass xxx
```

To list this newly imported certificate:

```
keytool -v -list -keystore keystore-2019 -storepass xxx
```


### Rename the alias

The certificate alias must be named `idv`.

```sh
keytool -changealias -keystore keystore-2019 -alias 'key from cert-manager.com' \
        -destalias idv -storepass xxx
```

This command will ask you for `.p12` password described previously.


### Copy the keystore file to fserv

Copy the keystore for this current year back onto `fserv`:

```sh
scp keystore-2019 me@fserv:/share/idv/.keystore-2019
```

Fix the soft link for the IDV build:

```sh
rm /share/idv/.keystore
ln -s /share/idv/.keystore-2019 /share/idv/.keystore 
```

Tomorrow, ensure the IDV nightly build has successfully completed without errors. Also, install the IDV JNLP (<https://www.unidata.ucar.edu/software/idv/nightly/webstart/IDV/idv.jnlp>) to verify it has been properly signed with the new certificate.


### Sign Java3D Jars

Since Unidata hosts the Java3D jars, those must be signed independently with the `keytool` command line utility. First copy the new keystore to the right location:

```sh
scp keystore-2019 \
    chastang@www:/web/content/software/idv/webstart/java3D-1.6.0/
```

You only need to do this once each time the keystore is updated with a new certificate. The Java 3D jars are located `www:/web/content/software/idv/webstart/java3D-1.6.0/`. To sign them:

```sh
find . -regex ".*\.\(jar\)" | xargs -I % sh -c \
  'jarsigner % idv -tsa http://timestamp.digicert.com -keystore keystore-2019 -storepass xxx'  
```

You can verify the cert has been properly signed with:

```sh
jarsigner -certs -verify -verbose -keystore keystore-2019 \
          ./vecmath/1.6.0/vecmath.jar
```


## MacOS

On MacOS a "Developer ID" certificate (and no other kind of certificate) is mandatory. Work with CISL to obtain a `.p12` file via developer.apple.com.

The instructions that follow have been cobbled together from trial and error and Google searches. There may be better and more direct ways of going about generating the `mac.p12` file which is the final objective here. This `mac.p12` will ultimately be referenced by the install4j build to sign the IDV.


### Ask CISL for a Developer ID Certificate

Contact CISL help desk (cislhelp@ucar.edu) for a Apple Developer ID certificate. They will point you to someone who has the correct credentials to obtain this certificate from developer.apple.com. Once you are put in touch with the right person, s/he will ask for a certificate signing request or `.csr` file.


### Generate CSR

One way to do this is to use the Java `keytool` command line utility and an existing Java keystore (`.jks`) file. See above about finding and existing keystore file.

```shell
keytool -certreq -alias idv -keyalg RSA -file idv.csr -keystore idv.jks
```

This is actually a convenient way of generating a `.csr` file since the `.jks` file will already have the "Owner" and "Issuer" metadata to produce a `.csr` file.

Send that `.csr` file over to CISL. They will respond with a `.cer` certificate file with your certificate.


### Convert .cer file to a .crt file

A `.cer` file is a binary version of a certificate file. Convert it to text format:

```shell
openssl x509 -inform der -in idv.cer -out idv.crt 
```


### Extract Private Key from Keystore

For reasons that will be come clear shortly, you also need to extract the private key from the keystore file that is associated with the `.csr` file. When prompted for passwords, use the same password described earlier for the keystore file (`fserv:/share/idv/.keypass`). This is a two step process:

```shell
keytool -importkeystore -srckeystore idv.jks -destkeystore tmp.p12\
        -deststoretype PKCS12 -srcalias idv 
```

followed by

```shell
openssl pkcs12 -in tmp.p12  -nodes -nocerts -out key.pem
```

Delete `tmp.p12` as it is no longer needed.


### Retrieve Root Certificates from existing .p12 file

The certificate file from CISL may not have the Apple root certificates so you may have to retrieve them from the existing `mac.p12` file. The existing `mac.p12` file has root Apple certificates that have expiration dates far in the future. Dump the certificates to a file. When prompted for the password, see the note above about `winKeystorePassword`.

```shell
openssl pkcs12 -in mac.p12 -out intermediate.crt
```

With a text editor remove everything but the two Apple root certificates.


### Create the mac.p12

At this point, you have all the files needed to generate the `mac.p12`. Again, when prompted with a password to protect the `.p12` file use the same password as described above for `winKeystorePassword`.

```shell
openssl pkcs12 -export -out mac.p12 -inkey key.pem -in idv.crt \
        -certfile intermediate.crt
```


### Copy mac.p12 onto fserv

First make a back up copy of `fserv:/share/idv/Certificates/mac.p12` e.g.,:

```sh
cp mac.p12 mac.p12.8.31.2018
```

Now copy the file to fserv:

```sh
scp mac.p12 me@fserv:/share/idv/Certificates/mac.p12
```

Tomorrow, ensure the IDV nightly build has successfully completed without errors. Also, install the nightly build on a MacOS machine to verify it has been properly signed with the new certificate.


### Checking You Work

At this point, you will want to check the signature of the IDV to make sure it is properly signed.

After the nightly build is complete, download and install the nightly IDV. The installation process will extract the idv macos installer `.dmg` into `/Volumes/idv`. Copy the contents of that directory into a tmp directory (e.g., `/tmp/idv`) and run the commands:

```shell
codesign -dvvvv --extract-certificates \
         /tmp/idv/Integrated Data Viewer Installer.app

openssl x509 -inform DER -in codesign0 -text
```

Examine the contents of the output for signature expiration information. This step will ensure your certificate has been properly installed.
