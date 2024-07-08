# Artemis4J
A Java client for the Artemis API, and a wrapper around it to simplify grading of programming exercises.

## Architecture

Artemis4J is split into two main parts: A stateless "client" part that maps 1:1 to the Artemis API, and a stateful "grading" part built on top of it.

The "client" part is entirely stateless.
It can be used without everything else, e.g. for one-off scripts or tools.
The client is mainly structured around DTOs, where each DTO describes a single request/response entity of Artemis.
DTOs contain static methods that describe associated API endpoints.

The "grading" part is stateful and provides a higher-level API for grading of programming exercises.
It is designed to suit the needs of KIT's programming lecture.
It parses grading configs, calculates points, and provides a simplified API for grading tools.
It also provides means to clone student's Git repositories.
The grading system is written in a way that (hopefully) doesn't require any API changes when Artemis changes (which happens quite frequently).
