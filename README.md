Spinnaker Auth Service
----------------------

[![Build Status](https://api.travis-ci.org/spinnaker/fiat.svg?branch=master)](https://travis-ci.org/spinnaker/fiat)

```
   ____ _         ____ __    ___               _            ______                  _
  / __/(_)__ __  /  _// /_  / _ | ___ _ ___ _ (_)___       /_  __/____ ___ _ _  __ (_)___
 / _/ / / \ \ / _/ / / __/ / __ |/ _ `// _ `// // _ \ _     / /  / __// _ `/| |/ // /(_-<
/_/  /_/ /_\_\ /___/ \__/ /_/ |_|\_, / \_,_//_//_//_/( )   /_/  /_/   \_,_/ |___//_//___/
                                /___/                |/
```

Fiat is the authorization server for the Spinnaker system.

It exposes a RESTful interface for querying the access permissions for a particular user. It currently supports three kinds of resources:
* Accounts
* Applications
* Service Accounts

---

### Accounts
Accounts are setup within Clouddriver and queried by Fiat for its configured `requiredGroupMembership` restrictions.

### Applications
Applications are the combination of config metadata pulled from Front50 and server group names (e.g., application-stack-details). Application permissions sit beside application configuration in S3/Google Cloud Storage.

### Service Accounts
Fiat Service Accounts are groups that act as a user during automated triggers (say, from a GitHub push or Jenkins build). Authorization is built in by making the service account a member of a group specified in `requiredGroupMembership`.

---

### User Role/Authorization Providers
Currently supported user role providers are:
* Google Groups (through a Google Apps for Work organization)
* GitHub Teams
* LDAP
* File based role provider
* SAML Groups

---

### Modular builds
By default, Fiat is built with all authorization providers included. To build only a subset of
providers, use the `includeProviders` flag:
 ```
./gradlew -PincludeProviders=google-groups,ldap clean build
```
 You can view the list of all providers in `gradle.properties`.

### Debugging

To start the JVM in debug mode, set the Java system property `DEBUG=true`:
```
./gradlew -DDEBUG=true
```

The JVM will then listen for a debugger to be attached on port 7103.  The JVM will _not_ wait for the debugger
to be attached before starting Fiat; the relevant JVM arguments can be seen and modified as needed in `build.gradle`.


$${\color{lightblue} Recent \space commits:}$$ 

              CommitID                   |   Author      | Commit Message          | Commit Date
----------------------------------------------------------------------------------------------------


135add054feedb16048d5ff145213e64887d0719 | Yugandharkumar | Create commits-preserve.yml | 2023-08-09 



91dfc2361cac8e20ee936bbd8ecc42b7aa2f7b1a | Kiran Godishala | add stopwatch to collect elapsed time of relevant methods (#9) | 2023-08-21 


ae6cb3cc2759532d9eb9b08f9e2c2c177d9bca14 | sanopsmx | Added logs to find which api is taking time. | 2023-08-29 


9408e01c7dd8e40ef6bcf898e6f5d09f8051e81e | Sanjeev Thatiparthi | Version 1.16.0 1.26.6 (#10) | 2023-08-29 