# Digital Lesson Planner (DLP)

## Digital Lesson Planner - South African Curriculum Management System

[![Code License](https://img.shields.io/badge/Code%20License-GPLv2-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

## Overview

The Digital Lesson Planner (DLP) is a desktop application designed to help South African teachers create, manage, and track lesson plans aligned with the CAPS (Curriculum Assessment Policy Statement) curriculum. The system includes smart scheduling, automated CAPS curriculum data population, and real-time progress monitoring for school management.

## Technology Stack

1. Methodologies/Project Management:
   - Agile Development

2. Coding Practices:
   - OOP (Object Oriented Programming)
   - MVC (Model View Controller)
   - DAO (Data Access Object) Pattern

3. Programming Languages/Frameworks:
   - JavaFX (Frontend UI)
   - Java 23
   - SQLite (Local Database)
   - Apache POI (Word Export)
   - OpenPDF (PDF Export)
   - Argon2 (Password Hashing)

## Features

### For Teachers
- Create lesson plans with automatic CAPS curriculum data population
- Select grade-, subject-, and term-specific CAPS topics; save the selected CAPS code and curriculum snapshot with each lesson
- Track lesson completion status (Completed, Incomplete, Extended)
- Smart scheduling with automatic rescheduling (Push-Back Protocol)
- Export lesson plans to PDF and Word formats
- View progress metrics and teaching days remaining
- Calendar view with scheduled lessons and school-event details
- Review system notifications and submit feedback from the teacher portal

### For Principals
- Review completion progress for every subject area represented in teachers' lesson plans
- See teachers marked "Behind" when they have at least one extended lesson; all others are marked "On Track"
- View the current academic year's teaching days and school events
- Export a CSV progress report for all active teachers

### For Administrators
- Manage user accounts (Teachers, Principals, Administrators)
- Edit CAPS curriculum database (Strands, Topics, Codes, Outcomes)
- Set system-wide teaching-day targets by academic year and manage school calendar events (Full Day, Half Day)
- View and export the system audit log
- Manage feedback

## Instructions

### Prerequisites
- Java Development Kit (JDK) 23 or higher
- NetBeans IDE (recommended) or any Java IDE

### Installation

1. Clone the repository:
   ```sh
   git clone https://github.com/iammelvink/NPRT63.git
   ```

2. Open the project in NetBeans IDE
3. Build the project (Build > Build Project)
4. Run the application (Run > Run Project)

### Default Accounts

The application creates default accounts on first run:

- **Administrator**: username `admin`, password `admin123`
- **Teacher**: username `teacher`, password `teacher123`
- **Principal**: username `principal`, password `principal123`

## Database

The application uses SQLite with the following features:
- Local database file storage
- AES-256 encryption at rest
- CAPS Data Snapshots for historical accuracy
- Automatic schema migration

## Security

- Argon2 password hashing (OWASP-recommended)
- Role-based access control (RBAC)
- 15-minute auto-logout for inactivity
- Database encryption using AES-256
- Tamper-proof audit logging
- POPIA compliance considerations

## Author(s)

Group project for NPRT63 - System Development

Team Members:
- Kelaetswe Egon Mokhomma (202228626)
- Kearabetswe Mashiane (202320783)
- Luciano Gomba (202456755)
- Kgomotsego Katlego Maruping (202114473)

## Acknowledgments

Special thanks to the project supervisor and lecturer for guidance throughout the development process.

## Documentation

For detailed system documentation, see:
- [Phase 3 System Documentation](./We%20Code%20SA%20System%20Documentation%20(Phase%203).md)
- [Database Schema Notes](./DATABASE_SCHEMA_NOTES.md)
- [Library Compliance](./LIBRARY_COMPLIANCE.md)
