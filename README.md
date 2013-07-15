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