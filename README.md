# Java implementations of Go channels

This project aims to implement Golang channels following a given framework in Java.
We will focus on 3 different implementations, and would like to implement a benchmark in order to compare the execution time and efficacy compared to the native Golang implementation.

## Added tests
* TestChannel.java: tests for the basic functionalities of the channel, such as sending and receiving messages, closing the channel, and checking if the channel is closed.
* TestFactory.java: tests for the factory class that creates instances of the channel, ensuring that it correctly creates channels of the specified type and that the created channels function as expected.
* TestSelector.java: tests for the selector class that allows for selecting between multiple channels, ensuring that it correctly handles cases where multiple channels are ready for communication and that it correctly handles cases where no channels are ready.

Run the tests: 