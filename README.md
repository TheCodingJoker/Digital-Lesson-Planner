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

### Quick Install (Windows)

Download and run [DigitalLessonPlanner-Setup.exe](./DLP/DigitalLessonPlanner-Setup.exe). The installer includes the Java runtime and JavaFX dependencies, so Java and NetBeans do not need to be installed separately. It installs for the current Windows user and adds Start menu and optional desktop shortcuts.

To rebuild the installer from source, install JDK 23 or newer, a JavaFX SDK, Apache Ant, and Inno Setup 6, then run `powershell -ExecutionPolicy Bypass -File .\DLP\installer\build-installer.ps1` from the repository root. The script copies the finished installer to the project and your Downloads folder.

Application data is stored under `%LOCALAPPDATA%\DLP`. The encrypted database is protected for the Windows account that created it; do not copy the database file to a different Windows user or computer.

### Prerequisites
- Windows (database encryption uses Windows DPAPI)
- Java Development Kit (JDK) 23 or higher
- JavaFX SDK/runtime configured in your IDE; JavaFX may need to be installed separately
- NetBeans IDE (recommended) or any Java IDE

### Installation

1. Clone the repository:
   ```sh
   git clone https://github.com/iammelvink/NPRT63.git
   ```

2. Open the `DLP` project in NetBeans IDE or another Java IDE and configure JavaFX if it is not bundled with your JDK.
3. For a fresh setup, back up and remove `DLP/dlp.db.enc` from the clone before the first launch. The application will create a new database and default accounts.
4. Build the project (Build > Build Project)
5. Run the application (Run > Run Project)

**Database portability:** DPAPI-protected database files are tied to the Windows account that encrypted them. Do not copy `dlp.db.enc` to another computer or Windows user; transfer data using an export/import method instead.

### Default Accounts

The application creates default accounts on first run:

- **Administrator**: username `admin`, password `admin123`
- **Teacher**: username `teacher`, password `teacher123`
- **Principal**: username `principal`, password `principal123`

## Database

The application uses SQLite with the following features:
- Local database file storage
- Windows DPAPI protection for the database file when the application shuts down
- CAPS Data Snapshots for historical accuracy
- Automatic schema migration

DPAPI protects the database for the current Windows user account and Windows
installation. Existing database files from the previous AES-CBC implementation
are still supported and are migrated to DPAPI protection the next time the
application shuts down. While the application is running, SQLite uses a temporary
plaintext database file; the DPAPI-protected file is the at-rest copy.

## Security

- Argon2 password hashing (OWASP-recommended)
- Role-based access control (RBAC)
- 15-minute auto-logout for inactivity
- Database file protected with Windows DPAPI for the current user (see the Database section)
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
