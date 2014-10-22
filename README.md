# Lightstreamer - Basic Messenger Demo - Java Adapter
<!-- START DESCRIPTION lightstreamer-example-messenger-adapter-java -->
The *Basic Messenger Demo* is a very simple instant messenger application based on [Lightstreamer](http://www.lightstreamer.com) for its real-time communication needs.

This project shows the Data Adapter and Metadata Adapters for the *Basic Messenger Demo* and how they can be plugged into Lightstreamer Server.
 
As an example of a client using this adapter, you may refer to the [Lightstreamer - Basic Messenger Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-Messenger-client-javascript) and view the corresponding [Live Demo](http://demos.lightstreamer.com/MessengerDemo/).
<!-- END DESCRIPTION lightstreamer-example-messenger-adapter-java -->
 
## Details

Please refer [here](http://www.lightstreamer.com/latest/Lightstreamer_Allegro-Presto-Vivace_5_1_Colosseo/Lightstreamer/DOCS-SDKs/General%20Concepts.pdf) for more details about Lightstreamer Adapters.

### Dig The Code
The project is comprised of source code and a deployment example. The source code is divided into two folders.

#### Messenger DataAdapter
Contains the source code for the Messenger Data Adapter. This Data Adapter provides a basic round-trip support for client-originated messages to client-specific Items.

#### Messenger MetadataAdapter
Contains the source code for a Metadata Adapter to be associated with the Messenger Demo Data Adapter. This Metadata Adapter inherits from the reusable `LiteralBasedProvider` in [Lightstreamer - Reusable Metadata Adapters - Java Adapter](https://github.com/Weswit/Lightstreamer-example-ReusableMetadata-adapter-java) and just adds a simple support for message submission.<br>
It should not be used as a reference for a real case of client-originated message handling, as no guaranteed delivery and no clustering support is shown.

#### The Adapter Set Configuration
This Adapter Set Name is configured and will be referenced by the clients as `MESSENGER`.

The `adapters.xml` file for this demo should look like:
```xml      
<?xml version="1.0"?>
  <adapters_conf id="MESSENGER">

    <!-- Mandatory. Define the Metadata Adapter. -->
    <metadata_provider>
      <!-- Mandatory. Java class name of the adapter. -->
      <adapter_class>messenger_demo.adapters.IMMetadataAdapter</adapter_class>
      <!-- Optional for IMMetadataAdapter.
          Configuration file for the Adapter's own logging.
          Logging is managed through log4j. -->
      <param name="log_config">adapters_log_conf.xml</param>
      <param name="log_config_refresh_seconds">10</param>

      <!-- Optional, managed by the inherited LiteralBasedProvider.
           See LiteralBasedProvider javadoc. -->
      <param name="item_family_1">buddy_list</param>
      <param name="modes_for_item_family_1">COMMAND</param>
      <!-- provided by the Data Provider for each client session -->
      <param name="item_family_2">im_.*</param>
      <param name="modes_for_item_family_2">DISTINCT</param>
      
    </metadata_provider>

    <data_provider name="SIMPLE_MESSENGER">

      <!-- Mandatory. Java class name of the adapter. -->
      <adapter_class>messenger_demo.adapters.IMDataAdapter</adapter_class>

      <!-- Optional for IMDataAdapter.
           Configuration file for the Adapter's own logging.
           Leans on the Metadata Adapter for the configuration refresh.
           Logging is managed through log4j. -->
      <param name="log_config">adapters_log_conf.xml</param>
    </data_provider>

  </adapters_conf>
```

## Install
If you want to install a version of this demo in your local Lightstreamer Server, follow these steps:
* Download *Lightstreamer Server* (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from [Lightstreamer Download page](http://www.lightstreamer.com/download.htm), and install it, as explained in the `GETTING_STARTED.TXT` file in the installation home directory.
* In the `adapters` folder of your Lightstreamer Server installation, you may find a `Demo` folder containing some adapters ready-made for several demo, including the Instant Messender one. If this is the case, you already have a Basic Messenger Demo Adapter installed and you may stop here. Please note that, in case of `Demo` folder already installed, the MetaData Adapter jar installed is a mixed one that combines the functionality of several demos. If the `Demo` folder is not installed, or you have removed it, or you want to install the Messenger Adapter Set alone, please continue to follow the next steps.
* Get the `deploy.zip` file of the [latest release](https://github.com/Weswit/Lightstreamer-example-Messenger-adapter-java/releases), unzip it, and copy the just unzipped `messenger` folder into the `adapters` folder of your Lightstreamer Server installation.
* Launch Lightstreamer Server.
* Test the Adapter, launching one of the [Clients Using This Adapter](https://github.com/Weswit/Lightstreamer-example-Messenger-adapter-java#clients-using-this-adapter).

## Build
To build your own version of `LS_messenger_data_adapter.jar` and `LS_messenger_metadata_adapter.jar`, instead of using the one provided in the `deploy.zip` file from the [Install](https://github.com/Weswit/Lightstreamer-example-Chat-adapter-java#install) section above, follow these steps:
* Clone this project.
* Get the `ls-adapter-interface.jar`, `ls-generic-adapters.jar`, and `log4j-1.2.15.jar` files from the [latest Lightstreamer distribution](http://www.lightstreamer.com/download), and copy them into the `lib` directory.
* Create the jars `LS_messenger_data_adapter.jar` and `LS_messenger_metadata_adapter.jar` created for something like these commands:
```sh
> javac -source 1.7 -target 1.7 -nowarn -g -classpath compile_libs/log4j-1.2.15.jar;compile_libs/ls-adapter-interface/ls-adapter-interface.jar;compile_libs/ls-generic-adapters/ls-generic-adapters.jar -sourcepath src/src_data -d tmp_classes src/src_data/messenger_demo/adapters/IMDataAdapter.java
 
> jar cvf LS_messenger_data_adapter.jar -C tmp_classes src_data
 
> javac -source 1.7 -target 1.7 -nowarn -g -classpath compile_libs/log4j-1.2.15.jar;compile_libs/ls-adapter-interface/ls-adapter-interface.jar;compile_libs/ls-generic-adapters/ls-generic-adapters.jar;LS_messenger_data_adapter.jar -sourcepath src/src_metadata -d tmp_classes src/src_metadata/messenger_demo/adapters/IMMetadataAdapter.java
 
> jar cvf LS_messenger_metadata_adapter.jar -C tmp_classes src_metadata
```
* Stop Lightstreamer Server; copy the just compiled LS_messenger_data_adapter.jar and LS_messenger_metadata_adapter.jar in the adapters/messenger/lib folder of your Lightstreamer Server installation; restart Lightstreamer Server.

## See Also

### Clients Using This Adapter
<!-- START RELATED_ENTRIES -->

* [Lightstreamer - Basic Messenger Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-Messenger-client-javascript)

<!-- END RELATED_ENTRIES -->

### Related Projects

* [Lightstreamer - Reusable Metadata Adapters - Java Adapter](https://github.com/Weswit/Lightstreamer-example-ReusableMetadata-adapter-java)
* [Lightstreamer - Basic Chat Demo - Java Adapter](https://github.com/Weswit/Lightstreamer-example-Chat-adapter-java)
* [Lightstreamer - Basic Chat Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-chat-client-javascript)

## Lightstreamer Compatibility Notes

- Compatible with Lightstreamer SDK for Java Adapters version 5.1.x
