# MediPilot – Smart Medication Scheduling and Adherence Monitoring

MediPilot is a full-stack web application designed to help patients follow their prescribed medication schedules and help doctors monitor medication adherence.

The system supports doctor-assisted medication management, automated medication scheduling, reminders, adherence tracking, and communication between doctors and patients.

## Features

- **Role-Based Access:** Separate access for doctors, patients, and administrators.
- **Prescription Management:** Supports creating and managing patient prescriptions.
- **Medication Scheduling:** Generates medication schedules based on prescription details.
- **Medication Reminders:** Helps patients remember their scheduled medication.
- **Medication Tracking:** Records medication status as Taken or Missed.
- **Adherence Monitoring:** Calculates medication adherence and maintains history.
- **Doctor Dashboard:** Helps doctors review patient medication adherence.
- **Voice and Video Calls:** Supports communication between doctors and patients.

## Technology Stack

### Frontend
- React

### Backend
- Java
- Spring Boot
- Spring Security
- JWT Authentication

### Database
- MySQL

### Communication
- WebRTC or the calling SDK configured in the project

## Project Structure

```text
MediPilot/
├── backend/     # Backend application
├── frontend/    # React frontend application
├── tests/       # Project tests
└── README.md    # Project documentation
```

## Getting Started

### Prerequisites

Install the tools required by your project:

- Java Development Kit (JDK), compatible with the backend configuration
- Node.js and npm
- MySQL Server
- Git

### Setup

1. Clone or download this repository.
2. Configure the MySQL database according to your backend configuration.
3. Configure the backend database connection and required environment variables.
4. Start the Spring Boot backend using the instructions for your project.
5. Install the frontend dependencies from the `frontend` directory using `npm install`.
6. Start the React development server using `npm run dev` if supported by the frontend configuration.

**Note:** The exact database name, environment variables, backend commands, and frontend scripts depend on your project configuration. Check the corresponding configuration files before running the application.

## Security