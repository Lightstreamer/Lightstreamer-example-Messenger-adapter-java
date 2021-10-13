# Lightstreamer - Basic Messenger Demo - Java Adapter
<!-- START DESCRIPTION lightstreamer-example-messenger-adapter-java -->
The *Basic Messenger Demo* is a very simple instant messenger application based on [Lightstreamer](http://www.lightstreamer.com) for its real-time communication needs.

This project shows the Data Adapter and Metadata Adapters for the *Basic Messenger Demo* and how they can be plugged into Lightstreamer Server.
 
As an example of a client using this adapter, you may refer to the [Lightstreamer - Basic Messenger Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-Messenger-client-javascript) and view the corresponding [Live Demo](http://demos.lightstreamer.com/MessengerDemo/).
<!-- END DESCRIPTION lightstreamer-example-messenger-adapter-java -->
 
## Details

### Dig The Code
The project is comprised of source code and a deployment example. The source code is divided into two folders.

#### Messenger DataAdapter
Contains the source code for the Messenger Data Adapter. This Data Adapter provides a basic round-trip support for client-originated messages to client-specific Items.

#### Messenger MetadataAdapter
Contains the source code for a Metadata Adapter to be associated with the Messenger Demo Data Adapter. This Metadata Adapter inherits from the reusable `LiteralBasedProvider` in [Lightstreamer Java In-Process Adapter SDK](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess#literalbasedprovider-metadata-adapter) and just adds a simple support for message submission.<br>
It should not be used as a reference for a real case of client-originated message handling, as no guaranteed delivery and no clustering support is shown.

#### The Adapter Set Configuration
This Adapter Set Name is configured and will be referenced by the clients as `MESSENGER`.

The `adapters.xml` file for this demo should look like:
```xml      
<?xml version="1.0"?>
  <adapters_conf id="MESSENGER">

    <metadata_adapter_initialised_first>Y</metadata_adapter_initialised_first>
  
      <metadata_provider>

          <adapter_class>com.lightstreamer.examples.messenger_demo.adapters.IMMetadataAdapter</adapter_class>

          <!-- Optional, managed by the inherited LiteralBasedProvider.
               See LiteralBasedProvider javadoc. -->
          <!--
          <param name="max_bandwidth">40</param>
          <param name="max_frequency">3</param>
          <param name="buffer_size">30</param>
          <param name="distinct_snapshot_length">10</param>
          <param name="prefilter_frequency">5</param>
          <param name="allowed_users">user123,user456</param>
          -->
        
          <!-- Optional, managed by the inherited LiteralBasedProvider.
               See LiteralBasedProvider javadoc. -->
          <param name="item_family_1">buddy_list</param>
          <param name="modes_for_item_family_1">COMMAND</param>
          <!-- provided by the Data Provider for each client session -->
          <param name="item_family_2">im_.*</param>
          <param name="modes_for_item_family_2">DISTINCT</param>
        
      </metadata_provider>

      <data_provider name="SIMPLE_MESSENGER">

          <adapter_class>messenger_demo.adapters.IMDataAdapter</adapter_class>

      </data_provider>

  </adapters_conf>
```

<i>NOTE: not all configuration options of an Adapter Set are exposed by the file suggested above. 
You can easily expand your configurations using the generic template, see the [Java In-Process Adapter Interface Project](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess#configuration) for details.</i><br>
<br>
Please refer [here](https://lightstreamer.com/docs/ls-server/latest/General%20Concepts.pdf) for more details about Lightstreamer Adapters.


## Install
If you want to install a version of this demo in your local Lightstreamer Server, follow these steps:
* Download *Lightstreamer Server* (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from [Lightstreamer Download page](https://lightstreamer.com/download/), and install it, as explained in the `GETTING_STARTED.TXT` file in the installation home directory.
* Get the `deploy.zip` file of the [latest release](https://github.com/Lightstreamer/Lightstreamer-example-Messenger-adapter-java/releases), unzip it, and copy the just unzipped `MessengerDemo` folder into the `adapters` folder of your Lightstreamer Server installation.
* [Optional] Customize the logging settings in log4j configuration file: `MessengerDemo/classes/log4j2.xml`.
* Launch Lightstreamer Server.
* Test the Adapter, launching one of the [Clients Using This Adapter](https://github.com/Lightstreamer/Lightstreamer-example-Messenger-adapter-java#clients-using-this-adapter).

## Build

To build your own version of `messenger--adapter-java-x.y.z.jar` instead of using the one provided in the `deploy.zip` file from the [Install](https://github.com/Lightstreamer/Lightstreamer-example-Messenger-adapter-java#install) section above, you have two options:
either use [Maven](https://maven.apache.org/) (or other build tools) to take care of dependencies and building (recommended) or gather the necessary jars yourself and build it manually.
For the sake of simplicity only the Maven case is detailed here.

### Maven

You can easily build and run this application using Maven through the pom.xml file located in the root folder of this project. As an alternative, you can use an alternative build tool (e.g. Gradle, Ivy, etc.) by converting the provided pom.xml file.

Assuming Maven is installed and available in your path you can build the demo by running
```sh 
 mvn install dependency:copy-dependencies 
```

## See Also

### Clients Using This Adapter
<!-- START RELATED_ENTRIES -->

* [Lightstreamer - Basic Messenger Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-Messenger-client-javascript)

<!-- END RELATED_ENTRIES -->

### Related Projects

* [LiteralBasedProvider Metadata Adapter](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess#literalbasedprovider-metadata-adapter)
* [Lightstreamer - Basic Chat Demo - Java Adapter](https://github.com/Lightstreamer/Lightstreamer-example-Chat-adapter-java)
* [Lightstreamer - Basic Chat Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-chat-client-javascript)

## Lightstreamer Compatibility Notes

- Compatible with Lightstreamer SDK for Java In-Process Adapters since 7.3.
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 6.0, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-Messenger-adapter-java/tree/pre_mvn).
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 5.1, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-Messenger-adapter-java/tree/for_Lightstreamer_5.1).
