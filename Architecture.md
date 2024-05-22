# Architecture

## Main Principles & Goals
- Clear separation between the interaction with Artemis and the user-facing application model & functionality.
    - The Artemis API changes often, but we don't want the user-facing API to change with it.
    - We use DTOs to achieve this goal. DTOs describe a subset of the Artemis API, in the sense that they represent the
        JSON that Artemis sends and accepts, and also the HTTP calls to send and receive this JSON.
- Eager fetching of data from Artemis where practical to reduce the number of HTTP calls.
- Simple API for one-off tools and scripts, but also exposition of e.g. point calculation details for complex grading tools.
- Proper I18N support.
