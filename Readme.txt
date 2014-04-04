==================
CS 425 Spring 2014 
	  MP 2
==================

------------------------
Netids: cting4, reziapo1
------------------------

-----------
Compilation
-----------
	- IDE 
		Import the project into a Java IDE (Eclipse or Intelli J)
		Make or build the project in the IDE
	- Manual
		javac *.java
		javac model/*.java
		javac strategy/*.java

----------------------------
Running / Command line usage
----------------------------
	java Chat [configFile] [delayTime] [dropRate] [selfId] [orderType] [printMode] [testMode]
		configFile - the path to the configuration file
		delayTime - time to delay unicast send in seconds
		dropRate - probability of dropping pakcets, between 0 amd 1.0
		selfId - the id of the chat client in configuration file
		orderType - ordering on messages, either "causal" or "total"
        printMode - "detail" or "brief" 
                    "detail" indicates to print out packet loss and retransmission event
                    "brief" indicates not to print out packet loss and retransmission event
        testMode -  "boost" or "normal"
                    "boost" indicates initially test system by automatically sending 20 messages.
                    "normal" do not initial program by automatically sending message.

	Note: there are two implementation of total ordering multicast isis algorithm and sequencer algorithm
    for sequencer algorithm, we need to start up "TotalOrderSequencer" before sending messages as follows
    (from project root folder).
		
	java model/TotalOrderSequencer [configFile]
		configFile - the path to the configuration file (same as Chat's)

	After all clients connect (also sequencer in total ordering)
	type messages to send

----------
Algorithms
----------
	- Causal Ordering
		
	- Total Ordering
