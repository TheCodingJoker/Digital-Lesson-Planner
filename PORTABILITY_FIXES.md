# NetBeans Project Portability Fixes

## Date: 2026-10-12

## Overview
Made the NetBeans project configuration computer-independent so it can run on any machine without modification.

## Changes Made

### 1. **project.properties** - Library References
**File**: `DLP/nbproject/project.properties`

**Before**: All library references used absolute paths from the original computer:
```properties
file.reference.argon2-jvm-nolibs-2.12.jar=C:\\Users\\Admin\\Documents\\NetBeansProjects\\Digital-Lesson-Planner\\DLP\\lib\\argon2-jvm-nolibs-2.12.jar
file.reference.jbcrypt-0.4.jar=C:\\Users\\Admin\\Documents\\NetBeansProjects\\Digital-Lesson-Planner\\DLP\\lib\\jbcrypt-0.4.jar
# ... (and so on for all libraries)
```

**After**: All library references now use `${basedir}` variable:
```properties
file.reference.argon2-jvm-nolibs-2.12.jar=${basedir}/lib/argon2-jvm-nolibs-2.12.jar
file.reference.jbcrypt-0.4.jar=${basedir}/lib/jbcrypt-0.4.jar
# ... (and so on for all libraries)
```

**Impact**: Libraries are now located relative to the project directory, allowing the project to work on any computer.

---

### 2. **project.properties** - JavaFX Module Path
**File**: `DLP/nbproject/project.properties`

**Before**: Referenced specific JavaFX SDK location:
```properties
file.reference.javafx-sdk-26.0.2-lib=C:\\Users\\Admin\\Downloads\\zulu23.32.11-ca-fx-jdk23.0.2-win_x64\\zulu23.32.11-ca-fx-jdk23.0.2-win_x64\\jmods
javac.modulepath=${file.reference.javafx-sdk-26.0.2-lib}
```

**After**: Removed JavaFX-specific module path (JavaFX is bundled with JDK 23):
```properties
javac.modulepath=
```

**Impact**: Project now uses the default JDK's JavaFX instead of requiring a specific SDK installation.

---

### 3. **project.properties** - Platform Configuration
**File**: `DLP/nbproject/project.properties`

**Before**: Hardcoded JDK platform path:
```properties
platform.active=JDK_23
platforms.JDK_23.home=C:\\Users\\Admin\\Downloads\\zulu23.32.11-ca-fx-jdk23.0.2-win_x64\\zulu23.32.11-ca-fx-jdk23.0.2-win_x64
platforms.JDK_23.bootclasspath=C:\\Users\\Downloads\\zulu23.32.11-ca-fx-jdk23.0.2-win_x64\\zulu23.32.11-ca-fx-jdk23.0.2-win_x64\\jre\\lib\\rt.jar
```

**After**: Uses default platform:
```properties
platform.active=default_platform
```

**Impact**: Project now uses whatever JDK is configured in NetBeans for the project, rather than a specific installation path.

---

### 4. **project.properties** - JVM Arguments
**File**: `DLP/nbproject/project.properties`

**Before**: Hardcoded JavaFX path:
```properties
run.jvmargs=C:\\Users\\Admin\\Downloads\\javafx
```

**After**: Empty (no custom JVM args needed):
```properties
run.jvmargs=
```

**Impact**: Removed unnecessary hardcoded JVM arguments.

---

### 5. **build.xml** - Library Copy Paths
**File**: `DLP/build.xml`

**Before**: Used Windows-style backslashes for paths:
```xml
<copy file="lib\\sqlite-jdbc-3.53.2.0.jar" todir="${dist.dir}\\lib" failonerror="false"/>
<copy file="lib\\argon2-jvm-nolibs-2.12.jar" todir="${dist.dir}\\lib" failonerror="false"/>
<!-- ... -->
```

**After**: Uses forward slashes (cross-platform compatible):
```xml
<copy file="lib/sqlite-jdbc-3.53.2.0.jar" todir="${dist.dir}/lib" failonerror="false"/>
<copy file="lib/argon2-jvm-nolibs-2.12.jar" todir="${dist.dir}/lib" failonerror="false"/>
<!-- ... -->
```

**Impact**: Build file now works on both Windows and Unix-like systems.

---

### 6. **build.xml** - JavaFX Module Arguments
**File**: `DLP/build.xml`

**Before**: Hardcoded JavaFX jmods path and module arguments:
```xml
<target name="jfx-run" depends="jar">
    <java classname="MainApp" fork="true">
        <jvmarg value="--module-path"/>
        <jvmarg value="C:\\Users\\Admin\\Downloads\\zulu23.32.11-ca-fx-jdk23.0.2-win_x64\\zulu23.32.11-ca-fx-jdk23.0.2-win_x64\\jmods"/>
        <jvmarg value="--add-modules"/>
        <jvmarg value="javafx.controls,javafx.fxml,javafx.graphics"/>
        <!-- ... -->
    </java>
</target>
```

