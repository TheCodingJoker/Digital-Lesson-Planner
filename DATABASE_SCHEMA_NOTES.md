# Database Schema Deviations from Specification

## Known Naming Conventions Differences

### LessonPlan Table
The specification defines the following column names:
- `lessonId` (VARCHAR 36 chars, PK NOT NULL)
- `userId` (VARCHAR 36 chars, FK → User.userId)

The implementation uses:
- `lessonPlanId` (VARCHAR 36 chars, PK NOT NULL)
- `teacherId` (VARCHAR 36 chars, FK → User.userId)

### Rationale
The implementation uses more descriptive names:
- `lessonPlanId` is more explicit than `lessonId`
- `teacherId` clearly indicates this references a teacher (not just any user)

### Impact
- **Functional**: No impact - the system functions correctly with the current naming
- **Documentation**: Any external documentation should reference the implementation names
- **Migration**: If future alignment is required, a database migration script would need to:
  1. Add new columns with spec names
  2. Copy data from old columns to new columns
  3. Update all DAO, Model, and Controller references
  4. Drop old columns

### Recommendation
Keep the current naming conventions as they provide better clarity and are consistent throughout the codebase. Update external documentation to reflect the actual implementation rather than forcing a breaking change.
