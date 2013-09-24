wolf-validator
==============

[![Build Status](https://travis-ci.org/thradec/wolf-validator.png)](https://travis-ci.org/thradec/wolf-validator)


Wolf-validator is a tool used to validate the internal consistency of a maven artifact repository.


Building
--------

- prerequisites Java, Maven and Git
- clone project from github `$ git clone git@github.com:thradec/wolf-validator.git`
- go into the newly created directory `$ cd wolf-validator`
- and run maven build `$ mvn clean package`
- executable distribution is available in `target/wolf-validator-$VERSION` directory or zip file


Usage
-----

This tool can be run from command line via `wolf-validator` script. The only prerequisite is Java 1.6 or greater on path.

Here is help output: 


    Wolf-validator is a tool used to validate the internal consistency of a maven artifact repository.
    
    Usage: wolf-validator [-c <file>] [-h] [-lr <dir>] [-rr <url>] [-vr <dir>] [-vd <dir>] [-r <file>]
        -c,--config <file>                 use given configuration file,
                                           default value is `wolf-validator-config.xml`
        -h,--help                          print help and exit
        -lr,--local-repository <dir>       use given local repository,
                                           default value is `workspace/local-repository`
        -rr,--remote-repository <url>      use given remote repository, this option can be used multiple times,
                                           default remote repository is only maven central
        -vr,--validated-repository <dir>   validate given repository,
                                           default value is `workspace/validated-repository`
        -vd,--validated-distribution <dir> validate given distribution, verify if current distribution is valid
                                           default value is `workspace/validated-distribution`
        -r,--report <file>                 write generated report into this file,
                                           default location for reports is in `workspace/report.txt`                                           
    Example: 
        to run against a given validated repository directory, use: 
        $ wolf-validator -vr ~/myrepository


Validators
----------

- `DependenciesValidator` try to resolve all required dependencies (scope test, runtime and provided, or optional dependencies are skipped)
- `ModelValidator` make sure that all pom files are "loadable" (maven can load it's model with strick validation level)
- `ChecksumValidator` validate checksums for all repository artifacts (by default  readme and example settings.xml are excluded from this rule)
- `JarSignatureValidator` validate that all jar files are signed/unsigned
- `SuspiciousFileValidator` try to find suspicious files in repository (eg. jar without pom, checksum without source file, empty directory, etc...)
- `BomDependencyNotFoundValidator` try to resolve all artifacts defined in dependency management
- `BomUnmanagedVersionValidator` try to find artifacts which are not defined in any bom files
- `BomAmbiguousVersionValidator` try to find artifacts which version is defined ambiguous in bom files
- `BomVersionPropertyValidator` try to find boms which define dependencies without version property
- `VersionAmiguityValidator` try to find artifacts, which have multiple versions in repository
- `VersionOverlapValidator` try to find artifacts, which overlap with others remote repositories
- `VersionPatternValidator` try to find artifacts, which version doesn't match regex pattern (eg. -redhat-x postfix)
- `JarSourcesValidator` try to find artifacts, which do not contain sources within them(verify if *-sources.jar exists)
- `XmlFileValidator` try to find xml files and then verify if they are valid
- `DistributionValidator` try to validate artifacts in distribution against validated repository


Reporters
---------

- `DefaultReporter`, `DependencyNotFoundReporter` and `BomDependencyNotFoundReporter` produces simple text reports, which are writen by default into log and into file `workspace/report.txt`
- `SurefireXmlReporter` produce xml files in same format like maven surefire plugin, which can be consumed by tools like Jenkins, default output directory is `workspace/surefire-reports`


How to
------

#### How to change configuration ?

Logging configuration can be changed in `wolf-validator-logback.xml` file, default logger output is console and file log.txt, located in workspace subdirectory.
Tool configuration can be changed in `wolf-validator-config.xml` file and it contains some examples already.


#### How to add whitelist/filter ?

Into each validator is injected file filter (interface `IOFileFilter`), which allow to skip selected files. 
By conventions, filter beans have id like validator name with suffix filter, for example `checksumValidatorFilter`. 
These filter beans can be redefined in external configuration file, see examples in xml or groovy `fooValidatorFilter `.


#### How to add remote repository ?

Remote repository can be added via command line options `-rr`, for example `$ wolf-validator -rr file://foo-repository`. 
Or permanently added in configuration file, see `fooRepository` snippet, where is variant with user authentication.


#### How to add custom validation/report ?

Validators/reporters have to implement interface `org.jboss.wolf.validator.Validator/Reporter`, 
for simple example take a look on `ChecksumValidator` implementation. 
Jar file with new validator/reporter add into `lib` subdirectory, so it will be automatically on classpath.
And register new bean in configuration file `wolf-validator-config.xml`, see example with `fooValidator`.        
