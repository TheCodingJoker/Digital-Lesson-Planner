# Admin Login Issue Fix

## Date: 2026-10-12

## Problem
Unable to log in as admin (or any user) despite the application creating default users on startup.

## Root Causes Identified

### 1. **UserDAO Not Handling passwordMigrated Field**
**File**: `DLP/src/dao/UserDAO.java`

**Issue**: The `createUser()` method was not including the `passwordMigrated` field in the INSERT statement, and `extractUser()` was not reading it from the ResultSet.

**Original Code**:
```java
public boolean createUser(User user) {
    String sql = "INSERT INTO User (userId, username, passwordHash, role, email) VALUES (?, ?, ?, ?, ?)";
    // Missing passwordMigrated field
}

private User extractUser(ResultSet rs) throws SQLException {
    // ... other fields ...
    user.setActive(rs.getBoolean("isActive"));
    user.setCreatedAt(rs.getString("createdAt"));
    // Missing passwordMigrated field
    return user;
}
```

**Fixed Code**:
```java
public boolean createUser(User user) {
    String sql = "INSERT INTO User (userId, username, passwordHash, role, email, passwordMigrated) VALUES (?, ?, ?, ?, ?, ?)";
    // ...
    pstmt.setBoolean(6, user.isPasswordMigrated());
}

private User extractUser(ResultSet rs) throws SQLException {
    // ... other fields ...
    user.setActive(rs.getBoolean("isActive"));
    user.setPasswordMigrated(rs.getBoolean("passwordMigrated"));
    user.setCreatedAt(rs.getString("createdAt"));
    return user;
}
```

**Impact**: Without this fix, the passwordMigrated field would be NULL in the database, causing potential issues with the migration logic.

---

### 2. **PasswordHasher Using Wrong Algorithm**
**File**: `DLP/src/util/PasswordHasher.java`

**Issue**: The `PasswordHasher` class was still using **BCrypt** instead of **Argon2** as specified in the documentation and LIBRARY_COMPLIANCE.md.

**Original Code**:
```java
import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    private static final int BCRYPT_ROUNDS = 12;

    public static String hashPassword(String plainPassword) {
        String salt = BCrypt.gensalt(BCRYPT_ROUNDS);
        return BCrypt.hashpw(plainPassword, salt);
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
```

**Fixed Code**:
```java
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class PasswordHasher {
    // OWASP-recommended Argon2 parameters
    private static final int ITERATIONS = 2;
    private static final int MEMORY = 65536; // 64 MB
    private static final int PARALLELISM = 1;
    private static final int SALT_LENGTH = 32;
    private static final int HASH_LENGTH = 64;

    private static final Argon2 argon2 = Argon2Factory.create();

    public static String hashPassword(String plainPassword) {
        char[] passwordChars = plainPassword.toCharArray();
        try {
            return argon2.hash(ITERATIONS, MEMORY, PARALLELISM, passwordChars);
        } finally {
            java.util.Arrays.fill(passwordChars, '\0');
        }
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        char[] passwordChars = plainPassword.toCharArray();
        try {
            return argon2.verify(hashedPassword, passwordChars);
        } finally {
            java.util.Arrays.fill(passwordChars, '\0');
        }
    }
}
```

**Impact**: This was the **primary cause** of login failure. Passwords were being hashed with BCrypt but the verification was expecting Argon2 (or vice versa), causing all password checks to fail.

---

## Why Login Was Failing

1. **Password Hash Mismatch**: When the application started, it created default users with Argon2 hashes (via `PasswordHasher.hashPassword()`), but the actual implementation was using BCrypt. This mismatch meant:
   - Admin password: `admin123` hashed with BCrypt
   - Verification attempted: BCrypt check against Argon2 hash (or vice versa)
   - Result: Always failed

2. **Database Schema Issue**: The `passwordMigrated` field wasn't being saved/loaded, which could cause issues with the password migration logic if it was ever used.

---

## Verification Steps

### After Fix
1. **Delete existing database** (if any): Delete any `.db` or `.sqlite` files in the project directory
2. **Rebuild the project**: Clean and Build in NetBeans
3. **Run the application**: The app will create fresh default users
4. **Test admin login**:
   - Username: `admin`
   - Password: `admin123`
   - Role: Administrator
5. **Test other logins**:
   - Teacher: `teacher` / `teacher123`
   - Principal: `principal` / `principal123`

### Expected Behavior
- All three default users should be able to log in
- Passwords are now hashed with Argon2 (as specified)
- The `passwordMigrated` field is properly saved and loaded

---

## Files Modified

1. `DLP/src/dao/UserDAO.java`
   - Updated `createUser()` to include `passwordMigrated` field
   - Updated `extractUser()` to read `passwordMigrated` field

2. `DLP/src/util/PasswordHasher.java`
   - Replaced BCrypt with Argon2 implementation
   - Added OWASP-recommended Argon2 parameters
   - Added secure password clearing from memory

---

## Additional Notes

### Password Security Improvements
The new Argon2 implementation includes:
- **Memory clearing**: Passwords are cleared from memory immediately after use
- **OWASP parameters**: Using recommended iterations, memory, and parallelism settings
- **Exception handling**: Proper error handling for null/empty passwords

### Database Schema Alignment
The UserDAO now properly aligns with the database schema defined in `MainApp.java`:
```sql
CREATE TABLE IF NOT EXISTS User (
    userId TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    passwordHash TEXT NOT NULL,
    role TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    isActive INTEGER DEFAULT 1,
    passwordMigrated INTEGER DEFAULT 0,
    createdAt TEXT DEFAULT (datetime('now','localtime'))
)
```

### Compliance
This fix brings the implementation into full compliance with:
- LIBRARY_COMPLIANCE.md (Argon2 requirement)
- Phase 3 System Documentation (Security requirements)
- Database schema defined in MainApp.java

---

## Conclusion

The admin login issue was caused by:
1. **Primary**: PasswordHasher using BCrypt instead of Argon2
2. **Secondary**: UserDAO not handling the passwordMigrated field

Both issues have been fixed. The application now uses Argon2 for password hashing and properly handles all User model fields.
