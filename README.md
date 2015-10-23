# Dropwizard Multi-Config

Have you ever wanted to have a DropWizard app which supported
overrides other than the built-in -Ddw.config.setting=override format?
Perhaps you'd like to override via having multiple .yaml files,
specified on a command line, which get merged in a well-defined
manner, yielding an effective configuration for your DropWizard app to
consume.

That's precisely what this project is for.

Once installed into your project, you can pass your project a series
of yaml files on the command line, and they will be merged by
MultipleConfigurationApplication<>, producing an effective
configuration which DropWizard will finally parse into your
configuration class.

# Motivation

If you're not sure why you might want this, here's some real-world
experiences which might help.

Suppose you have a DropWizard application which uses logging as a
means to communicate status, and which usually expects to be run by
something like launchd, init, or upstart. In that sort of environment,
you may want to have logging.appenders[0].threshold=OFF, but have
logging.appenders[1].threshold=INFO, where appenders[0] is console and
appenders[1] is a log file. However, you may also have some simple
tools, like an "install" or "update" command, which expect to be run
via a command line. In that case, you really want
logging.appenders[0].threshold=INFO, and
logging.appenders[1].threshold=OFF. You may also want to have certain
classes log at DEBUG level while the rest log at INFO level, perhaps
even just occasionally. Perhaps you have a cluster running multiple
instances of your application, and each instance must have its own
instance identifier. And when running one of the tools, you may want
to have a different instance identifier, so changes can be tracked to
the user who issued the command.

So how do you manage this? With Multi-Config, you pass a series of
yaml files to your application on the command line. Multi-Config reads
the yaml files in turn, and uses each one to build an effective
configuration.  As each yaml file is processed, new nodes will be
inserted into the effective configuration, and existing nodes will be
updated with new information.

In this manner, your service startup script might start your
application like this:

```bash
java -jar /path/server.jar server -- /path/service.yaml server-logging.yaml server-instance-id.yaml
```

While running a tool might look more like this:

```bash
java -jar /path/server.jar install -- /path/service.yaml console-logging.yaml ~/.etc/user-instance-id.yaml
```

As you can see, in both cases, you start by configuring the service
with the yaml that the service ships with. From there, you can mixin
server or console logging, and then server or user instance-ids.

You could also craft yamls to help debug a specific problem, and add
them to your server scripts temporarily:

```bash
java -jar /path/server.jar server -- /path/service.yaml server-logging.yaml server-instance-id.yaml /debug-assistance/special-logging.yaml
```

# Installation

1) Configure Maven:
```xml
<dependency>
  <groupId>com.thenewentity</groupId>
  <artifactId>dropwizard-multi-config</artifactId>
  <version>{version}</version>
</dependency>
```

2) Change your main class so that instead of extending
io.dropwizard.Application<>, it extends
com.thenewentity.utils.dropwizard.MultipleConfigurationApplication<>:

```java
import com.thenewentity.utils.dropwizard.MultipleConfigurationApplication;

public class DemoApplication extends MultipleConfigurationApplication<DemoConfiguration> {
  // ...
}
```

3) Change your main() method so that, instead of passing arguments to
Application.run(), it passes them to the constructor, and the constructor
passes them to super():

```java
import com.thenewentity.utils.dropwizard.MultipleConfigurationApplication;

public class DemoApplication extends MultipleConfigurationApplication<DemoConfiguration> {
  public static void main(String[] args) throws Exception {
    new DemoApplication(args).run();
  }

  DemoApplication(String... arguments) {
    super(arguments,
      new String[] { "server", "--", "demo-service.yaml",
        "demo-service-dev.overrides", "~/.etc/demo-service-dev.overrides" });
  }

  // ...
}
```

4) (Optional) - Change your run() method to log what happened:

```java
public class DemoApplication extends MultipleConfigurationApplication<DemoConfiguration> {
  // ...

  @Override
  public void run(FsmServiceConfiguration configuration, Environment environment) throws Exception {
    // Log configuration related stuff...
    log.info("Original command line arguments:" + StringUtils.join(getMungedCommandLine().getOriginalArguments(), " "));
    log.info("Effective command line arguments:" + StringUtils.join(getMungedCommandLine().getMungedArguments(), " "));
    log.info("Configuration override files:" + getMungedCommandLine().getYamlOverrides());
    log.info("Effective configuration\n" + getMultipleConfigurationProvider().getEffectiveConfig());

    // ...
  }
}
```

# Example

Let's suppose we have sample.yaml and override.yaml, as follows:
```yaml
# sample.yaml
server:
  applicationConnectors:
    - type: http
      port: 5307
  adminConnectors:
    - type: http
      port: 5310

logging:
  level: INFO
  loggers:
    org.eclipse.jetty: INFO
    io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper: DEBUG
```

```yaml
# override.yaml
server:
  applicationConnectors:
    - port: 5309
    - type: http
      port: 5308

logging:
  loggers:
    org.thenewentity: DEBUG
```

If you then launch your app:
```bash
$ java -jar sample.jar server -- sample.yaml override.yaml
```

This is the effective configuration you would expect:
```yaml
# effective-config
server:
  applicationConnectors:
    - type: http
      port: 5309
    - type: http
      port: 5308
  adminConnectors:
    - type: http
      port: 5310

logging:
  level: INFO
  loggers:
    org.eclipse.jetty: INFO
    io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper: DEBUG
    org.thenewentity: DEBUG
```

Of note here are that our override file was able to change the port on applicationConnector[0], add applicationConnector[1], and add org.thenewentity: DEBUG to logging.loggers.
