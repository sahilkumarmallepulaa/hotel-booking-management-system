

\# Hotel Booking Management System



A full-stack hotel booking application built using Java, Spring Boot, Microservices, MySQL and React.



\## Features

\- Hotel and room management

\- Room availability checking

\- Room booking and cancellation

\- User management

\- REST API integration



\## Technology Stack

\- Java 21

\- Spring Boot

\- Spring Data JPA

\- Spring Cloud

\- Netflix Eureka

\- MySQL

\- React.js

\- HTML, CSS, JavaScript

\- Git and GitHub



\## Project Status

Under Development

"Document backend implementation progress"

## End-to-End Integration Testing

The backend microservices have successfully completed end-to-end booking integration testing.

### Test Environment

| Service | Port |
|---|---:|
| Eureka Server | 8761 |
| Hotel Service | 8081 |
| Booking Service | 8082 |
| MySQL | 3306 |

### Verified Integration Scenarios

- Eureka service registration and discovery.
- Hotel Service communication through OpenFeign.
- Successful booking creation (HTTP 201).
- Booking retrieval by booking ID and user ID.
- Overlapping booking prevention (HTTP 409).
- Back-to-back booking validation.
- Booking cancellation.
- Duplicate cancellation handling (HTTP 400).
- Database verification of booking status.

### Test Results

**9 out of 9 reported integration checks passed.**

Two test bookings were created, verified, and subsequently cancelled. No database records were deleted.

No Java source code or configuration changes were required during integration testing.

### Next Development Milestone

Implement the User Service with registration, login, password encryption, and authentication.
