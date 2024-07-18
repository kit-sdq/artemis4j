# Artemis4J
A Java client for the [Artemis](https://github.com/ls1intum/Artemis) API, and a wrapper around it to simplify grading of programming exercises.
The project is developed and used as part of the [KIT](https://www.kit.edu)'s programming lecture, with the main user being [IntelliGrade](https://github.com/kit-sdq/intelligrade).

Features include:
- A stateless `client` API that maps 1:1 to the Artemis REST API.
- A stateful `grading` API for programming exercises built on top of the API. This API uses a simplified model of the Artemis data model.
- (Partially) automatic grading of programming exercises via the [Autograder](https://github.com/Feuermagier/autograder).



## Usage
The entry point for the `client` API is the [ArtemisClient](src/main/java/edu/kit/kastel/sdq/artemis4j/client/ArtemisClient.java) class, with which the various DTOs can be used.
The [UtilitiesTest](src/test/java/edu/kit/kastel/sdq/artemis4j/UtilitiesTest.java) class demonstrates this.

The entry point for the `grading` API is the [ArtemisConnection](src/main/java/edu/kit/kastel/sdq/artemis4j/grading/ArtemisConnection.java) class.
The [API example](src/test/java/edu/kit/kastel/sdq/artemis4j/APIExampleTest.java) demonstrates the intended usage of the `grading` API.

## Architecture

Artemis4J is split into two main parts: A stateless `client` part that maps 1:1 to the Artemis API, and a stateful `grading` part built on top of it.

The [client](src/main/java/edu/kit/kastel/sdq/artemis4j/client) part is entirely stateless.
It can be used without everything else, e.g. for one-off scripts or tools.
The client is mainly structured around DTOs, where each DTO describes a single request/response entity of Artemis.
DTOs contain static methods that describe associated API endpoints.

The [grading](src/main/java/edu/kit/kastel/sdq/artemis4j/grading) part is stateful and provides a higher-level API for grading of programming exercises.
It is designed to suit the needs of KIT's programming lecture.
It parses grading configs, calculates points, and provides a simplified API for grading tools.
It also provides means to clone student's Git repositories.
The grading system is written in a way that (hopefully) doesn't require any API changes when Artemis changes (which happens quite frequently).