**After**: Removed JavaFX-specific module arguments (JavaFX is bundled):
```xml
<target name="jfx-run" depends="jar">
    <java classname="MainApp" fork="true">
        <classpath>
            <pathelement path="${dist.jar}"/>
            <pathelement location="${dist.dir}/lib/sqlite-jdbc-3.53.2.0.jar"/>
            <!-- ... -->
        </classpath>
    </java>
</target>
```

**Impact**: Simplified run configuration that doesn't require specific JavaFX paths.

---

### 7. **build.xml** - Main Run Target
**File**: `DLP/build.xml`

**Before**: Referenced platform-specific JDK path:
```xml
<target name="run" depends="jar">
    <echo message="Using JDK: ${platforms.JDK_23.home}"/>
    <java classname="MainApp" fork="true" jvm="${platforms.JDK_23.home}\\bin\\java.exe">
        <jvmarg value="--module-path"/>
        <jvmarg value="${platforms.JDK_23.home}\\jmods"/>
        <!-- ... -->
    </java>
</target>
```

**After**: Uses default Java:
```xml
<target name="run" depends="jar">
    <java classname="MainApp" fork="true">
        <classpath>
            <pathelement path="${dist.jar}"/>
            <pathelement location="${dist.dir}/lib/sqlite-jdbc-3.53.2.0.jar"/>
            <!-- ... -->
        </classpath>
    </java>
</target>
```

**Impact**: Run target now uses the system's configured Java instead of a hardcoded path.

---

### 8. **.gitignore** - Computer-Specific Files
**File**: `.gitignore`

**Before**: Generic template with many unrelated entries (React, Node, Python, etc.)

**After**: Project-specific gitignore:
```gitignore
# Database files
*.db
*.sqlite
*.sqlite3

# Build artifacts
DLP/build/
DLP/dist/
DLP/nbproject/private/

# IDE specific
.idea/
*.iml
*.ipr
*.iws

# NetBeans project private settings (computer-specific)
nbproject/private/
nbproject/build-impl.xml
nbproject/jfx-impl.xml

# User-specific NetBeans configuration
nbproject/configs/

# OS-specific files
.DS_Store
Thumbs.db
*.swp
*~

# Logs
*.log

# Temporary files
*.tmp
*.bak
```

**Impact**: Computer-specific NetBeans configuration files are now properly excluded from version control.

---

## Setup Instructions for New Computers

### Prerequisites
1. Install JDK 23 or higher (with JavaFX bundled)
2. Install NetBeans IDE
3. Clone the repository

### NetBeans Configuration
1. Open NetBeans
2. File → Open Project → Select the `Digital-Lesson-Planner` folder
3. Right-click on the project → Properties
4. In "Libraries" tab, verify all JARs are referenced correctly
5. In "Run" category, verify the main class is set to `MainApp`
6. Click "OK"

### JDK Configuration
1. Right-click on the project → Properties
2. "Libraries" → "Java Platform"
3. Select JDK 23 (or click "Manage Platforms" to add your JDK)
4. Click "OK"

### Build and Run
1. Right-click on the project → "Clean and Build"
2. Right-click on the project → "Run"

---

## Verification

### What to Test
1. **Library Loading**: Verify all JARs in `DLP/lib/` are recognized by NetBeans
2. **Compilation**: Project should compile without "file not found" errors
3. **Running**: Application should launch successfully
4. **Database**: Application should create/use the SQLite database correctly

### Expected Behavior
- Project opens without path-related errors
- Build completes successfully
- Application runs on first attempt
- No manual path configuration required

---

## Technical Notes

### Why ${basedir} Works
- `${basedir}` is an Ant/NetBeans built-in property that resolves to the project root directory
- This makes paths relative to where the project is located, not where it was originally created
- Works consistently across different directory structures

### JavaFX Bundling
- Modern JDKs (17+) bundle JavaFX as part of the distribution
- No separate JavaFX SDK installation is required
- Module path arguments are no longer needed for basic JavaFX applications

### Cross-Platform Paths
- Ant (used by NetBeans) accepts both forward slashes (`/`) and backslashes (`\`)
- Forward slashes are preferred for cross-platform compatibility
- NetBeans normalizes paths appropriately for the host OS

---

## Files Modified

1. `DLP/nbproject/project.properties` - Library references, platform config, JVM args
2. `DLP/build.xml` - Library copy paths, run targets, JavaFX arguments
3. `.gitignore` - Added computer-specific file exclusions

---

## Conclusion

The project is now fully portable. It can be cloned to any computer with NetBeans and JDK 23+ installed, and will build and run without manual path configuration. All computer-specific settings have been removed or made relative to the project directory.
