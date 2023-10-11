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


9f4a1b029d12da3a479755e223fe6e2c1fc53726 | Yugandharkumar | Create commits-preserve.yml | 2023-08-09 



1028add6058f12b11562c63c5e4210ab9ce84fef | Yugandharkumar | Update Dockerfile | 2023-08-11 


355e998aa564649e68bb92f6eebaf063b371ed42 | Kiran Godishala | added logs to trace permission denied exception and stopwatch for elapsed time (#8) | 2023-08-17 


8cb3b1a2b46ded6c823e3911940e9c646ed025ae | sanopsmx | Added logs to find which api is taking time. | 2023-08-30 


4b424d307bad9dfffb4dc1be97e1154e48f638fa | Sanjeev Thatiparthi | 1.30.1 v1.37.1 (#11) | 2023-08-30 


66b73bd5634f58caa306925f919db132b33fa34e | sanopsmx | Increased the sql properties to improve the performance of fiat-sql. | 2023-10-11 


18a6caa3141ab5df53d069cce0985f0945e84c1d | Sanjeev Thatiparthi | Increased the sql properties to improve the performance of fiat-sql. (#12) | 2023-10-11 