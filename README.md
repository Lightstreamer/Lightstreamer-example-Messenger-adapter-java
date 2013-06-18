
Lightstreamer Instant Messenger Demo Adapter
============================================

This project shows the Messenger Demo Data and Metadata Adapters and how they can be plugged into Lightstreamer Server and used to feed the [Messenger Demo](https://github.com/Weswit/Lightstreamer-example-Messenger-client-javascript) front-end.


Java Data Adapter and Metadata Adapter
--------------------------------------

The Data Adapter provides a basic round-trip support for client-originated messages to client-specific Items.<br>

The Metadata Adapter inherits from the reusable [LiteralBasedProvider](https://github.com/Weswit/Lightstreamer-example-ReusableMetadata-adapter-java) and just adds a simple support for message submission.<br>
It should not be used as a reference for a real case of client-originated message handling, as no guaranteed delivery and no clustering support is shown.

Configure Lightstreamer
-----------------------

After you have Downloaded and installed Lightstreamer, please go to the "adapters" folder of your Lightstreamer Server installation. You should find a "Demo" folder containing some adapter ready-made for several demo including the Messenger ones, please note that the MetaData Adapter jar installed is a mixed one that combines the functionality of several demos. If this is not your case because you have removed the "Demo" folder or you want to install the Messenger Adapter Set alone, please follow this steps to configure the Messenger Adapter Set properly:

1. Go to the "adapters" folder of your Lightstreamer Server installation. You have to create a new folder to deploy the Messenger adapters, let's call it "Messenger", and a "lib" folder inside it.
2. Create an "adapters.xml" file inside the "Messenger" folder and use the following contents (this is an example configuration, you can modify it to your liking):
```xml      
<?xml version="1.0"?>
  <!-- Mandatory. Define an Adapter Set and sets its unique ID. -->
  <adapters_conf id="DEMO">

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
3. Get the ls-adapter-interface.jar, ls-generic-adapters.jar, and log4j-1.2.15.jar files from the [Lightstreamer 5 Colosseo distribution](http://www.lightstreamer.com/download).
4. Copy into "lib" folder the jars LS_messenger_metadata_adapter.jar and LS_messenger_data_adapter.jar created for something like these commands:
```sh
 >javac -source 1.7 -target 1.7 -nowarn -g -classpath compile_libs/log4j-1.2.15.jar;compile_libs/ls-adapter-interface/ls-adapter-interface.jar;compile_libs/ls-generic-adapters/ls-generic-adapters.jar -sourcepath src/src_data -d tmp_classes src/src_data/messenger_demo/adapters/IMDataAdapter.java
 
 >jar cvf LS_messenger_data_adapter.jar -C tmp_classes src_data
 
 >javac -source 1.7 -target 1.7 -nowarn -g -classpath compile_libs/log4j-1.2.15.jar;compile_libs/ls-adapter-interface/ls-adapter-interface.jar;compile_libs/ls-generic-adapters/ls-generic-adapters.jar;LS_messenger_data_adapter.jar -sourcepath src/src_metadata -d tmp_classes src/src_metadata/messenger_demo/adapters/IMMetadataAdapter.java
 
 >jar cvf LS_messenger_metadata_adapter.jar -C tmp_classes src_metadata
```

See Also
--------

* [Lightstreamer Messenger Demo Client for JavaScript](https://github.com/Weswit/Lightstreamer-example-Messenger-client-javascript)
* [Lightstreamer Reusable Metadata Adapter in Java](https://github.com/Weswit/Lightstreamer-example-ReusableMetadata-adapter-java)

Lightstreamer Compatibility Notes
---------------------------------

- Compatible with Lightstreamer SDK for Java Adapters since 5.1
