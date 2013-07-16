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
    
    Usage: wolf-validator [-c <file>] [-h] [-lr <dir>] [-rr <url>] [-vr <dir>]
        -c,--config <file>                 use given configuration file,
                                           default value is `wolf-validator-config.xml`
        -h,--help                          print help and exit
        -lr,--local-repository <dir>       use given local repository,
                                           default value is `workspace/local-repository`
        -rr,--remote-repository <url>      use given remote repository,
                                           default remote repository is only maven central
        -vr,--validated-repository <dir>   validate given repository,
                                           default value is `workspace/validated-repository`
    Example: 
        to run against a given validated repository directory, use: 
        $ wolf-validator -vr ~/myrepository


How to
------

#### How to change configuration ?

Logging configuration can be changed in `wolf-validator-logback.xml` file, default logger output is console and file log.txt, located in workspace subdirectory.
Tool configuration can be changed in `wolf-validator-config.xml` file and it contains some examples already.


#### How to add remote repository ?

Remote repository can be added via command line options `-rr`, for example `$ wolf-validator -rr file://foo-repository`. 
Or permanently added in configuration file, see `fooRepository` snippet, where is variant with user authentication.


#### How to add custom validation/report ?

Validators/reporters have to implement interface `org.jboss.wolf.validator.Validator/Reporter`, 
for simple example take a look on `ChecksumValidator` implementation. 
Jar file with new validator/reporter add into `lib` subdirectory, so it will be automatically on classpath.
And register new bean in configuration file `wolf-validator-config.xml`, see example with `fooValidator`.


#### How to write reports to file ?

Reporters by default write its output to console. This can be changed globaly for all reporters, 
by configuring custom bean named `defaultReporterStream` or per one reporter with bean 
named like reporter class and with suffix stream, for examle `checksumReporterStream`.
Type of this bean should be `java.io.PrintStream`.        